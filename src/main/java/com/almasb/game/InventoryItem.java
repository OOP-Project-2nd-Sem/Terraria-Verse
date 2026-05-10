package com.almasb.game;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.texture.Texture;

public class InventoryItem {

    public enum ItemCategory { BLOCK, ITEM, TOOL, ARMOR }

    private String name;
    private int count;
    private ItemCategory category;
    private transient Texture icon;

    public InventoryItem(String name, int count, ItemCategory category, Texture icon) {
        this.name     = name;
        this.count    = count;
        this.category = category;
        this.icon     = icon;
    }

    public void initTexture() {
        if(this.category == null){
            this.category = CraftingRecipe.categoryOf(this.name);
        }
        this.icon = switch (category) {
            case BLOCK        -> Config.getTexture(getBlockTypeForName(name));
            case ITEM, TOOL, ARMOR -> Config.getItemTexture(getItemTypeForName(name));
        };
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
            case "plank"   -> Config.DEFAULT_PLANK_BLOCK;
            case "leaves"  -> Config.DEFAULT_LEAVES_BLOCK;
            default        -> Config.SURFACE_GRASS_BLOCK;
        };
    }

    private Config.ItemType getItemTypeForName(String name) {
        return switch (name.toLowerCase()) {
            case "torch"   -> Config.ItemType.TORCH;
            case "stick"   -> Config.ItemType.STICK;
            case "wooden_pickaxe"->Config.ItemType.WOODEN_PICKAXE;
            // add more items/tools/armor here...
            default        -> Config.ItemType.EYE;
        };
    }

    public String getName()           { return name; }
    public int getCount()             { return count; }
    public ItemCategory getCategory() { return category; }
    public Texture getIcon()          { return icon; }
    public void setCount(int count)   { this.count = count; }
}