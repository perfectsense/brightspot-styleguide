package com.psddev.styleguide.codegen;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves a JSON file by resolving template paths and verifying that the
 * template exists and is valid as well as resolving data URLs that reference
 * other JSON files and doing validation on the resulting data set.
 */
class JsonFileResolver {

    private JsonFile file;

    /**
     * Creates a new JSON file resolve for the given file.
     *
     * @param file the file to resolve.
     */
    public JsonFileResolver(JsonFile file) {
        this.file = file;
    }

    /**
     * Resolves a JSON file into a JSON view map. It first
     * {@link JsonFile#normalize() normalizes} the file and then resolves all
     * the special keys and validates that all the data is valid. Specifically,
     * it imports any data URL references to other JSON files, as well as
     * validates that any template path references are valid.
     *
     * @return the resolved file as a JSON view map.
     */
    public JsonViewMap resolve() {

        JsonValue value = file.normalize();

        if (value instanceof JsonMap) {
            return resolveViewMap((JsonMap) value);
        } else {
            addError("JSON file must be a map!", value);
            return null;
        }
    }

    /*
     * Helper method for resolving JSON view maps.
     */
    private JsonViewMap resolveViewMap(JsonMap jsonMap) {

        JsonMap resolved = resolveMap(jsonMap, true, new LinkedHashSet<>(), true);
        if (resolved instanceof JsonViewMap) {
            return (JsonViewMap) resolved;

        } else {
            return null;
        }
    }

    /*
     * Recursive helper method for resolving JSON maps.
     */
    private JsonMap resolveMap(JsonMap jsonMap, boolean isViewExpected, Set<Path> visitedDataUrlPaths, boolean isBeginningOfFile) {

        Map<JsonKey, JsonValue> resolved = new LinkedHashMap<>();

        // look for a _dataUrl and return a new unresolved map containing all the values from the data url, or just return the same map
        jsonMap = tryFetchAndMergeDataUrl(jsonMap, visitedDataUrlPaths);

        // Get all of the keys of the map excluding those prefixed with an underscore.
        Set<JsonKey> keys = jsonMap.getValues().keySet().stream()
                .filter(key -> !key.getName().startsWith(JsonFile.SPECIAL_KEY_PREFIX))
                .collect(Collectors.toSet());

        Optional<Boolean> isDelegate = isDelegate(jsonMap);
        if (!isDelegate.isPresent()) {
            return null;
        }

        Optional<Boolean> isAbstract = isAbstract(jsonMap);
        if (!isAbstract.isPresent()) {
            return null;
        }

        ViewKey viewKey = null;
        if (!isDelegate.get() && !isAbstract.get()) {

            viewKey = getViewKey(jsonMap, isViewExpected);

            for (JsonKey key : keys) {
                key = new JsonKey(key.getName(), key.getLocation(), getNotes(key, jsonMap));

                JsonValue value = jsonMap.getValue(key);

                // only resolve the value if it's a view, otherwise it's just a raw map.
                if (viewKey != null) {
                    value = resolveValue(key, value, new LinkedHashSet<>(visitedDataUrlPaths));
                }

                resolved.put(key, value);
            }
        }

        JsonFile wrapperJsonFile = getWrapper(jsonMap, resolved, isBeginningOfFile);

        if (viewKey != null) {
            return new JsonViewMap(jsonMap.getLocation(), resolved, wrapperJsonFile, viewKey, getNotes(jsonMap));

        } else if (isAbstract.get()) {
            return new JsonAbstractMap(jsonMap.getLocation(), resolved);

        } else if (isDelegate.get()) {
            return new JsonDelegateMap(jsonMap.getLocation(), resolved, file);

        } else {
            return new JsonMap(jsonMap.getLocation(), resolved);
        }
    }

