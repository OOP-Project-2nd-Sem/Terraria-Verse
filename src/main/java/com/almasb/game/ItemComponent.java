package com.almasb.game;

import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.texture.Texture;

public class ItemComponent extends Component {
    private String name;
    private int count;
    private Texture icon;

    public ItemComponent(String name, int count, Texture icon) {
        this.name = name;
        this.count = count;
        this.icon=icon;
    }

    public String getName() { return name; }
    public int getCount() { return count; }
    public Texture getIcon(){return icon;}
}