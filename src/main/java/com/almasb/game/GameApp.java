package com.almasb.game;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.input.Input;
import com.almasb.fxgl.input.UserAction;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import com.almasb.fxgl.time.TimerAction;
import javafx.geometry.Rectangle2D;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import javafx.scene.control.Button;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import javafx.scene.control.TextField;

import static com.almasb.fxgl.dsl.FXGL.*;

public class GameApp extends GameApplication {

    private Entity player, background;
    private GridPane inventoryRoot;
    private int selectedSlotIndex= -1;

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(16 * 80);
        settings.setHeight(16 * 60);
    }

    @Override
    protected void initInput() {
        Input input = FXGL.getInput();

        input.addAction(new UserAction("Move Left") {
            @Override
            protected void onAction() {
                if (player != null) {
                    player.getComponent(PlayerComponent.class).left();
                }
            }
            @Override
            protected void onActionEnd() {
                if (player.getComponent(PlayerComponent.class).isGrounded()) {
                    player.getComponent(PlayerComponent.class).stop();
                }
            }
        }, KeyCode.A);

        input.addAction(new UserAction("Move Right") {
            @Override
            protected void onAction() {
                if (player != null) {
                    player.getComponent(PlayerComponent.class).right();
                }
            }

            @Override
            protected void onActionEnd() {
                if (player.getComponent(PlayerComponent.class).isGrounded()) {
                    player.getComponent(PlayerComponent.class).stop();
                }
            }
        }, KeyCode.D);

        input.addAction(new UserAction("Jump") {
            @Override
            protected void onAction() {
                if (player != null) {
                    player.getComponent(PlayerComponent.class).jump();
                }
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

                double maxRange = 2 * 16;
                double distance = player.getCenter().distance(worldX, worldY);

                //Do not mine blocks if they are too far
                if(distance > maxRange)
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

                //Only execute after a duration of mineTime has passed
                timer = FXGL.getGameTimer().runOnceAfter(() -> {
                    spawn("item", new SpawnData(blockToMine.getX(), blockToMine.getY())
                            .put("width", 10)
                            .put("height", 10)
                            .put("count", 1)
                            .put("color", Color.BROWN));
                    blockToMine.removeFromWorld();
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
                //Ignore if the game hasn't started yet
                if (player == null) return;

                //Do nothing if no inventory slot is selected
                if (selectedSlotIndex == -1)
                    return;

                double worldX = input.getMouseXWorld();
                double worldY = input.getMouseYWorld();

                double maxRange = 2 * 16;
                double distance = player.getCenter().distance(worldX, worldY);

                //Do nothing if the distance is greater than the range allowed
                if (distance > maxRange)
                    return;

                Rectangle2D mouseBounds = new Rectangle2D(worldX, worldY, 1, 1);

                //Do not allow to place a block if there is already a block placed there
                for(Entity e : FXGL.getGameWorld().getEntitiesInRange(mouseBounds)) {
                    if (e.isType(EntityType.BLOCK))
                        return;
                }

                //Find the snapped to grid coordinates on the map to perfectly place the block
                int row = (int) Math.floor(worldX/16);
                int col = (int) Math.floor(worldY/16);
                int snappedX = row * 16;
                int snappedY = col * 16;

                Rectangle2D targetCell = new Rectangle2D(snappedX, snappedY, 16, 16);
                Rectangle2D playerBounds = new Rectangle2D(player.getX(), player.getY(), player.getWidth(), player.getHeight());

                //Do not allow to place the block on the same block the player is standing on
                if (targetCell.intersects(playerBounds))
                    return;

                List<InventoryItem> inv = player.getComponent(PlayerComponent.class).getInventory();
                InventoryItem itemToPlace = inv.get(selectedSlotIndex);

                //If the inventory slot selected do not have an item
                if(itemToPlace == null)
                    return;

                String itemName = itemToPlace.getName().toLowerCase();
                String spawnType = "";

                if (itemName.contains("grass"))
                    spawnType = "grass";
                else if (itemName.contains("stone"))
                    spawnType = "stone";
                else
                    return;

                FXGL.spawn(spawnType, new SpawnData(snappedX, snappedY).put("width", 16).put("height", 16));

                //Decrement the blocks from inventory
                itemToPlace.setCount(itemToPlace.getCount() - 1);
                if(itemToPlace.getCount() <= 0) {
                    inv.set(selectedSlotIndex, null);
                    selectedSlotIndex = -1;
                }

                refreshInventory();
            }
        }, MouseButton.SECONDARY);

        input.addAction(new UserAction("Toggle Inventory") {
            @Override
            protected void onActionBegin() {
                inventoryRoot.setVisible(!inventoryRoot.isVisible());
            }
        }, KeyCode.E);

        input.addAction(new UserAction("Get grass") {
            @Override
            protected void onActionBegin() {
                InventoryItem invItem = new InventoryItem("grass block", 1, new Rectangle(10,10, Color.GREEN));
                player.getComponent(PlayerComponent.class).addItem(invItem);
                refreshInventory();
            }
        }, KeyCode.F);

        input.addAction(new UserAction("Get stone") {
            @Override
            protected void onActionBegin() {
                spawn("item",new SpawnData(player.getX()+30,player.getY()-20)
                        .put("width",10)
                        .put("height",10)
                        .put("count",1)
                        .put("color",Color.BLUE));
            }
        }, KeyCode.G);
    }

    @Override
    protected void initUI() {
        showMainMenu();
    }

    @Override
    protected void initPhysics() {
        FXGL.onCollisionBegin(EntityType.PLAYER, EntityType.ITEM, (player, item) -> {
            String itemName = item.getComponent(ItemComponent.class).getName();
            int itemCount = item.getComponent(ItemComponent.class).getCount();
            Rectangle itemIcon = item.getComponent(ItemComponent.class).getIcon();

            InventoryItem invItem = new InventoryItem(itemName, itemCount, itemIcon);
            player.getComponent(PlayerComponent.class).addItem(invItem);

            item.removeFromWorld();
            refreshInventory();
        });
    }

    private StackPane createSlot(int size, int index) {
        StackPane slot = new StackPane();
        slot.setPrefSize(size, size);
        slot.setStyle("-fx-border-color: gray; -fx-border-width: 2; -fx-background-color: #555;");

        slot.setOnMouseClicked(e -> {
            List<InventoryItem> inv = player.getComponent(PlayerComponent.class).getInventory();

            if (selectedSlotIndex == -1) {
                // Pehla click — slot select karo
                if (inv.get(index) != null) {
                    selectedSlotIndex = index;
                    slot.setStyle("-fx-border-color: yellow; -fx-border-width: 2; -fx-background-color: #555;");
                }
            } else {
                // Doosra click — swap karo
                InventoryItem temp = inv.get(selectedSlotIndex);
                inv.set(selectedSlotIndex, inv.get(index));
                inv.set(index, temp);

                selectedSlotIndex = -1;
                refreshInventory();
            }
        });

        List<InventoryItem> inv = player.getComponent(PlayerComponent.class).getInventory();
        if (inv.get(index) != null) {
            InventoryItem item = inv.get(index);

            //ImageView icon = new ImageView(item.getIcon());
            Rectangle icon = (Rectangle) item.getIcon();
            icon.setWidth(size - 8);
            icon.setHeight(size - 8);

            Label countLabel = new Label(String.valueOf(item.getCount()));
            countLabel.setStyle("-fx-text-fill: white; -fx-font-size: 10;");
            StackPane.setAlignment(countLabel, Pos.BOTTOM_RIGHT);

            slot.getChildren().addAll(icon, countLabel);
        }

        return slot;
    }

    private void initInventory(){

        inventoryRoot = new GridPane(4,4);
        inventoryRoot.setHgap(4);
        inventoryRoot.setVgap(4);
        inventoryRoot.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-padding: 10;");

        int COLS = 10;
        int ROWS = 4;
        int SLOT_SIZE = 50;

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                StackPane slot = createSlot(SLOT_SIZE, row * COLS + col);
                inventoryRoot.add(slot, col, row);
            }
        }

        inventoryRoot.setTranslateX(25);
        inventoryRoot.setTranslateY(25);
        inventoryRoot.setVisible(false);

        getGameScene().addUINode(inventoryRoot);
    }

    private void refreshInventory() {
        inventoryRoot.getChildren().clear();

        int COLS = 10;
        int ROWS = 4;
        int SLOT_SIZE = 50;

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                inventoryRoot.add(createSlot(SLOT_SIZE, row * COLS + col), col, row);
            }
        }
    }

    private void showMainMenu() {
        FXGL.getGameWorld().addEntityFactory(new GameFactory());
        background= spawn("menu background");
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
            String name = nameField.getText();

            // Save character (file / memory)
            saveCharacter(name);

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
        List<String> characters = loadCharacters();

        for (String name : characters) {
            Button btn = new Button(name);

            btn.setOnAction(e -> {
                showWorldSelection(name);
            });

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

        world1.setOnAction(e -> {
            startGame(characterName, "world1");
        });

        menu.getChildren().add(world1);
        FXGL.getGameScene().addUINode(menu);
    }

    private void startGame(String character, String world) {
        FXGL.getGameScene().clearUINodes();
        FXGL.getGameScene().getViewport().setBounds(-1500, 0, 1500, FXGL.getAppHeight());
        // load world + player
        FXGL.setLevelFromMap("map1.tmx");
        background.removeFromWorld();
        spawn("background");
        player = FXGL.getGameWorld().getSingleton(EntityType.PLAYER);

        FXGL.getGameScene().getViewport().bindToEntity(
                player,
                FXGL.getAppWidth() / 2,
                FXGL.getAppHeight() / 2
        );
        initInventory();
    }

    private void saveCharacter(String name) {
        try {
            Files.write(
                    Paths.get("characters.txt"),
                    (name + "\n").getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private List<String> loadCharacters() {
        try {
            return Files.readAllLines(Paths.get("characters.txt"));
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
