package com.psddev.styleguide;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.psddev.dari.util.StringUtils;

public class ViewClassGenerator {

    public static void main(String[] args) {

        System.out.println("---------------------------------");
        System.out.println("----- View Class Generator ------");
        System.out.println("---------------------------------");

        Arguments arguments = new Arguments(args);

        Set<String> jsonDirectories = arguments.getJsonDirectories();
        String javaPackageName = arguments.getJavaPackageName();
        String javaSourceDirectory = arguments.getJavaSourceDirectory();
        List<String> mapBasedTemplates = new ArrayList<>(arguments.getMapTemplates());
        Set<String> ignoredFileNames = arguments.getIgnoredFileNames();

        System.out.println("           --json-dir= " + jsonDirectories);
        System.out.println("--java-package-prefix= " + javaPackageName);
        System.out.println("          --build-dir= " + javaSourceDirectory);
        System.out.println("      --map-templates= " + mapBasedTemplates);
        System.out.println("       --ignore-files= " + ignoredFileNames);

        JsonDataFiles dataFiles = new JsonDataFiles(new ArrayList<>(jsonDirectories), ignoredFileNames, mapBasedTemplates);

        List<TemplateDefinition> templateDefinitions = dataFiles.getTemplateDefinitions();

        int commonPrefixLength = getCommonPrefixLength(templateDefinitions
                .stream()
                .map(TemplateDefinition::getName)
                .collect(Collectors.toList()));

        for (TemplateDefinition templateDef : templateDefinitions) {

            String name = templateDef.getName();
            if (commonPrefixLength >= 0) {

                int lastSlashAt = name.lastIndexOf('/');
                if (lastSlashAt > commonPrefixLength) {
                    name = name.substring(commonPrefixLength, lastSlashAt);

                } else {
                    name = null;
                }

            } else {
                name = null;
            }

            String packageName = javaPackageName;
            String sourceDirectory = javaSourceDirectory;

            if (name != null) {
                packageName += "." + StringUtils.removeSurrounding(name.replace('/', '.'), ".");
                sourceDirectory += "/" + StringUtils.removeSurrounding(name, "/");
            }

            String classSource = templateDef.getJavaClassSource(packageName);
            String className = templateDef.getJavaClassName();

            saveJavaFile(className + ".java", classSource, sourceDirectory);
        }
    }

    private static int getCommonPrefixLength(List<String> names) {

        int namesLength = names.size();

        if (namesLength > 1) {

            Collections.sort(names);

            String first = names.get(0);
            String last = names.get(namesLength - 1);
            int commonLength = first.length();
            int commonIndex = 0;

            while (commonIndex < commonLength
                    && first.charAt(commonIndex) == last.charAt(commonIndex)) {

                ++commonIndex;
            }

            return first.substring(0, commonIndex).length();

        } else {
            return -1;
        }
    }

