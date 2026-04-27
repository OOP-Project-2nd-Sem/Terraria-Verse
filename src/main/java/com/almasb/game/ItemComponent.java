package com.almasb.game;


import com.almasb.fxgl.entity.component.Component;

public class ItemComponent extends Component {
    private String name;
    private int count;

    public ItemComponent(String name, int count) {
        this.name = name;
        this.count = count;
    }

    public String getName() { return name; }
    public int getCount() { return count; }
}