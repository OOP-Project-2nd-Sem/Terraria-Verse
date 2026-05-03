package com.almasb.game;

import com.almasb.fxgl.dsl.views.ScrollingBackgroundView;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.almasb.fxgl.entity.components.IrremovableComponent;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import com.almasb.fxgl.physics.PhysicsComponent;
import com.almasb.fxgl.physics.box2d.dynamics.BodyType;
import com.almasb.fxgl.texture.Texture;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import static com.almasb.fxgl.dsl.FXGL.*;
import static com.almasb.fxgl.dsl.FXGL.getAppHeight;

public class GameFactory implements EntityFactory {

    @Spawns("player")
    public Entity newPlayer(SpawnData data) {
        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.DYNAMIC);

        return entityBuilder(data)
                .type(EntityType.PLAYER)
                .viewWithBBox(new Rectangle(10, 10, Color.RED))
                .with(physics)
                .with(new PlayerComponent())
                .collidable()
                .build();
    }

    @Spawns("background")
    public Entity newBackground(SpawnData data) {
        return entityBuilder()
                .view(new ScrollingBackgroundView(texture("backgrounds/forest.png").getImage(), getAppWidth(), getAppHeight()))
                .zIndex(-1)
                .with(new IrremovableComponent())
                .build();
    }

    @Spawns("menu background")
    public Entity newMenuBackground(SpawnData data) {
        return entityBuilder()
                .view(new ScrollingBackgroundView(texture("backgrounds/eiffeltower.png").getImage(), getAppWidth(), getAppHeight()))
                .zIndex(-1)
                .build();
    }

    private Entity createBlock(SpawnData data, Texture blockTex, double minetime) {
        return entityBuilder(data)
                .type(EntityType.BLOCK)
                .view(blockTex)
                .bbox(new HitBox(BoundingShape.box(data.<Integer>get("width"), data.<Integer>get("height"))))
                .with(new PhysicsComponent())
                .with("mine_time", minetime)
                .collidable()
                .build();
    }

    @Spawns("grass")
    public Entity newGrass(SpawnData data) {
        return createBlock(data, Config.GRASS_TEX, 0.5);
    }

    @Spawns("stone")
    public Entity newStone(SpawnData data) {
        return createBlock(data, Config.STONE_TEX, 2.5);
    }

    @Spawns("item")
    public Entity newItem(SpawnData data) {
        String blockType = data.get("type");

        Texture blockTex = Config.GRASS_TEX;

        if (blockType.equals("grass")) {blockTex = Config.GRASS_TEX;}
        else if (blockType.equals("stone")) {blockTex = Config.STONE_TEX;}

        Texture worldView = blockTex.copy();
        Texture inventoyView = blockTex.copy();

        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.DYNAMIC);
        return entityBuilder(data)
                .with(new ItemComponent(blockType, data.get("count"), inventoyView))
                .type(EntityType.ITEM)
                .view(worldView)
                .bbox(new HitBox(BoundingShape.box(data.<Integer>get("width"), data.<Integer>get("height"))))
                .with(physics)
                .collidable()
                .build();
    }
}
