package com.almasb.game;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.input.Input;
import com.almasb.fxgl.input.UserAction;
import com.almasb.game.InventoryItem;
import javafx.geometry.Pos;
import javafx.scene.image.ImageView;
import javafx.scene.control.Label;
import com.almasb.fxgl.time.TimerAction;
import javafx.geometry.Rectangle2D;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.util.Duration;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseButton;

import static com.almasb.fxgl.dsl.FXGL.*;

public class GameApp extends GameApplication {

    private Entity player;
    private GridPane inventoryRoot;
    private int selectedSlotIndex= -1;

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(16 * 80);
        settings.setHeight(16 * 60);
    }

    @Override
    protected void initGame() {
        FXGL.getGameWorld().addEntityFactory(new GameFactory());
        FXGL.setLevelFromMap("map1.tmx");

        player = FXGL.getGameWorld().getSingleton(EntityType.PLAYER);

        FXGL.getGameScene().getViewport().setBounds(-1500, 0, 1500, FXGL.getAppHeight());
        FXGL.getGameScene().getViewport().bindToEntity(player, FXGL.getAppWidth() / 2, FXGL.getAppHeight() / 2);
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
                double worldX = input.getMouseXWorld();
                double worldY = input.getMouseYWorld();

                Rectangle2D mouseBounds = new Rectangle2D(worldX, worldY, 1, 1);

                timer = FXGL.getGameTimer().runOnceAfter(() -> {
                    for (Entity e : FXGL.getGameWorld().getEntitiesInRange(mouseBounds)) {
                        if (e.isType(EntityType.BLOCK)) {
                            spawn("item", new SpawnData(e.getX(), e.getY())
                                    .put("width", 10)
                                    .put("height", 10)
                                    .put("count", 1)
                                    .put("color", Color.BROWN));
                            e.removeFromWorld();
                        }
                    }
                }, Duration.seconds(2));
            }

            @Override
            protected void onActionEnd() {
                if (timer != null)
                    timer.expire();
            }
        }, MouseButton.PRIMARY);

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
        // Inventory toggle ke liye
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

    public static void main(String[] args) {
        launch(args);
    }
}
