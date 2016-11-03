package com.psddev.styleguide.viewgenerator;

/**
 * A String key of a JSON map.
 */
class JsonKey {

    private String name;

    private String notes;

    private JsonDataLocation location;

    /**
     * Creates a new key with the given name and file location.
     *
     * @param name the name of the key.
     * @param location the location of the key within a JSON file.
     */
    public JsonKey(String name, JsonDataLocation location) {
        this.name = name;
        this.location = location;
    }

    /**
     * Creates a new key with the given name and file location, and documentation notes.
     *
     * @param name the name of the key.
     * @param location the location of the key within a JSON file.
     * @param notes the documentation notes associated with the key.
     */
    public JsonKey(String name, JsonDataLocation location, String notes) {
        this(name, location);
        this.notes = notes;
    }

    /**
     * Gets the name of this key.
     *
     * @return the name of the key.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the location of this key within a JSON file.
     *
     * @return the location of the key within a file.
     */
    public JsonDataLocation getLocation() {
        return location;
    }

    /**
     * Gets documentation notes associated with this key.
     *
     * @return the notes.
     */
    public String getNotes() {
        return notes;
    }

    /*
     * NOTE: equals and hashCode only evaluate the name of the key so that this
     * object can easily be used as keys in a Map and looked up by name,
     * however, each key has unique information about its location within a
     * JSON file and specific documentation notes about its purpose, so they
     * shouldn't be interchanged without regard for losing or changing that data.
     */
    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        JsonKey jsonKey = (JsonKey) o;

        return name.equals(jsonKey.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
