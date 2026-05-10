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
                .bbox(new HitBox(BoundingShape.box(12, 18)))
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
                .view(texture("backgrounds/MainMenu_Forest.png", getAppWidth(), getAppHeight()))
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
         InventoryItem.ItemCategory category = CraftingRecipe.categoryOf(itemType);

         // Get texture based on item type
         Texture itemTex = getItemTexture(itemType);
         Texture worldView = itemTex.copy();
         Texture inventoryView = itemTex.copy();
         // Scale world view to 80%
         worldView.setFitWidth(worldView.getImage().getWidth() * 0.6);
         worldView.setFitHeight(worldView.getImage().getHeight() * 0.6);

         PhysicsComponent physics = new PhysicsComponent();
         physics.setBodyType(BodyType.DYNAMIC);
         return entityBuilder(data)
                 .with(new ItemComponent(itemType, data.get("count"), category, inventoryView))
                 .type(EntityType.ITEM)
                 .view(worldView)
                 .bbox(new HitBox(BoundingShape.box(data.<Integer>get("width"), data.<Integer>get("height"))))
                 .with(physics)
                 .collidable()
                 .build();
     }

     private Texture getItemTexture(String itemType) {
         // Return appropriate texture based on item type
         if (itemType.equalsIgnoreCase("grass") || itemType.equalsIgnoreCase("Grass")) {
             return Config.GRASS_TEX;
         } else if (itemType.equalsIgnoreCase("stone") || itemType.equalsIgnoreCase("Stone")) {
             return Config.STONE_TEX;
         } else if (itemType.equalsIgnoreCase("dirt") || itemType.equalsIgnoreCase("Dirt")) {
             return texture("textures_02_08_25.png")
                     .subTexture(new Rectangle2D(Config.BlockType.DIRT_BLOCK.col * 16, Config.BlockType.DIRT_BLOCK.row * 16, 16, 16));
         } else if (itemType.equalsIgnoreCase("coal") || itemType.equalsIgnoreCase("Coal")) {
             return texture("textures_02_08_25.png")
                     .subTexture(new Rectangle2D(Config.BlockType.COAL_BLOCK_1.col * 16, Config.BlockType.COAL_BLOCK_1.row * 16, 16, 16));
         } else if (itemType.equalsIgnoreCase("iron") || itemType.equalsIgnoreCase("Iron")) {
             return texture("textures_02_08_25.png")
                     .subTexture(new Rectangle2D(Config.BlockType.IRON_BLOCK_1.col * 16, Config.BlockType.IRON_BLOCK_1.row * 16, 16, 16));
         } else if (itemType.equalsIgnoreCase("diamond") || itemType.equalsIgnoreCase("Diamond")) {
             return texture("textures_02_08_25.png")
                     .subTexture(new Rectangle2D(Config.BlockType.DIAMOND_BLOCK_1.col * 16, Config.BlockType.DIAMOND_BLOCK_1.row * 16, 16, 16));
         } else if (itemType.equalsIgnoreCase("gold") || itemType.equalsIgnoreCase("Gold")) {
             return texture("textures_02_08_25.png")
                     .subTexture(new Rectangle2D(Config.BlockType.GOLD_BLOCK.col * 16, Config.BlockType.GOLD_BLOCK.row * 16, 16, 16));
         } else if (itemType.equalsIgnoreCase("emerald") || itemType.equalsIgnoreCase("Emerald")) {
             return texture("textures_02_08_25.png")
                     .subTexture(new Rectangle2D(Config.BlockType.EMERALD_BLOCK.col * 16, Config.BlockType.EMERALD_BLOCK.row * 16, 16, 16));
         } else if (itemType.equalsIgnoreCase("lapis") || itemType.equalsIgnoreCase("Lapis")) {
             return texture("textures_02_08_25.png")
                     .subTexture(new Rectangle2D(Config.BlockType.LAPIS_LAZULI_BLOCK.col * 16, Config.BlockType.LAPIS_LAZULI_BLOCK.row * 16, 16, 16));
         } else if (itemType.equalsIgnoreCase("wood") || itemType.equalsIgnoreCase("Wood")) {
             return texture("textures_02_08_25.png")
                     .subTexture(new Rectangle2D(Config.BlockType.OAK_TREE_BOTTOM.col * 16, Config.BlockType.OAK_TREE_BOTTOM.row * 16, 16, 16));
         } else if (itemType.equalsIgnoreCase("leaves") || itemType.equalsIgnoreCase("Leaves")) {
             return texture("textures_02_08_25.png")
                     .subTexture(new Rectangle2D(Config.BlockType.LEAF_VARIANT_1.col * 16, Config.BlockType.LEAF_VARIANT_1.row * 16, 16, 16));
         }

         // Default to grass texture
         return Config.GRASS_TEX;
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



