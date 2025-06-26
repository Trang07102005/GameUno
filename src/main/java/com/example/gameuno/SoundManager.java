package com.example.gameuno;

import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.net.URL;

public class SoundManager {

    private static boolean isSoundOn = true;
    private static MediaPlayer bgmPlayer;

    public static void playClick() {
        playSound("click.mp3");
    }

    public static void playError() {
        playSound("error.mp3");
    }

    public static void playWin() {
        playSound("win.mp3");
    }

    public static void playCardShuffle() {
        playSound("card_shuffle.mp3");
    }

    public static void playBGM() {
        if (!isSoundOn) return;
        if (bgmPlayer == null) {
            URL resource = SoundManager.class.getResource("/sounds/bgm.mp3");
            System.out.println("🎵 Loading BGM: " + resource); // Debug log
            bgmPlayer = new MediaPlayer(new Media(resource.toString()));
            bgmPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            bgmPlayer.setVolume(0.3); // 👈 Giảm âm lượng nhạc nền
        }
        bgmPlayer.play();
    }


    public static void stopBGM() {
        if (bgmPlayer != null) {
            bgmPlayer.stop();
        }
    }

    public static void setSoundOn(boolean on) {
        isSoundOn = on;
        if (on) {
            playBGM();
        } else {
            stopBGM();
        }
    }

    public static boolean isSoundOn() {
        return isSoundOn;
    }

    private static void playSound(String fileName) {
        if (!isSoundOn) return;
        URL resource = SoundManager.class.getResource("/sounds/" + fileName);
        System.out.println("🔊 Loading sound: " + resource); // <- Thêm dòng này
        AudioClip clip = new AudioClip(resource.toString());
        clip.play();
    }
}

