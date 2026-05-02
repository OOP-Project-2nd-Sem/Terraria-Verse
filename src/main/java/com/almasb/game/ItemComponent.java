package com.almasb.game;


import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.component.Component;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import static com.almasb.fxgl.dsl.FXGL.spawn;

public class ItemComponent extends Component {
    private String name;
    private int count;
    private Rectangle icon;

    public ItemComponent(String name, int count, Rectangle icon) {
        this.name = name;
        this.count = count;
        this.icon=icon;
    }

    @Override
    public void onUpdate(double tpf) {
        FXGL.onCollisionBegin(EntityType.ITEM, EntityType.ITEM, (item1,item2)->{
            if(item1.getComponent(ItemComponent.class).getName().equals(item1.getComponent(ItemComponent.class).getName())) {
                spawn("item", new SpawnData(item1.getX(), item1.getY())
                        .put("width", 10)
                        .put("height", 10)
                        .put("count", item1.getComponent(ItemComponent.class).getCount() + item2.getComponent(ItemComponent.class).getCount())
                        .put("color", (Color)item1.getComponent(ItemComponent.class).getIcon().getFill()));
                item1.removeFromWorld();
                item2.removeFromWorld();
            }
        });
    }
    public String getName() { return name; }
    public int getCount() { return count; }
    public Rectangle getIcon(){return icon;}
}