    private static void saveJavaFile(String fileName, String fileContents, String targetDirectory) {

        try {
            File targetFile = new File(targetDirectory, fileName);
            targetFile.getParentFile().mkdirs();
            PrintWriter writer = new PrintWriter(targetFile);
            writer.print(fileContents);
            writer.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static class Arguments {

        // argument prefixes
        private static final String JSON_DIRECTORY_PREFIX = "--json-dir=";
        private static final String JAVA_PACKAGE_PREFIX = "--java-package-prefix=";
        private static final String BUILD_DIRECTORY_PREFIX = "--build-dir=";
        private static final String MAP_TEMPLATES_PREFIX = "--map-templates=";
        private static final String IGNORE_FILES_PREFIX = "--ignore-files=";

        // default argument values
        private static final String DEFAULT_JSON_DIRECTORY = PathUtils.buildPath(System.getProperty("user.dir"), "styleguide");

        private static final String DEFAULT_JAVA_PACKAGE = "com.psddev.view";

        private static final String DEFAULT_BUILD_DIRECTORY = PathUtils.buildPath(System.getProperty("user.dir"), "target", "generated-sources", "styleguide");

        private static final String[] DEFAULT_MAP_TEMPLATES = {
                "/assets/templates/base/common/attributes",
                "/assets/templates/base/common/json-object",
                "/render/common/attributes",
                "/render/common/json-object"
        };

        private static final String[] DEFAULT_IGNORED_FILE_NAMES = {
                "_config.json"
        };

        private Set<String> jsonDirectories = new LinkedHashSet<>();
        private String javaPackageName;
        private String buildDirectory;
        private Set<String> mapTemplates = new LinkedHashSet<>();
        private Set<String> ignoredFileNames = new HashSet<>();

        public Arguments(String[] args) {

            // legacy argument syntax
            if (args.length == 2
                    && !args[0].startsWith("--")
                    && !args[1].startsWith("--")) {

                System.out.println("Using legacy argument syntax. Please use ["
                        + StringUtils.join(Arrays.asList(
                        JSON_DIRECTORY_PREFIX,
                        JAVA_PACKAGE_PREFIX,
                        BUILD_DIRECTORY_PREFIX,
                        MAP_TEMPLATES_PREFIX,
                        IGNORE_FILES_PREFIX), ", ")
                        + "] instead.");

                this.jsonDirectories.add(PathUtils.buildPath(args[0], "styleguide"));
                this.buildDirectory = PathUtils.buildPathWithEndingSlash(args[0], "target", "generated-sources", "styleguide");
                this.javaPackageName = args[1];

            } else {
                for (String arg : args) {

                    if (arg != null) {
                        if (arg.startsWith(JSON_DIRECTORY_PREFIX)) {
                            jsonDirectories.addAll(processStringSetArgument(JSON_DIRECTORY_PREFIX, arg));

                        } else if (arg.startsWith(JAVA_PACKAGE_PREFIX)) {
                            javaPackageName = processStringArgument(JAVA_PACKAGE_PREFIX, arg);

                        } else if (arg.startsWith(BUILD_DIRECTORY_PREFIX)) {
                            buildDirectory = processStringArgument(BUILD_DIRECTORY_PREFIX, arg);

                        } else if (arg.startsWith(MAP_TEMPLATES_PREFIX)) {
                            processStringSetArgument(MAP_TEMPLATES_PREFIX, arg).forEach(
                                    (template) -> mapTemplates.add(StringUtils.ensureStart(template, "/")));
                        } else if (arg.startsWith(IGNORE_FILES_PREFIX)) {
                            processStringSetArgument(IGNORE_FILES_PREFIX, arg).forEach(ignoredFileNames::add);
                        }
                    }
                }
            }

            if (jsonDirectories.isEmpty()) {
                System.out.println("No JSON directories specified with [" + JSON_DIRECTORY_PREFIX
                        + "], defaulting to [" + DEFAULT_JSON_DIRECTORY + "].");
                jsonDirectories.add(DEFAULT_JSON_DIRECTORY);
            }

            if (javaPackageName == null) {
                System.out.println("No java package specified with [" + JAVA_PACKAGE_PREFIX
                        + "], defaulting to [" + DEFAULT_JAVA_PACKAGE + "].");
                javaPackageName = DEFAULT_JAVA_PACKAGE;
            }

            if (buildDirectory == null) {
                System.out.println("No build directory specified with [" + BUILD_DIRECTORY_PREFIX
                        + "], defaulting to [" + DEFAULT_BUILD_DIRECTORY + "].");
                buildDirectory = DEFAULT_BUILD_DIRECTORY;
            }

            if (mapTemplates.isEmpty()) {
                System.out.println("No map templates specified with [" + MAP_TEMPLATES_PREFIX
                        + "], defaulting to " + Arrays.asList(DEFAULT_MAP_TEMPLATES) + ".");
                mapTemplates.addAll(Arrays.asList(DEFAULT_MAP_TEMPLATES));
            }

            if (ignoredFileNames.isEmpty()) {
                System.out.println("No ignored files specified with [" + IGNORE_FILES_PREFIX
                        + "], defaulting to " + Arrays.asList(DEFAULT_IGNORED_FILE_NAMES) + ".");
                ignoredFileNames.addAll(Arrays.asList(DEFAULT_IGNORED_FILE_NAMES));
            }

            validateJsonDirectories();
            validateJavaPackageName();
            validateBuildDirectory();
            validateMapTemplates();
            validateIgnoredFileNames();
        }

        public Set<String> getJsonDirectories() {
            return jsonDirectories;
        }

        public String getJavaPackageName() {
            return javaPackageName;
        }

        public String getBuildDirectory() {
            return buildDirectory;
        }

        public Set<String> getMapTemplates() {
            return mapTemplates;
        }

        public Set<String> getIgnoredFileNames() {
            return ignoredFileNames;
        }

        public String getJavaSourceDirectory() {
            return StringUtils.ensureEnd(getBuildDirectory(), PathUtils.SLASH) + PathUtils.replaceAllWithSlash(getJavaPackageName(), "\\x2e");
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
            for (String dir : jsonDirectories) {
                if (!new File(dir).isDirectory()) {
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
    }
}
