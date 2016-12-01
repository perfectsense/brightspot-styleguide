package com.psddev.styleguide.codegen;

import java.util.Arrays;

/**
 * A message builder for a {@link CliLogger logger} that allows easy chaining
 * and color switching to build the resulting log message output.
 */
final class CliLoggerMessageBuilder {

    private CliLogger logger;

    private StringBuilder messageBuilder;

    private CliColor currentColor;

    /**
     * Creates a new message builder with the backing {@code logger} instance.
     *
     * @param logger the backing logger instance.
     */
    public CliLoggerMessageBuilder(CliLogger logger) {
        this(logger, null);
    }

    /**
     * Creates a new message builder with the backing {@code logger} instance
     * initialized with the given {@code color}.
     *
     * @param logger the backing logger instance.
     * @param color the initial color.
     */
    public CliLoggerMessageBuilder(CliLogger logger, CliColor color) {
        this.logger = logger;
        this.messageBuilder = new StringBuilder();
        color(color);
    }

    /**
     * Adds the objects to the message buffer.
     *
     * @param objects the objects to append to the message buffer
     * @return this message builder.
     */
    public CliLoggerMessageBuilder append(Object... objects) {
        Arrays.stream(objects).forEach(messageBuilder::append);
        return this;
    }

    /**
     * Changes the current color to {@link CliColor#BLACK black}.
     *
     * @return this message builder.
     */
    public CliLoggerMessageBuilder black() {
        return color(CliColor.BLACK);
    }

    /**
     * Changes the current color to {@link CliColor#BLUE blue}.
     *
     * @return this message builder.
     */
    public CliLoggerMessageBuilder blue() {
        return color(CliColor.BLUE);
    }

    /**
     * Changes the current color to {@link CliColor#CYAN cyan}.
     *
     * @return this message builder.
     */
    public CliLoggerMessageBuilder cyan() {
        return color(CliColor.CYAN);
    }

    /**
     * Changes the current color to {@link CliColor#GREEN green}.
     *
     * @return this message builder.
     */
    public CliLoggerMessageBuilder green() {
        return color(CliColor.GREEN);
    }

    /**
     * Changes the current color to {@link CliColor#PURPLE purple}.
     *
     * @return this message builder.
     */
    public CliLoggerMessageBuilder purple() {
        return color(CliColor.PURPLE);
    }

    /**
     * Changes the current color to {@link CliColor#RED red}.
     *
     * @return this message builder.
     */
    public CliLoggerMessageBuilder red() {
        return color(CliColor.RED);
    }

    /**
     * Changes the current color to {@link CliColor#WHITE white}.
     *
     * @return this message builder.
     */
    public CliLoggerMessageBuilder white() {
        return color(CliColor.WHITE);
    }

    /**
     * Changes the current color to {@link CliColor#YELLOW yellow}.
     *
     * @return this message builder.
     */
    public CliLoggerMessageBuilder yellow() {
        return color(CliColor.YELLOW);
    }

    /**
     * Changes the current color back to the system default.
     *
     * @return this message builder.
     */
    public CliLoggerMessageBuilder reset() {
        return color(null);
    }

    /**
     * Flushes the message builder buffer and logs its content using the
     * underlying logger.
     */
    public void log() {
        if (!logger.isNoColor() && currentColor != null) {
            messageBuilder.append(CliColor.RESET.getControlSequence());
            currentColor = null;
        }
        logger.log(messageBuilder.toString());
        this.messageBuilder = new StringBuilder();
    }

    private CliLoggerMessageBuilder color(CliColor color) {
        if (!logger.isNoColor()) {

            if (currentColor != null && currentColor != color) {
                messageBuilder.append(CliColor.RESET.getControlSequence());
            }

            if (color != null && color != currentColor) {
                messageBuilder.append(color.getControlSequence());
            }
        }
        currentColor = color;
        return this;
    }
}
