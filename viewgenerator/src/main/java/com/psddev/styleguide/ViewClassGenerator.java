package com.psddev.styleguide;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

import com.psddev.dari.util.IoUtils;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;

public class ViewClassGenerator {

    private static final String BRIGHTSPOT_ASCII =
              " _____ _____ _____ _____ _____ _____ _____ _____ _____ _____ \n"
            + "| __  | __  |     |   __|  |  |_   _|   __|  _  |     |_   _|\n"
            + "| __ -|    -|-   -|  |  |     | | | |__   |   __|  |  | | |  \n"
            + "|_____|__|__|_____|_____|__|__| |_| |_____|__|  |_____| |_|  \n";
    //                                                      48 |-----| 55

    private static final int BRIGHTSPOT_ASCII_O_START_INDEX = 48;
    private static final int BRIGHTSPOT_ASCII_O_END_INDEX = 55;

    private static final String VIEW_GENERATOR_ASCII =
               " _____ _              _____                     _            \n"
            +  "|  |  |_|___ _ _ _   |   __|___ ___ ___ ___ ___| |_ ___ ___  \n"
            +  "|  |  | | -_| | | |  |  |  | -_|   | -_|  _| .'|  _| . |  _| \n"
            + " \\___/|_|___|_____|  |_____|___|_|_|___|_| |__,|_| |___|_|   \n";

    static final String DATE_FORMAT = "EEE MMM dd kk:mm:ss zzz yyyy";
    private static final String DATE_FORMAT_PATTERN = "\\w{3} \\w{3} \\d{2} \\d{2}:\\d{2}:\\d{2} \\w{3} \\d{4}";

    /**
     * Main method that can be invoked from the command line
     *
     * @param args command line arguments.
     */
    public static void main(String[] args) {

        ViewClassGeneratorCliArguments arguments = new ViewClassGeneratorCliArguments(args);

        ViewClassGenerator viewClassGenerator = new ViewClassGenerator(arguments);

        if (arguments.isWatch()) {
            viewClassGenerator.watch();

        } else {
            viewClassGenerator.generateClasses();
        }
    }

    private Set<Path> jsonDirectories;
    private String javaPackageName;
    private Path javaSourceDirectory;

    private Set<String> mapBasedTemplates;
    private Set<Path> ignoredFileNames;
    private String classNamePrefix;
    private boolean removeDeprecations = true;

    private CliLogger logger = CliLogger.getLogger();

    public ViewClassGenerator(Set<Path> jsonDirectories, Path javaSourceDirectory, String javaPackageName) {
        this.jsonDirectories = jsonDirectories;
        this.javaSourceDirectory = javaSourceDirectory;
        this.javaPackageName = javaPackageName;

        // TODO: Need to expose these somehow.
        this.mapBasedTemplates = Collections.emptySet();
        this.ignoredFileNames = Collections.emptySet();
        this.classNamePrefix = null;
        this.removeDeprecations = true;
    }

    private ViewClassGenerator(ViewClassGeneratorCliArguments arguments) {
        this.jsonDirectories = arguments.getJsonDirectories();
        this.javaPackageName = arguments.getJavaPackageName();
        this.javaSourceDirectory = arguments.getBuildDirectory();
        this.mapBasedTemplates = arguments.getMapTemplates();
        this.ignoredFileNames = arguments.getIgnoredFileNames();
        this.classNamePrefix = arguments.getClassNamePrefix();
        this.removeDeprecations = arguments.isRemoveDeprecations();
    }

    public void disableLogColors() {
        logger = CliLogger.getColorlessLogger();
    }

    public Set<String> getMapBasedTemplates() {
        return mapBasedTemplates;
    }

    public void setMapBasedTemplates(Set<String> mapBasedTemplates) {
        this.mapBasedTemplates = mapBasedTemplates;
    }

    public Set<Path> getIgnoredFileNames() {
        return ignoredFileNames;
    }

    public void setIgnoredFileNames(Set<Path> ignoredFileNames) {
        this.ignoredFileNames = ignoredFileNames;
    }

    public String getClassNamePrefix() {
        return classNamePrefix;
    }

    public void setClassNamePrefix(String classNamePrefix) {
        this.classNamePrefix = classNamePrefix;
    }

    public boolean isRemoveDeprecations() {
        return removeDeprecations;
    }

    public void setRemoveDeprecations(boolean removeDeprecations) {
        this.removeDeprecations = removeDeprecations;
    }

    public void printGeneratedClasses() {
        getGeneratedClasses().forEach((file, source) -> {
            String fileName = file.toString();
            String underline = new String(new char[fileName.length()]).replace("\0", "-");
            logger.green().and(fileName, "\n", underline, "\n").reset().and(source, "\n").log();
        });
    }

    JsonDataFiles getJsonDataFiles() {
        return new JsonDataFiles(new ArrayList<>(jsonDirectories), ignoredFileNames, mapBasedTemplates, javaPackageName, classNamePrefix);
    }

    TemplateDefinitions getTemplateDefinitions() {
        return getJsonDataFiles().getTemplateDefinitions();
    }

