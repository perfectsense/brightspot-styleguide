package com.psddev.styleguide.codegen;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
     * Resolves a JSON file into 1 or many JSON view maps. It first
     * {@link JsonFile#normalize() normalizes} the file and then resolves all
     * the special keys and validates that all the data is valid. Specifically,
     * it imports any data URL references to other JSON files, as well as
     * validates that any template path references are valid.
     *
     * @return the resolved file as a JSON view map.
     */
    public List<JsonViewMap> resolve() {

        JsonValue value = file.normalize();

        if (value instanceof JsonMap) {
            return resolveViewMaps((JsonMap) value);

        } else if (value instanceof JsonList) {
            List<JsonValue> values = ((JsonList) value).getValues();

            List<JsonMap> jsonMaps = values.stream()
                    .filter(JsonMap.class::isInstance)
                    .map(JsonMap.class::cast)
                    .collect(Collectors.toList());

            if (jsonMaps.size() < values.size()) {
                addError("JSON Array files must only contain maps!", value);
                return Collections.emptyList();
            }

            return jsonMaps.stream()
                    .map(this::resolveViewMaps)
                    .flatMap(Collection::stream)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    /*
     * Helper method for resolving JSON view maps.
     */
    private List<JsonViewMap> resolveViewMaps(JsonMap jsonMap) {
        return resolveMap(jsonMap, true, new LinkedHashSet<>(), true)
                .stream()
                .filter(JsonViewMap.class::isInstance)
                .map(JsonViewMap.class::cast)
                .collect(Collectors.toList());
    }

    /*
     * Recursive helper method for resolving JSON maps.
     * The method signature returns a List because of the one case where a Map
     * defines a _dataUrl/_include that points to a List, and thus the Map
     * should be converted as such.
     */
    private List<JsonMap> resolveMap(JsonMap jsonMap, boolean isViewExpected, Set<Path> visitedDataUrlPaths, boolean isBeginningOfFile) {

        List<JsonMap> resolveMaps = new ArrayList<>();

        // look for a _dataUrl and return a new unresolved map (or list of maps) containing all the values from the data url, or just return the same map
        List<JsonMap> mergedList = tryFetchAndMergeDataUrl(jsonMap, visitedDataUrlPaths);

        for (JsonMap mergedJsonMap : mergedList) {

            Map<JsonKey, JsonValue> resolved = new LinkedHashMap<>();

            // Get all of the keys of the map excluding those prefixed with an underscore.
            Set<JsonKey> keys = mergedJsonMap.getValues().keySet().stream()
                    .filter(key -> !key.getName().startsWith(JsonSpecialKey.PREFIX))
                    .collect(Collectors.toSet());

            Optional<Boolean> isDelegate = isDelegate(mergedJsonMap);
            if (!isDelegate.isPresent()) {
                continue;
            }

            Optional<Boolean> isAbstract = isAbstract(mergedJsonMap);
            if (!isAbstract.isPresent()) {
                continue;
            }

            ViewKey viewKey = null;
            if (!isDelegate.get() && !isAbstract.get()) {

                if (isViewExpected) {
                    viewKey = requireViewKey(mergedJsonMap);
                }

                for (JsonKey key : keys) {
                    key = new JsonKey(key.getName(), key.getLocation(), getNotes(key, mergedJsonMap));

                    JsonValue value = mergedJsonMap.getValue(key);

                    // only resolve the value if it's a view, otherwise it's just a raw map.
                    if (isViewExpected) {
                        value = resolveValue(key, value, new LinkedHashSet<>(visitedDataUrlPaths));
                    }

                    resolved.put(key, value);
                }
            }

            JsonFile wrapperJsonFile = getWrapper(mergedJsonMap, resolved, isBeginningOfFile);

            if (viewKey != null) {
                resolveMaps.add(new JsonViewMap(mergedJsonMap.getLocation(), resolved, wrapperJsonFile, viewKey, getNotes(mergedJsonMap)));

            } else if (isAbstract.get()) {
                resolveMaps.add(new JsonAbstractMap(mergedJsonMap.getLocation(), resolved));

            } else if (isDelegate.get()) {
                resolveMaps.add(new JsonDelegateMap(mergedJsonMap.getLocation(), resolved, file));

            } else {
                resolveMaps.add(new JsonMap(mergedJsonMap.getLocation(), resolved));
            }
        }

        return resolveMaps;
    }

    /*
     * Recursive helper method for resolving JSON values. This method prevents
     * JSON lists from being directly nested (immediate child) within another
     * list.
     */
    private JsonValue resolveValue(JsonKey key, JsonValue value, Set<Path> visitedDataUrlPaths) {

        if (value instanceof JsonMap) {

            List<JsonMap> resolvedList = resolveMap((JsonMap) value, !isMapBasedKey(key), visitedDataUrlPaths, false);

            // The only times this will be empty is if { "_delegate": false } or { "_abstract": false } is in the map.
            if (resolvedList.isEmpty()) {
                return new JsonNull(null);

            } else if (resolvedList.size() == 1) {
                return resolvedList.get(0);

            } else {
                return new JsonList(value.getLocation(), resolvedList);
            }

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

        JsonValue wrapper = jsonMap.getValue(JsonSpecialKey.WRAPPER_KEY);

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
            addError("JSON key [" + JsonSpecialKey.WRAPPER_KEY.getAlias(jsonMap::containsKey) + "] must be at the root level of the JSON file.", wrapper);
            return null;
        }

        // If it's null or false, return null.
        if (wrapper instanceof JsonNull
                || wrapper instanceof JsonBoolean && Boolean.FALSE.equals(((JsonBoolean) wrapper).toRawValue())) {
            return null;
        }

        if (!(wrapper instanceof JsonString)) {
            addError("JSON key [" + JsonSpecialKey.WRAPPER_KEY.getAlias(jsonMap::containsKey) + "] must be a String.", wrapper);
            return null;
        }

        String wrapperPath = ((JsonString) wrapper).toRawValue();

        // find the corresponding json file
        JsonFile wrapperFile = file.getBaseDirectory().getNormalizedFile(file, Paths.get(wrapperPath));

        if (wrapperFile == null) {
            addError("Couldn't find " + JsonSpecialKey.WRAPPER_KEY.getAlias(jsonMap::containsKey) + ": " + wrapperPath, wrapper);
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
        JsonValue delegate = jsonMap.getValue(JsonSpecialKey.DELEGATE_KEY);
        if (delegate != null) {

            if (jsonMap.getValues().size() > 1) {
                addError("JSON key [" + JsonSpecialKey.DELEGATE_KEY.getAlias(jsonMap::containsKey) + "] must be the only key in the map.", jsonMap);
            }

            if (delegate instanceof JsonBoolean) {

                if (((JsonBoolean) delegate).toRawValue()) {
                    return Optional.of(true);

                } else {
                    // if _delegate is set to false, then just treat it as if the entire thing is null
                    return Optional.empty();
                }

            } else {
                addError("JSON key [" + JsonSpecialKey.DELEGATE_KEY.getAlias(jsonMap::containsKey) + "] must be a boolean.", delegate);
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
        JsonValue abstractValue = jsonMap.getValue(JsonSpecialKey.ABSTRACT_KEY);
        if (abstractValue != null) {

            if (jsonMap.getValues().size() > 1) {
                addError("JSON key [" + JsonSpecialKey.ABSTRACT_KEY.getAlias(jsonMap::containsKey) + "] must be the only key in the map.", jsonMap);
            }

            if (abstractValue instanceof JsonBoolean) {

                if (((JsonBoolean) abstractValue).toRawValue()) {
                    return Optional.of(true);

                } else {
                    // if _delegate is set to false, then just treat it as if the entire thing is null
                    return Optional.empty();
                }

            } else {
                addError("JSON key [" + JsonSpecialKey.ABSTRACT_KEY.getAlias(jsonMap::containsKey) + "] must be a boolean.", abstractValue);
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
    private ViewKey requireViewKey(JsonMap jsonMap) {

        JsonString viewKey = null;
        JsonString template = null;

        if (jsonMap.containsKey(JsonSpecialKey.VIEW_KEY)) {

            // get the view key and ensure it's a String.
            JsonValue viewKeyValue = jsonMap.getValue(JsonSpecialKey.VIEW_KEY);
            if (viewKeyValue instanceof JsonString) {
                viewKey = (JsonString) viewKeyValue;

            } else {
                addError(JsonSpecialKey.VIEW_KEY.getAlias(jsonMap::containsKey) + " key must be a String!", viewKeyValue);
                return null;
            }
        }

        if (jsonMap.containsKey(JsonSpecialKey.TEMPLATE_KEY)) {

            // get the template and ensure it's a String.
            JsonValue templateValue = jsonMap.getValue(JsonSpecialKey.TEMPLATE_KEY);
            if (templateValue instanceof JsonString) {
                template = (JsonString) templateValue;

            } else {
                addError(JsonSpecialKey.TEMPLATE_KEY.getAlias(jsonMap::containsKey) + " must be a String!", templateValue);
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
            addError("Must specify the view via the " + JsonSpecialKey.VIEW_KEY + " key or " + JsonSpecialKey.TEMPLATE_KEY + " key!", jsonMap);
            return null;
        }
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
    private List<JsonMap> tryFetchAndMergeDataUrl(JsonMap jsonMap, Set<Path> visitedDataUrlPaths) {

        // if there's no data url key, just return the original
        if (!jsonMap.containsKey(JsonSpecialKey.DATA_URL_KEY)) {
            return Collections.singletonList(jsonMap);
        }

        // get its value
        JsonValue dataUrlValue = jsonMap.getValue(JsonSpecialKey.DATA_URL_KEY);
        if (!(dataUrlValue instanceof JsonString)) {
            addError(JsonSpecialKey.DATA_URL_KEY.getAlias(jsonMap::containsKey) + " must be a String", dataUrlValue);
            return Collections.singletonList(jsonMap);
        }

        String dataUrl = ((JsonString) dataUrlValue).toRawValue();

        // find the corresponding json file
        JsonFile dataUrlFile = file.getBaseDirectory().getNormalizedFile(file, Paths.get(dataUrl));

        // if no file can be found, error and return.
        if (dataUrlFile == null) {
            addError("Couldn't find " + JsonSpecialKey.DATA_URL_KEY.getAlias(jsonMap::containsKey) + ": " + dataUrl, dataUrlValue);
            return Collections.singletonList(jsonMap);
        }

        // Prevent cyclic references by adding the path to the set. If it's already present error and return.
        if (visitedDataUrlPaths.contains(dataUrlFile.getRelativePath())) {
            addError(JsonSpecialKey.DATA_URL_KEY.getAlias(jsonMap::containsKey) + " contains a cyclic reference: " + visitedDataUrlPaths, dataUrlValue);
            return Collections.singletonList(jsonMap);
        }

        // Parse the file to get the unresolved value.
        JsonValue dataUrlContents = dataUrlFile.normalize();

        List<JsonMap> dataUrlMaps = new ArrayList<>();

        if (dataUrlContents instanceof JsonMap) {
            dataUrlMaps.add((JsonMap) dataUrlContents);

        } else if (dataUrlContents instanceof JsonList) {

            List<JsonValue> values = ((JsonList) dataUrlContents).getValues();

            List<JsonMap> jsonMaps = values.stream()
                    .filter(JsonMap.class::isInstance)
                    .map(JsonMap.class::cast)
                    .collect(Collectors.toList());

            if (jsonMaps.size() < values.size()) {
                addError("JSON Array files must only contain maps!", dataUrlContents);
                return Collections.singletonList(jsonMap);
            }

            dataUrlMaps.addAll(jsonMaps);

        } else {
            addError("The contents of a " + JsonSpecialKey.DATA_URL_KEY.getAlias(jsonMap::containsKey) + " must be a Map or List!", dataUrlContents);
            return Collections.singletonList(jsonMap);
        }

        List<JsonMap> mergedMaps = new ArrayList<>();

        for (JsonMap dataUrlMap : dataUrlMaps) {

            // Check if the dataUrl map contains another _dataUrl key anywhere inside of it.
            // If it does then we need to track that we visited this file in case we encounter
            // it again as we recurse down the tree.
            if (dataUrlMap.containsKeyAnywhere(JsonSpecialKey.DATA_URL_KEY)) {
                visitedDataUrlPaths.add(dataUrlFile.getRelativePath());
            }

            // Finally, merge the values of the data url and the original map together.
            Map<JsonKey, JsonValue> mergedValues = new LinkedHashMap<>();

            // put all the dataUrl values into the merged map
            mergedValues.putAll(dataUrlMap.getValues());

            // overlay all of the original json map's values onto the merged map.
            jsonMap.getValues().entrySet().stream()
                    .filter(entry -> !JsonSpecialKey.DATA_URL_KEY.getAliases().contains(entry.getKey().getName()))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (value1, value2) -> value2,
                            () -> mergedValues));

            JsonMap mergedMap = new JsonMap(jsonMap.getLocation(), mergedValues);

            // recurse in case the data url contained another data url
            mergedMaps.addAll(tryFetchAndMergeDataUrl(mergedMap, visitedDataUrlPaths));
        }

        return mergedMaps;
    }

    /*
     * Gets the overall notes for a view based map.
     */
    private String getNotes(JsonMap jsonMap) {
        return jsonMap.getRawValueAs(String.class, JsonSpecialKey.NOTES_KEY);
    }

    /*
     * Gets the corresponding notes field for a given JSON key.
     */
    private String getNotes(JsonKey jsonKey, JsonMap jsonMap) {
        return JsonSpecialKey.FIELD_NOTES_KEY_PATTERN.getAliases().stream()
                .map(pattern -> String.format(pattern, jsonKey.getName()))
                .map(name -> jsonMap.getRawValueAs(String.class, name))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /*
     * Returns true if the specified JSON key is expected to contain a non-view
     * based map as its value. Meaning there should be no nested views within
     * the resulting map.
     */
    private boolean isMapBasedKey(JsonKey key) {
        return JsonFile.JSON_MAP_KEYS.contains(key.getName());
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
