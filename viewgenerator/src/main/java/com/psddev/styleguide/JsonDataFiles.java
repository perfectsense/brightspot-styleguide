package com.psddev.styleguide;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import com.psddev.dari.util.IoUtils;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;

class JsonDataFiles {

    private Map<String, JsonDataFile> dataFilesByFile;

    private Map<String, List<JsonDataFile>> dataFilesByTemplate;

    private Map<String, TemplateDefinition> templateDefinitions;

    private boolean isDataFilesResolved = false;

    private Set<String> mapTemplates;

    private String javaPackagePrefix;

    private String javaClassNamePrefix;

    public JsonDataFiles(List<Path> jsonDataFilesPaths,
                         Set<Path> ignoredFileNames,
                         Set<String> mapTemplates,
                         String javaPackagePrefix,
                         String javaClassNamePrefix) {

        List<JsonDataFile> jsonDataFiles = new ArrayList<>();

        for (Path jsonDataFilesPath : jsonDataFilesPaths) {
            File jsonDataFilesPathFile = jsonDataFilesPath.toFile();

            if (jsonDataFilesPathFile.exists()) {
                jsonDataFiles.addAll(FileUtils.listFiles(jsonDataFilesPathFile, new String[] { "json" }, true)
                        .stream()
                        .filter((file) -> !ignoredFileNames.contains(Paths.get(file.getName())))
                        .map((file) -> new JsonDataFile(
                                jsonDataFilesPath.toString(),
                                fileToName(file, jsonDataFilesPath.toString()),
                                fileToJsonObject(file, jsonDataFilesPath.toString())))
                        .collect(Collectors.toList()));
            }
        }

        dataFilesByFile = new HashMap<>();
        dataFilesByTemplate = new HashMap<>();

        for (JsonDataFile jsonDataFile : jsonDataFiles) {

            dataFilesByFile.put(jsonDataFile.getFileName(), jsonDataFile);

            String template = jsonDataFile.getTemplateName();
            if (template != null) {

                List<JsonDataFile> dataFilesForTemplate = dataFilesByTemplate.get(template);
                if (dataFilesForTemplate == null) {
                    dataFilesForTemplate = new ArrayList<>();
                    dataFilesByTemplate.put(template, dataFilesForTemplate);
                }

                dataFilesForTemplate.add(jsonDataFile);

            } else {
                throw new MissingTemplateException(jsonDataFile);
            }
        }

        this.mapTemplates = mapTemplates != null ? mapTemplates : Collections.emptySet();

        if (StringUtils.isBlank(javaPackagePrefix)) {
            this.javaPackagePrefix = "";
        } else {
            this.javaPackagePrefix = StringUtils.ensureEnd(javaPackagePrefix, ".");
        }
        this.javaClassNamePrefix = javaClassNamePrefix;
    }

    public JsonDataFile getByFileName(String fileName) {
        return dataFilesByFile.get(fileName);
    }

    public List<JsonDataFile> getByTemplate(String templateName) {
        List<JsonDataFile> byTemplate = dataFilesByTemplate.get(templateName);
        return byTemplate != null ? new ArrayList<>(byTemplate) : null;
    }

    public TemplateDefinition getTemplateDefintion(String templateName) {
        resolveAllDataFileTemplates();
        return templateDefinitions.get(templateName);
    }

    public List<TemplateDefinition> getTemplateDefinitions() {
        resolveAllDataFileTemplates();
        return new ArrayList<>(templateDefinitions.values());
    }

    private void resolveAllDataFileTemplates() {

        if (!isDataFilesResolved) {

            for (JsonDataFile jsonDataFile : dataFilesByFile.values()) {
                jsonDataFile.resolveTemplate(this);
            }

            templateDefinitions = new HashMap<>();

            Set<JsonTemplateObject> set = Collections.newSetFromMap(new IdentityHashMap<>());
            for (JsonDataFile jsonDataFile : dataFilesByFile.values()) {
                JsonTemplateObject jsonTemplateObject = jsonDataFile.getTemplateObject(this);
                set.addAll(jsonTemplateObject.getIdentityTemplateObjects());
            }

            Map<String, List<JsonTemplateObject>> jsonTemplateObjectsMap = new HashMap<>();

            for (JsonTemplateObject jsonTemplateObject : set) {

                String templateName = jsonTemplateObject.getTemplateName();

                List<JsonTemplateObject> jsonTemplateObjects = jsonTemplateObjectsMap.get(templateName);
                if (jsonTemplateObjects == null) {
                    jsonTemplateObjects = new ArrayList<>();
                    jsonTemplateObjectsMap.put(templateName, jsonTemplateObjects);
                }

                jsonTemplateObjects.add(jsonTemplateObject);
            }

            // find the common path prefix ensuring that the paths do not start with slash
            int commonPrefixIndex = PathUtils.getCommonPrefixIndex(
                    jsonTemplateObjectsMap.keySet().stream()
                            .map(name -> StringUtils.removeStart(name, "/"))
                            .collect(Collectors.toList()), '/');

            jsonTemplateObjectsMap.entrySet().forEach((entry) -> {

                //System.out.println(entry.getKey() + " -> " + entry.getValue());

                // again for consistency, remove the leading slash
                String name = StringUtils.removeStart(entry.getKey(), "/");

                String commonName = name.substring(commonPrefixIndex);
                String commonNamePath;
                int lastSlashAt = commonName.lastIndexOf('/');
                if (lastSlashAt > 0) {
                    commonNamePath = commonName.substring(0, lastSlashAt);
                } else {
                    commonNamePath = "";
                }

                String javaPackageName = javaPackagePrefix + commonNamePath.replaceAll("/", ".");

                if (!mapTemplates.contains(name)) {
                    templateDefinitions.put(name, new TemplateDefinition(
                            // add the leading slash back when passing to the TemplateDefinition
                            StringUtils.ensureStart(name, "/"),
                            StringUtils.removeSurrounding(javaPackageName, "."),
                            entry.getValue(),
                            mapTemplates,
                            javaClassNamePrefix));
                }
            });

            isDataFilesResolved = true;
        }
    }

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();

        for (Map.Entry<String, List<JsonDataFile>> entry : dataFilesByTemplate.entrySet()) {

            String template = entry.getKey();
            List<JsonDataFile> dataFiles = entry.getValue();

            builder.append(template).append("\n");
            builder.append("-----------------------------------------\n");
            for (JsonDataFile dataFile : dataFiles) {
                builder.append(dataFile).append("\n");
            }
            builder.append("\n\n");
        }

        return builder.toString();
    }

    private static Map<String, Object> fileToJsonObject(File file, String baseFilePath) {
        try {
            String jsonString = IoUtils.toString(file, StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object> jsonObject = (Map<String, Object>) ObjectUtils.fromJson(jsonString);
            return jsonObject;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (RuntimeException e) {
            throw new RuntimeException("Error parsing JSON file: [" + fileToName(file, baseFilePath) + "] is not a Map!");
        }
    }

    private static String fileToName(File file, String baseFilePath) {
        return StringUtils.ensureStart(file.getAbsolutePath().replace(baseFilePath, ""), PathUtils.SLASH);
    }
}
