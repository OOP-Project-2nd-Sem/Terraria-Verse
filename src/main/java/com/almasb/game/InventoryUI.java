package com.almasb.game;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.texture.Texture;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;

import java.util.List;

/**
 * Main inventory grid panel (toggle with E).
 *
 * Integration in GameApp
 * ──────────────────────
 * Field:
 *     private final InventoryUI inventoryUI = new InventoryUI(selectionState);
 *
 * In initUI() or wherever you set up static UI:
 *     inventoryUI.init();
 *
 * After player spawns (in startGame()):
 *     inventoryUI.setPlayer(player);
 *
 * Toggle E key:
 *     boolean visible = inventoryUI.toggle();
 *     armorUI.setVisible(visible);
 *     if (!visible) selectionState.clear();
 *
 * In refreshAll():
 *     inventoryUI.refresh();
 */
public class InventoryUI {

    private final SlotSelectionState selection;
    private Entity   player;
    private GridPane root;

    private final Runnable onSwap;

    public InventoryUI(SlotSelectionState selection, Runnable onSwap) {
        this.selection = selection;
        this.onSwap = onSwap;
    }
    // ── Public API ────────────────────────────────────────────────────────────

    /** Call once to create the GridPane and add it to the scene. */
    public void init() {
        root = new GridPane();
        root.setHgap(4);
        root.setVgap(4);
        root.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-padding: 10;");
        root.setTranslateX(25);
        root.setTranslateY(25);
        root.setVisible(false);
        FXGL.getGameScene().addUINode(root);
    }

    /** Call after the player entity has been spawned. */
    public void setPlayer(Entity player) {
        this.player = player;
    }

    /** Toggles visibility. Returns the new visibility state. */
    public boolean toggle() {
        boolean nowVisible = !root.isVisible();
        root.setVisible(nowVisible);
        return nowVisible;
    }

    public boolean isVisible() {
        return root != null && root.isVisible();
    }

    public void refresh() {
        if (root == null || player == null) return;
        root.getChildren().clear();
        for (int row = 0; row < Config.INVENTORY_ROWS; row++) {
            for (int col = 0; col < Config.INVENTORY_COLS; col++) {
                int index = row * Config.INVENTORY_COLS + col;
                root.add(createSlot(Config.INVENTORY_SLOT_SIZE, index), col, row);
            }
        }
    }

    // ── Slot building ─────────────────────────────────────────────────────────

    private StackPane createSlot(int size, int index) {
        StackPane slot = new StackPane();
        slot.setPrefSize(size, size);
        slot.setStyle(slotStyle(false));

        slot.setOnMouseClicked(e -> {
            PlayerComponent pc = player.getComponent(PlayerComponent.class);
            List<InventoryItem> inventory = pc.getInventory();

            if (selection.isEmpty()) {
                if (inventory.get(index) != null) {
                    selection.select(index, "inventory");
                    slot.setStyle(slotStyle(true));
                }
            } else {
                List<InventoryItem> fromList = getSlotList(pc, selection.getSlotType());
                InventoryItem temp = fromList.get(selection.getIndex());
                fromList.set(selection.getIndex(), inventory.get(index));
                inventory.set(index, temp);
                selection.clear();
                onSwap.run();
            }
        });

        // Draw item if present
        PlayerComponent pc = player.getComponent(PlayerComponent.class);
        InventoryItem item = pc.getInventory().get(index);
        if (item != null) {
            Texture icon = item.getIcon();
            icon.setFitWidth(size - 8);
            icon.setFitHeight(size - 8);
            Label count = new Label(String.valueOf(item.getCount()));
            count.setStyle("-fx-text-fill: white; -fx-font-size: 10;");
            StackPane.setAlignment(count, Pos.BOTTOM_RIGHT);
            slot.getChildren().addAll(icon, count);
        }

        // Keep highlight if this slot is the active selection
        if (!selection.isEmpty()
                && selection.getIndex() == index
                && selection.getSlotType().equals("inventory")) {
            slot.setStyle(slotStyle(true));
        }

        return slot;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<InventoryItem> getSlotList(PlayerComponent pc, String slotType) {
        return switch (slotType) {
            case "hotbar" -> pc.getHotbar();
            case "armor"  -> pc.getArmor();
            default       -> pc.getInventory();
        };
    }

    private String slotStyle(boolean selected) {
        return selected
                ? "-fx-border-color: yellow; -fx-border-width: 2; -fx-background-color: #555;"
                : "-fx-border-color: gray;   -fx-border-width: 2; -fx-background-color: #555;";
    }
}
