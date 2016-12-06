package com.psddev.styleguide.codegen;

import java.util.Map;

/**
 * A specialized JSON map that is found only in "Wrapper" JSON files. Wrapper
 * JSON files serve as a common outer data structure for various types of
 * components. The delegate map is the entry point or placeholder for
 * those components, thus eliminating the need for each individual component
 * to explicitly declare its wrapping data structure.
 */
class JsonDelegateMap extends JsonMap {

    /*
     * The "effective" file containing this key. Meaning if the key was brought
     * in via data URL, it would be the file it was brought into and NOT the
     * data URL file.
     */
    private JsonFile declaringJsonFile;

    public JsonDelegateMap(JsonDataLocation location, Map<JsonKey, JsonValue> values, JsonFile declaringJsonFile) {
        super(location, values);
        this.declaringJsonFile = declaringJsonFile;
    }

    /**
     * Gets the effective JSON file that declared this map.
     *
     * @return the declaring JSON file for this map.
     */
    public JsonFile getDeclaringJsonFile() {
        return declaringJsonFile;
    }

    @Override
    public String getTypeLabel() {
        return "View";
    }
}
