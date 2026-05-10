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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.almasb.fxgl.dsl.FXGL.*;

public class GameApp extends GameApplication {

    private Entity player, background;
    private final Map<Long, Entity> activeTerrainBlocks = new HashMap<>();
    private final Map<Integer, Config.BlockType[]> terrainColumns = new HashMap<>();
    private final Map<Integer, Integer> surfaceHeights = new HashMap<>();
    private final Set<Integer> generatedColumns = new HashSet<>();
    private final Set<Integer> generatedTreeColumns = new HashSet<>();
    private final Set<Integer> generatedTunnelRegions = new HashSet<>();

    private static final int MAP_HEIGHT_TILES = 80;
    private static final int VIEW_WIDTH_TILES = 64;
    private static final int VIEW_HEIGHT_TILES = 40;
    private static final int ACTIVE_TERRAIN_MARGIN_TILES = 1;
    private static final int SURFACE_HEIGHT_TILE = 20;
    private static final int PLAYER_BBOX_HEIGHT = 19;
    private static final int PLAYER_SAFE_LOAD_RADIUS_X = 10;
    private static final int PLAYER_SAFE_LOAD_RADIUS_Y = 8;
    private static final int TUNNEL_REGION_WIDTH = 32;

    private int loadedMinTileX = Integer.MAX_VALUE;
    private int loadedMaxTileX = Integer.MIN_VALUE;
    private int loadedMinTileY = Integer.MAX_VALUE;
    private int loadedMaxTileY = Integer.MIN_VALUE;

    private long worldSeed = 1337L;
    private TerrainNoiseGenerator terrainNoise;

    // UI roots
    // ADD:
    Runnable refreshAll = this::refreshAll;
    private final SlotSelectionState selectionState = new SlotSelectionState();
    private final InventoryUI inventoryUI = new InventoryUI(selectionState, refreshAll);
    private final HotbarUI    hotbarUI    = new HotbarUI(selectionState, refreshAll);
    private final ArmorUI     armorUI     = new ArmorUI(selectionState, refreshAll);
    private CraftingMenu craftingMenu;

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
                if (selectionState.isEmpty()) return;          // ← changed

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

                PlayerComponent pc = player.getComponent(PlayerComponent.class);

                // ↓ changed: read from selectionState instead of selectedSlotIndex/Type
                List<InventoryItem> sourceList = switch (selectionState.getSlotType()) {
                    case "hotbar" -> pc.getHotbar();
                    case "armor"  -> pc.getArmor();
                    default       -> pc.getInventory();
                };
                InventoryItem itemToPlace = sourceList.get(selectionState.getIndex());

                if (itemToPlace == null) return;

