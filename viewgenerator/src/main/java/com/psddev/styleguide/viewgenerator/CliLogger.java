package com.psddev.styleguide.viewgenerator;

import java.util.Arrays;

class CliLogger {

    private static enum CliColor {

        BLACK("\u001B[30m"),
        BLUE("\u001B[34m"),
        CYAN("\u001B[36m"),
        GREEN("\u001B[32m"),
        PURPLE("\u001B[35m"),
        RED("\u001B[31m"),
        WHITE("\u001B[37m"),
        YELLOW("\u001B[33m"),
        RESET("\u001B[0m");

        private String controlSequence;

        private CliColor(String controlSequence) {
            this.controlSequence = controlSequence;
        }

        public String getControlSequence() {
            return controlSequence;
        }

        @Override
        public String toString() {
            return controlSequence;
        }
    }

    private boolean noColor;

    public boolean isNoColor() {
        return noColor;
    }

    public static CliLogger getLogger() {
        return new CliLogger();
    }

    public static CliLogger getColorlessLogger() {
        CliLogger logger = new CliLogger();
        logger.noColor = true;
        return logger;
    }

    public void log(Object... objects) {
        Arrays.stream(objects).forEach(System.out::print);
        System.out.println();
    }

    public void black(Object... objects) {
        color(CliColor.BLACK, objects);
    }

    public void blue(Object... objects) {
        color(CliColor.BLUE, objects);
    }

    public void cyan(Object... objects) {
        color(CliColor.CYAN, objects);
    }

    public void green(Object... objects) {
        color(CliColor.GREEN, objects);
    }

    public void purple(Object... objects) {
        color(CliColor.PURPLE, objects);
    }

    public void red(Object... objects) {
        color(CliColor.RED, objects);
    }

    public void white(Object... objects) {
        color(CliColor.WHITE, objects);
    }

    public void yellow(Object... objects) {
        color(CliColor.YELLOW, objects);
    }

    public MessageBuilder black() {
        return color(CliColor.BLACK);
    }

    public MessageBuilder blue() {
        return color(CliColor.BLUE);
    }

    public MessageBuilder cyan() {
        return color(CliColor.CYAN);
    }

    public MessageBuilder green() {
        return color(CliColor.GREEN);
    }

    public MessageBuilder purple() {
        return color(CliColor.PURPLE);
    }

    public MessageBuilder red() {
        return color(CliColor.RED);
    }

    public MessageBuilder white() {
        return color(CliColor.WHITE);
    }

    public MessageBuilder yellow() {
        return color(CliColor.YELLOW);
    }

    private void color(CliColor color, Object... objects) {
        color(color).and(objects).log();
    }

    private MessageBuilder color(CliColor color) {
        return new MessageBuilder(this, color);
    }

    public static final class MessageBuilder {

        private CliLogger logger;

        private StringBuilder messageBuilder;

        private CliColor currentColor;

        private MessageBuilder(CliLogger logger) {
            this(logger, null);
        }

        private MessageBuilder(CliLogger logger, CliColor color) {
            this.logger = logger;
            this.messageBuilder = new StringBuilder();
            color(color);
        }

        public MessageBuilder and(Object... objects) {
            Arrays.stream(objects).forEach(messageBuilder::append);
            return this;
        }

        public MessageBuilder black() {
            return color(CliColor.BLACK);
        }

        public MessageBuilder blue() {
            return color(CliColor.BLUE);
        }

        public MessageBuilder cyan() {
            return color(CliColor.CYAN);
        }

        public MessageBuilder green() {
            return color(CliColor.GREEN);
        }

        public MessageBuilder purple() {
            return color(CliColor.PURPLE);
        }

        public MessageBuilder red() {
            return color(CliColor.RED);
        }

        public MessageBuilder white() {
            return color(CliColor.WHITE);
        }

        public MessageBuilder yellow() {
            return color(CliColor.YELLOW);
        }

        public MessageBuilder reset() {
            return color(null);
        }

        public void log() {
            if (!logger.isNoColor() && currentColor != null) {
                messageBuilder.append(CliColor.RESET.getControlSequence());
                currentColor = null;
            }
            logger.log(messageBuilder.toString());
        }

        private MessageBuilder color(CliColor color) {
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
}
