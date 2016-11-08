package com.psddev.styleguide.viewgenerator;

import java.util.Arrays;

/**
 * A custom System.out based logger that provides greater control over the color
 * of the resulting output.
 */
final class CliLogger {

    private boolean noColor;

    private CliLogger() {
    }

    /**
     * Checks whether this logger will print color control sequences.
     *
     * @return true if this logger will print color, false otherwise.
     */
    public boolean isNoColor() {
        return noColor;
    }

    /**
     * Creates a new logger instance.
     *
     * @return a new logger instance.
     */
    public static CliLogger getLogger() {
        return new CliLogger();
    }

    /**
     * Creates a new colorless logger instance.
     *
     * @return a new colorless logger instance.
     */
    public static CliLogger getColorlessLogger() {
        CliLogger logger = new CliLogger();
        logger.noColor = true;
        return logger;
    }

    /**
     * Prints each of the objects to standard out along with a trailing newline.
     *
     * @param objects the objects to log.
     */
    public void log(Object... objects) {
        Arrays.stream(objects).forEach(System.out::print);
        System.out.println();
    }

    /**
     * Prints each of the objects to standard out in the color
     * {@link CliColor#BLACK black} along with a trailing newline.
     *
     * @param objects the objects to log.
     */
    public void black(Object... objects) {
        color(CliColor.BLACK, objects);
    }

    /**
     * Prints each of the objects to standard out in the color
     * {@link CliColor#BLUE blue} along with a trailing newline.
     *
     * @param objects the objects to log.
     */
    public void blue(Object... objects) {
        color(CliColor.BLUE, objects);
    }

    /**
     * Prints each of the objects to standard out in the color
     * {@link CliColor#CYAN cyan} along with a trailing newline.
     *
     * @param objects the objects to log.
     */
    public void cyan(Object... objects) {
        color(CliColor.CYAN, objects);
    }

    /**
     * Prints each of the objects to standard out in the color
     * {@link CliColor#GREEN green} along with a trailing newline.
     *
     * @param objects the objects to log.
     */
    public void green(Object... objects) {
        color(CliColor.GREEN, objects);
    }

    /**
     * Prints each of the objects to standard out in the color
     * {@link CliColor#PURPLE purple} along with a trailing newline.
     *
     * @param objects the objects to log.
     */
    public void purple(Object... objects) {
        color(CliColor.PURPLE, objects);
    }

    /**
     * Prints each of the objects to standard out in the color
     * {@link CliColor#RED red} along with a trailing newline.
     *
     * @param objects the objects to log.
     */
    public void red(Object... objects) {
        color(CliColor.RED, objects);
    }

    /**
     * Prints each of the objects to standard out in the color
     * {@link CliColor#WHITE white} along with a trailing newline.
     *
     * @param objects the objects to log.
     */
    public void white(Object... objects) {
        color(CliColor.WHITE, objects);
    }

    /**
     * Prints each of the objects to standard out in the color
     * {@link CliColor#YELLOW yellow} along with a trailing newline.
     *
     * @param objects the objects to log.
     */
    public void yellow(Object... objects) {
        color(CliColor.YELLOW, objects);
    }

    /**
     * Creates a logger message builder with the initial color set to
     * {@link CliColor#BLACK black}.
     *
     * @return a new logger message builder.
     */
    public CliLoggerMessageBuilder black() {
        return color(CliColor.BLACK);
    }

    /**
     * Creates a logger message builder with the initial color set to
     * {@link CliColor#BLUE blue}.
     *
     * @return a new logger message builder.
     */
    public CliLoggerMessageBuilder blue() {
        return color(CliColor.BLUE);
    }

    /**
     * Creates a logger message builder with the initial color set to
     * {@link CliColor#CYAN cyan}.
     *
     * @return a new logger message builder.
     */
    public CliLoggerMessageBuilder cyan() {
        return color(CliColor.CYAN);
    }

    /**
     * Creates a logger message builder with the initial color set to
     * {@link CliColor#GREEN green}.
     *
     * @return a new logger message builder.
     */
    public CliLoggerMessageBuilder green() {
        return color(CliColor.GREEN);
    }

    /**
     * Creates a logger message builder with the initial color set to
     * {@link CliColor#PURPLE purple}.
     *
     * @return a new logger message builder.
     */
    public CliLoggerMessageBuilder purple() {
        return color(CliColor.PURPLE);
    }

    /**
     * Creates a logger message builder with the initial color set to
     * {@link CliColor#RED red}.
     *
     * @return a new logger message builder.
     */
    public CliLoggerMessageBuilder red() {
        return color(CliColor.RED);
    }

    /**
     * Creates a logger message builder with the initial color set to
     * {@link CliColor#WHITE white}.
     *
     * @return a new logger message builder.
     */
    public CliLoggerMessageBuilder white() {
        return color(CliColor.WHITE);
    }

    /**
     * Creates a logger message builder with the initial color set to
     * {@link CliColor#YELLOW yellow}.
     *
     * @return a new logger message builder.
     */
    public CliLoggerMessageBuilder yellow() {
        return color(CliColor.YELLOW);
    }

    private void color(CliColor color, Object... objects) {
        color(color).and(objects).log();
    }

    private CliLoggerMessageBuilder color(CliColor color) {
        return new CliLoggerMessageBuilder(this, color);
    }
}
