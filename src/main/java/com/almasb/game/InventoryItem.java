package com.almasb.game;

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

    // JSON load hone ke baad TextureRegistry se reload karo
    public void initTexture() {
        this.icon = TextureRegistry.getTexture(name);
    }

    public String getName() { return name; }
    public int getCount() { return count; }
    public Texture getIcon() { return icon; }
    public void setCount(int count) { this.count = count; }
}