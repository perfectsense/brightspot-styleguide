package com.psddev.styleguide.viewgenerator;

import java.util.Collections;
import java.util.Map;

final class DelegateViewKey extends ViewKey {

    public static final DelegateViewKey INSTANCE = new DelegateViewKey();

    private DelegateViewKey() {
        super(null, JsonFile.DELEGATE_KEY);
    }

    @Override
    public void validate() {
    }

    @Override
    public String getFullyQualifiedClassName() {
        return ViewClassFieldNativeJavaType.OBJECT.getFullyQualifiedClassName();
    }

    @Override
    public String getAnnotationClass() {
        return null;
    }

    @Override
    public Map<String, String> getAnnotationArguments() {
        return Collections.emptyMap();
    }
}
