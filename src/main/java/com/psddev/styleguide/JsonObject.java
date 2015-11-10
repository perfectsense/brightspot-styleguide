package com.psddev.styleguide;

import java.util.Set;

abstract class JsonObject {

    public abstract Set<JsonTemplateObject> getIdentityTemplateObjects();

    public abstract JsonObjectType getType();

    public abstract String getNotes();
}
