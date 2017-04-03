package com.psddev.styleguide.codegen;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Normalizes all String values for specific keys that represent paths to other
 * files so they are relative to the base directory.
 */
class JsonFileNormalizer {

    private static final Set<String> KEYS_TO_NORMALIZE = Stream.of(
            JsonSpecialKey.TEMPLATE_KEY,
            JsonSpecialKey.DATA_URL_KEY,
            JsonSpecialKey.WRAPPER_KEY)
            .map(JsonSpecialKey::getAliases)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

    private JsonFile file;

    /**
     * Creates a new JSON file normalizer.
     *
     * @param file the file to normalize.
     */
    public JsonFileNormalizer(JsonFile file) {
        this.file = file;
    }

    /**
     * Normalizes the file such that all String values for specific keys that
     * represent paths to other files are made absolutely relative to the file's
     * base directory.
     *
     * @return the normalized JSON value for the file.
     */
    public JsonValue normalize() {
        JsonValue value = copyValue(file.parse());
        normalizeValue(value);
        return value;
    }

    /*
     * Copies the JSON value just by copying the list and map collections,
     * since all other data is immutable.
     */
    private JsonValue copyValue(JsonValue value) {
        if (value instanceof JsonMap) {
            return new JsonMap(value.getLocation(),
                    ((JsonMap) value).getValues().entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

        } else if (value instanceof JsonList) {
            return new JsonList(value.getLocation(),
                    ((JsonList) value).getValues().stream()
                            .map(this::copyValue)
                            .collect(Collectors.toList()));

        } else {
            return value;
        }
    }

    /*
     * Performs the actual normalization on the parsed value.
     */
    private void normalizeValue(JsonValue value) {

        if (value instanceof JsonMap) {

            JsonMap jsonMap = (JsonMap) value;

            // First grab the keys and save them to a new set so we can modify the original map.
            Set<JsonKey> keys = jsonMap.getValues().keySet().stream()
                    .collect(Collectors.toSet());

            // Iterate through the keys in the map.
            keys.forEach(key -> {

                JsonValue keyValue = jsonMap.getValue(key);

                // If the value is a String and matches one of the keys that should be normalized,
                // then try to normalize it.
                if (keyValue instanceof JsonString && KEYS_TO_NORMALIZE.contains(key.getName())) {

                    String rawPath = ((JsonString) keyValue).toRawValue();

                    // Normalize the path.
                    Path normalizedPath = file.getNormalizedPath(Paths.get(rawPath), true);

                    // If the file is outside the scope of the base directory, add an error.
                    if ("..".equals(normalizedPath.getName(0).toString())) {

                        file.addError(new JsonFileError("External path reference. ["
                                + key.getName() + "] key with value ["
                                + rawPath + "] refers to a path outside of the base directory.", keyValue.getLocation()));

                    } else {
                        // Prepend a slash so when it's read later it's treated as an absolute path
                        // from the base directory rather than from the file it is contained in and
                        // replace the existing value in the map
                        jsonMap.getValues().put(key, new JsonString(keyValue.getLocation(), "/" + normalizedPath.toString()));
                    }

                } else {
                    // Recurse.
                    normalizeValue(keyValue);
                }
            });

        } else if (value instanceof JsonList) {
            // Normalize all values of the list.
            ((JsonList) value).getValues().stream().forEach(this::normalizeValue);
        }
    }
}
