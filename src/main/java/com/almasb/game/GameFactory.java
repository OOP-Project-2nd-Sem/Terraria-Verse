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
import com.almasb.fxgl.physics.box2d.dynamics.FixtureDef;
import com.almasb.fxgl.texture.Texture;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import static com.almasb.fxgl.dsl.FXGL.*;
import static com.almasb.fxgl.dsl.FXGL.getAppHeight;

public class GameFactory implements EntityFactory {

    @Spawns("player")
    public Entity newPlayer(SpawnData data) {
        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.DYNAMIC);
        physics.setFixtureDef(new FixtureDef().friction(0f)); //Make the friction 0 to reduce the chances of some bugs and inconsistencies

        return entityBuilder(data)
                .type(EntityType.PLAYER)
                .zIndex(10)
                .view(new Rectangle(15, 19, Color.TRANSPARENT))
                .bbox(new HitBox(BoundingShape.box(15, 19)))
                .with(physics)
                .with(new PlayerComponent())
                .collidable()
                .build();
    }

    @Spawns("enemy")
    public Entity newEnemy(SpawnData data) {
        EnemyType type = data.get("type");

        //Default health and speed
        int health = 30;
        double speed = 40;
        double damage = 15;
        // Texture texture =

        switch (type) {
            case SLIME:
                health = 20;
                speed = 40;
                damage = 10;
                //texture =
                break;
            case ZOMBIE:
                health = 50;
                speed = 25;
                damage = 20;
                //texture =
                break;
        }

        //If a custom health and speed is passed through spawnData
        if (data.hasKey("health")) health = data.get("health");
        if (data.hasKey("speed")) speed = data.get("speed");
        if(data.hasKey("damage")) damage = data.get("damage");

        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.DYNAMIC);
        physics.setFixtureDef(new FixtureDef().friction(0f)); //Make the friction 0 to reduce the chances of some bugs and inconsistencies

        return entityBuilder()
                .type(EntityType.ENEMY)
                .viewWithBBox(new Rectangle(10, 10, Color.GREEN))
                .with(physics)
                .with(new EnemyComponent(health, speed, damage))
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
        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.STATIC);

        return entityBuilder(data)
                .type(EntityType.BLOCK)
                .view(blockTex.copy())
                .bbox(new HitBox(BoundingShape.box(data.<Integer>get("width"), data.<Integer>get("height"))))
                .with(physics)
                .with("mine_time", minetime)
                .neverUpdated()
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
         String itemType = data.get("type");

         // Get texture based on item type
         Texture itemTex = getItemTexture(itemType);
         Texture worldView = itemTex.copy();
         Texture inventoryView = itemTex.copy();

         PhysicsComponent physics = new PhysicsComponent();
         physics.setBodyType(BodyType.DYNAMIC);
         return entityBuilder(data)
                 .with(new ItemComponent(itemType, data.get("count"), inventoryView))
                 .type(EntityType.ITEM)
                 .view(worldView)
                 .bbox(new HitBox(BoundingShape.box(data.<Integer>get("width"), data.<Integer>get("height"))))
                 .with(physics)
                 .collidable()
                 .build();
     }

     private Texture getItemTexture(String itemType) {
         String normalized = itemType == null ? "" : itemType.trim().toLowerCase();

         return switch (normalized) {
             case "grass" -> Config.getTexture(Config.SURFACE_GRASS_BLOCK);
             case "dirt" -> Config.getTexture(Config.DEFAULT_DIRT_BLOCK);
             case "stone" -> Config.getTexture(Config.DEFAULT_STONE_BLOCK);
             case "coal" -> Config.getTexture(Config.BlockType.COAL_BLOCK_1);
             case "iron" -> Config.getTexture(Config.BlockType.IRON_BLOCK_1);
             case "diamond" -> Config.getTexture(Config.BlockType.DIAMOND_BLOCK_1);
             case "gold" -> Config.getTexture(Config.BlockType.GOLD_BLOCK);
             case "emerald" -> Config.getTexture(Config.BlockType.EMERALD_BLOCK);
             case "lapis" -> Config.getTexture(Config.BlockType.LAPIS_LAZULI_BLOCK);
             case "wood" -> Config.getTexture(Config.DEFAULT_TREE_TRUNK_BLOCK);
             case "leaves" -> Config.getTexture(Config.DEFAULT_LEAVES_BLOCK);
             default -> Config.getTexture(Config.SURFACE_GRASS_BLOCK);
         };
     }
     @Spawns("block")
     public Entity newBlock(SpawnData data) {
         // Retrieve the Enum value passed during the spawn() call
         Config.BlockType type = data.get("type");

         // Determine mine time based on block type
         double mineTime = 1.5; // Default
         if (type.toString().contains("STONE") || type.toString().contains("ORE") || type.toString().contains("BLOCK")) {
             mineTime = 2.5;
         } else if (type.toString().contains("DIRT")) {
             mineTime = 0.5;
         } else if (type.toString().contains("GRASS")) {
             mineTime = 0.6;
         } else if (type.toString().contains("LEAF") || type.toString().contains("TREE")) {
             mineTime = 0.3;
         }

         PhysicsComponent physics = new PhysicsComponent();
         physics.setBodyType(BodyType.STATIC);

         return entityBuilder(data)
                 // Use the math we established earlier
                 .view(texture("textures_02_08_25.png")
                         .subTexture(new Rectangle2D(type.col * 16, type.row * 16, 16, 16)).copy())
                 .bbox(new HitBox(BoundingShape.box(16, 16)))
                 .with(physics)
                 .with("mine_time", mineTime)
                 .type(EntityType.BLOCK)
                 .neverUpdated()
                 .collidable()
                 .build();
     }
}



