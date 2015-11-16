package com.psddev.styleguide;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.psddev.dari.util.StringUtils;

public class ViewClassGenerator {

    public static void main(String[] args) {

        System.out.println("---------------------------------");
        System.out.println("----- View Class Generator ------");
        System.out.println("---------------------------------");

        Arguments arguments = new Arguments(args);

        String jsonDirectory = arguments.getJsonDirectory();
        String javaPackageName = arguments.getJavaPackageName();
        String javaSourceDirectory = arguments.getJavaSourceDirectory();
        List<String> mapBasedTemplates = new ArrayList<>(arguments.getMapBasedTemplates());

        System.out.println("args:                " + Arrays.stream(args).collect(Collectors.joining(" ")));
        System.out.println("jsonDirectory:       " + jsonDirectory);
        System.out.println("javaPackageName:     " + javaPackageName);
        System.out.println("javaSourceDirectory: " + javaSourceDirectory);
        System.out.println("mapBasedTemplates:   " + mapBasedTemplates);

        JsonDataFiles dataFiles = new JsonDataFiles(jsonDirectory, mapBasedTemplates);

        List<TemplateDefinition> templateDefinitions = dataFiles.getTemplateDefinitions();

        Map<String, String> javaSources = new LinkedHashMap<>();

        for (TemplateDefinition templateDef : templateDefinitions) {

            String javaClassSource = templateDef.getJavaClassSource(javaPackageName);
            String javaClassName = templateDef.getJavaClassName();

            javaSources.put(javaClassName, javaClassSource);
        }

        for (Map.Entry<String, String> entry : javaSources.entrySet()) {

            String javaClassName = entry.getKey();
            String javaClassSource = entry.getValue();

            saveJavaFile(javaClassName + ".java", javaClassSource, javaSourceDirectory);
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

        private List<String> arguments;

        private String projectDirectory;
        private String javaPackageName;

        public Arguments(String[] args) {

            if (args.length < 2) {
                throw new IllegalArgumentException("Must specify at least 2 arguments: [projectDir] [packageName]");
            }

            projectDirectory = args[0];
            javaPackageName = args[1];

            validateProjectDirectory(projectDirectory);
            validateJavaPackageName(javaPackageName);
        }

        private void validateProjectDirectory(String directory) {
            if (!new File(directory).isDirectory()) {
                throw new IllegalArgumentException("[" + directory + "] must be a directory!");
            }
        }

        private void validateJavaPackageName(String packageName) {
            if (!packageName.matches("([A-Z_a-z0-9]+\\x2e)*[A-Z_a-z0-9]+")) {
                throw new IllegalArgumentException("[" + packageName + "] must be a valid java package name!");
            }
        }

        public String getJsonDirectory() {
            return StringUtils.ensureEnd(projectDirectory, "/") + "styleguide";
        }

        public String getJavaPackageName() {
            return javaPackageName;
        }

        public String getJavaSourceDirectory() {
            return StringUtils.ensureEnd(projectDirectory, "/")
                    + "target/generated-sources/styleguide/"
                    + javaPackageName.replaceAll("\\x2e", "/");
        }

        public Set<String> getMapBasedTemplates() {
            Set<String> mapBasedTemplates = new HashSet<>();
            mapBasedTemplates.add("/assets/templates/base/common/attributes");
            mapBasedTemplates.add("/assets/templates/base/common/json-object");
            mapBasedTemplates.add("/render/common/attributes");
            mapBasedTemplates.add("/render/common/json-object");
            return mapBasedTemplates;
        }
    }
}
