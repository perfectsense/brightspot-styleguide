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

public class JsonDataFile {

    private static final Set<String> JSON_MAP_KEYS = new HashSet<>(Arrays.asList(
            "displayOptions",
            "extraAttributes",
            "jsonObject"));

    static final String DELEGATE_TEMPLATE_OBJECT_KEY = "_delegate";

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
        return JsonTemplateObject.getTemplateName(jsonData);
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

        // if it's a relative URL
        if (!dataUrl.startsWith("/")) {
            try {
                dataUrl = Paths.get(getFileDirectoryPath(), dataUrl)
                        .toRealPath(/*LinkOption.NOFOLLOW_LINKS*/)
                        .toString()
                        .substring(Paths.get(getFilePath()).toRealPath().toString().length());

            } catch (IOException | IllegalArgumentException e) {
                e.printStackTrace();
                return null;
            }
        }

        String slash = System.getProperty("file.separator");

        return jsonDataFiles.getByFileName(StringUtils.ensureStart(dataUrl.replaceAll("/", "\\".equals(slash) ? "\\\\" : slash), slash));
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

        JsonObjectType effectiveType = null;

        Set<JsonObjectType> allTypes = jsonObjects.stream()
                .map(JsonObject::getType)
                .collect(Collectors.toSet());

        int typesCount = allTypes.size();
        if (typesCount > 1) {

            // We allow Strings and Objects to co-exist and just treat them as if it is Object
            if (typesCount == 2 && allTypes.contains(JsonObjectType.STRING) && allTypes.contains(JsonObjectType.TEMPLATE_OBJECT)) {
                effectiveType = JsonObjectType.TEMPLATE_OBJECT;

            } else {
                String error = "ERROR: Error in [" + getFileName()
                        + "]. List can only contain one kind of JSON object type but found " + allTypes;

                throw new RuntimeException(error);
            }
        } else if (typesCount == 1) {
            effectiveType = allTypes.iterator().next();
        }

        return new JsonList(jsonObjects, effectiveType, notes);
    }

    private JsonTemplateObject resolveJsonTemplateObject(JsonDataFiles jsonDataFiles, Map<String, ?> map, String fieldNotes) {
        // check if the special delegate key is the only key present and create a dummy template object.
        if (map.get(DELEGATE_TEMPLATE_OBJECT_KEY) != null && map.size() == 1) {
            return new JsonTemplateObject(null, null, null, null, null);
        }

        String dataUrl = (String) map.get("_dataUrl");
        if (dataUrl != null) {

            JsonDataFile jsonDataFile = resolveDataUrl(dataUrl, jsonDataFiles);
            if (jsonDataFile != null) {

                // don't allow the use of _dataUrl and either _template or _view since they should never be overridden.
                if (JsonTemplateObject.getTemplateName(map) != null) {
                    throw new DataUrlTemplateOverrideException(this, dataUrl);
                }

                // resolve the data URL.
                jsonDataFile.resolveTemplate(jsonDataFiles);

                // if the map contains extra values then we need to override the data URL data.
                if (map.keySet().stream().filter(key -> !key.startsWith("_")).count() > 0) {

                    // copy the data URL data
                    Map<String, Object> extendedJsonData = deepJsonCopyMap(jsonDataFile.getJsonData());

                    // overlay the current data on top of the dataUrl's data
                    map.entrySet().stream()
                            .filter(entry -> !"_dataUrl".equals(entry.getKey()))
                            .forEach(entry -> extendedJsonData.put(entry.getKey(), entry.getValue()));

                    return resolveJsonTemplateObject(jsonDataFiles, extendedJsonData, null);

                } else {
                    // just return the dataUrl's pre-resolved template object
                    return jsonDataFile.templateObject;
                }

            } else {
                throw new MissingDataReferenceException(this, dataUrl);
            }

        } else {
            Map<String, JsonObject> fields = new LinkedHashMap<>();

            String template = JsonTemplateObject.getTemplateName(map);
            JsonTemplateObject.TemplateFormat format = JsonTemplateObject.getTemplateFormat(map);
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

                return new JsonTemplateObject(StringUtils.ensureStart(template, "/"), fields, fieldNotes, templateNotes, format);

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
