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
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.almasb.fxgl.dsl.FXGL.*;

public class GameApp extends GameApplication {

    private Entity player, background;
    private final Map<Long, Entity> activeTerrainBlocks = new HashMap<>();
    private Config.BlockType[][] terrainMap;
    private int[] surfaceHeights;

    private static final int MAP_WIDTH_TILES = 120;
    private static final int MAP_HEIGHT_TILES = 80;
    private static final int VIEW_WIDTH_TILES = 64;
    private static final int VIEW_HEIGHT_TILES = 40;
    private static final int ACTIVE_TERRAIN_MARGIN_TILES = 1;
    private static final int SURFACE_HEIGHT_TILE = 20;
    private static final int PLAYER_BBOX_HEIGHT = 19;
    private static final int PLAYER_SAFE_LOAD_RADIUS_X = 10;
    private static final int PLAYER_SAFE_LOAD_RADIUS_Y = 8;
    private static final boolean USE_NOISE_TERRAIN_GENERATION = true;

    private int loadedMinTileX = Integer.MAX_VALUE;
    private int loadedMaxTileX = Integer.MIN_VALUE;
    private int loadedMinTileY = Integer.MAX_VALUE;
    private int loadedMaxTileY = Integer.MIN_VALUE;

    private long worldSeed = 1337L;
    private TerrainNoiseGenerator terrainNoise;

    // UI roots
    private GridPane inventoryRoot;
    private GridPane hotbarRoot;
    private GridPane armorRoot;

    // Selection state
    private int selectedSlotIndex = -1;
    private String selectedSlotType = "";

    // Current character
    private String currentCharacterName = "";

    @Override
    protected void initGame() {
        FXGL.getGameWorld().addEntityFactory(new GameFactory());
    }

    // ─── Settings ──────────────────────────────────────────────────────────────

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(Config.TILE_SIZE * VIEW_WIDTH_TILES);
        settings.setHeight(Config.TILE_SIZE * VIEW_HEIGHT_TILES);
        settings.setProfilingEnabled(false);
        settings.addEngineService(MiniProfilerService.class);
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
                //Ignore if the game hasn't started yet
                if (player == null) return;

                double worldX = input.getMouseXWorld();
                double worldY = input.getMouseYWorld();

                //Do not mine blocks if they are too far
                if(!isWithinReach(worldX, worldY))
                    return;

                //A rectangle to detect minable blocks because looking for an Entity at a single point can be unreliable
                Rectangle2D mouseBounds = new Rectangle2D(worldX, worldY, 1, 1);

                final Entity blockToMine;
                Entity foudBlock = null;

                //Find the first minable block
                for(Entity e : FXGL.getGameWorld().getEntitiesInRange(mouseBounds)) {
                    if (e.isType(EntityType.BLOCK)) {
                        foudBlock = e;
                        break;
                    }
                }

                blockToMine = foudBlock;

                //Do not mine if not minable block is found
                if (blockToMine == null)
                    return;

                //Set the default value of mine time to 2.0 if the Entity does not have the property of "mine_time"
                double mineTime = blockToMine.getProperties().exists("mine_time") ? blockToMine.getDouble("mine_time") : 2.0;

                Config.BlockType blockType = blockToMine.getObject("type");
                String itemName = getItemNameFromBlockType(blockType);

