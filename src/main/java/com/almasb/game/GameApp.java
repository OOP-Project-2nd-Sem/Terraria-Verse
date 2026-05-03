package com.almasb.game;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.input.Input;
import com.almasb.fxgl.input.UserAction;
import com.almasb.fxgl.texture.Texture;
import com.almasb.fxgl.time.TimerAction;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import java.util.List;
import java.util.ArrayList;

import static com.almasb.fxgl.dsl.FXGL.*;

public class GameApp extends GameApplication {

    private Entity player, background;

    // UI roots
    private GridPane inventoryRoot;
    private GridPane hotbarRoot;
    private GridPane armorRoot;

    // Selection state
    private int selectedSlotIndex = -1;
    private String selectedSlotType = "";

    // Current character
    private String currentCharacterName = "";

    // ─── Settings ──────────────────────────────────────────────────────────────

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(Config.TILE_SIZE * 80);
        settings.setHeight(Config.TILE_SIZE * 60);
    }

    // ─── Input ─────────────────────────────────────────────────────────────────

    @Override
    protected void initInput() {
        Input input = FXGL.getInput();

        input.addAction(new UserAction("Move Left") {
            @Override
            protected void onAction() {
                if (player != null)
                    player.getComponent(PlayerComponent.class).left();
            }
            @Override
            protected void onActionEnd() {
                if (player != null && player.getComponent(PlayerComponent.class).isGrounded())
                    player.getComponent(PlayerComponent.class).stop();
            }
        }, KeyCode.A);

        input.addAction(new UserAction("Move Right") {
            @Override
            protected void onAction() {
                if (player != null)
                    player.getComponent(PlayerComponent.class).right();
            }
            @Override
            protected void onActionEnd() {
                if (player != null && player.getComponent(PlayerComponent.class).isGrounded())
                    player.getComponent(PlayerComponent.class).stop();
            }
        }, KeyCode.D);

        input.addAction(new UserAction("Jump") {
            @Override
            protected void onAction() {
                if (player != null)
                    player.getComponent(PlayerComponent.class).jump();
            }
        }, KeyCode.SPACE);

        input.addAction(new UserAction("Mine") {
            private TimerAction timer;

            @Override
            protected void onActionBegin() {
                if (player == null) return;

                double worldX = input.getMouseXWorld();
                double worldY = input.getMouseYWorld();

                if (!isWithinReach(worldX, worldY)) return;

                Rectangle2D mouseBounds = new Rectangle2D(worldX, worldY, 1, 1);
                Entity blockToMine = null;

                for (Entity e : FXGL.getGameWorld().getEntitiesInRange(mouseBounds)) {
                    if (e.isType(EntityType.BLOCK)) {
                        blockToMine = e;
                        break;
                    }
                }

                if (blockToMine == null) return;

                double mineTime = blockToMine.getProperties().exists("mine_time")
                        ? blockToMine.getDouble("mine_time") : 2.0;

                String spawnName = blockToMine.getString("type");
                Entity finalBlock = blockToMine;

                timer = FXGL.getGameTimer().runOnceAfter(() -> {
                    spawn("item", new SpawnData(finalBlock.getX(), finalBlock.getY())
                            .put("type", spawnName)
                            .put("width", 10)
                            .put("height", 10)
                            .put("count", 1));
                    finalBlock.removeFromWorld();
                }, Duration.seconds(mineTime));
            }

            @Override
            protected void onActionEnd() {
                if (timer != null) timer.expire();
            }
        }, MouseButton.PRIMARY);

        input.addAction(new UserAction("Place") {
            @Override
            protected void onActionBegin() {
                if (player == null) return;
                if (selectedSlotIndex == -1) return;

                double worldX = input.getMouseXWorld();
                double worldY = input.getMouseYWorld();

                if (!isWithinReach(worldX, worldY)) return;

                Rectangle2D mouseBounds = new Rectangle2D(worldX, worldY, 1, 1);
                for (Entity e : FXGL.getGameWorld().getEntitiesInRange(mouseBounds)) {
                    if (e.isType(EntityType.BLOCK)) return;
                }

                Point2D snapped = snapCoordinates(worldX, worldY);
                Rectangle2D targetCell = new Rectangle2D(snapped.getX(), snapped.getY(), Config.TILE_SIZE, Config.TILE_SIZE);
                Rectangle2D playerBounds = new Rectangle2D(player.getX(), player.getY(), player.getWidth(), player.getHeight());
                if (targetCell.intersects(playerBounds)) return;

                // Hotbar se place karo (selected slot hotbar mein hai)
                PlayerComponent pc = player.getComponent(PlayerComponent.class);
                List<InventoryItem> sourceList = getSlotList(pc, selectedSlotType);
                InventoryItem itemToPlace = sourceList.get(selectedSlotIndex);

                if (itemToPlace == null) return;

                String itemName = itemToPlace.getName().toLowerCase();
                String spawnType = "";

                if (itemName.contains("grass")) spawnType = "grass";
                else if (itemName.contains("stone")) spawnType = "stone";
                else return;

                FXGL.spawn(spawnType, new SpawnData(snapped.getX(), snapped.getY())
                        .put("width", Config.TILE_SIZE)
                        .put("height", Config.TILE_SIZE));

                itemToPlace.setCount(itemToPlace.getCount() - 1);
                if (itemToPlace.getCount() <= 0) {
                    sourceList.set(selectedSlotIndex, null);
                    selectedSlotIndex = -1;
                    selectedSlotType = "";
                }

                refreshAll();
            }
        }, MouseButton.SECONDARY);

        // Toggle Inventory + Armor — E
        input.addAction(new UserAction("Toggle Inventory") {
            @Override
            protected void onActionBegin() {
                if (inventoryRoot == null) return;
                boolean visible = !inventoryRoot.isVisible();
                inventoryRoot.setVisible(visible);
                armorRoot.setVisible(visible);

                if (!visible) {
                    selectedSlotIndex = -1;
                    selectedSlotType = "";
                    refreshAll();
                }
            }
        }, KeyCode.E);

        // Save — F5
        input.addAction(new UserAction("Save Game") {
            @Override
            protected void onActionBegin() {
                if (player != null && !currentCharacterName.isEmpty()) {
                    PlayerComponent pc = player.getComponent(PlayerComponent.class);
                    SaveManager.saveCharacter(pc, currentCharacterName);
                    showSaveNotification();
                }
            }
        }, KeyCode.F5);

        // Hotbar keys 1-9, 0
        for (int i = 0; i < 10; i++) {
            final int slot = i;
            KeyCode key = KeyCode.getKeyCode(String.valueOf(i == 9 ? 0 : i + 1));
            input.addAction(new UserAction("Hotbar " + slot) {
                @Override
                protected void onActionBegin() {
                    if (player == null) return;
                    player.getComponent(PlayerComponent.class).setSelectedHotbarSlot(slot);
                    refreshHotbar();
                }
            }, key);
        }
    }

    // ─── UI ────────────────────────────────────────────────────────────────────

    @Override
    protected void initUI() {
        showMainMenu();
    }

    // ─── Physics ───────────────────────────────────────────────────────────────

    @Override
    protected void initPhysics() {
        FXGL.onCollisionBegin(EntityType.PLAYER, EntityType.ITEM, (player, item) -> {
            InventoryItem invItem = item.getComponent(ItemComponent.class).toInventoryItem();
            player.getComponent(PlayerComponent.class).addItem(invItem);
            item.removeFromWorld();
            refreshAll();
        });

        FXGL.onCollisionBegin(EntityType.ITEM, EntityType.ITEM, (item1, item2) -> {
            ItemComponent comp1 = item1.getComponent(ItemComponent.class);
            ItemComponent comp2 = item2.getComponent(ItemComponent.class);

            if (comp1.getName().equals(comp2.getName())) {
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

    // ─── Inventory UI ──────────────────────────────────────────────────────────

    private StackPane createSlot(int size, int index, String slotType) {
        StackPane slot = new StackPane();
        slot.setPrefSize(size, size);
        slot.setStyle("-fx-border-color: gray; -fx-border-width: 2; -fx-background-color: #555;");

        slot.setOnMouseClicked(e -> {
            PlayerComponent pc = player.getComponent(PlayerComponent.class);
            List<InventoryItem> targetList = getSlotList(pc, slotType);

            if (selectedSlotIndex == -1) {
                if (targetList.get(index) != null) {
                    selectedSlotIndex = index;
                    selectedSlotType = slotType;
                    slot.setStyle("-fx-border-color: yellow; -fx-border-width: 2; -fx-background-color: #555;");
                }
            } else {
                List<InventoryItem> fromList = getSlotList(pc, selectedSlotType);
                InventoryItem temp = fromList.get(selectedSlotIndex);
                fromList.set(selectedSlotIndex, targetList.get(index));
                targetList.set(index, temp);

                selectedSlotIndex = -1;
                selectedSlotType = "";
                refreshAll();
            }
        });

        PlayerComponent pc = player.getComponent(PlayerComponent.class);
        List<InventoryItem> list = getSlotList(pc, slotType);
        if (list.get(index) != null) {
            InventoryItem item = list.get(index);

            Texture icon = item.getIcon();
            icon.setFitWidth(size - 8);
            icon.setFitHeight(size - 8);

            Label countLabel = new Label(String.valueOf(item.getCount()));
            countLabel.setStyle("-fx-text-fill: white; -fx-font-size: 10;");
            StackPane.setAlignment(countLabel, Pos.BOTTOM_RIGHT);

            slot.getChildren().addAll(icon, countLabel);
        }

        return slot;
    }

    private List<InventoryItem> getSlotList(PlayerComponent pc, String slotType) {
        return switch (slotType) {
            case "hotbar" -> pc.getHotbar();
            case "armor"  -> pc.getArmor();
            default       -> pc.getInventory();
        };
    }

    private void initInventory() {
        // Main inventory
        inventoryRoot = new GridPane();
        inventoryRoot.setHgap(4);
        inventoryRoot.setVgap(4);
        inventoryRoot.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-padding: 10;");
        inventoryRoot.setTranslateX(25);
        inventoryRoot.setTranslateY(25);
        inventoryRoot.setVisible(false);
        getGameScene().addUINode(inventoryRoot);

        // Armor
        armorRoot = new GridPane();
        armorRoot.setHgap(4);
        armorRoot.setVgap(4);
        armorRoot.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-padding: 10;");
        armorRoot.setTranslateX(25 + (Config.INVENTORY_COLS * (Config.INVENTORY_SLOT_SIZE + 4)) + 20);
        armorRoot.setTranslateY(25);
        armorRoot.setVisible(false);
        getGameScene().addUINode(armorRoot);

        // Hotbar — hamesha visible, screen ke neeche
        hotbarRoot = new GridPane();
        hotbarRoot.setHgap(4);
        hotbarRoot.setVgap(4);
        hotbarRoot.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-padding: 6;");
        hotbarRoot.setTranslateX((FXGL.getAppWidth() / 2.0) - (10 * (Config.INVENTORY_SLOT_SIZE + 4)) / 2.0);
        hotbarRoot.setTranslateY(FXGL.getAppHeight() - Config.INVENTORY_SLOT_SIZE - 20);
        getGameScene().addUINode(hotbarRoot);

        refreshAll();
    }

    private void refreshInventory() {
        inventoryRoot.getChildren().clear();
        for (int row = 0; row < Config.INVENTORY_ROWS; row++) {
            for (int col = 0; col < Config.INVENTORY_COLS; col++) {
                int index = row * Config.INVENTORY_COLS + col;
                inventoryRoot.add(createSlot(Config.INVENTORY_SLOT_SIZE, index, "inventory"), col, row);
            }
        }
    }

    private void refreshHotbar() {
        hotbarRoot.getChildren().clear();
        PlayerComponent pc = player.getComponent(PlayerComponent.class);
        for (int i = 0; i < 10; i++) {
            StackPane slot = createSlot(Config.INVENTORY_SLOT_SIZE, i, "hotbar");
            if (i == pc.getSelectedHotbarSlot()) {
                slot.setStyle("-fx-border-color: white; -fx-border-width: 2; -fx-background-color: #777;");
            }
            hotbarRoot.add(slot, i, 0);
        }
    }

    private void refreshArmor() {
        armorRoot.getChildren().clear();
        String[] armorLabels = {"Helmet", "Chestplate", "Leggings", "Boots"};
        for (int i = 0; i < 4; i++) {
            VBox slotWithLabel = new VBox(2);
            slotWithLabel.setAlignment(Pos.CENTER);
            Label label = new Label(armorLabels[i]);
            label.setStyle("-fx-text-fill: white; -fx-font-size: 9;");
            StackPane slot = createSlot(Config.INVENTORY_SLOT_SIZE, i, "armor");
            slotWithLabel.getChildren().addAll(label, slot);
            armorRoot.add(slotWithLabel, 0, i);
        }
    }

    private void refreshAll() {
        if (inventoryRoot == null) return;
        refreshInventory();
        refreshHotbar();
        refreshArmor();
    }

    // ─── Save Notification ─────────────────────────────────────────────────────

    private void showSaveNotification() {
        Label saved = new Label("Game Saved!");
        saved.setStyle("-fx-text-fill: white; -fx-font-size: 14; -fx-background-color: rgba(0,0,0,0.6); -fx-padding: 8;");
        saved.setTranslateX(FXGL.getAppWidth() / 2.0 - 50);
        saved.setTranslateY(50);
        getGameScene().addUINode(saved);
        FXGL.runOnce(() -> getGameScene().removeUINode(saved), Duration.seconds(2));
    }

    // ─── Menu ──────────────────────────────────────────────────────────────────

    private void showMainMenu() {
        FXGL.getGameWorld().addEntityFactory(new GameFactory());
        background = spawn("menu background");

        VBox menu = new VBox(10);
        menu.setTranslateX(500);
        menu.setTranslateY(300);
        Button newGame = new Button("New Character");
        Button loadGame = new Button("Load Character");

        newGame.setOnAction(e -> showCharacterCreation());
        loadGame.setOnAction(e -> showCharacterSelection());

        menu.getChildren().addAll(newGame, loadGame);
        FXGL.getGameScene().addUINode(menu);
    }

    private void showCharacterCreation() {
        FXGL.getGameScene().clearUINodes();

        VBox menu = new VBox(10);
        menu.setTranslateX(500);
        menu.setTranslateY(300);
        TextField nameField = new TextField();
        nameField.setPromptText("Enter Name");

        Button create = new Button("Create");
        create.setOnAction(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) return;

            CharacterData newChar = new CharacterData(name);
            SaveManager.saveCharacter(newChar);
            showWorldSelection(name);
        });

        menu.getChildren().addAll(nameField, create);
        FXGL.getGameScene().addUINode(menu);
    }

    private void showCharacterSelection() {
        FXGL.getGameScene().clearUINodes();

        VBox menu = new VBox(10);
        menu.setTranslateX(500);
        menu.setTranslateY(300);

        List<String> characters = SaveManager.getAllCharacters();

        if (characters.isEmpty()) {
            Label noChars = new Label("No characters found!");
            noChars.setStyle("-fx-text-fill: white;");
            menu.getChildren().add(noChars);
        }

        for (String name : characters) {
            Button btn = new Button(name);
            btn.setOnAction(e -> showWorldSelection(name));
            menu.getChildren().add(btn);
        }

        FXGL.getGameScene().addUINode(menu);
    }

    private void showWorldSelection(String characterName) {
        FXGL.getGameScene().clearUINodes();

        VBox menu = new VBox(10);
        menu.setTranslateX(500);
        menu.setTranslateY(300);
        Button world1 = new Button("World 1");
        world1.setOnAction(e -> startGame(characterName, "world1"));
        menu.getChildren().add(world1);
        FXGL.getGameScene().addUINode(menu);
    }

    private void startGame(String characterName, String world) {
        currentCharacterName = characterName;
        FXGL.getGameScene().clearUINodes();
        FXGL.getGameScene().getViewport().setBounds(0, 0, 80 * 16, 60 * 16);
        // load world + player
        generateMap();
        // Player spawns on the surface (grass layer) in the center of the map
        player = spawn("player", new SpawnData(40 * 16, (20 - 2) * 16));
        background.removeFromWorld();
        spawn("background");

        CharacterData data = SaveManager.loadCharacter(characterName);
        if (data != null) {
            player.getComponent(PlayerComponent.class).loadFromSave(data);
        }

        FXGL.getGameScene().getViewport().bindToEntity(
                player,
                (int)(FXGL.getAppWidth() / 2.0),
                (int)(FXGL.getAppHeight() / 2.0)
        );

        initInventory();
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private boolean isWithinReach(double worldX, double worldY) {
        if (player == null) return false;
        double maxRange = 2 * Config.TILE_SIZE;
        return player.getCenter().distance(worldX, worldY) <= maxRange;
    }

    private Point2D snapCoordinates(double worldX, double worldY) {
        int snappedX = (int) Math.floor(worldX / Config.TILE_SIZE) * Config.TILE_SIZE;
        int snappedY = (int) Math.floor(worldY / Config.TILE_SIZE) * Config.TILE_SIZE;
        return new Point2D(snappedX, snappedY);
    }

    // Convert BlockType to item name string for inventory
    private String getItemNameFromBlockType(Config.BlockType blockType) {
        String name = blockType.toString();

        if (name.contains("GRASS")) return "Grass";
        if (name.contains("DIRT")) return "Dirt";
        if (name.contains("STONE") || name.contains("SLATE") || name.contains("GENERIC")) return "Stone";
        if (name.contains("COAL")) return "Coal";
        if (name.contains("IRON")) return "Iron";
        if (name.contains("DIAMOND")) return "Diamond";
        if (name.contains("GOLD")) return "Gold";
        if (name.contains("EMERALD")) return "Emerald";
        if (name.contains("LAPIS")) return "Lapis";
        if (name.contains("TREE") || name.contains("WOOD")) return "Wood";
        if (name.contains("LEAF")) return "Leaves";
        if (name.contains("BED_ROCK")) return "Bedrock";

        return "Block";
    }

    public void generateMap() {
        int mapWidth = 80;
        int mapHeight = 60;
        int surfaceHeight = 20; // Height where grass appears
        int tileSize = 16;

        // Ground generation - layers of grass/dirt/stone
        for (int x = 0; x < mapWidth; x++) {
            for (int y = surfaceHeight; y < mapHeight; y++) {
                double posX = x * tileSize;
                double posY = y * tileSize;

                Config.BlockType type;
                int depthFromSurface = y - surfaceHeight;

                // Surface grass (top layer)
                if (depthFromSurface == 0) {
                    type = Config.BlockType.GRASS_TOP_LAYER_1;
                }
                // Upper dirt layer (1-2 blocks)
                else if (depthFromSurface == 1 || depthFromSurface == 2) {
                    type = Config.BlockType.DIRT_BLOCK;
                }
                // Upper stone with common ores (coal, iron)
                else if (depthFromSurface >= 3 && depthFromSurface < 15) {
                    int rand = FXGL.random(0, 100);
                    if (rand < 8) {
                        type = Config.BlockType.IRON_BLOCK_1;
                    } else if (rand < 12) {
                        type = Config.BlockType.COAL_BLOCK_1;
                    } else {
                        type = Config.BlockType.GENERIC_STONE_1;
                    }
                }
                // Middle stone with rarer ores (diamond, gold)
                else if (depthFromSurface >= 15 && depthFromSurface < 35) {
                    int rand = FXGL.random(0, 100);
                    if (rand < 3) {
                        type = Config.BlockType.DIAMOND_BLOCK_1;
                    } else if (rand < 6) {
                        type = Config.BlockType.GOLD_BLOCK;
                    } else if (rand < 10) {
                        type = Config.BlockType.COAL_BLOCK_1;
                    } else {
                        type = Config.BlockType.GENERIC_STONE_1;
                    }
                }
                // Deep stone with rare ores (emerald, lapis)
                else if (depthFromSurface >= 35 && depthFromSurface < 58) {
                    int rand = FXGL.random(0, 100);
                    if (rand < 2) {
                        type = Config.BlockType.EMERALD_BLOCK;
                    } else if (rand < 4) {
                        type = Config.BlockType.LAPIS_LAZULI_BLOCK;
                    } else if (rand < 6) {
                        type = Config.BlockType.DIAMOND_BLOCK_1;
                    } else {
                        type = Config.BlockType.GENERIC_STONE_1;
                    }
                }
                // Bedrock at the bottom
                else {
                    type = Config.BlockType.DARK_BED_ROCK_1;
                }

                spawn("block", new SpawnData(posX, posY).put("type", type));
            }
        }

        // Generate trees scattered across the surface (on top of the grass)
        generateTrees(mapWidth, surfaceHeight, tileSize);
    }

    private void generateTrees(int mapWidth, int surfaceHeight, int tileSize) {
        // Trees spawn at roughly 12% density - only on the surface grass
        for (int x = 0; x < mapWidth; x += 8) {
            int randomOffset = FXGL.random(0, 7);
            int treeX = x + randomOffset;

            if (treeX < mapWidth && FXGL.random(0, 100) < 60) {
                // Tree position - trunk sits on top of the grass block
                double treePosX = treeX * tileSize;
                double treePosY = (surfaceHeight - 2) * tileSize;  // On top of grass

                // Spawn trunk (OAK_TREE_BOTTOM)
                spawn("block", new SpawnData(treePosX, treePosY).put("type", Config.BlockType.OAK_TREE_BOTTOM));

                // Spawn tree top (OAK_TREE_TOP) - one block above trunk
                spawn("block", new SpawnData(treePosX, treePosY - tileSize).put("type", Config.BlockType.OAK_TREE_TOP));

                // Spawn leaves around the tree top (but NOT above the surface)
                // Layer 1 - 3 leaves at same height as tree top
                spawn("block", new SpawnData(treePosX - tileSize, treePosY - tileSize).put("type", Config.BlockType.LEAF_VARIANT_1));
                spawn("block", new SpawnData(treePosX + tileSize, treePosY - tileSize).put("type", Config.BlockType.LEAF_VARIANT_1));

                // Layer 2 - 2 leaves slightly above
                spawn("block", new SpawnData(treePosX - tileSize, treePosY - 2 * tileSize).put("type", Config.BlockType.LEAF_VARIANT_1));
                spawn("block", new SpawnData(treePosX + tileSize, treePosY - 2 * tileSize).put("type", Config.BlockType.LEAF_VARIANT_1));
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}