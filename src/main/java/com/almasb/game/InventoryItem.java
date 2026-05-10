package com.almasb.game;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.texture.Texture;




public class InventoryItem {
    private String name;
    private int count;
    private transient Texture icon;

    public InventoryItem(String name, int count, Texture icon) {
        this.name = name;
        this.count = count;
        this.icon = icon;
    }

    // JSON load hone ke baad config se reload karo
    public void initTexture() {
        this.icon = Config.getTexture(getBlockTypeForName(name));
    }

    private Config.BlockType getBlockTypeForName(String name) {
        return switch (name.toLowerCase()) {
            case "grass"   -> Config.SURFACE_GRASS_BLOCK;
            case "dirt"    -> Config.DEFAULT_DIRT_BLOCK;
            case "stone"   -> Config.DEFAULT_STONE_BLOCK;
            case "coal"    -> Config.BlockType.COAL_BLOCK_1;
            case "iron"    -> Config.BlockType.IRON_BLOCK_1;
            case "diamond" -> Config.BlockType.DIAMOND_BLOCK_1;
            case "gold"    -> Config.BlockType.GOLD_BLOCK;
            case "emerald" -> Config.BlockType.EMERALD_BLOCK;
            case "lapis"   -> Config.BlockType.LAPIS_LAZULI_BLOCK;
            case "wood"    -> Config.DEFAULT_TREE_TRUNK_BLOCK;
            case "leaves"  -> Config.DEFAULT_LEAVES_BLOCK;
            default        -> Config.SURFACE_GRASS_BLOCK;
        };
    }

    public String getName() { return name; }
    public int getCount() { return count; }
    public Texture getIcon() { return icon; }
    public void setCount(int count) { this.count = count; }
}