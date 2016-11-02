package com.psddev.styleguide.viewgenerator;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class JsonFileResolver {

    private static final Set<String> JSON_MAP_KEYS = new HashSet<>(Arrays.asList(
            "displayOptions",
            "extraAttributes",
            "jsonObject"));

    private JsonFile file;

    public JsonFileResolver(JsonFile file) {
        this.file = file;
    }

    public JsonViewMap resolve() {

        JsonValue value = file.normalize();

        if (value instanceof JsonMap) {
            return resolveViewMap((JsonMap) value);
        } else {
            addError("JSON file must be a map!", value);
            return null;
        }
    }

    private JsonViewMap resolveViewMap(JsonMap jsonMap) {

        JsonMap resolved = resolveMap(jsonMap, true, new LinkedHashSet<>());
        if (resolved instanceof JsonViewMap) {
            return (JsonViewMap) resolved;

        } else {
            return null;
        }
    }

    private JsonMap resolveMap(JsonMap jsonMap, boolean isViewExpected, Set<Path> visitedDataUrlPaths) {

        Map<JsonKey, JsonValue> resolved = new LinkedHashMap<>();

        // look for a _dataUrl and return a new unresolved map containing all the values from the data url, or just return the same map
        jsonMap = tryFetchAndMergeDataUrl(jsonMap, visitedDataUrlPaths);

        // Get all of the keys of the map excluding those prefixed with an underscore.
        Set<JsonKey> keys = jsonMap.getValues().keySet().stream()
                .filter(key -> !key.getName().startsWith(JsonFile.SPECIAL_KEY_PREFIX))
                .collect(Collectors.toSet());

        ViewKey viewKey = null;
        if (isViewExpected) {
            viewKey = requireViewKey(jsonMap);

            for (JsonKey key : keys) {
                key = new JsonKey(key.getName(), key.getLocation(), getNotes(key, jsonMap));
                resolved.put(key, resolveValue(key, jsonMap.getValue(key), new LinkedHashSet<>(visitedDataUrlPaths)));
            }
        }

        if (viewKey != null) {
            return new JsonViewMap(jsonMap.getLocation(), resolved, viewKey, getNotes(jsonMap));

        } else {
            return new JsonMap(jsonMap.getLocation(), resolved);
        }
    }

    private JsonValue resolveValue(JsonKey key, JsonValue value, Set<Path> visitedDataUrlPaths) {

        if (value instanceof JsonMap) {
            return resolveMap((JsonMap) value, !isMapBasedKey(key), visitedDataUrlPaths);

        } else if (value instanceof JsonList) {

            List<JsonValue> values = ((JsonList) value).getValues()
                    .stream()
                    .map(jsonValue -> resolveValue(key, jsonValue, new LinkedHashSet<>(visitedDataUrlPaths)))
                    .collect(Collectors.toList());

            if (values.stream().anyMatch(jsonValue -> jsonValue instanceof JsonList)) {
                addError("Nested lists are not supported", value);
            }

            return new JsonList(value.getLocation(), values);

        } else {
            return value;
        }
    }

    private ViewKey requireViewKey(JsonMap jsonMap) {

        JsonString viewKey = null;
        JsonString template = null;

        if (jsonMap.containsKey(JsonFile.VIEW_KEY)) {

            // get the view key and ensure it's a String.
            JsonValue viewKeyValue = jsonMap.getValue(JsonFile.VIEW_KEY);
            if (viewKeyValue instanceof JsonString) {
                viewKey = (JsonString) viewKeyValue;

            } else {
                addError(JsonFile.VIEW_KEY + " key must be a String!", viewKeyValue);
                return null;
            }
        }

        if (jsonMap.containsKey(JsonFile.TEMPLATE_KEY)) {

            // get the template and ensure it's a String.
            JsonValue templateValue = jsonMap.getValue(JsonFile.TEMPLATE_KEY);
            if (templateValue instanceof JsonString) {
                template = (JsonString) templateValue;

            } else {
                addError(JsonFile.TEMPLATE_KEY + " must be a String!", templateValue);
                return null;
            }
        }

        if (viewKey != null || template != null) {

            if (template != null) {

                TemplateViewKey templateViewKey = resolveTemplateViewKey(viewKey, template);

                if (templateViewKey != null) {
                    return templateViewKey;
                } else {
                    addError("Could not resolve template path [" + template.toRawValue() + "]", template);
                    return null;
                }

            } else {
                return new ViewKey(
                        file.getBaseDirectory().getContext(),
                        viewKey.toRawValue());
            }
        } else {
            addError("Must specify the view via the " + JsonFile.VIEW_KEY + " key or " + JsonFile.TEMPLATE_KEY + " key!", jsonMap);
            return null;
        }
    }

    private TemplateViewKey resolveTemplateViewKey(JsonString viewKey, JsonString template) {

        // resolve the template path. It may or may not have an extension, i.e. templates/foo/Bar vs templates/foo/Bar.hbs
        Path templatePath = file.getNormalizedPath(Paths.get(template.toRawValue()));

        if ("..".equals(templatePath.getName(0).toString())) {
            addError("External template reference. [" + template.toRawValue()
                    + "] refers to a path outside of the base directory.", template);
            return null;
        }

        Path templateDirectory = templatePath.getParent();
        String templateName = templatePath.getName(templatePath.getNameCount() - 1).toString();

        // find the configuration file
        TemplateViewConfiguration templateConfig = file.getBaseDirectory().getTemplateViewConfiguration(templateDirectory);

        TemplateType templateType = null;

        int lastDotAt = templateName.lastIndexOf('.');
        // if there is no extension, find it
        if (lastDotAt < 0) {

            String templateExtension = null;
            String missingTemplateExtensionErrorMessage = null;

            if (templateConfig != null) {
                templateType = templateConfig.getTemplateType();

                if (templateType != null) {
                    templateExtension = templateType.getExtension();

                } else {
                    missingTemplateExtensionErrorMessage = "Could not find [templateExtension] setting in view configuration to determine template extension.";
                }

            } else {
                missingTemplateExtensionErrorMessage = "Could not find view configuration to determine template extension.";
            }

            if (templateExtension == null) {
                templateExtension = file.getBaseDirectory().getContext().getDefaultTemplateExtension();
                templateType = TemplateType.findByExtension(templateExtension);
            }

            if (templateExtension != null) {

                // append the file extension
                templateName = templateName + "." + templateExtension;

                if (templateDirectory != null) {
                    templatePath = templateDirectory.resolve(templateName);
                } else {
                    // template is at the root of the base directory.
                    templatePath = Paths.get(templateName);
                }

            } else {
                addError(missingTemplateExtensionErrorMessage, template);
            }

        } else {
            // else validate that the current extension is a known type

            String templateExtension = templateName.substring(lastDotAt);
            templateType = TemplateType.findByExtension(templateExtension);

            if (templateType == null) {
                addError("Could not find a valid template type with extension [" + templateExtension + "]", template);
            }
        }

        // check to make sure the file actually exists.
        try {
            file.getBaseDirectory().getPath().resolve(templatePath).toRealPath();
        } catch (IOException e) {
            addError(e, template);
        }

        if (templateType != null) {
            return new TemplateViewKey(
                    file.getBaseDirectory().getContext(),
                    viewKey != null ? viewKey.toRawValue() : null,
                    templatePath,
                    templateType,
                    templateConfig);
        } else {
            return null;
        }
    }

    private JsonMap tryFetchAndMergeDataUrl(JsonMap jsonMap, Set<Path> visitedDataUrlPaths) {

        // if there's no data url key, just return the original
        if (!jsonMap.containsKey(JsonFile.DATA_URL_KEY)) {
            return jsonMap;
        }

        // get its value
        JsonValue dataUrlValue = jsonMap.getValue(JsonFile.DATA_URL_KEY);
        if (!(dataUrlValue instanceof JsonString)) {
            addError(JsonFile.DATA_URL_KEY + " must be a String", dataUrlValue);
            return jsonMap;
        }

        String dataUrl = ((JsonString) dataUrlValue).toRawValue();

        // find the corresponding json file
        JsonFile dataUrlFile = file.getBaseDirectory().getNormalizedFile(file, Paths.get(dataUrl));

        // if no file can be found, error and return.
        if (dataUrlFile == null) {
            addError("Couldn't find " + JsonFile.DATA_URL_KEY + ": " + dataUrl, dataUrlValue);
            return jsonMap;
        }

        // Prevent cyclic references by adding the path to the set. If it's already present error and return.
        if (visitedDataUrlPaths.contains(dataUrlFile.getRelativePath())) {
            addError(JsonFile.DATA_URL_KEY + " contains a cyclic reference: " + visitedDataUrlPaths, dataUrlValue);
            return jsonMap;
        }

        // Parse the file to get the unresolved value.
        JsonValue dataUrlContents = dataUrlFile.normalize();

        // If the contents of the data url file is not a map, error and return.
        if (!(dataUrlContents instanceof JsonMap)) {
            addError("The contents of a " + JsonFile.DATA_URL_KEY + " must be a Map!", dataUrlContents);
            return jsonMap;
        }

        // Check if the dataUrl map contains another _dataUrl key anywhere inside of it.
        // If it does then we need to track that we visited this file in case we encounter
        // it again as we recurse down the tree.
        if (((JsonMap) dataUrlContents).containsKeyAnywhere(JsonFile.DATA_URL_KEY)) {
            visitedDataUrlPaths.add(dataUrlFile.getRelativePath());
        }

        // Finally, merge the values of the data url and the original map together.
        Map<JsonKey, JsonValue> mergedValues = new LinkedHashMap<>();

        // put all the dataUrl values into the merged map
        mergedValues.putAll(((JsonMap) dataUrlContents).getValues());

        // overlay all of the original json map's values onto the merged map.
        jsonMap.getValues().entrySet().stream()
                .filter(entry -> !entry.getKey().getName().equals(JsonFile.DATA_URL_KEY))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (value1, value2) -> value2,
                        () -> mergedValues));

        JsonMap mergedMap = new JsonMap(jsonMap.getLocation(), mergedValues);

        // recurse in case the data url contained another data url
        return tryFetchAndMergeDataUrl(mergedMap, visitedDataUrlPaths);
    }

    /*
     * Gets the overall notes for a view based map.
     */
    private String getNotes(JsonMap jsonMap) {
        return jsonMap.getRawValueAs(String.class, JsonFile.NOTES_KEY);
    }

    /*
     * Gets the corresponding notes field for a given JSON key.
     */
    private String getNotes(JsonKey jsonKey, JsonMap jsonMap) {
        return jsonMap.getRawValueAs(String.class, String.format(JsonFile.FIELD_NOTES_KEY_PATTERN, jsonKey.getName()));
    }

    /*
     * Returns true if the specified JSON key is expected to contain a non-view
     * based map as its value. Meaning there should be no nested views within
     * the resulting map.
     */
    private boolean isMapBasedKey(JsonKey key) {
        return JSON_MAP_KEYS.contains(key.getName());
    }

    /*
     * Adds an error message to the JSON file being resolved.
     */
    private void addError(String message, JsonValue value) {
        file.addError(new JsonFileError(message, value != null ? value.getLocation() : null));
    }

    /*
     * Adds an error to the JSON file being resolved.
     */
    private void addError(Throwable error, JsonValue value) {
        file.addError(new JsonFileError(error, value != null ? value.getLocation() : null));
    }
}