                //Only execute after a duration of mineTime has passed
                timer = FXGL.getGameTimer().runOnceAfter(() -> {
                    spawn("item", new SpawnData(blockToMine.getX(), blockToMine.getY())
                            .put("type", itemName)
                            .put("width", 10)
                            .put("height", 10)
                            .put("count", 1));

                    int tileX = worldToTile(blockToMine.getX());
                    int tileY = worldToTile(blockToMine.getY());

                    setTerrainTile(tileX, tileY, null);
                    despawnTerrainTile(tileX, tileY);
                }, Duration.seconds(mineTime));
            }

            //Reset the timer after user release the primary button
            @Override
            protected void onActionEnd() {
                if (timer != null)
                    timer.expire();
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

                Config.BlockType placedType = resolveBlockTypeForItemName(itemToPlace.getName());
                if (placedType == null) {
                    return;
                }

                int tileX = worldToTile(snapped.getX());
                int tileY = worldToTile(snapped.getY());

                if (!isInMapBounds(tileX, tileY)) return;
                if (getTerrainTile(tileX, tileY) != null) return;

                setTerrainTile(tileX, tileY, placedType);
                if (isWithinLoadedWindow(tileX, tileY)) {
                    spawnTerrainTile(tileX, tileY, placedType);
                }

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

    @Override
    protected void onUpdate(double tpf) {
        updateLoadedTerrainWindowIfCameraMoved();
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

        FXGL.onCollisionBegin(EntityType.PLAYER, EntityType.ENEMY, (player, enemy) -> {
            double knowckbackDir = enemy.getX() > player.getX() ? -200: 200;
            double damage = enemy.getComponent(EnemyComponent.class).getDamage();

            player.getComponent(PlayerComponent.class).takeDamage(damage, knowckbackDir);
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
        terrainNoise = new TerrainNoiseGenerator(worldSeed);
        FXGL.getGameScene().clearUINodes();
        activeTerrainBlocks.clear();
        resetLoadedTerrainWindow();
        FXGL.getGameScene().getViewport().setBounds(0, 0, MAP_WIDTH_TILES * Config.TILE_SIZE, MAP_HEIGHT_TILES * Config.TILE_SIZE);
        // load world + player
        generateMap();
        // Player spawns on surface near center of the map
        int spawnTileX = findSafeSpawnTileX(MAP_WIDTH_TILES / 2);
        int spawnSurfaceY = (surfaceHeights != null) ? surfaceHeights[spawnTileX] : SURFACE_HEIGHT_TILE;
        if (isInMapBounds(spawnTileX, spawnSurfaceY) && getTerrainTile(spawnTileX, spawnSurfaceY) == null) {
            setTerrainTile(spawnTileX, spawnSurfaceY, Config.SURFACE_GRASS_BLOCK);
        }
        ensureSpawnPlatform(spawnTileX, spawnSurfaceY);
        clearSpawnArea(spawnTileX, spawnSurfaceY);
        preloadTerrainWindowAround(spawnTileX, spawnSurfaceY);
        double spawnWorldX = spawnTileX * Config.TILE_SIZE;
        double spawnWorldY = spawnSurfaceY * Config.TILE_SIZE - PLAYER_BBOX_HEIGHT - 1;
        player = spawn("player", new SpawnData(spawnWorldX, spawnWorldY));

        //Test enemy
        spawn("enemy", new SpawnData(player.getX() + 300, player.getY() - 100).put("type", EnemyType.SLIME));

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
        FXGL.getGameScene().getViewport().setZoom(1.55);
        updateLoadedTerrainWindow(true);

        initInventory();

        displayHpBar();
    }

    public void resetToMainMenu() {
        FXGL.getGameScene().getViewport().unbind();
        FXGL.getGameWorld().getEntitiesCopy().forEach(Entity::removeFromWorld);
        FXGL.getGameScene().clearUINodes();

        player = null;
        activeTerrainBlocks.clear();
        terrainMap = null;
        surfaceHeights = null;
        resetLoadedTerrainWindow();

        showMainMenu();
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

    private int worldToTile(double worldCoord) {
        return (int) Math.floor(worldCoord / Config.TILE_SIZE);
    }

    private long tileKey(int tileX, int tileY) {
        return (((long) tileX) << 32) | (tileY & 0xffffffffL);
    }

    private boolean isInMapBounds(int tileX, int tileY) {
        return tileX >= 0 && tileX < MAP_WIDTH_TILES && tileY >= 0 && tileY < MAP_HEIGHT_TILES;
    }

    private Config.BlockType getTerrainTile(int tileX, int tileY) {
        if (terrainMap == null || !isInMapBounds(tileX, tileY)) {
            return null;
        }
        return terrainMap[tileY][tileX];
    }

    private void setTerrainTile(int tileX, int tileY, Config.BlockType type) {
        if (terrainMap == null || !isInMapBounds(tileX, tileY)) {
            return;
        }
        terrainMap[tileY][tileX] = type;
    }

    private boolean isWithinLoadedWindow(int tileX, int tileY) {
        return tileX >= loadedMinTileX && tileX <= loadedMaxTileX
                && tileY >= loadedMinTileY && tileY <= loadedMaxTileY;
    }

    private Entity spawnTerrainTile(int tileX, int tileY, Config.BlockType type) {
        long key = tileKey(tileX, tileY);
        Entity existing = activeTerrainBlocks.get(key);
        if (existing != null && existing.isActive()) {
            return existing;
        }

        Entity block = spawn("block", new SpawnData(tileX * Config.TILE_SIZE, tileY * Config.TILE_SIZE).put("type", type));
        activeTerrainBlocks.put(key, block);
        return block;
    }

    private void despawnTerrainTile(int tileX, int tileY) {
        Entity block = activeTerrainBlocks.remove(tileKey(tileX, tileY));
        if (block != null && block.isActive()) {
            block.removeFromWorld();
        }
    }

    private void resetLoadedTerrainWindow() {
        loadedMinTileX = Integer.MAX_VALUE;
        loadedMaxTileX = Integer.MIN_VALUE;
        loadedMinTileY = Integer.MAX_VALUE;
        loadedMaxTileY = Integer.MIN_VALUE;
    }

    private void updateLoadedTerrainWindowIfCameraMoved() {
        updateLoadedTerrainWindow(false);
    }

    private void updateLoadedTerrainWindow(boolean force) {
        if (player == null || terrainMap == null) {
            return;
        }

        Rectangle2D visibleArea = FXGL.getGameScene().getViewport().getVisibleArea();

        int minTileX = Math.max(0, worldToTile(visibleArea.getMinX()) - ACTIVE_TERRAIN_MARGIN_TILES);
        int maxTileX = Math.min(MAP_WIDTH_TILES - 1, worldToTile(visibleArea.getMaxX()) + ACTIVE_TERRAIN_MARGIN_TILES);
        int minTileY = Math.max(0, worldToTile(visibleArea.getMinY()) - ACTIVE_TERRAIN_MARGIN_TILES);
        int maxTileY = Math.min(MAP_HEIGHT_TILES - 1, worldToTile(visibleArea.getMaxY()) + ACTIVE_TERRAIN_MARGIN_TILES);

        // Always keep a small collider-safe area around the player loaded.
        int playerTileX = worldToTile(player.getCenter().getX());
        int playerTileY = worldToTile(player.getCenter().getY());
        int safeMinTileX = Math.max(0, playerTileX - PLAYER_SAFE_LOAD_RADIUS_X);
        int safeMaxTileX = Math.min(MAP_WIDTH_TILES - 1, playerTileX + PLAYER_SAFE_LOAD_RADIUS_X);
        int safeMinTileY = Math.max(0, playerTileY - PLAYER_SAFE_LOAD_RADIUS_Y);
        int safeMaxTileY = Math.min(MAP_HEIGHT_TILES - 1, playerTileY + PLAYER_SAFE_LOAD_RADIUS_Y);

        minTileX = Math.min(minTileX, safeMinTileX);
        maxTileX = Math.max(maxTileX, safeMaxTileX);
        minTileY = Math.min(minTileY, safeMinTileY);
        maxTileY = Math.max(maxTileY, safeMaxTileY);

        final int finalMinTileX = minTileX;
        final int finalMaxTileX = maxTileX;
        final int finalMinTileY = minTileY;
        final int finalMaxTileY = maxTileY;

        if (!force
                && minTileX == loadedMinTileX && maxTileX == loadedMaxTileX
                && minTileY == loadedMinTileY && maxTileY == loadedMaxTileY) {
            return;
        }

        activeTerrainBlocks.entrySet().removeIf(entry -> {
            long key = entry.getKey();
            int tileX = (int) (key >> 32);
            int tileY = (int) key;

            boolean inside = tileX >= finalMinTileX && tileX <= finalMaxTileX
                    && tileY >= finalMinTileY && tileY <= finalMaxTileY;
            if (!inside) {
                Entity block = entry.getValue();
                if (block != null && block.isActive()) {
                    block.removeFromWorld();
                }
                return true;
            }
            return false;
        });

        for (int y = finalMinTileY; y <= finalMaxTileY; y++) {
            for (int x = finalMinTileX; x <= finalMaxTileX; x++) {
                Config.BlockType type = terrainMap[y][x];
                if (type != null) {
                    spawnTerrainTile(x, y, type);
                }
            }
        }

        loadedMinTileX = finalMinTileX;
        loadedMaxTileX = finalMaxTileX;
        loadedMinTileY = finalMinTileY;
        loadedMaxTileY = finalMaxTileY;
    }

    private void displayHpBar() {
        ProgressBar hpBar = new ProgressBar(1.0);

        hpBar.setStyle("-fx-accent: red; -fx-control-inner-background: #333333;");
        hpBar.setPrefWidth(200);
        hpBar.setPrefHeight(20);

        hpBar.setTranslateX(20);
        hpBar.setTranslateY(20);

        PlayerComponent pc = player.getComponent(PlayerComponent.class);
        hpBar.progressProperty().bind(pc.getHpProperty().divide((double) pc.getMaxHealth()));

        Label hpLabel = new Label("HP");
        hpLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14;");
        hpLabel.setTranslateX(25);
        hpLabel.setTranslateY(20);

        FXGL.getGameScene().addUINode(hpBar);
        FXGL.getGameScene().addUINode(hpLabel);
    }

    // Convert BlockType to item name string for inventory
    private String getItemNameFromBlockType(Config.BlockType blockType) {
        String name = blockType.toString();

        if (name.contains("BED_ROCK")) return "Bedrock";
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

        return "Block";
    }

    private Config.BlockType resolveBlockTypeForItemName(String itemName) {
        if (itemName == null) {
            return null;
        }

        String normalized = itemName.trim().toLowerCase();

        return switch (normalized) {
            case "grass" -> Config.SURFACE_GRASS_BLOCK;
            case "dirt" -> Config.DEFAULT_DIRT_BLOCK;
            case "stone" -> Config.DEFAULT_STONE_BLOCK;
            case "coal" -> Config.BlockType.COAL_BLOCK_1;
            case "iron" -> Config.BlockType.IRON_BLOCK_1;
            case "diamond" -> Config.BlockType.DIAMOND_BLOCK_1;
            case "gold" -> Config.BlockType.GOLD_BLOCK;
            case "emerald" -> Config.BlockType.EMERALD_BLOCK;
            case "lapis" -> Config.BlockType.LAPIS_LAZULI_BLOCK;
            case "wood" -> Config.DEFAULT_TREE_TRUNK_BLOCK;
            case "leaves" -> Config.DEFAULT_LEAVES_BLOCK;
            default -> null;
        };
    }

    public void generateMap() {
        int mapWidth = MAP_WIDTH_TILES;
        int mapHeight = MAP_HEIGHT_TILES;

        terrainMap = new Config.BlockType[mapHeight][mapWidth];
        surfaceHeights = new int[mapWidth];
        activeTerrainBlocks.clear();
        resetLoadedTerrainWindow();

        if (terrainNoise == null) {
            terrainNoise = new TerrainNoiseGenerator(worldSeed);
        }

        if (USE_NOISE_TERRAIN_GENERATION) {
            generateNoiseTerrain(mapWidth, mapHeight);
        } else {
            generateLegacyTerrain(mapWidth, mapHeight, SURFACE_HEIGHT_TILE);
        }

        // Generate trees scattered across the surface (on top of the grass)
        generateTrees(mapWidth);
    }

    private void generateNoiseTerrain(int mapWidth, int mapHeight) {
        int minSurfaceY = 10;
        int maxSurfaceY = mapHeight - 14;
        int amplitude = 6;

        for (int x = 0; x < mapWidth; x++) {
            int surfaceY = terrainNoise.surfaceY(x, SURFACE_HEIGHT_TILE, amplitude);
            surfaceY = clampInt(surfaceY, minSurfaceY, maxSurfaceY);
            surfaceHeights[x] = surfaceY;

            for (int y = surfaceY; y < mapHeight; y++) {
                int depth = y - surfaceY;
                terrainMap[y][x] = selectLayeredBlock(x, y, depth, mapHeight);
            }
        }
    }

    private void generateLegacyTerrain(int mapWidth, int mapHeight, int surfaceHeight) {
        for (int x = 0; x < mapWidth; x++) {
            surfaceHeights[x] = surfaceHeight;

            for (int y = surfaceHeight; y < mapHeight; y++) {
                int depth = y - surfaceHeight;
                terrainMap[y][x] = selectLayeredBlock(x, y, depth, mapHeight);
            }
        }
    }

    private Config.BlockType selectLayeredBlock(int x, int y, int depth, int mapHeight) {
        if (y >= mapHeight - 2) {
            return Config.BlockType.DARK_BED_ROCK_1;
        }

        if (depth == 0) {
            return Config.SURFACE_GRASS_BLOCK;
        }

        if (depth <= 2) {
            return Config.DEFAULT_DIRT_BLOCK;
        }

        double oreRoll = terrainNoise.coordinateRandom01(x, y, 17);
        double strataBias = terrainNoise.fbm(x * 0.08 + y * 0.01, 2, 2.0, 0.5);

        if (depth < 14) {
            double ironChance = 0.05 + Math.max(0.0, strataBias) * 0.03;
            double coalChance = 0.10 + Math.max(0.0, -strataBias) * 0.04;

            if (oreRoll < ironChance) return Config.BlockType.IRON_BLOCK_1;
            if (oreRoll < coalChance) return Config.BlockType.COAL_BLOCK_1;
            return Config.DEFAULT_STONE_BLOCK;
        }

        if (depth < 30) {
            if (oreRoll < 0.03) return Config.BlockType.DIAMOND_BLOCK_1;
            if (oreRoll < 0.07) return Config.BlockType.GOLD_BLOCK;
            if (oreRoll < 0.12) return Config.BlockType.COAL_BLOCK_1;
            return Config.DEFAULT_STONE_BLOCK;
        }

        if (depth < 44) {
            if (oreRoll < 0.02) return Config.BlockType.EMERALD_BLOCK;
            if (oreRoll < 0.04) return Config.BlockType.LAPIS_LAZULI_BLOCK;
            if (oreRoll < 0.06) return Config.BlockType.DIAMOND_BLOCK_1;
            return Config.DEFAULT_STONE_BLOCK;
        }

        return Config.DEFAULT_STONE_BLOCK;
    }

    private int clampInt(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    private int findSafeSpawnTileX(int preferredX) {
        int clampedPreferred = clampInt(preferredX, 0, MAP_WIDTH_TILES - 1);

        if (isSpawnColumnClear(clampedPreferred)) {
            return clampedPreferred;
        }

        int maxRadius = Math.max(clampedPreferred, MAP_WIDTH_TILES - 1 - clampedPreferred);
        for (int radius = 1; radius <= maxRadius; radius++) {
            int left = clampedPreferred - radius;
            if (left >= 0 && isSpawnColumnClear(left)) {
                return left;
            }

            int right = clampedPreferred + radius;
            if (right < MAP_WIDTH_TILES && isSpawnColumnClear(right)) {
                return right;
            }
        }

        return clampedPreferred;
    }

    private boolean isSpawnColumnClear(int tileX) {
        if (surfaceHeights == null || tileX < 0 || tileX >= surfaceHeights.length) {
            return false;
        }

        int surfaceY = surfaceHeights[tileX];
        int feetY = surfaceY - 1;
        int bodyY = surfaceY - 2;
        int headY = surfaceY - 3;

        if (!isInMapBounds(tileX, feetY) || !isInMapBounds(tileX, bodyY) || !isInMapBounds(tileX, headY)) {
            return false;
        }

        Config.BlockType surfaceBlock = getTerrainTile(tileX, surfaceY);
        boolean hasValidSurface = surfaceBlock != null;
        if (!hasValidSurface) {
            return false;
        }

        // Avoid spawn columns occupied by tree trunk/leaves around player capsule.
        for (int x = tileX - 1; x <= tileX + 1; x++) {
            for (int y = headY; y <= feetY; y++) {
                if (!isInMapBounds(x, y)) {
                    continue;
                }
                Config.BlockType t = getTerrainTile(x, y);
                if (t != null) {
                    String n = t.name();
                    if (n.contains("TREE") || n.contains("LEAF")) {
                        return false;
                    }
                }
            }
        }

        return getTerrainTile(tileX, feetY) == null
                && getTerrainTile(tileX, bodyY) == null
                && getTerrainTile(tileX, headY) == null;
    }

    private void clearSpawnArea(int centerTileX, int surfaceY) {
        // Ensure spawn tile and immediate side tiles are free for player capsule and movement start.
        for (int x = centerTileX - 1; x <= centerTileX + 1; x++) {
            for (int y = surfaceY - 3; y <= surfaceY - 1; y++) {
                if (!isInMapBounds(x, y)) {
                    continue;
                }
                setTerrainTile(x, y, null);
                despawnTerrainTile(x, y);
            }
        }
    }

    private void ensureSpawnPlatform(int centerTileX, int surfaceY) {
        for (int x = centerTileX - 1; x <= centerTileX + 1; x++) {
            if (!isInMapBounds(x, surfaceY)) {
                continue;
            }

            if (getTerrainTile(x, surfaceY) == null) {
                setTerrainTile(x, surfaceY, Config.SURFACE_GRASS_BLOCK);
            }
            if (isInMapBounds(x, surfaceY + 1) && getTerrainTile(x, surfaceY + 1) == null) {
                setTerrainTile(x, surfaceY + 1, Config.DEFAULT_DIRT_BLOCK);
            }
        }
    }

    private void preloadTerrainWindowAround(int centerTileX, int centerTileY) {
        int halfWindowX = VIEW_WIDTH_TILES / 2 + ACTIVE_TERRAIN_MARGIN_TILES;
        int halfWindowY = VIEW_HEIGHT_TILES / 2 + ACTIVE_TERRAIN_MARGIN_TILES;

        int minTileX = Math.max(0, centerTileX - halfWindowX);
        int maxTileX = Math.min(MAP_WIDTH_TILES - 1, centerTileX + halfWindowX);
        int minTileY = Math.max(0, centerTileY - halfWindowY);
        int maxTileY = Math.min(MAP_HEIGHT_TILES - 1, centerTileY + halfWindowY);

        for (int y = minTileY; y <= maxTileY; y++) {
            for (int x = minTileX; x <= maxTileX; x++) {
                Config.BlockType type = terrainMap[y][x];
                if (type != null) {
                    spawnTerrainTile(x, y, type);
                }
            }
        }

        loadedMinTileX = minTileX;
        loadedMaxTileX = maxTileX;
        loadedMinTileY = minTileY;
        loadedMaxTileY = maxTileY;
    }

    public int getActiveTerrainBlockCount() {
        return activeTerrainBlocks.size();
    }

    public int getTotalTerrainBlockCount() {
        if (terrainMap == null) {
            return 0;
        }

        int total = 0;
        for (int y = 0; y < terrainMap.length; y++) {
            for (int x = 0; x < terrainMap[y].length; x++) {
                if (terrainMap[y][x] != null) {
                    total++;
                }
            }
        }
        return total;
    }

    private void generateTrees(int mapWidth) {
        // Trees spawn at moderate density with variable trunk height and fuller canopies.
        for (int x = 0; x < mapWidth; x += 9) {
            int randomOffset = (int) (terrainNoise.coordinateRandom01(x, 0, 91) * 9);
            int treeX = x + randomOffset;
            if (treeX >= mapWidth) continue;

            if (terrainNoise.coordinateRandom01(treeX, 1, 92) < 0.52) {
                placeTreeAt(treeX);
            }
        }
    }

    private void placeTreeAt(int treeX) {
        int surfaceY = surfaceHeights[treeX];
        int trunkHeight = 4 + (int) (terrainNoise.coordinateRandom01(treeX, surfaceY, 93) * 3.0); // 4..6

        int trunkTopY = surfaceY - trunkHeight;
        if (!isInMapBounds(treeX, trunkTopY - 2) || !isInMapBounds(treeX, surfaceY - 1)) {
            return;
        }

        for (int y = surfaceY - 1; y >= trunkTopY; y--) {
            if (getTerrainTile(treeX, y) != null) {
                return;
            }
        }

        for (int y = surfaceY - 1; y >= trunkTopY; y--) {
            Config.BlockType trunkType = (y == trunkTopY) ? Config.BlockType.OAK_TREE_TOP : Config.DEFAULT_TREE_TRUNK_BLOCK;
            setTerrainTile(treeX, y, trunkType);
        }

        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 1; dy++) {
                int leafX = treeX + dx;
                int leafY = trunkTopY + dy;

                if (!isInMapBounds(leafX, leafY)) {
                    continue;
                }
                if (getTerrainTile(leafX, leafY) != null) {
                    continue;
                }

                double shapeRoll = terrainNoise.coordinateRandom01(leafX, leafY, 94);
                int manhattanDistance = Math.abs(dx) + Math.abs(dy);

                boolean shouldPlaceLeaf =
                        manhattanDistance <= 2
                        || (manhattanDistance == 3 && shapeRoll > 0.25)
                        || (dy == -2 && Math.abs(dx) <= 1 && shapeRoll > 0.1);

                if (shouldPlaceLeaf) {
                    setTerrainTile(leafX, leafY, Config.DEFAULT_LEAVES_BLOCK);
                }
            }
        }
    }

    public long getWorldSeed() {
        return worldSeed;
    }

    public void setWorldSeed(long worldSeed) {
        this.worldSeed = worldSeed;
        this.terrainNoise = new TerrainNoiseGenerator(worldSeed);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
