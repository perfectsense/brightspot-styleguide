package com.psddev.styleguide.codegen;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.stream.JsonLocation;
import javax.json.stream.JsonParser;

/**
 * Reads and parses a JSON file validating that it has correct JSON syntax and
 * stores the result in a Map-like structure.
 */
class JsonFileParser {

    private JsonFile file;

    /**
     * Creates a parser for the given file.
     *
     * @param file the file to parse.
     */
    public JsonFileParser(JsonFile file) {
        this.file = file;
    }

    /**
     * Parses the file, storing any errors detected in the file's metadata.
     *
     * @return a Map-like (or List-like) structure that contains the parsed JSON data.
     */
    public JsonValue parse() {
        JsonParser parser = null;
        try {
            parser = Json.createParser(new StringReader(file.getData()));

            JsonParser.Event event = parser.next();
            switch (event) {
                case START_OBJECT: {
                    return processObject(parser);
                }
                case START_ARRAY: {
                    return processArray(parser);
                }
                default: {
                    throw new IllegalStateException("Illegal event start event: " + event.name());
                }
            }
        } catch (RuntimeException e) {
            file.addError(new JsonFileError(e, parser != null ? getCurrentParserLocation(parser) : null));
            return null;
        }
    }

    private JsonMap processObject(JsonParser parser) {

        Map<JsonKey, JsonValue> map = new LinkedHashMap<>();

        JsonDataLocation objectStartLocation = getCurrentParserLocation(parser);

        JsonKey key = null;
        JsonValue value;

        while (parser.hasNext()) {

            JsonParser.Event event = parser.next();

            if (key == null) {
                switch (event) {
                    case KEY_NAME: {
                        key = new JsonKey(parser.getString(), getCurrentParserLocation(parser));
                        break;
                    }
                    case END_OBJECT: {
                        return new JsonMap(objectStartLocation, map);
                    }
                    default: {
                        throw new IllegalStateException("Illegal event during key processing: " + event.name());
                    }
                }
            } else { // process value
                value = processValue(parser, event);
                map.put(key, value);
                key = null;
            }
        }

        throw new IllegalStateException("Never received an " + JsonParser.Event.END_OBJECT + " event!");
    }

    private JsonList processArray(JsonParser parser) {

        List<JsonValue> values = new ArrayList<>();

        JsonLocation arrayStartLocation = parser.getLocation();

        while (parser.hasNext()) {

            JsonParser.Event event = parser.next();

            switch (event) {
                case END_ARRAY: {
                    return new JsonList(new JsonDataLocation(file, arrayStartLocation), values);
                }
                default: {
                    values.add(processValue(parser, event));
                }
            }
        }

        throw new IllegalStateException("Never received an " + JsonParser.Event.END_ARRAY + " event!");
    }

    private JsonValue processValue(JsonParser parser, JsonParser.Event event) {
        switch (event) {
            case START_OBJECT: {
                return processObject(parser);
            }
            case START_ARRAY: {
                return processArray(parser);
            }
            case VALUE_STRING: {
                return new JsonString(getCurrentParserLocation(parser), parser.getString());
            }
            case VALUE_NUMBER: {
                if (parser.isIntegralNumber()) {
                    return new JsonNumber(getCurrentParserLocation(parser), parser.getLong());
                } else {
                    return new JsonNumber(getCurrentParserLocation(parser), parser.getBigDecimal().doubleValue());
                }
            }
            case VALUE_TRUE: {
                return new JsonBoolean(getCurrentParserLocation(parser), true);
            }
            case VALUE_FALSE: {
                return new JsonBoolean(getCurrentParserLocation(parser), false);
            }
            case VALUE_NULL: {
                return new JsonNull(getCurrentParserLocation(parser));
            }
            default: {
                throw new IllegalStateException("Illegal event during value processing: " + event.name());
            }
        }
    }

    private JsonDataLocation getCurrentParserLocation(JsonParser parser) {
        return new JsonDataLocation(file, parser.getLocation());
    }
}
