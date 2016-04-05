package com.psddev.styleguide;

public class DataUrlTemplateOverrideException extends RuntimeException {

    public DataUrlTemplateOverrideException(JsonDataFile jsonDataFile, String dataUrl) {
        super("Error in [" + jsonDataFile.getFileName() + "] for _dataUrl [" + dataUrl + "]. Can't override _template field!");
    }
}
