package com.example.gameuno;

public class UnoCard {

    public enum Color {
        Red, Yellow, Green, Blue, Wild
    }

    public enum Value {
        Zero, One, Two, Three, Four, Five, Six, Seven, Eight, Nine,
        Skip, Reverse, DrawTwo, Wild, WildDrawFour
    }

    private final Color originalColor; // luôn giữ màu gốc
    private Color dynamicColor; // màu hiện tại
    private final Value value;

    public UnoCard(Color color, Value value) {
        this.originalColor = color;
        this.dynamicColor = color;
        this.value = value;
    }

    public Color getColor() {
        return dynamicColor;
    }

    public Value getValue() {
        return value;
    }

    public void setDynamicColor(Color newColor) {
        if (originalColor == Color.Wild) {
            this.dynamicColor = newColor;
        }
    }

    public void resetColor() {
        this.dynamicColor = originalColor;
    }

    public String getImagePath() {
        if (value == Value.Wild || value == Value.WildDrawFour) {
            return "/cards/" + splitCamel(value.name()) + ".png";
        } else {
            return "/cards/" + getColor().name() + "_" + splitCamel(value.name()) + ".png";
        }
    }

    private String splitCamel(String s) {
        return s.replaceAll("([a-z])([A-Z])", "$1_$2");
    }

    @Override
    public String toString() {
        return getColor() + " " + value;
    }
    public static UnoCard fromString(String s) {
        String[] parts = s.trim().split(" ");
        return new UnoCard(UnoCard.Color.valueOf(parts[0]), UnoCard.Value.valueOf(parts[1]));
    }

}
