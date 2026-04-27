package com.almasb.game;


import java.awt.*;
import javafx.scene.shape.Rectangle;


public class InventoryItem {
    private String name;
    private int count;
    private Rectangle icon;

    public InventoryItem(String name, int count, Rectangle icon) {
        this.name = name;
        this.count = count;
        this.icon = icon;
    }

    // getters/setters
    public String getName() { return name; }
    public int getCount() { return count; }
    public Rectangle getIcon() { return icon; }
    public void setCount(int count) { this.count = count; }
}