    /*
     * Recursive helper method for resolving JSON values. This method prevents
     * JSON lists from being directly nested (immediate child) within another
     * list.
     */
    private JsonValue resolveValue(JsonKey key, JsonValue value, Set<Path> visitedDataUrlPaths) {

        if (value instanceof JsonMap) {
            JsonMap jsonMap = resolveMap((JsonMap) value, false, visitedDataUrlPaths, false);
            // The only time this will be null is if { "_delegate": false } in the map.
            return jsonMap != null ? jsonMap : new JsonNull(null);

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

    /*
     * Gets the effective wrapper JSON file for the file being resolved. The
     * isBeginningOfFile flag signifies whether the jsonMap is the outermost
     * map within the JSON file being resolved.
     */
    private JsonFile getWrapper(JsonMap jsonMap, Map<JsonKey, JsonValue> resolvedValues, boolean isBeginningOfFile) {

        JsonValue wrapper = jsonMap.getValue(JsonFile.WRAPPER_KEY);

        /*
         * If there's no wrapper defined and it's the beginning of the file and
         * this file does not contain a delegate key, then find the nearest
         * wrapper JSON file to it. If it did contain a delegate key, then it
         * means that the map itself is a wrapper, and wrappers shouldn't
         * contain wrappers.
         */
        if (wrapper == null) {
            if (isBeginningOfFile && resolvedValues.values().stream().noneMatch(this::containsDelegateMapAnywhere)) {
                return file.getNearestWrapperJsonFile();
            } else {
                return null;
            }
        }

        if (!isBeginningOfFile) {
            addError("JSON key [" + JsonFile.WRAPPER_KEY + "] must be at the root level of the JSON file.", wrapper);
            return null;
        }

        // If it's null or false, return null.
        if (wrapper instanceof JsonNull
                || wrapper instanceof JsonBoolean && Boolean.FALSE.equals(((JsonBoolean) wrapper).toRawValue())) {
            return null;
        }

        if (!(wrapper instanceof JsonString)) {
            addError("JSON key [" + JsonFile.WRAPPER_KEY + "] must be a String.", wrapper);
            return null;
        }

        String wrapperPath = ((JsonString) wrapper).toRawValue();

        // find the corresponding json file
        JsonFile wrapperFile = file.getBaseDirectory().getNormalizedFile(file, Paths.get(wrapperPath));

        if (wrapperFile == null) {
            addError("Couldn't find " + JsonFile.WRAPPER_KEY + ": " + wrapperPath, wrapper);
            return null;
        }

        return wrapperFile;
    }

    /*
     * Checks if the given resolved value contains a delegate map anywhere
     * by traversing any children the value may have.
     */
    private boolean containsDelegateMapAnywhere(JsonValue resolvedValue) {

        if (resolvedValue instanceof JsonMap) {
            return resolvedValue instanceof JsonDelegateMap
                    || ((JsonMap) resolvedValue).getValues().values().stream().anyMatch(this::containsDelegateMapAnywhere);

        } else if (resolvedValue instanceof JsonList) {
            return ((JsonList) resolvedValue).getValues().stream().anyMatch(this::containsDelegateMapAnywhere);

        } else {
            return false;
        }
    }

    /*
     * Checks to see if this map uses the delegate key, and returns true if it
     * does, or false if it does not. If it uses the delegate key, but sets the
     * value to false, this method returns an empty optional signifying that
     * this particular map is a no-op and has no value at all. There's not
     * really a valid use case for it, but it also doesn't hurt anything so we
     * allow it for now.
     */
    private Optional<Boolean> isDelegate(JsonMap jsonMap) {
        JsonValue delegate = jsonMap.getValue(JsonFile.DELEGATE_KEY);
        if (delegate != null) {

            if (jsonMap.getValues().size() > 1) {
                addError("JSON key [" + JsonFile.DELEGATE_KEY + "] must be the only key in the map.", jsonMap);
            }

            if (delegate instanceof JsonBoolean) {

                if (((JsonBoolean) delegate).toRawValue()) {
                    return Optional.of(true);

                } else {
                    // if _delegate is set to false, then just treat it as if the entire thing is null
                    return Optional.empty();
                }

            } else {
                addError("JSON key [" + JsonFile.DELEGATE_KEY + "] must be a boolean.", delegate);
                return Optional.of(true);
            }

        } else {
            return Optional.of(false);
        }
    }

    /*
     * Checks to see if this map uses the abstract key, and returns true if it
     * does, or false if it does not. If it uses the abstract key, but sets the
     * value to false, this method returns an empty optional signifying that
     * this particular map is a no-op and has no value at all. There's not
     * really a valid use case for it, but it also doesn't hurt anything so we
     * allow it for now.
     */
    private Optional<Boolean> isAbstract(JsonMap jsonMap) {
        JsonValue abstractValue = jsonMap.getValue(JsonFile.ABSTRACT_KEY);
        if (abstractValue != null) {

            if (jsonMap.getValues().size() > 1) {
                addError("JSON key [" + JsonFile.ABSTRACT_KEY + "] must be the only key in the map.", jsonMap);
            }

            if (abstractValue instanceof JsonBoolean) {

                if (((JsonBoolean) abstractValue).toRawValue()) {
                    return Optional.of(true);

                } else {
                    // if _delegate is set to false, then just treat it as if the entire thing is null
                    return Optional.empty();
                }

            } else {
                addError("JSON key [" + JsonFile.ABSTRACT_KEY + "] must be a boolean.", abstractValue);
                return Optional.of(true);
            }

        } else {
            return Optional.of(false);
        }
    }

    /*
     * Verifies that the given JSON map contains a valid view or template key,
     * adding an error to the file being resolved if it doesn't.
     */
    private ViewKey getViewKey(JsonMap jsonMap, boolean isRequired) {

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
        } else if (isRequired) {
            addError("Must specify the view via the " + JsonFile.VIEW_KEY + " key or " + JsonFile.TEMPLATE_KEY + " key!", jsonMap);
        }

        return null;
    }

    /*
     * Resolves a template path finding its template extension (if not present
     * in the path) as well as its associated configuration file, and validates
     * that it is in fact a valid template type.
     */
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
        List<ViewConfiguration> viewConfigs = file.getBaseDirectory().getViewConfigurations(templateDirectory);

        TemplateType templateType;

        int lastDotAt = templateName.lastIndexOf('.');
        // if there is no extension, find it
        if (lastDotAt < 0) {

            String templateExtension = null;
            String missingTemplateExtensionErrorMessage = null;

            templateType = viewConfigs.stream()
                    .map(ViewConfiguration::getTemplateType)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);

            if (templateType != null) {
                templateExtension = templateType.getExtension();

            } else {
                missingTemplateExtensionErrorMessage = "Could not find ["
                        + JsonDirectory.CONFIG_FILE_NAME
                        + "] with ["
                        + ViewConfiguration.TEMPLATE_TYPE_KEY
                        + "] setting to determine template extension.";
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
            addError(e.getClass().getName() + ": " + templatePath, template);
        }

        if (templateType != null) {
            return new TemplateViewKey(
                    file.getBaseDirectory(),
                    viewKey != null ? viewKey.toRawValue() : null,
                    templatePath,
                    templateType,
                    viewConfigs);
        } else {
            return null;
        }
    }

    /*
     * Given a JSON map with a _dataUrl key, this method fetches the JSON file
     * referenced by the key's value and inserts its values into the map. Any
     * keys that were present in the map prior to fetching the _dataUrl are
     * overlaid on top of the _dataUrl's key/values, effectively allowing you
     * to override the values from the _dataUrl. This method will recursively
     * fetch subsequent _dataUrls that are found in the resulting map while
     * also preventing cyclic references that could result in a stack overflow.
     */
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
