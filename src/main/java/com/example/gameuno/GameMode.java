package com.example.gameuno;

public enum GameMode {
    OFFLINE, ONLINE;

    private static GameMode mode;

    public static void setMode(GameMode m) {
        mode = m;
    }

    public static GameMode getMode() {
        return mode;
    }
}
