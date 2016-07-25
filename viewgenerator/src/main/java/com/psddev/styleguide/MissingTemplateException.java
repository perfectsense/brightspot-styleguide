package com.psddev.styleguide;

import java.util.Map;

import com.psddev.dari.util.ObjectUtils;

public class MissingTemplateException extends RuntimeException {

    public MissingTemplateException(JsonDataFile jsonDataFile) {
        super("File [" + jsonDataFile.getFileName() + "] does not have a template!");
    }

    public MissingTemplateException(JsonDataFile jsonDataFile, Map<String, ?> map) {
        super("Error in [" + jsonDataFile.getFileName() + "]. No _template or _view in map [" + ObjectUtils.toJson(map) + "].");
    }
}
