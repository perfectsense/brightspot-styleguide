package com.psddev.styleguide.viewgenerator;

class ViewClassGeneratorException extends RuntimeException {

    public ViewClassGeneratorException() {
    }

    public ViewClassGeneratorException(String message) {
        super(message);
    }

    public ViewClassGeneratorException(String message, Throwable cause) {
        super(message, cause);
    }

    public ViewClassGeneratorException(Throwable cause) {
        super(cause);
    }

    public ViewClassGeneratorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
