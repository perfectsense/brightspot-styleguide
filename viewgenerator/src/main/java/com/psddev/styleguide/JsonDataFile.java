package com.psddev.styleguide;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;

class JsonDataFile {

    private static final Set<String> JSON_MAP_KEYS = new HashSet<>(Arrays.asList(
            "displayOptions",
            "extraAttributes",
            "jsonObject"));

    private String filePath;
    private String fileName;
    private Map<String, Object> jsonData;

    private JsonTemplateObject templateObject;

    private boolean isTemplateObjectResolved;

    public JsonDataFile(String filePath, String fileName, Map<String, Object> jsonData) {
        this.filePath = filePath;
        this.fileName = fileName;
        this.jsonData = jsonData;
    }

    /**
     * @return the base path of this JSON data file and all its associated files.
     * This is NOT necessarily the lowest level directory where the file actually
     * lives. See {@link #getFileDirectoryPath()} for that information.
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * @return the path and file name of this JSON data file relative to {@link #getFilePath()}.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @return the path to the lowest level directory containing this JSON data file.
     */
    public String getFileDirectoryPath() {

        String fileNamePath = getFileName();

        int lastSlashAt = fileNamePath.lastIndexOf(System.getProperty("file.separator"));
        if (lastSlashAt >= 0) {
            fileNamePath = fileNamePath.substring(0, lastSlashAt);
        } else {
            fileNamePath = "";
        }

        return getFilePath() + fileNamePath;
    }

    public Map<String, Object> getJsonData() {
        return jsonData;
    }

    public String getTemplateName() {
        return (String) jsonData.get("_template");
    }

    public JsonTemplateObject getTemplateObject(JsonDataFiles jsonDataFiles) {
        resolveTemplate(jsonDataFiles);
        return templateObject;
    }

    public void resolveTemplate(JsonDataFiles jsonDataFiles) {
        if (!isTemplateObjectResolved) {
            templateObject = resolveJsonTemplateObject(jsonDataFiles, deepJsonCopyMap(jsonData), null);
            isTemplateObjectResolved = true;
        }
    }

    @Override
    public String toString() {
        return getFileName() + " (" + getTemplateName() + "): " + ObjectUtils.toJson(getJsonData());
    }

    private JsonDataFile resolveDataUrl(String dataUrl, JsonDataFiles jsonDataFiles) {

        boolean debug = "_item.json".equals(dataUrl);

        // if it's a relative URL
        if (!dataUrl.startsWith("/")) {
            try {
                if (debug) {
                    System.out.println("getFileDirectoryPath(): " + getFileDirectoryPath());
                    System.out.println("dataUrl: " + dataUrl);
                    System.out.println("getFilePath(): " + getFilePath());
                }
                dataUrl = Paths.get(getFileDirectoryPath(), dataUrl)
                        .toRealPath(/*LinkOption.NOFOLLOW_LINKS*/)
                        .toString()
                        .substring(getFilePath().length());
                if (debug) {
                    System.out.println("relativeUrl: " + dataUrl);
                }

            } catch (IOException | IllegalArgumentException e) {
                e.printStackTrace();
                return null;
            }
        }

        return jsonDataFiles.getByFileName(StringUtils.ensureStart(dataUrl, "/"));
    }

    private Set<String> jsonMapKeys() {
        return JSON_MAP_KEYS;
    }

    private JsonObject resolveJsonObject(String key, JsonDataFiles jsonDataFiles, Object object, String notes) {

        if (object instanceof String) {
            return new JsonString((String) object, notes);

        } else if (object instanceof Number) {
            return new JsonNumber((Number) object, notes);

        } else if (object instanceof Boolean) {
            return new JsonBoolean((Boolean) object, notes);

        } else if (object instanceof List) {
            return resolveJsonList(jsonDataFiles, (List<?>) object, notes);

        } else if (object instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, ?> valueMap = (Map<String, ?>) object;

            if (jsonMapKeys().contains(key)) {
                return new JsonMap(valueMap, notes);

            } else {
                return resolveJsonTemplateObject(jsonDataFiles, valueMap, notes);
            }

        } else {
            String error = "ERROR: Error in [" + getFileName() + "]. Unknown value type [" + object.getClass() + "].";
            System.out.println(error);
            throw new RuntimeException(error);
        }
    }

    private JsonList resolveJsonList(JsonDataFiles jsonDataFiles, List<?> list, String notes) {

        List<JsonObject> jsonObjects = list.stream()
                .map((item) -> resolveJsonObject(null, jsonDataFiles, item, null))
                .filter(object -> object != null)
                .collect(Collectors.toList());

        JsonObjectType previousType = null;

        for (JsonObject jsonObject : jsonObjects) {

            JsonObjectType itemType = jsonObject.getType();
            if (previousType != null) {

                if (previousType != itemType) {
                    String error = "ERROR: Error in [" + getFileName() + "]. List can only contain one kind of JSON object type but found ["
                            + previousType + "] and [" + itemType + "].";

                    throw new RuntimeException(error);
                }

            } else {
                previousType = itemType;
            }
        }

        return new JsonList(jsonObjects, previousType, notes);
    }

    private JsonTemplateObject resolveJsonTemplateObject(JsonDataFiles jsonDataFiles, Map<String, ?> map, String fieldNotes) {
        if (map.get("_delegate") != null) {
            return null;
        }

        String dataUrl = (String) map.get("_dataUrl");
        if (dataUrl != null) {

            JsonDataFile jsonDataFile = resolveDataUrl(dataUrl, jsonDataFiles);
            if (jsonDataFile != null) {
                jsonDataFile.resolveTemplate(jsonDataFiles);
                return jsonDataFile.templateObject;

            } else {
                throw new MissingDataReferenceException(this, dataUrl);
            }

        } else {
            Map<String, JsonObject> fields = new LinkedHashMap<>();

            String template = (String) map.get("_template");
            String templateNotes = (String) map.get("_notes");

            if (template != null) {

                map.keySet().forEach((key) -> {
                    if (!key.startsWith("_")) {

                        Object value = map.get(key);
                        String valueNotes = ObjectUtils.to(String.class, map.get("_" + key + "Notes"));
                        JsonObject resolved = resolveJsonObject(key, jsonDataFiles, value, valueNotes);

                        if (resolved != null) {
                            fields.put(key, resolved);
                        }
                    }
                });

                return new JsonTemplateObject(template, fields, fieldNotes, templateNotes);

            } else {
                throw new MissingTemplateException(this, map);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> deepJsonCopyMap(Map<String, Object> jsonMap) {
        return (Map<String, Object>) ObjectUtils.fromJson(ObjectUtils.toJson(jsonMap));
    }
}
