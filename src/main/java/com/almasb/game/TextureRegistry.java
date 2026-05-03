package com.almasb.game;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.texture.Texture;

import java.util.HashMap;
import java.util.Map;

public class TextureRegistry {
    private static Map<String, Texture> registry = null;

    private static void init() {
        if (registry != null) return;
        registry = new HashMap<>();

        // GameFactory ke blockType strings se match karo
        registry.put("grass", Config.GRASS_TEX);
        registry.put("stone", Config.STONE_TEX);
    }

    public static Texture getTexture(String name) {
        init();
        Texture t = registry.get(name);
        if (t == null) System.out.println("MISSING in registry: " + name);
        return t != null ? t : FXGL.texture("missing.png");
    }
}