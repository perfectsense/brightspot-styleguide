package com.psddev.styleguide.viewgenerator;

import java.io.File;
import java.io.FileFilter;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;

import com.psddev.dari.util.IoUtils;
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

    private static final String DATE_FORMAT = "EEE MMM dd kk:mm:ss zzz yyyy";

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

    private CliLogger logger = CliLogger.getLogger();

    private ViewClassGeneratorContext context;

    ViewClassGenerator(ViewClassGeneratorContext context) {
        this.context = context;
    }

    ViewClassGenerator(ViewClassGeneratorCliArguments arguments) {

        context = new ViewClassGeneratorContext();

        Path jsonDir;

        Set<Path> jsonDirs = arguments.getJsonDirectories();

        if (jsonDirs.isEmpty()) {
            throw new RuntimeException("No JSON directory specified!");

        } else if (jsonDirs.size() > 1) {

            // To support backward compatibility

            try {
                jsonDir = createTempDirectory(jsonDirs);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            context.setRelativePaths(false);
            context.setDefaultTemplateExtension(TemplateType.HANDLEBARS.getExtension());

        } else {
            jsonDir = jsonDirs.iterator().next();
        }

        context.setJsonDirectory(jsonDir);
        context.setJavaSourceDirectory(arguments.getBuildDirectory());

        context.setExcludedPaths(arguments.getIgnoredFileNames());

        context.setGenerateDefaultMethods(arguments.isDefaultMethods());
        context.setGenerateStrictTypes(arguments.isStrictTypes());

        context.setDefaultJavaPackagePrefix(arguments.getJavaPackageName());
    }

    ViewClassGeneratorContext getContext() {
        return context;
    }

    /*
     * To support backward compatibility, this method takes multiple directories
     * and copies the contents of each of them into a temp directory. It also
     * searches for a template directory relative to it based on the standard
     * maven project directory structure, and copies the contents of that
     * directory too if it exists. This puts all templates and JSON files into
     * a single directory so that the view generator can correctly resolve
     * the template paths.
     */
    private Path createTempDirectory(Set<Path> jsonDirs) throws IOException {

        Path tempDir = Files.createTempDirectory("view-class-generator-");
        logger.yellow("Created temp directory [", tempDir, "]");

        tempDir.toFile().deleteOnExit();

        // Create a filter for directories and handlebars files
        FileFilter templateFilter = FileFilterUtils.or(
                DirectoryFileFilter.DIRECTORY,
                FileFilterUtils.and(
                        FileFileFilter.FILE,
                        FileFilterUtils.suffixFileFilter("." + TemplateType.HANDLEBARS.getExtension())));

        final String[] additionalTemplateDirs = {
                "../src/main/webapp",
                "../src/main/resources" };

        for (Path jsonDir : jsonDirs) {

            // Copy the entire JSON directory using the filter
            FileUtils.copyDirectory(jsonDir.toFile(), tempDir.toFile());
            logger.yellow("Copied [", jsonDir, "] to temp directory.");

            for (String templateDirPath : new String[] {
                    "../src/main/webapp",
                    "../src/main/resources" }) {

                // Copy the template directory if it exists
                Path templateDir = jsonDir.resolve(templateDirPath);
                if (templateDir.toFile().exists()) {

                    FileUtils.copyDirectory(templateDir.toFile(), tempDir.toFile(), templateFilter);
                    logger.yellow("Copied templates from [", templateDir.normalize(), "] to temp directory.");
                }
            }
        }

        return tempDir;
    }

    void disableLogColors() {
        logger = CliLogger.getColorlessLogger();
    }

    void printGeneratedClasses() {
        getGeneratedClasses().forEach((file, source) -> {
            String fileName = file.toString();
            String underline = new String(new char[fileName.length()]).replace("\0", "-");
            logger.green().and(fileName, "\n", underline, "\n").reset().and(source, "\n").log();
        });
    }

    List<ViewClassDefinition> getViewClassDefinitions() {

        JsonDirectory directory;
        Set<JsonViewMap> jsonViewMaps;
        Map<ViewKey, Set<JsonViewMap>> jsonViewMapsByViewKey;

        directory = new JsonDirectory(context, context.getJsonDirectory());

        // can throw an exception
        jsonViewMaps = directory.resolveViewMaps();

        // Be able to lookup JSON view maps by view key name
        jsonViewMapsByViewKey = new HashMap<>();

        for (JsonViewMap jsonViewMap : jsonViewMaps) {

            ViewKey viewKey = jsonViewMap.getViewKey();

            // can only be null if it's a delegate field.
            if (viewKey != null) {
                Set<JsonViewMap> set = jsonViewMapsByViewKey.get(viewKey);
                if (set == null) {
                    set = new HashSet<>();
                    jsonViewMapsByViewKey.put(viewKey, set);
                }
                set.add(jsonViewMap);
            }
        }

        List<ViewClassDefinition> classDefinitions = jsonViewMapsByViewKey.entrySet().stream()
                .map(entry -> context.createViewClassDefinition(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        // throws a RuntimeException if there are errors
        checkForErrors(new HashSet<>(classDefinitions));

        return classDefinitions;
    }

    Map<Path, String> getGeneratedClasses() {

        List<ViewClassDefinition> classDefinitions = getViewClassDefinitions();

        // throws a RuntimeException if there are errors
        checkForErrors(new HashSet<>(classDefinitions));

        List<ViewClassSource> sources = classDefinitions.stream()
                .map(classDef -> new ViewClassSourceGenerator(context, classDef).generateSources())
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        Map<Path, String> generated = new LinkedHashMap<>();

        for (ViewClassSource source : sources) {

            String packageName = source.getPackageName();
            Path sourceDirectory = Paths.get(context.getJavaSourceDirectory().toString(), packageName.split("\\x2e"));

            String sourceCode = source.getSourceCode();
            Path classFile = sourceDirectory.resolve(source.getClassName() + ".java");

            generated.put(classFile, sourceCode);
        }

        return generated;
    }

    List<Path> generateClasses() {

        printLogo();

        logger.green().and("Scanning Directory: ").reset().and(context.getJsonDirectory()).log();

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
                            .reset().and(classFile)
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

    void watch() {

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
            WatchDirectory watchDirectory = new WatchDirectory(Collections.singleton(context.getJsonDirectory()));

            watchDirectory.setProcessChangeFunction((path, watchEventKind) -> {

                boolean changed = false;

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

                        changed = true;
                    }
                } else if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {

                    if (watchEventKind == StandardWatchEventKinds.ENTRY_MODIFY) {

                        logger.green().and(">>")
                                .reset().and(" Directory \"")
                                .green().and(path)
                                .reset().and("\" modified.\n")
                                .log();

                        changed = true;
                    }
                }

                if (changed) {
                    generator.get();

                    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
                    RuntimeException error = generationError.getAndSet(null);
                    if (error != null) {
                        String errorMessage = error.getMessage();
                        logger.red("Failed to generate classes: ", error.getMessage());
                        if (errorMessage == null) {
                            error.printStackTrace();
                        }

                    } else if (!viewsChanged.getAndSet(false)) {
                        logger.cyan("No views affected by changes...");
                    }
                    return true;

                } else {
                    return false;
                }
            });

            logger.green().and("Watching Directory: ").reset().and(context.getJsonDirectory()).log();

            watchDirectory.start();

        } catch (IOException e) {
            logger.red("Failed create watch service: ", e.getMessage());
        }
    }

    private boolean sourceFileChanged(Path classFile, String classSource) {

        if (classFile.toFile().exists()) {

            try {
                String existingClassSource = IoUtils.toString(classFile.toFile(), StandardCharsets.UTF_8);

                String existingHash = StringUtils.hex(StringUtils.md5(existingClassSource));
                String newHash = StringUtils.hex(StringUtils.md5(classSource));

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

    /*
     * Looks at each of the files to check if there were any errors detected
     * in them, and logs and throws an error if there are any.
     */
    private void checkForErrors(Set<ViewClassDefinition> classDefs) {

        // Validate each class definition
        Set<ViewClassDefinition> errorClassDefs = classDefs.stream()
                .peek(ViewClassDefinition::validate)
                .filter(ViewClassDefinition::hasAnyErrors)
                .collect(Collectors.toSet());

        // if there are errors, log them and stop
        if (!errorClassDefs.isEmpty()) {
            logErrorDefinitions(errorClassDefs);
        }
    }

    /*
     * Logs all of the errors for each file and throws an exception with the details.
     */
    private void logErrorDefinitions(Set<ViewClassDefinition> classDefs) {

        StringBuilder builder = new StringBuilder();

        builder.append("Error while validating view class definitions: \n");

        for (ViewClassDefinition classDef : classDefs) {

            List<ViewClassDefinitionError> errors = classDef.getErrors();

            builder.append("    ");
            builder.append(classDef.getViewKey().getName());
            builder.append(" has ");
            builder.append(errors.size());
            builder.append(" error");
            if (errors.size() != 1) {
                builder.append("s");
            }
            builder.append(": \n");

            for (ViewClassDefinitionError error : errors) {
                builder.append("        ");

                ViewClassFieldDefinition fieldDef = error.getFieldDefinition();
                if (fieldDef != null) {

                    builder.append(fieldDef.getFieldName());
                    builder.append(": ");
                    builder.append(error.getMessage());
                    builder.append("\n");

                    List<JsonDataLocation> locations = fieldDef.getFieldKeyValues().stream()
                            .map(entry -> entry.getKey().getLocation())
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

                    for (JsonDataLocation location : locations) {
                        builder.append("             ");
                        builder.append(location.getFile().getRelativePath());
                        builder.append(" at (line=");
                        builder.append(location.getLineNumber());
                        builder.append(":, col=");
                        builder.append(location.getColumnNumber());
                        builder.append(", offset=");
                        builder.append(location.getStreamOffset());
                        builder.append(")");
                        builder.append("\n");
                    }

                } else {
                    builder.append(error.getMessage());
                    builder.append("\n");
                }
            }
        }

        throw new RuntimeException(builder.toString());
    }
}
