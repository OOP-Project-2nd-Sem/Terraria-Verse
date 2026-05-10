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
 * Always-visible hotbar at the bottom of the screen.
 *
 * Integration in GameApp
 * ──────────────────────
 * Field:
 *     private final HotbarUI hotbarUI = new HotbarUI(selectionState);
 *
 * In initUI() or wherever you set up static UI:
 *     hotbarUI.init();
 *
 * After player spawns (in startGame()):
 *     hotbarUI.setPlayer(player);
 *
 * Hotbar number keys:
 *     player.getComponent(PlayerComponent.class).setSelectedHotbarSlot(slot);
 *     hotbarUI.refresh();
 *
 * In refreshAll():
 *     hotbarUI.refresh();
 */
public class HotbarUI {

    private static final int HOTBAR_SLOTS = 10;

    private final SlotSelectionState selection;
    private Entity   player;
    private GridPane root;

    private final Runnable onSwap;

    public HotbarUI(SlotSelectionState selection, Runnable onSwap) {
        this.selection = selection;
        this.onSwap = onSwap;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Call once to create the GridPane and add it to the scene. */
    public void init() {
        root = new GridPane();
        root.setHgap(4);
        root.setVgap(4);
        root.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-padding: 6;");

        double totalWidth = HOTBAR_SLOTS * (Config.INVENTORY_SLOT_SIZE + 4);
        root.setTranslateX((FXGL.getAppWidth() / 2.0) - totalWidth / 2.0);
        root.setTranslateY(FXGL.getAppHeight() - Config.INVENTORY_SLOT_SIZE - 20);

        FXGL.getGameScene().addUINode(root);
    }

    /** Call after the player entity has been spawned. */
    public void setPlayer(Entity player) {
        this.player = player;
    }

    public void refresh() {
        if (root == null || player == null) return;
        root.getChildren().clear();
        PlayerComponent pc = player.getComponent(PlayerComponent.class);

        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            StackPane slot = createSlot(Config.INVENTORY_SLOT_SIZE, i);
            if (i == pc.getSelectedHotbarSlot()) {
                slot.setStyle("-fx-border-color: white; -fx-border-width: 2; -fx-background-color: #777;");
            }
            root.add(slot, i, 0);
        }
    }

    // ── Slot building ─────────────────────────────────────────────────────────

    private StackPane createSlot(int size, int index) {
        StackPane slot = new StackPane();
        slot.setPrefSize(size, size);
        slot.setStyle(slotStyle(false));

        slot.setOnMouseClicked(e -> {
            PlayerComponent pc = player.getComponent(PlayerComponent.class);
            List<InventoryItem> hotbar = pc.getHotbar();

            if (selection.isEmpty()) {
                if (hotbar.get(index) != null) {
                    selection.select(index, "hotbar");
                    slot.setStyle(slotStyle(true));
                }
            } else {
                List<InventoryItem> fromList = getSlotList(pc, selection.getSlotType());
                InventoryItem temp = fromList.get(selection.getIndex());
                fromList.set(selection.getIndex(), hotbar.get(index));
                hotbar.set(index, temp);
                selection.clear();
                refresh();
            }
        });

        // Draw item if present
        PlayerComponent pc = player.getComponent(PlayerComponent.class);
        InventoryItem item = pc.getHotbar().get(index);
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
                && selection.getSlotType().equals("hotbar")) {
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
