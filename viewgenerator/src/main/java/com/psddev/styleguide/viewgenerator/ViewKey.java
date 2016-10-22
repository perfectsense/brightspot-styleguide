package com.psddev.styleguide.viewgenerator;

/**
 * A unique key for a particular view, whose name will ultimately drive what
 * the Java class name and package as well as the view renderer annotation will
 * be for the resulting view interface.
 */
class ViewKey {

    private String name;

    /**
     * Creates a new view key with the given name.
     *
     * @param name the view key name.
     */
    public ViewKey(String name) {
        this.name = name;
    }

    /**
     * Gets the view key name.
     *
     * @return the name of the view key.
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName();
    }
}