                Config.BlockType placedType = resolveBlockTypeForItemName(itemToPlace.getName());
                if (placedType == null) return;

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
                    sourceList.set(selectionState.getIndex(), null);
                    selectionState.clear();                    // ← changed
                }

                refreshAll();
            }
        }, MouseButton.SECONDARY);



        for (int i = 0; i < 10; i++) {
            final int slot = i;
            KeyCode key = KeyCode.getKeyCode(String.valueOf(i == 9 ? 0 : i + 1));
            input.addAction(new UserAction("Hotbar " + slot) {
                @Override
                protected void onActionBegin() {
                    if (player == null) return;
                    player.getComponent(PlayerComponent.class).setSelectedHotbarSlot(slot);
                    if (hotbarUI != null) hotbarUI.refresh();  // ← changed
                }
            }, key);
        }

        // Toggle Inventory + Armor — E
        input.addAction(new UserAction("Toggle Inventory") {
            @Override
            protected void onActionBegin() {
                boolean visible = inventoryUI.toggle();
                armorUI.setVisible(visible);
                if (!visible) {
                    selectionState.clear();
                    refreshAll();
                }
            }
        }, KeyCode.E);
        input.addAction(new UserAction("Toggle Crafting Menu") {
            @Override
            protected void onActionBegin() {
                craftingMenu.toggle();
            }
        }, KeyCode.C);

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



    private void refreshAll() {
        inventoryUI.refresh();
        hotbarUI.refresh();
        armorUI.refresh();
        if (craftingMenu != null) craftingMenu.refresh();
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
        FXGL.getGameScene().getViewport().setBounds(-1_000_000, 0, 1_000_000, MAP_HEIGHT_TILES * Config.TILE_SIZE);
        // load world + player
        generateMap();
        // Player spawns on surface near the world origin.
        int spawnTileX = findSafeSpawnTileX(0);
        int spawnSurfaceY = surfaceHeights.getOrDefault(spawnTileX, SURFACE_HEIGHT_TILE);
        if (isInMapBounds(spawnTileX, spawnSurfaceY) && getTerrainTile(spawnTileX, spawnSurfaceY) == null) {
            setTerrainTile(spawnTileX, spawnSurfaceY, Config.SURFACE_GRASS_BLOCK);
        }
        ensureSpawnPlatform(spawnTileX, spawnSurfaceY);
        clearSpawnArea(spawnTileX, spawnSurfaceY);
        preloadTerrainWindowAround(spawnTileX, spawnSurfaceY);
        double spawnWorldX = spawnTileX * Config.TILE_SIZE;
        double spawnWorldY = spawnSurfaceY * Config.TILE_SIZE - PLAYER_BBOX_HEIGHT - 1;
        player = spawn("player", new SpawnData(spawnWorldX, spawnWorldY));

        inventoryUI.init();
        inventoryUI.setPlayer(player);

        armorUI.init();
        armorUI.setPlayer(player);

        hotbarUI.init();
        hotbarUI.setPlayer(player);
        craftingMenu = new CraftingMenu(player);
        craftingMenu.init();

        //Test enemy
        spawn("enemy", new SpawnData(player.getX() + 100, player.getY() - 50).put("type", EnemyType.SLIME));

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



        displayHpBar();
    }

    public void resetToMainMenu() {
        FXGL.getGameScene().getViewport().unbind();
        FXGL.getGameWorld().getEntitiesCopy().forEach(Entity::removeFromWorld);
        FXGL.getGameScene().clearUINodes();

        player = null;
        activeTerrainBlocks.clear();
        terrainColumns.clear();
        surfaceHeights.clear();
        generatedColumns.clear();
        generatedTreeColumns.clear();
        generatedTunnelRegions.clear();
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
        return tileY >= 0 && tileY < MAP_HEIGHT_TILES;
    }

    private Config.BlockType getTerrainTile(int tileX, int tileY) {
        if (!isInMapBounds(tileX, tileY)) {
            return null;
        }

        ensureColumnGenerated(tileX);
        Config.BlockType[] column = terrainColumns.get(tileX);
        return column == null ? null : column[tileY];
    }

    private void setTerrainTile(int tileX, int tileY, Config.BlockType type) {
        if (!isInMapBounds(tileX, tileY)) {
            return;
        }

        if (type == null && !terrainColumns.containsKey(tileX)) {
            return;
        }

        ensureColumnGenerated(tileX);
        Config.BlockType[] column = terrainColumns.get(tileX);
        if (column != null) {
            column[tileY] = type;
        }
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
        if (player == null) {
            return;
        }

        Rectangle2D visibleArea = FXGL.getGameScene().getViewport().getVisibleArea();

        int minTileX = Math.max(0, worldToTile(visibleArea.getMinX()) - ACTIVE_TERRAIN_MARGIN_TILES);
        int maxTileX = worldToTile(visibleArea.getMaxX()) + ACTIVE_TERRAIN_MARGIN_TILES;
        int minTileY = Math.max(0, worldToTile(visibleArea.getMinY()) - ACTIVE_TERRAIN_MARGIN_TILES);
        int maxTileY = Math.min(MAP_HEIGHT_TILES - 1, worldToTile(visibleArea.getMaxY()) + ACTIVE_TERRAIN_MARGIN_TILES);

        // Always keep a small collider-safe area around the player loaded.
        int playerTileX = worldToTile(player.getCenter().getX());
        int playerTileY = worldToTile(player.getCenter().getY());
        int safeMinTileX = playerTileX - PLAYER_SAFE_LOAD_RADIUS_X;
        int safeMaxTileX = playerTileX + PLAYER_SAFE_LOAD_RADIUS_X;
        int safeMinTileY = Math.max(0, playerTileY - PLAYER_SAFE_LOAD_RADIUS_Y);
        int safeMaxTileY = Math.min(MAP_HEIGHT_TILES - 1, playerTileY + PLAYER_SAFE_LOAD_RADIUS_Y);

        minTileX = Math.min(minTileX, safeMinTileX);
        maxTileX = Math.max(maxTileX, safeMaxTileX);
        minTileY = Math.min(minTileY, safeMinTileY);
        maxTileY = Math.max(maxTileY, safeMaxTileY);

        ensureColumnsGeneratedInRange(minTileX, maxTileX);

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
                Config.BlockType type = getTerrainTile(x, y);
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
        terrainColumns.clear();
        surfaceHeights.clear();
        generatedColumns.clear();
        generatedTreeColumns.clear();
        generatedTunnelRegions.clear();
        activeTerrainBlocks.clear();
        resetLoadedTerrainWindow();

        if (terrainNoise == null) {
            terrainNoise = new TerrainNoiseGenerator(worldSeed);
        }
    }

    private void ensureColumnsGeneratedInRange(int minTileX, int maxTileX) {
        for (int x = minTileX; x <= maxTileX; x++) {
            ensureColumnGenerated(x);
        }
    }

    private void ensureColumnGenerated(int tileX) {
        if (generatedColumns.contains(tileX)) {
            return;
        }

        Config.BlockType[] column = generateColumnBase(tileX);
        terrainColumns.put(tileX, column);
        generatedColumns.add(tileX);

        maybeGenerateTreeAt(tileX);

        int region = Math.floorDiv(tileX, TUNNEL_REGION_WIDTH);
        generateTunnelRegionIfNeeded(region);
    }

    private Config.BlockType[] generateColumnBase(int x) {
        Config.BlockType[] column = new Config.BlockType[MAP_HEIGHT_TILES];
        int minSurfaceY = 10;
        int maxSurfaceY = MAP_HEIGHT_TILES - 14;
        int amplitude = 6;

        int surfaceY = terrainNoise.surfaceY(x, SURFACE_HEIGHT_TILE, amplitude);
        surfaceY = clampInt(surfaceY, minSurfaceY, maxSurfaceY);
        surfaceHeights.put(x, surfaceY);

        for (int y = surfaceY; y < MAP_HEIGHT_TILES; y++) {
            int depth = y - surfaceY;
            Config.BlockType block = selectLayeredBlock(x, y, depth);

            if (block != null && depth > 4 && y < MAP_HEIGHT_TILES - 3) {
                double caveNoise = terrainNoise.fbm2D(x * 0.065, y * 0.065, 4, 2.0, 0.5);
                if (caveNoise > 0.50) {
                    block = null;
                }
            }

            column[y] = block;
        }

        return column;
    }

    private Config.BlockType selectLayeredBlock(int x, int y, int depth) {
        if (y >= MAP_HEIGHT_TILES - 2) {
            return Config.BlockType.DARK_BED_ROCK_1;
        }

        if (depth == 0) {
            return Config.SURFACE_GRASS_BLOCK;
        }

        if (depth <= 2) {
            return Config.DEFAULT_DIRT_BLOCK;
        }

        Config.BlockType ore = selectOreBlock(x, y, depth);
        return ore != null ? ore : Config.DEFAULT_STONE_BLOCK;
    }

    private Config.BlockType selectOreBlock(int x, int y, int depth) {
        // Higher frequencies produce tighter ore clusters.
        double coalNoise = terrainNoise.fbm2D(x * 0.24, y * 0.24, 3, 2.0, 0.5);
        if (depth > 4 && coalNoise > 0.18) return Config.BlockType.COAL_BLOCK_1;

        double ironNoise = terrainNoise.fbm2D(x * 0.20, y * 0.20, 3, 2.0, 0.5);
        if (depth > 8 && ironNoise > 0.32) return Config.BlockType.IRON_BLOCK_1;

        double goldNoise = terrainNoise.fbm2D(x * 0.16, y * 0.16, 3, 2.0, 0.5);
        if (depth > 18 && goldNoise > 0.52) return Config.BlockType.GOLD_BLOCK;

        double lapisNoise = terrainNoise.fbm2D(x * 0.15, y * 0.15, 3, 2.0, 0.5);
        if (depth > 20 && lapisNoise > 0.56) return Config.BlockType.LAPIS_LAZULI_BLOCK;

        double emeraldNoise = terrainNoise.fbm2D(x * 0.14, y * 0.14, 3, 2.0, 0.5);
        if (depth > 24 && emeraldNoise > 0.64) return Config.BlockType.EMERALD_BLOCK;

        double diamondNoise = terrainNoise.fbm2D(x * 0.12, y * 0.12, 4, 2.0, 0.5);
        if (depth > 28 && diamondNoise > 0.80) return Config.BlockType.DIAMOND_BLOCK_1;

        return null;
    }

    private void generateTunnelRegionIfNeeded(int regionX) {
        if (generatedTunnelRegions.contains(regionX)) {
            return;
        }
        generatedTunnelRegions.add(regionX);

        int regionStartX = regionX * TUNNEL_REGION_WIDTH;
        int regionEndX = regionStartX + TUNNEL_REGION_WIDTH - 1;

        for (int x = regionStartX; x <= regionEndX; x++) {
            if (!generatedColumns.contains(x)) {
                Config.BlockType[] column = generateColumnBase(x);
                terrainColumns.put(x, column);
                generatedColumns.add(x);
                maybeGenerateTreeAt(x);
            }
        }

        int walkers = 5;
        for (int i = 0; i < walkers; i++) {
            int startX = regionStartX + (int) (terrainNoise.coordinateRandom01(regionX, i, 201) * TUNNEL_REGION_WIDTH);
            int baseSurface = surfaceHeights.getOrDefault(startX, SURFACE_HEIGHT_TILE);
            int startY = clampInt(baseSurface + 10 + (int) (terrainNoise.coordinateRandom01(regionX, i, 202) * 20), 12, MAP_HEIGHT_TILES - 8);
            int life = 45 + (int) (terrainNoise.coordinateRandom01(regionX, i, 203) * 45);
            carveTunnelWalker(startX, startY, life, 1);
        }

    }

    private void carveTunnelWalker(int startX, int startY, int life, int digRadius) {
        int x = startX;
        int y = startY;

        for (int step = 0; step < life; step++) {
            for (int dx = -digRadius; dx <= digRadius; dx++) {
                for (int dy = -digRadius; dy <= digRadius; dy++) {
                    int tx = x + dx;
                    int ty = y + dy;
                    if (isInMapBounds(tx, ty) && ty > 4 && ty < MAP_HEIGHT_TILES - 2) {
                        setTerrainTile(tx, ty, null);
                    }
                }
            }

            double roll = terrainNoise.coordinateRandom01(x, y, 300 + step);
            if (roll < 0.34) x++;
            else if (roll < 0.68) x--;
            else if (roll < 0.84) y++;
            else y--;

            y = clampInt(y, 8, MAP_HEIGHT_TILES - 6);
            ensureColumnGenerated(x);
        }
    }

    private int clampInt(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    private int findSafeSpawnTileX(int preferredX) {
        int clampedPreferred = preferredX;

        if (isSpawnColumnClear(clampedPreferred)) {
            return clampedPreferred;
        }

        for (int radius = 1; radius <= 256; radius++) {
            int left = clampedPreferred - radius;
            if (isSpawnColumnClear(left)) {
                return left;
            }

            int right = clampedPreferred + radius;
            if (isSpawnColumnClear(right)) {
                return right;
            }
        }

        return clampedPreferred;
    }

    private boolean isSpawnColumnClear(int tileX) {
        ensureColumnGenerated(tileX);
        Integer surfaceYObj = surfaceHeights.get(tileX);
        if (surfaceYObj == null) {
            return false;
        }

        int surfaceY = surfaceYObj;
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

        int minTileX = centerTileX - halfWindowX;
        int maxTileX = centerTileX + halfWindowX;
        int minTileY = Math.max(0, centerTileY - halfWindowY);
        int maxTileY = Math.min(MAP_HEIGHT_TILES - 1, centerTileY + halfWindowY);

        ensureColumnsGeneratedInRange(minTileX, maxTileX);

        for (int y = minTileY; y <= maxTileY; y++) {
            for (int x = minTileX; x <= maxTileX; x++) {
                Config.BlockType type = getTerrainTile(x, y);
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
        if (terrainColumns.isEmpty()) {
            return 0;
        }

        int total = 0;
        for (Config.BlockType[] column : terrainColumns.values()) {
            if (column == null) continue;
            for (int y = 0; y < column.length; y++) {
                if (column[y] != null) {
                    total++;
                }
            }
        }
        return total;
    }

    private void maybeGenerateTreeAt(int treeX) {
        if (generatedTreeColumns.contains(treeX)) {
            return;
        }
        generatedTreeColumns.add(treeX);

        if (terrainNoise.coordinateRandom01(treeX, 1, 92) < 0.52) {
            placeTreeAt(treeX);
        }
    }

    private void placeTreeAt(int treeX) {
        int surfaceY = surfaceHeights.getOrDefault(treeX, SURFACE_HEIGHT_TILE);
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
