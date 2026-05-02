package com.almasb.game;


import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.texture.Texture;

import static com.almasb.fxgl.dsl.FXGL.spawn;

public class ItemComponent extends Component {
    private String name;
    private int count;
    private Texture icon;

    public ItemComponent(String name, int count, Texture icon) {
        this.name = name;
        this.count = count;
        this.icon=icon;
    }

    @Override
    public void onUpdate(double tpf) {
        FXGL.onCollisionBegin(EntityType.ITEM, EntityType.ITEM, (item1,item2)->{
            ItemComponent comp1 = item1.getComponent(ItemComponent.class);
            ItemComponent comp2 = item2.getComponent(ItemComponent.class);

            if(comp1.getName().equals(comp2.getName())) {
                spawn("item", new SpawnData(item1.getX(), item1.getY())
                        .put("type", comp1.getName())
                        .put("width", 10)
                        .put("height", 10)
                        .put("count", comp1.getCount() + comp2.getCount()));
                item1.removeFromWorld();
                item2.removeFromWorld();
            }
        });
    }
    public String getName() { return name; }
    public int getCount() { return count; }
    public Texture getIcon(){return icon;}
}