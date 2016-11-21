package com.psddev.styleguide.viewgenerator;

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
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

import com.psddev.dari.util.IoUtils;
import com.psddev.dari.util.StringUtils;

/**
 * The main view class source code generator. This is the entry point into the
 * view generation APIs. This class can generate the source code in memory
 * and/or write the files the disk. It can also be run in a "watch" mode, where
 * it can detect changes to JSON files and re-generate the view class source
 * code on the fly.
 */
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

        context.setJsonDirectories(arguments.getJsonDirectories());
        context.setJavaSourceDirectory(arguments.getBuildDirectory());

        context.setExcludedPaths(arguments.getIgnoredFileNames());

        context.setGenerateDefaultMethods(arguments.isDefaultMethods());
        context.setGenerateStrictTypes(arguments.isStrictTypes());

        context.setDefaultJavaPackagePrefix(arguments.getJavaPackageName());
    }

    ViewClassGeneratorContext getContext() {
        return context;
    }

    void disableLogColors() {
        logger = CliLogger.getColorlessLogger();
    }

    void printGeneratedClasses() {
        getGeneratedClasses().forEach((file, source) -> {
            String fileName = file.toString();
            String underline = new String(new char[fileName.length()]).replace("\0", "-");
            logger.green().append(fileName, "\n", underline, "\n").reset().append(source, "\n").log();
        });
    }

    Map<Path, String> getGeneratedClasses() {

        JsonDirectory directory = new JsonDirectory(context);

        Set<JsonViewMap> jsonViewMaps = directory.resolveViewMaps();

        List<ViewClassDefinition> classDefinitions = ViewClassDefinition.createDefinitions(context, jsonViewMaps);

        // Throws an exception if there are any errors
        logErrorDefinitions(classDefinitions);

        List<ViewClassSource> sources = classDefinitions.stream()
                .map(classDef -> new ViewClassSourceGenerator(context, classDef).generateSources())
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        Map<Path, String> generated = new TreeMap<>();

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

                    logger.green().append("Wrote file: ")
                            .reset().append(classFile)
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
            WatchDirectory watchDirectory = new WatchDirectory(context.getJsonDirectories());

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
                        logger.green().append(">>")
                                .reset().append(" File \"")
                                .green().append(path)
                                .reset().append("\" ", eventType, ".\n")
                                .log();

                        changed = true;
                    }
                } else if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {

                    if (watchEventKind == StandardWatchEventKinds.ENTRY_MODIFY) {

                        logger.green().append(">>")
                                .reset().append(" Directory \"")
                                .green().append(path)
                                .reset().append("\" modified.\n")
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

            context.getJsonDirectories().forEach(dir -> logger.green().append("Watching Directory: ").reset().append(dir).log());

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
            logger.blue().append(row.substring(0, BRIGHTSPOT_ASCII_O_START_INDEX))
                    .red().append(row.substring(BRIGHTSPOT_ASCII_O_START_INDEX, BRIGHTSPOT_ASCII_O_END_INDEX))
                    .blue().append(row.substring(BRIGHTSPOT_ASCII_O_END_INDEX))
                    .log();
        }
        logger.blue(VIEW_GENERATOR_ASCII);
    }

    /*
     * Logs all of the errors for each file and throws an exception with the details.
     */
    private void logErrorDefinitions(List<ViewClassDefinition> allClassDefs) {

        // Find all the class definitions that contain errors.
        List<ViewClassDefinition> errorClassDefs = allClassDefs.stream()
                .filter(ViewClassDefinition::hasAnyErrors)
                .collect(Collectors.toList());

        if (errorClassDefs.isEmpty()) {
            return;
        }

        long totalErrorCount = errorClassDefs.stream().map(ViewClassDefinition::getErrors).flatMap(Collection::stream).count();

        CliLoggerMessageBuilder builder = new CliLoggerMessageBuilder(logger, CliColor.RED);

        builder.append("Found ");
        builder.cyan().append(totalErrorCount).red();
        builder.append(" error");
        if (totalErrorCount != 1) {
            builder.append("s");
        }
        builder.append(" while validating view class definitions: \n");

        for (ViewClassDefinition classDef : errorClassDefs) {

            List<ViewClassDefinitionError> errors = classDef.getErrors();

            builder.append("\n    ");
            builder.cyan().append(classDef.getViewKey().getName()).red();
            builder.append(" has ");
            builder.cyan().append(errors.size()).red();
            builder.append(" error");
            if (errors.size() != 1) {
                builder.append("s");
            }
            builder.append(": \n");

            for (ViewClassDefinitionError error : errors) {
                builder.append("        ");

                ViewClassFieldDefinition fieldDef = error.getFieldDefinition();
                if (fieldDef != null) {

                    builder.cyan().append(fieldDef.getFieldName()).red();
                    builder.append(" ");
                    builder.append(error.getMessage());
                    builder.append("\n");

                    // tree map to sort and de-dupe based on json data location.
                    Map<JsonDataLocation, Map.Entry<JsonKey, JsonValue>> locations = new TreeMap<>();
                    for (Map.Entry<JsonKey, JsonValue> entry : fieldDef.getFieldKeyValues()) {
                        locations.put(entry.getKey().getLocation(), entry);
                    }

                    for (Map.Entry<JsonKey, JsonValue> entry : locations.values()) {

                        JsonDataLocation location = entry.getKey().getLocation();
                        String typeLabel = entry.getValue().getTypeLabel();

                        builder.append("             ");
                        builder.cyan().append(typeLabel).red();
                        builder.append(" ");
                        builder.append(location.getFile().getRelativePath());
                        builder.append(" at ");
                        builder.append("(line=");
                        builder.red().append(location.getLineNumber()).red();
                        builder.append(", col=");
                        builder.red().append(location.getColumnNumber()).red();
                        builder.append(", offset=");
                        builder.red().append(location.getStreamOffset()).red();
                        builder.append(")");
                        builder.red();
                        builder.append("\n");
                    }

                } else {
                    builder.append(error.getMessage());
                    builder.append("\n");
                }
            }
        }

        builder.log();

        throw new ViewClassGeneratorException("\nFailed to generate view classes due to "
                + totalErrorCount
                + " previous error" + (totalErrorCount == 1 ? "" : "s") + ".");
    }
}
