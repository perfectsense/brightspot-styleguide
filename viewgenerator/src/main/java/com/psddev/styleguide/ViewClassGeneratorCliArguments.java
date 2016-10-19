package com.psddev.styleguide;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;

/**
 * Processes the command line arguments for the view class generator command line application.
 */
class ViewClassGeneratorCliArguments {

    // argument prefixes
    private static final String JSON_DIRECTORY_PREFIX =      "--json-dir=";
    private static final String JAVA_PACKAGE_PREFIX =        "--java-package-prefix=";
    private static final String BUILD_DIRECTORY_PREFIX =     "--build-dir=";
    private static final String MAP_TEMPLATES_PREFIX =       "--map-templates=";
    private static final String IGNORE_FILES_PREFIX =        "--ignore-files=";
    private static final String CLASS_NAME_PREFIX_PREFIX =   "--class-name-prefix=";
    private static final String WATCH_PREFIX =               "--watch=";
    private static final String DEFAULT_METHODS_PREFIX =     "--default-methods=";
    private static final String STRICT_TYPES_PREFIX =        "--script-types=";

    // default argument values
    private static final Path DEFAULT_JSON_DIRECTORY = Paths.get(System.getProperty("user.dir"), "styleguide");

    private static final String DEFAULT_JAVA_PACKAGE = "com.psddev.styleguide.view";

    private static final Path DEFAULT_BUILD_DIRECTORY = Paths.get(System.getProperty("user.dir"), "target", "generated-sources", "styleguide");

    private static final Path[] DEFAULT_IGNORED_FILE_NAMES = { Paths.get("_config.json") };

    private Set<Path> jsonDirectories = new LinkedHashSet<>();
    private String javaPackageName;
    private Path buildDirectory;
    private Set<String> mapTemplates = new LinkedHashSet<>();
    private Set<Path> ignoredFileNames = new HashSet<>();
    private String classNamePrefix = null;
    private boolean watch = false;
    private boolean isDefaultMethods = false;
    private boolean isStrictTypes = true;

    public ViewClassGeneratorCliArguments(String[] args) {

        final CliLogger logger = CliLogger.getLogger();

        for (String arg : args) {

            if (arg != null) {
                if (arg.startsWith(JSON_DIRECTORY_PREFIX)) {
                    processStringSetArgument(JSON_DIRECTORY_PREFIX, arg).stream()
                            .map(Paths::get)
                            .forEach(jsonDirectories::add);

                } else if (arg.startsWith(JAVA_PACKAGE_PREFIX)) {
                    javaPackageName = processStringArgument(JAVA_PACKAGE_PREFIX, arg);

                } else if (arg.startsWith(BUILD_DIRECTORY_PREFIX)) {
                    buildDirectory = Paths.get(processStringArgument(BUILD_DIRECTORY_PREFIX, arg));

                } else if (arg.startsWith(MAP_TEMPLATES_PREFIX)) {
                    processStringSetArgument(MAP_TEMPLATES_PREFIX, arg).stream()
                            .map(template -> StringUtils.ensureStart(template, "/"))
                            .forEach(mapTemplates::add);

                } else if (arg.startsWith(IGNORE_FILES_PREFIX)) {
                    processStringSetArgument(IGNORE_FILES_PREFIX, arg).stream()
                            .map(Paths::get)
                            .forEach(ignoredFileNames::add);

                } else if (arg.startsWith(CLASS_NAME_PREFIX_PREFIX)) {
                    classNamePrefix = processStringArgument(CLASS_NAME_PREFIX_PREFIX, arg);

                } else if (arg.startsWith(WATCH_PREFIX)) {
                    watch = ObjectUtils.to(boolean.class, processStringArgument(WATCH_PREFIX, arg));

                } else if (arg.startsWith(DEFAULT_METHODS_PREFIX)) {
                    isDefaultMethods = ObjectUtils.to(boolean.class, processStringArgument(DEFAULT_METHODS_PREFIX, arg));

                } else if (arg.startsWith(STRICT_TYPES_PREFIX)) {
                    isStrictTypes = ObjectUtils.to(boolean.class, processStringArgument(STRICT_TYPES_PREFIX, arg));
                }
            }
        }

        if (jsonDirectories.isEmpty()) {
            logger.yellow("No JSON directories specified with [", JSON_DIRECTORY_PREFIX, "], defaulting to [", DEFAULT_JSON_DIRECTORY, "].");
            jsonDirectories.add(DEFAULT_JSON_DIRECTORY);
        }

        if (javaPackageName == null) {
            logger.yellow("No java package specified with [", JAVA_PACKAGE_PREFIX, "], defaulting to [", DEFAULT_JAVA_PACKAGE, "].");
            javaPackageName = DEFAULT_JAVA_PACKAGE;
        }

        if (buildDirectory == null) {
            logger.yellow("No build directory specified with [", BUILD_DIRECTORY_PREFIX, "], defaulting to [", DEFAULT_BUILD_DIRECTORY, "].");
            buildDirectory = DEFAULT_BUILD_DIRECTORY;
        }

        if (ignoredFileNames.isEmpty()) {
            logger.yellow("No ignored files specified with [", IGNORE_FILES_PREFIX, "], defaulting to ", Arrays.asList(DEFAULT_IGNORED_FILE_NAMES), ".");
            Arrays.stream(DEFAULT_IGNORED_FILE_NAMES).forEach(ignoredFileNames::add);
        }

        validateJsonDirectories();
        validateJavaPackageName();
        validateBuildDirectory();
        validateMapTemplates();
        validateIgnoredFileNames();
        validateClassNamePrefix();
    }

    public Set<Path> getJsonDirectories() {
        return jsonDirectories;
    }

    public String getJavaPackageName() {
        return javaPackageName;
    }

    public Path getBuildDirectory() {
        return buildDirectory;
    }

    public Set<String> getMapTemplates() {
        return mapTemplates;
    }

    public Set<Path> getIgnoredFileNames() {
        return ignoredFileNames;
    }

    public String getClassNamePrefix() {
        return classNamePrefix;
    }

    public boolean isWatch() {
        return watch;
    }

    public boolean isDefaultMethods() {
        return isDefaultMethods;
    }

    public boolean isStrictTypes() {
        return isStrictTypes;
    }

    private String processStringArgument(String argName, String argValue) {
        String value = argValue.substring(argName.length());
        return !value.isEmpty() ? value : null;
    }

    private Set<String> processStringSetArgument(String argPrefix, String argValue) {

        Set<String> valueSet = new LinkedHashSet<>();

        String valueString = argValue.substring(argPrefix.length());
        if (!valueString.isEmpty()) {
            String[] values = valueString.split(",");
            for (String value : values) {
                if (!value.isEmpty()) {
                    valueSet.add(value);
                }
            }
        }

        return valueSet;
    }

    private void validateJsonDirectories() {
        for (Path dir : jsonDirectories) {
            if (!dir.toFile().isDirectory()) {
                throw new IllegalArgumentException("JSON Directory [" + dir + "] must be a directory!");
            }
        }
    }

    private void validateJavaPackageName() {
        if (!javaPackageName.matches("([A-Z_a-z0-9]+\\x2e)*[A-Z_a-z0-9]+")) {
            throw new IllegalArgumentException("Java Package [" + javaPackageName + "] must be a valid java package name!");
        }
    }

    private void validateBuildDirectory() {
        // nothing to do yet...
    }

    private void validateMapTemplates() {
        // nothing to do yet...
    }

    private void validateIgnoredFileNames() {
        // nothing to do yet...
    }

    private void validateClassNamePrefix() {
        // nothing to do yet
    }
}
