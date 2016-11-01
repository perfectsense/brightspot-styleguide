package com.psddev.styleguide.viewgenerator;

enum CliColor {

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
