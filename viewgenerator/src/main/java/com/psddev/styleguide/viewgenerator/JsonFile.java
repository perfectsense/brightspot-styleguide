package com.psddev.styleguide.viewgenerator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.psddev.dari.util.IoUtils;

/**
 * A JSON file within a directory that can be parsed and resolved.
 */
class JsonFile {

    public static final String SPECIAL_KEY_PREFIX = "_";
    public static final String VIEW_KEY = "_view";
    public static final String TEMPLATE_KEY = "_template";
    public static final String DATA_URL_KEY = "_dataUrl";
    public static final String DELEGATE_KEY = "_delegate";
    public static final String NOTES_KEY = "_notes";
    public static final String FIELD_NOTES_KEY_PATTERN = "_%sNotes";

    private JsonDirectory baseDirectory;

    private Path path;
    private String data;

    private boolean isParsed;
    private boolean isNormalized;
    private boolean isResolved;

    private JsonValue parsedValue;
    private JsonValue normalizedValue;
    private JsonViewMap resolvedViewMap;

    private List<JsonFileError> errors = new ArrayList<>();

    /**
     * Creates a new JSON file for the given path.
     *
     * @param path the path to the JSON file.
     */
    public JsonFile(JsonDirectory baseDirectory, Path path) {
        this.baseDirectory = baseDirectory;
        this.path = path;
    }

    /**
     * Gets the base directory of this file.
     *
     * @return the base directory for this file.
     */
    public JsonDirectory getBaseDirectory() {
        return baseDirectory;
    }

    /**
     * Gets the path to this file.
     *
     * @return the file path.
     */
    public Path getPath() {
        return path;
    }

    /**
     * Gets the raw data from this file.
     *
     * @return the raw data.
     */
    public String getData() {
        return data;
    }

    /**
     * Gets the path of this file relative to the base directory.
     *
     * @return the relative path of this file.
     */
    public Path getRelativePath() {
        return baseDirectory.getPath().relativize(path);
    }

    /**
     * Gets the normalized path of {@code other} where "normalized" is defined
     * as the most direct path relative to the base directory. The path is
     * interpreted as relative to this file unless it starts with a slash in
     * which case it is considered to be relative to the base directory.
     *
     * @param other the path to normalize.
     * @return the normalized path.
     */
    public Path getNormalizedPath(Path other) {
        return getNormalizedPath(other, true);
    }

    /**
     * Gets the normalized path of {@code other} where "normalized" is defined
     * as the most direct path relative to the base directory. The path is
     * interpreted as relative to this file if {@code fileRelative} is true,
     * otherwise it is interpreted as relative to the base directory unless
     * it starts with a ".".
     *
     * @param other the path to normalize.
     * @param fileRelative true if paths not starting "/" or "." should be
     *                     considered relative to this file, or false if they
     *                     should be considered relative to this file's base
     *                     directory.
     * @return the normalized path.
     */
    public Path getNormalizedPath(Path other, boolean fileRelative) {
        return getBaseDirectory().getNormalizedPath(this, other, fileRelative);
    }

    /**
     * Parse this file's raw data as JSON and stores it as a {@link JsonValue}.
     *
     * @return the parsed JsonValue representing this file.
     */
    public JsonValue parse() {
        if (!isParsed && errors.isEmpty()) {

            try {
                data = IoUtils.toString(path.toFile(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                errors.add(new JsonFileError(e));
            }

            if (data != null) {
                parsedValue = new JsonFileParser(this).parse();
            }

            isParsed = true;
        }
        return parsedValue;
    }

    /**
     * Finds all String values for keys that represent paths to other files so
     * they are defined as absolute paths relative to the base directory.
     *
     * @return a normalized JsonValue.
     */
    public JsonValue normalize() {
        if (!isNormalized && errors.isEmpty()) {

            if (isParsed() || parse() != null) {
                normalizedValue = new JsonFileNormalizer(this).normalize();
                isNormalized = true;
            }
        }
        return normalizedValue;
    }

    /**
     * Resolves any _dataUrl references to other files and brings them inline
     * into this file and converts special keys (Ex. _template) to structured
     * data types for later processing.
     *
     * @return a structured map-like object representing this file.
     */
    public JsonViewMap resolve() {
        if (!isResolved && errors.isEmpty()) {

            if (isNormalized() || normalize() != null) {
                resolvedViewMap = new JsonFileResolver(this).resolve();
                isResolved = true;
            }
        }
        return resolvedViewMap;
    }

    /**
     * Checks if this file has been parsed as valid JSON without error.
     *
     * @return true if this file has been parsed successfully.
     */
    public boolean isParsed() {
        return isParsed && errors.isEmpty();
    }

    /**
     * Checks if this file has been normalized without error. The file is
     * considered normalized when all of the String values for keys that
     * represent paths to other files have been converted to their normalized
     * form.
     *
     * @return true if this file has been normalized successfully.
     */
    public boolean isNormalized() {
        return isNormalized && errors.isEmpty();
    }

    /**
     * Checks if this file has been resolved (_dataUrl resolution) without error.
     *
     * @return true if this file has been resolved successfully.
     */
    public boolean isResolved() {
        return isResolved && errors.isEmpty();
    }

    /**
     * Gets the list of errors that have been found in this file.
     *
     * @return the list errors.
     */
    public List<JsonFileError> getErrors() {
        return new ArrayList<>(errors);
    }

    /**
     * Returns true if this file contains any errors.
     *
     * @return true if there are any errors.
     */
    public boolean hasAnyErrors() {
        return !errors.isEmpty();
    }

    /**
     * Adds an error to this file.
     *
     * @param error the error to add.
     */
    public void addError(JsonFileError error) {
        errors.add(error);
    }

    @Override
    public String toString() {
        return getRelativePath().toString();
    }
}
