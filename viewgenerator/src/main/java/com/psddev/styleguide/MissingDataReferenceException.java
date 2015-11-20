package com.psddev.styleguide;

public class MissingDataReferenceException extends RuntimeException {

    public MissingDataReferenceException(JsonDataFile jsonDataFile, String dataUrl) {
        super("Error in [" + jsonDataFile.getFileName() + "]. Could not resolve _dataUrl [" + dataUrl + "].");
    }
}
