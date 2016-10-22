package com.psddev.styleguide.viewgenerator;

/**
 * An error detected during the read, parse, normalize or resolve phases of
 * a JSON file's processing.
 */
class JsonFileError {

    private String message;

    private Throwable throwable;

    private JsonDataLocation location;

    /**
     * Creates a new error with the given message.
     *
     * @param message the error message.
     */
    public JsonFileError(String message) {
        this.message = message;
    }

    /**
     * Creates a new error for the given exception.
     *
     * @param throwable the exception causing the error.
     */
    public JsonFileError(Throwable throwable) {
        this.message = throwable.getMessage();
        this.throwable = throwable;
    }

    /**
     * Creates a new error with the given message for the given location.
     *
     * @param message the error message.
     * @param location the location of the error.
     */
    public JsonFileError(String message, JsonDataLocation location) {
        this.message = message;
        this.location = location;
    }

    /**
     * Creates a new error for the given exception at the given location.
     *
     * @param throwable the exception causing the error.
     * @param location the location of the error.
     */
    public JsonFileError(Throwable throwable, JsonDataLocation location) {
        this.message = throwable.getMessage();
        this.throwable = throwable;
        this.location = location;
    }

    /**
     * Gets the error message.
     *
     * @return the error message.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the exception that caused the error.
     *
     * @return the exception that caused the error.
     */
    public Throwable getThrowable() {
        return throwable;
    }

    /**
     * Gets the location in the file where the error was detected.
     *
     * @return the location in the file where the error was detected.
     */
    public JsonDataLocation getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return "Cause: " + message + " At: " + location;
    }
}
