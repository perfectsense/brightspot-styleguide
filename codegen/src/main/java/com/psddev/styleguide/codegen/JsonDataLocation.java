package com.psddev.styleguide.codegen;

import javax.json.stream.JsonLocation;

import com.psddev.dari.util.ObjectUtils;

/**
 * Contains location information for a piece of JSON data, such as the
 * originating file, and line number information.
 */
class JsonDataLocation implements Comparable<JsonDataLocation> {

    private JsonFile file;

    private JsonLocation location;

    /**
     * Creates a new data location within a JSON file.
     *
     * @param file the JSON file containing the data.
     * @param location the location of the data within the file.
     */
    public JsonDataLocation(JsonFile file, JsonLocation location) {
        this.file = file;
        this.location = location;
    }

    /**
     * Gets the JSON file that contains a piece of data.
     *
     * @return the JSON file containing the data.
     */
    public JsonFile getFile() {
        return file;
    }

    /**
     * Gets the line number for where a piece of data starts.
     *
     * @return the line number.
     */
    public Long getLineNumber() {
        long value = location != null ? location.getLineNumber() : -1;
        return value >= 0 ? value : null;
    }

    /**
     * Gets the column number for where a piece of data starts.
     *
     * @return the column number.
     */
    public Long getColumnNumber() {
        long value = location != null ? location.getColumnNumber() : -1;
        return value >= 0 ? value : null;
    }

    /**
     * Gets the character offset for a piece of data.
     *
     * @return the character offset within the file.
     */
    public Long getStreamOffset() {
        long value = location != null ? location.getStreamOffset() : -1;
        return value >= 0 ? value : null;
    }

    @Override
    public int compareTo(JsonDataLocation other) {

        if (other == null) {
            return 1;
        }

        int result = ObjectUtils.compare(file.getRelativePath().toString(), other.getFile().getRelativePath().toString(), true);

        if (result == 0) {
            result = ObjectUtils.compare(getLineNumber(), other.getLineNumber(), true);
        }

        if (result == 0) {
            result = ObjectUtils.compare(getColumnNumber(), other.getColumnNumber(), true);
        }

        if (result == 0) {
            result = ObjectUtils.compare(getStreamOffset(), other.getStreamOffset(), true);
        }

        return result;
    }

    @Override
    public String toString() {
        return String.format("%s at (line=%s, col=%s, offset=%s)",
                file.getRelativePath(), getLineNumber(), getColumnNumber(), getStreamOffset());
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        JsonDataLocation that = (JsonDataLocation) o;

        return ObjectUtils.equals(file.getRelativePath().toString(), that.getFile().getRelativePath().toString())
                && ObjectUtils.equals(getLineNumber(), that.getLineNumber())
                && ObjectUtils.equals(getColumnNumber(), that.getColumnNumber())
                && ObjectUtils.equals(getStreamOffset(), that.getStreamOffset());
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hashCode(
                file.getRelativePath().toString(),
                getLineNumber(),
                getColumnNumber(),
                getStreamOffset());
    }
}
