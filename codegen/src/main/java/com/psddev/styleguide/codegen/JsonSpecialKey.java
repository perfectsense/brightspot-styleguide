package com.psddev.styleguide.codegen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A special JSON key prefixed with an underscore.
 */
enum JsonSpecialKey {

    /**
     * The JSON key used to name JSON based Views.
     */
    VIEW_KEY("view"),

    /**
     * The JSON key used to reference template based Views.
     */
    TEMPLATE_KEY("template"),

    /**
     * The JSON key for referencing another JSON file.
     */
    DATA_URL_KEY("include", "dataUrl"),

    /**
     * The JSON key specifying a custom wrapper JSON.
     */
    WRAPPER_KEY("wrapper"),

    /**
     * The JSON key for denoting that a particular field's value can be ANY view.
     */
    DELEGATE_KEY("delegate"),

    /**
     * The JSON key for denoting that a particular field's value is abstract, and not defined in this module.
     */
    ABSTRACT_KEY("abstract"),

    /**
     * The JSON key for providing documentation for the view map it's contained in.
     */
    NOTES_KEY("notes"),

    /**
     * The JSON key pattern for providing documentation for a specific field within a view.
     */
    FIELD_NOTES_KEY_PATTERN("%sNotes");

    private List<String> aliases;

    JsonSpecialKey(String... aliases) {
        this.aliases = Arrays.stream(aliases).map(alias -> PREFIX + alias).collect(Collectors.toList());
    }

    /**
     * @return The default alias for this key.
     */
    public String getDefaultAlias() {
        return aliases.get(0);
    }

    /**
     * @return All of the aliases for this key.
     */
    public List<String> getAliases() {
        return new ArrayList<>(aliases);
    }

    /**
     * @param test A predicate test.
     * @return The first alias to match the {@code test}.
     */
    public String getAlias(Predicate<String> test) {

        for (String alias : getAliases()) {
            if (test.test(alias)) {
                return alias;
            }
        }

        return getDefaultAlias();
    }

    /**
     * @return The default alias.
     */
    @Override
    public String toString() {
        return getDefaultAlias();
    }

    /**
     * The standard prefix for all specialized JSON keys.
     */
    public static final String PREFIX = "_";
}
