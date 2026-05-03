package com.almasb.game;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static com.almasb.game.Config.SAVE_DIR;

public class SaveManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // ─── Save ──────────────────────────────────────────────

    // SaveManager mein dono versions rakho

    // Naya character banate waqt (sirf naam/defaults)
    public static void saveCharacter(CharacterData data) {
        try {
            Files.createDirectories(Paths.get(SAVE_DIR));
            String json = GSON.toJson(data);
            Files.writeString(Paths.get(Config.SAVE_DIR + data.name + ".json"), json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Game mein save karte waqt (player ki inventory ke saath)
    public static void saveCharacter(PlayerComponent player, String characterName) {
        CharacterData data = new CharacterData(characterName);
        player.saveToCharacterData(data);
        saveCharacter(data);  // upar wala call karo
    }

    // ─── Load ──────────────────────────────────────────────

    public static CharacterData loadCharacter(String name) {
        try {
            String json = Files.readString(Paths.get(Config.SAVE_DIR + name + ".json"));
            CharacterData data = GSON.fromJson(json, CharacterData.class);
            data.initAllTextures();  // TextureRegistry se reload hoga
            return data;
        } catch (IOException e) {
            return null;
        }
    }

    // ─── List all characters ───────────────────────────────

    public static List<String> getAllCharacters() {
        List<String> names = new ArrayList<>();
        try {
            Path dir = Paths.get(SAVE_DIR);
            if (!Files.exists(dir)) return names;  // pehli baar, folder nahi hai abhi

            Files.list(dir)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(p -> {
                        String filename = p.getFileName().toString();
                        names.add(filename.replace(".json", ""));
                    });

        } catch (IOException e) {
            e.printStackTrace();
        }
        return names;
    }
}