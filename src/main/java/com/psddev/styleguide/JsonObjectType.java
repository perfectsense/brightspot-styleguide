package com.psddev.styleguide;

import java.util.List;
import java.util.Map;

enum JsonObjectType {

    BOOLEAN,
    NUMBER,
    STRING,
    LIST,
    MAP;

    public static JsonObjectType fromObject(Object object) {

        if (object instanceof Boolean) {
            return BOOLEAN;

        } else if (object instanceof String) {
            return STRING;

        } else if (object instanceof Number) {
            return NUMBER;

        } else if (object instanceof List) {
            return LIST;

        } else if (object instanceof Map) {
            return MAP;

        } else {
            return null;
        }
    }
}
