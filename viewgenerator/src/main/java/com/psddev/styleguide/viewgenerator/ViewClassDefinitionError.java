package com.psddev.styleguide.viewgenerator;

class ViewClassDefinitionError {

    private ViewClassDefinition classDef;
    private ViewClassFieldDefinition fieldDef;
    private String message;

    public ViewClassDefinitionError(ViewClassFieldDefinition fieldDef, String message) {
        this.classDef = fieldDef.getClassDefinition();
        this.fieldDef = fieldDef;
        this.message = message;
    }

    public ViewClassDefinitionError(ViewClassDefinition classDef, String message) {
        this.classDef = classDef;
        this.message = message;
    }

    public ViewClassDefinition getClassDefinition() {
        return classDef;
    }

    public ViewClassFieldDefinition getFieldDefinition() {
        return fieldDef;
    }

    public String getMessage() {
        return message;
    }
}
