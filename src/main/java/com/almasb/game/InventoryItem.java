package com.almasb.game;

import com.almasb.fxgl.texture.Texture;


public class InventoryItem {
    private String name;
    private int count;
    private Texture icon;

    public InventoryItem(String name, int count, Texture icon) {
        this.name = name;
        this.count = count;
        this.icon = icon;
    }

    // getters/setters
    public String getName() { return name; }
    public int getCount() { return count; }
    public Texture getIcon() { return icon; }
    public void setCount(int count) { this.count = count; }
}