package com.almasb.game;

import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.texture.Texture;



public class ItemComponent extends Component {
    private String name;
    private int count;
    private InventoryItem.ItemCategory category;
    private transient Texture icon;

    public ItemComponent(String name, int count, InventoryItem.ItemCategory category, Texture icon) {
        this.name     = name;
        this.count    = count;
        this.category = category;
        this.icon     = icon;
    }

    public InventoryItem toInventoryItem() {
        return new InventoryItem(name, count, category, icon);
    }

    public String getName()                    { return name; }
    public int getCount()                      { return count; }
    public InventoryItem.ItemCategory getCategory() { return category; }
    public Texture getIcon()                   { return icon; }
}