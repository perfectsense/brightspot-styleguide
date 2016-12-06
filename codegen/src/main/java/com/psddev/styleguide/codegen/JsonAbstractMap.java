package com.psddev.styleguide.codegen;

import java.util.Map;

/**
 * A specialized JSON map that declares a field as being "abstract". This allows
 * libraries to declare field and use it without knowing exactly what types of
 * objects will be returned from it.
 */
class JsonAbstractMap extends JsonMap {

    public JsonAbstractMap(JsonDataLocation location, Map<JsonKey, JsonValue> values) {
        super(location, values);
    }

    @Override
    public String getTypeLabel() {
        return "View";
    }
}
