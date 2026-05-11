package com.almasb.game;

import com.almasb.fxgl.audio.Music;
import com.almasb.fxgl.dsl.FXGL;
import javafx.scene.media.AudioClip;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Centralised audio manager for Terraria Verse.
 *
 * Music  → FXGL.loopBGM() / AudioPlayer.stopMusic()
 *           (uses FXGL's asset-loader path: assets/music/<file>)
 *
 * Sounds → raw JavaFX AudioClip loaded from the classpath.
 *           AudioClip.setRate() gives us per-play pitch randomisation without
 *           needing any internal FXGL Sound accessor methods.
 *           Clips are cached so each file is loaded from disk only once.
 *           (classpath path: /assets/sounds/<file>)
 *
 * NOTE: JavaFX AudioClip does NOT support FLAC.
 *       If gold_mine.flac fails to load, convert it to gold_mine.wav or
 *       gold_mine.mp3 and update the SOUND_FILES entry below.
 */
public class SoundManager {

    // ── Pitch randomisation ───────────────────────────────────────────────────
    private static final double MIN_PITCH = 0.85;
    private static final double MAX_PITCH = 1.15;
    private static final Random RANDOM = new Random();

    // ── Volume levels ─────────────────────────────────────────────────────────
    private static final double MASTER_VOLUME   = 0.7;
    private static final double MUSIC_VOLUME    = 0.5;
    private static final double HITSOUND_VOLUME = 0.6;   // 0.0–1.0 for AudioClip.play()

    // ── Currently-playing music handle (needed to stop it later) ──────────────
    private static Music currentMusic = null;

    // ── AudioClip cache: logical name → loaded clip ───────────────────────────
    private static final Map<String, AudioClip> clipCache = new HashMap<>();

    // ── File-name registries ──────────────────────────────────────────────────
    private static final Map<String, String> SOUND_FILES = new HashMap<>();
    private static final Map<String, String> MUSIC_FILES = new HashMap<>();

    static {
        // Sounds (under assets/sounds/ on the classpath)
        SOUND_FILES.put("crafting_success",   "crafting_success.wav");
        SOUND_FILES.put("diamond_lapis_mine", "diamond_lapis_mine.wav");
        SOUND_FILES.put("gold_mine",          "gold_mine.wav");  // convert to wav/mp3 if this fails
        SOUND_FILES.put("iron_coal_mine",     "iron_coal_mine.mp3");
        SOUND_FILES.put("jump",               "jump.wav");
        SOUND_FILES.put("leaf_crunch",        "leaf_crunch.wav");
        SOUND_FILES.put("menu_click",         "menu_click.wav");
        SOUND_FILES.put("stone_hit",          "stone_hit.mp3");
        SOUND_FILES.put("walking_running",    "walking_running.wav");
        SOUND_FILES.put("water_move",         "water_move.wav");
        SOUND_FILES.put("wood_hits",          "wood_hits.wav");

        // Music (under assets/music/ on the classpath – path used by FXGL.loopBGM)
        MUSIC_FILES.put("menu_music", "menu_music.mp3");  // OGG is unsupported by JavaFX on Windows – use MP3
        MUSIC_FILES.put("game_music", "game_music.mp3");
    }

    // ── Internal: load and cache an AudioClip ─────────────────────────────────

    /**
     * Loads the JavaFX AudioClip for a logical sound name on first access,
     * then returns the cached instance on subsequent calls.
     * Returns null (and logs) if the file cannot be found or loaded.
     */
    private static AudioClip getClip(String soundName) {
        if (clipCache.containsKey(soundName)) {
            return clipCache.get(soundName); // may be null if a previous load failed
        }

        String fileName = SOUND_FILES.get(soundName);
        if (fileName == null) {
            System.err.println("[SoundManager] No file registered for sound '" + soundName + "'");
            clipCache.put(soundName, null);
            return null;
        }

        // AudioClip requires an absolute URL string; load from classpath root.
        String resourcePath = "/assets/sounds/" + fileName;
        URL url = SoundManager.class.getResource(resourcePath);
        if (url == null) {
            System.err.println("[SoundManager] Resource not found on classpath: " + resourcePath);
            clipCache.put(soundName, null);
            return null;
        }

        try {
            AudioClip clip = new AudioClip(url.toExternalForm());
            clipCache.put(soundName, clip);
            return clip;
        } catch (Exception e) {
            System.err.println("[SoundManager] Failed to load AudioClip '" + fileName + "': " + e.getMessage());
            clipCache.put(soundName, null);
            return null;
        }
    }

    // ── Music API ─────────────────────────────────────────────────────────────

    /** Switch to looping gameplay music. */
    public static void playGameMusic() {
        switchMusic("game_music");
    }

    /** Switch to looping main-menu music. */
    public static void playMenuMusic() {
        switchMusic("menu_music");
    }

    /** Stop whatever music track is currently playing. */
    public static void stopMusic() {
        if (currentMusic == null) return;
        try {
            FXGL.getAudioPlayer().stopMusic(currentMusic);
        } catch (Exception e) {
            System.err.println("[SoundManager] Failed to stop music: " + e.getMessage());
        } finally {
            currentMusic = null;
        }
    }

    /**
     * Stops the current track (if any) then starts the requested one looping.
     * FXGL.loopBGM() resolves the file name from assets/music/ automatically
     * and returns the Music handle we need later for stopMusic().
     */
    private static void switchMusic(String musicName) {
        // Stop current track first using the stored Music handle
        stopMusic();

        String fileName = MUSIC_FILES.get(musicName);
        if (fileName == null) {
            System.err.println("[SoundManager] No file registered for music '" + musicName + "'");
            return;
        }

        try {
            // FXGL.loopBGM() loads from assets/music/<fileName>, starts looping,
            // and returns the Music object – we hold onto it so we can stop it later.
            currentMusic = FXGL.loopBGM(fileName);

            // Adjust global music volume via FXGL settings
            FXGL.getSettings().setGlobalMusicVolume(MUSIC_VOLUME * MASTER_VOLUME);
        } catch (Exception e) {
            System.err.println("[SoundManager] Failed to start music '" + musicName + "': " + e.getMessage());
            currentMusic = null;
        }
    }

    // ── Hitsound API ──────────────────────────────────────────────────────────

    /**
     * Play a hitsound with pitch randomisation enabled.
     *
     * @param soundName logical name registered in SOUND_FILES
     */
    public static void playHitsound(String soundName) {
        playHitsound(soundName, true);
    }

    /**
     * Play a hitsound with optional pitch randomisation.
     *
     * AudioClip.setRate() controls playback speed / pitch (1.0 = normal).
     * AudioClip.play(volume) accepts [0.0, 1.0].
     * Effective volume = HITSOUND_VOLUME × MASTER_VOLUME = 0.42 by default.
     *
     * @param soundName     logical name registered in SOUND_FILES
     * @param randomizePitch true → slight random pitch shift on every play
     */
    public static void playHitsound(String soundName, boolean randomizePitch) {
        AudioClip clip = getClip(soundName);
        if (clip == null) {
            // Already logged in getClip(); nothing more to do.
            return;
        }
        try {
            double pitch  = randomizePitch ? getRandomPitch() : 1.0;
            double volume = HITSOUND_VOLUME * MASTER_VOLUME;
            clip.setRate(pitch);
            clip.play(volume);
        } catch (Exception e) {
            System.err.println("[SoundManager] Failed to play hitsound '" + soundName + "': " + e.getMessage());
        }
    }

    // ── Block-type dispatcher ─────────────────────────────────────────────────

    /**
     * Plays the correct mining hitsound based on the block being broken.
     */
    public static void playMineSoundForBlockType(Config.BlockType blockType) {
        if (blockType == null) return;

        String name = blockType.toString();
        String soundName;

        if      (name.contains("TREE")    || name.contains("WOOD"))                              soundName = "wood_hits";
        else if (name.contains("LEAF"))                                                           soundName = "leaf_crunch";
        else if (name.contains("GRASS")   || name.contains("DIRT"))                              soundName = "gold_mine";
        else if (name.contains("STONE")   || name.contains("GENERIC") || name.contains("SLATE")) soundName = "stone_hit";
        else if (name.contains("COAL")    || name.contains("IRON"))                               soundName = "iron_coal_mine";
        else if (name.contains("GOLD"))                                                           soundName = "gold_mine";
        else if (name.contains("DIAMOND") || name.contains("LAPIS"))                             soundName = "diamond_lapis_mine";
        else return;

        playHitsound(soundName, true);
    }

    // ── Convenience one-liners ────────────────────────────────────────────────

    /** Player jump. No pitch randomisation – consistent feel. */
    public static void playJumpSound() {
        playHitsound("jump", false);
    }

    /** Footstep while walking/running. Slightly randomised for variety. */
    public static void playWalkingSound() {
        playHitsound("walking_running", true);
    }

    /**
     * Button click on the launch menu, seed-select, and character-select screens.
     * No pitch randomisation – UI clicks should sound consistent.
     */
    public static void playMenuClickSound() {
        playHitsound("menu_click", false);
    }

    /** Fired when the player successfully crafts an item. */
    public static void playCraftingSuccessSound() {
        playHitsound("crafting_success", true);
    }

    /** Player moves through water. */
    public static void playWaterMoveSound() {
        playHitsound("water_move", false);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private static double getRandomPitch() {
        return MIN_PITCH + (MAX_PITCH - MIN_PITCH) * RANDOM.nextDouble();
    }
}
