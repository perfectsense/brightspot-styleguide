package com.psddev.styleguide.viewgenerator;

import java.util.Collections;
import java.util.Map;

/**
 * A specialized view key that represents a JsonViewMap that contains a single
 * key value pair of "_delegate: true", representing that the field pointing
 * to the referenced JsonViewMap can contain or "delegate to" any type of view
 * and therefore should not be strictly typed and instead be represented as
 * just Object. As such, this class overrides its parent's
 * {@link #getFullyQualifiedClassName()} method to return the native Java
 * Object class.
 */
final class DelegateViewKey extends ViewKey {

    /**
     * The single shared instance of a DelegateViewKey.
     */
    public static final DelegateViewKey INSTANCE = new DelegateViewKey();

    private DelegateViewKey() {
        super(null, JsonFile.DELEGATE_KEY);
    }

    /*
     * No validation is needed for this instance.
     */
    @Override
    public void validate() {
    }

    /*
     * Returns the Java Object class name instead.
     */
    @Override
    public String getFullyQualifiedClassName() {
        return ViewClassFieldNativeJavaType.OBJECT.getFullyQualifiedClassName();
    }

    /*
     * There would be no annotation for this view key since it isn't a real view.
     */
    @Override
    public String getAnnotationClass() {
        return null;
    }

    /*
     * There would be no annotation arguments for this view key since it isn't
     * a real view.
     */
    @Override
    public Map<String, String> getAnnotationArguments() {
        return Collections.emptyMap();
    }
}
