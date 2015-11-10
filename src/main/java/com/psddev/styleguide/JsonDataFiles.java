package com.psddev.styleguide;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    private List<String> mapTemplates;

    public JsonDataFiles(String jsonDataFilesPath, List<String> mapTemplates) {

        List<JsonDataFile> jsonDataFiles = FileUtils.listFiles(new File(jsonDataFilesPath), new String[] { "json" }, true)
                .stream()
                .map((file) -> new JsonDataFile(jsonDataFilesPath, fileToName(file, jsonDataFilesPath), fileToJsonObject(file, jsonDataFilesPath)))
                .collect(Collectors.toList());

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
                System.out.println("ERROR: File [" + jsonDataFile.getFileName() + "] does not have a template!");
            }
        }

        this.mapTemplates = mapTemplates != null ? mapTemplates : Collections.emptyList();
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

            // TODO: Extract this out so we can get at this data too for printing.
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

            jsonTemplateObjectsMap.entrySet().forEach((entry) -> {
                if (!mapTemplates.contains(entry.getKey())) {
                    templateDefinitions.put(entry.getKey(), new TemplateDefinition(entry.getKey(), entry.getValue(), mapTemplates));
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
        return StringUtils.ensureStart(file.getAbsolutePath().replace(baseFilePath, ""), "/");
    }
}