    public Map<Path, String> getGeneratedClasses() {

        Map<Path, String> generated = new LinkedHashMap<>();

        TemplateDefinitions templateDefinitions = getTemplateDefinitions();

        for (TemplateDefinition templateDef : templateDefinitions.get().stream()
                .sorted((td1, td2) -> ObjectUtils.compare(td1.getName(), td2.getName(), true))
                .collect(Collectors.toList())) {

            String packageName = templateDef.getPackageName();
            Path sourceDirectory = Paths.get(javaSourceDirectory.toString(), packageName.split("\\x2e"));

            String classSource = templateDef.getJavaClassSource(removeDeprecations);
            Path classFile = sourceDirectory.resolve(templateDef.getClassName() + ".java");

            generated.put(classFile, classSource);
        }

        return generated;
    }

    public List<Path> generateClasses() {

        printLogo();

        for (Path jsonDirectory : jsonDirectories) {
            logger.green().and("Scanning Directory: ").reset().and(jsonDirectory).log();
        }

        return generateClasses(true);
    }

    private List<Path> generateClasses(boolean overwriteAll) {

        long start = System.currentTimeMillis();

        // list of files generated AND written
        List<Path> generatedFiles = new ArrayList<>();

        getGeneratedClasses().forEach((classFile, classSource) -> {

            boolean overwrite = overwriteAll || sourceFileChanged(classFile, classSource);

            if (overwrite) {

                try {
                    saveJavaFile(classFile, classSource);

                    generatedFiles.add(classFile);

                    logger.green().and("Wrote file: ")
                            .reset().and(PathUtils.getRelativeCommonPath(classFile, jsonDirectories))
                            .log();

                } catch (IOException e) {
                    logger.red("Failed to write file: ", classFile);
                    logger.red("Cause: ", e.getMessage());
                }
            }
        });

        long duration = System.currentTimeMillis() - start;

        if (!generatedFiles.isEmpty()) {
            logger.cyan("Generated ", generatedFiles.size(), " files in ", duration, "ms at ", new SimpleDateFormat(DATE_FORMAT).format(new Date()));
        }

        return generatedFiles;
    }

    public void watch() {

        printLogo();

        final AtomicBoolean viewsChanged = new AtomicBoolean(false);
        final AtomicReference<RuntimeException> generationError = new AtomicReference<>(null);

        final Supplier<Void> generator = Suppliers.memoizeWithExpiration(() -> {

            try {
                List<Path> generated = generateClasses(false);
                viewsChanged.set(!generated.isEmpty());

            } catch (RuntimeException e) {
                generationError.set(e);
            }

            return null;

        }, 1, TimeUnit.SECONDS);

        try {
            WatchDirectory watchDirectory = new WatchDirectory(jsonDirectories);

            watchDirectory.setProcessChangeFunction((path, watchEventKind) -> {

                if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {

                    String eventType = null;

                    if (watchEventKind == StandardWatchEventKinds.ENTRY_CREATE) {
                        eventType = "created";

                    } else if (watchEventKind == StandardWatchEventKinds.ENTRY_DELETE) {
                        eventType = "deleted";

                    } else if (watchEventKind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        eventType = "modified";
                    }

                    if (eventType != null) {
                        logger.green().and(">>")
                                .reset().and(" File \"")
                                .green().and(path)
                                .reset().and("\" ", eventType, ".\n")
                                .log();

                        generator.get();

                        @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
                        RuntimeException error = generationError.getAndSet(null);
                        if (error != null) {
                            logger.red("Failed to generate classes: ", error.getMessage());

                        } else if (!viewsChanged.getAndSet(false)) {
                            logger.cyan("No views affected by changes...");
                        }

                        return true;
                    }
                }

                return false;
            });

            for (Path jsonDirectory : jsonDirectories) {
                logger.green().and("Watching Directory: ").reset().and(jsonDirectory).log();
            }

            watchDirectory.start();

        } catch (IOException e) {
            logger.red("Failed create watch service: ", e.getMessage());
        }
    }

    private boolean sourceFileChanged(Path classFile, String classSource) {

        if (classFile.toFile().exists()) {

            try {
                String existingClassSource = IoUtils.toString(classFile.toFile(), StandardCharsets.UTF_8);

                String existingHash = StringUtils.hex(StringUtils.md5(existingClassSource.replaceFirst(DATE_FORMAT_PATTERN, "")));
                String newHash = StringUtils.hex(StringUtils.md5(classSource.replaceFirst(DATE_FORMAT_PATTERN, "")));

                return !newHash.equals(existingHash);

            } catch (IOException e) {
                // if we can't read the existing file
                logger.yellow("Could not read file [", classFile, "]. Cause: ", e.getMessage());
                return true;
            }

        } else {
            return true;
        }
    }

    private static void saveJavaFile(Path javaFile, String javaSource) throws IOException {
        File targetFile = javaFile.toFile();
        targetFile.getParentFile().mkdirs();
        PrintWriter writer = new PrintWriter(targetFile);
        writer.print(javaSource);
        writer.close();
    }

    private void printLogo() {

        String[] brightspotAsciiRows = BRIGHTSPOT_ASCII.split("\\n");
        for (String row : brightspotAsciiRows) {
            logger.blue().and(row.substring(0, BRIGHTSPOT_ASCII_O_START_INDEX))
                    .red().and(row.substring(BRIGHTSPOT_ASCII_O_START_INDEX, BRIGHTSPOT_ASCII_O_END_INDEX))
                    .blue().and(row.substring(BRIGHTSPOT_ASCII_O_END_INDEX))
                    .log();
        }
        logger.blue(VIEW_GENERATOR_ASCII);
    }
}
