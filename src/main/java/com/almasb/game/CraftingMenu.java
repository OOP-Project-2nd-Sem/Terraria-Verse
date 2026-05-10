package com.almasb.game;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.texture.Texture;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.List;
import java.util.Map;

/**
 * CraftingMenu — two tabs:
 *   ✋ Hand     : recipes that need no crafting table
 *   🪵 Table   : recipes that require a crafting_table in inventory/hotbar
 *                (tab is locked + shows a warning when you don't have one)
 *
 * Integration (same as before):
 *   craftingMenu = new CraftingMenu(player);
 *   craftingMenu.init();
 *   onKeyDown(KeyCode.C, () -> craftingMenu.toggle());
 *   // in refreshAll(): craftingMenu.refresh();
 */
public class CraftingMenu {

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final int SLOT_SIZE    = Config.INVENTORY_SLOT_SIZE;
    private static final int PANEL_WIDTH  = 440;
    private static final int PANEL_HEIGHT = 520;
    private static final int RECIPE_ROW_H = 62;

    // Tab identifiers
    private static final int TAB_HAND  = 0;
    private static final int TAB_TABLE = 1;

    // ── State ─────────────────────────────────────────────────────────────────
    private final Entity player;
    private VBox         rootPane;
    private VBox         recipeListBox;
    private boolean      visible      = false;
    private int          activeTab    = TAB_HAND;

    // Tab button references so we can re-style them on switch
    private Label tabHandBtn;
    private Label tabTableBtn;

    // "No crafting table" warning row
    private HBox noTableWarning;

    // Detail area
    private Label resultLabel;
    private HBox  ingredientBar;

    // Selected recipe
    private CraftingRecipe selectedRecipe = null;
    private HBox           selectedRow    = null;

    // ── Constructor ───────────────────────────────────────────────────────────
    public CraftingMenu(Entity player) {
        this.player = player;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void init() {
        buildUI();
        FXGL.getGameScene().addUINode(rootPane);
    }

    public void toggle() {
        visible = !visible;
        rootPane.setVisible(visible);
        if (visible) refresh();
    }

    public boolean isVisible() { return visible; }

    /** Call whenever inventory changes. */
    public void refresh() {
        if (rootPane == null) return;
        updateTabLock();
        buildRecipeRows();
    }

    // ── UI construction ───────────────────────────────────────────────────────

    private void buildUI() {
        rootPane = new VBox(8);
        rootPane.setPrefSize(PANEL_WIDTH, PANEL_HEIGHT);
        rootPane.setMaxSize(PANEL_WIDTH, PANEL_HEIGHT);
        rootPane.setPadding(new Insets(12));
        rootPane.setStyle(
                "-fx-background-color: rgba(18,18,18,0.93);" +
                "-fx-border-color: #777;" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 4;" +
                "-fx-background-radius: 4;"
        );
        rootPane.setTranslateX((FXGL.getAppWidth()  - PANEL_WIDTH)  / 2.0);
        rootPane.setTranslateY((FXGL.getAppHeight() - PANEL_HEIGHT) / 2.0);
        rootPane.setVisible(false);

        // ── Title ─────────────────────────────────────────────────────────
        HBox titleBar = new HBox();
        titleBar.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("⚒  CRAFTING");
        title.setStyle("-fx-text-fill: #e8c84a; -fx-font-size: 16; -fx-font-weight: bold;");
        Label hint = new Label("[C] close");
        hint.setStyle("-fx-text-fill: #666; -fx-font-size: 11;");
        HBox.setHgrow(hint, Priority.ALWAYS);
        hint.setMaxWidth(Double.MAX_VALUE);
        hint.setAlignment(Pos.CENTER_RIGHT);
        titleBar.getChildren().addAll(title, hint);

        // ── Tabs ──────────────────────────────────────────────────────────
        HBox tabs = buildTabs();

        // ── "No table" warning (hidden by default) ────────────────────────
        noTableWarning = buildNoTableWarning();

        // ── Recipe scroll list ────────────────────────────────────────────
        recipeListBox = new VBox(4);
        recipeListBox.setPadding(new Insets(4, 0, 4, 0));

        ScrollPane scroll = new ScrollPane(recipeListBox);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(PANEL_HEIGHT - 210);
        scroll.setStyle(
                "-fx-background: transparent;" +
                "-fx-background-color: transparent;" +
                "-fx-border-color: transparent;"
        );
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // ── Detail / craft area ───────────────────────────────────────────
        VBox detailBox = buildDetailBox();

        rootPane.getChildren().addAll(
                titleBar,
                sep(),
                tabs,
                noTableWarning,
                scroll,
                sep(),
                detailBox
        );
    }

    private HBox buildTabs() {
        HBox tabs = new HBox(0);
        tabs.setAlignment(Pos.CENTER_LEFT);

        tabHandBtn  = makeTabLabel("✋  Hand",  true);
        tabTableBtn = makeTabLabel("🪵  Table", false);

        tabHandBtn.setOnMouseClicked(e  -> switchTab(TAB_HAND));
        tabTableBtn.setOnMouseClicked(e -> switchTab(TAB_TABLE));

        tabs.getChildren().addAll(tabHandBtn, tabTableBtn);
        return tabs;
    }

    private Label makeTabLabel(String text, boolean active) {
        Label l = new Label(text);
        applyTabStyle(l, active);
        return l;
    }

    private void applyTabStyle(Label l, boolean active) {
        l.setStyle(
                "-fx-padding: 6 20 6 20;" +
                "-fx-font-size: 13;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;" +
                (active
                    ? "-fx-background-color: #333; -fx-text-fill: #e8c84a; -fx-border-color: #e8c84a #e8c84a transparent #e8c84a; -fx-border-width: 1;"
                    : "-fx-background-color: #1a1a1a; -fx-text-fill: #777; -fx-border-color: #444 #444 #444 #444; -fx-border-width: 1;")
        );
    }

    private HBox buildNoTableWarning() {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(6, 8, 6, 8));
        box.setStyle("-fx-background-color: rgba(100,50,10,0.7); -fx-background-radius: 3;");

        Label icon = new Label("⚠");
        icon.setStyle("-fx-text-fill: #e8a030; -fx-font-size: 14;");
        Label msg  = new Label("You need a Crafting Table in your inventory to use these recipes.");
        msg.setStyle("-fx-text-fill: #c89050; -fx-font-size: 11;");
        msg.setWrapText(true);

        box.getChildren().addAll(icon, msg);
        box.setVisible(false);   // shown only on Table tab when player lacks one
        box.setManaged(false);
        return box;
    }

    private VBox buildDetailBox() {
        VBox box = new VBox(6);
        box.setPadding(new Insets(4, 0, 0, 0));

        ingredientBar = new HBox(6);
        ingredientBar.setAlignment(Pos.CENTER_LEFT);
        ingredientBar.setMinHeight(40);

        resultLabel = new Label("Select a recipe above");
        resultLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12;");

        Label craftBtn = new Label("  CRAFT  ");
        craftBtn.setStyle(
                "-fx-background-color: #2a6e2a;" +
                "-fx-text-fill: #c8f0c8;" +
                "-fx-font-size: 13;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 6 18 6 18;" +
                "-fx-cursor: hand;" +
                "-fx-border-color: #5ab85a;" +
                "-fx-border-width: 1;"
        );
        craftBtn.setOnMouseEntered(e -> craftBtn.setStyle(craftBtn.getStyle().replace("#2a6e2a", "#3a9e3a")));
        craftBtn.setOnMouseExited (e -> craftBtn.setStyle(craftBtn.getStyle().replace("#3a9e3a", "#2a6e2a")));
        craftBtn.setOnMouseClicked(e -> tryCraft());

        HBox btnRow = new HBox(craftBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        box.getChildren().addAll(ingredientBar, resultLabel, btnRow);
        return box;
    }

    private Rectangle sep() {
        Rectangle r = new Rectangle(PANEL_WIDTH - 24, 1);
        r.setFill(Color.web("#444"));
        return r;
    }

    // ── Tab switching ─────────────────────────────────────────────────────────

    private void switchTab(int tab) {
        activeTab = tab;
        applyTabStyle(tabHandBtn,  tab == TAB_HAND);
        applyTabStyle(tabTableBtn, tab == TAB_TABLE);

        // Reset selection when switching tabs
        selectedRecipe = null;
        selectedRow    = null;
        ingredientBar.getChildren().clear();
        resultLabel.setText("Select a recipe above");
        resultLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12;");

        updateTabLock();
        buildRecipeRows();
    }

    /** Show/hide the "no crafting table" warning on the Table tab. */
    private void updateTabLock() {
        if (activeTab == TAB_TABLE) {
            boolean hasTable = hasCraftingTable();
            noTableWarning.setVisible(!hasTable);
            noTableWarning.setManaged(!hasTable);
        } else {
            noTableWarning.setVisible(false);
            noTableWarning.setManaged(false);
        }
    }

    private boolean hasCraftingTable() {
        PlayerComponent pc = player.getComponent(PlayerComponent.class);
        return countInInventory(pc, "crafting_table") > 0;
    }

    // ── Recipe rows ───────────────────────────────────────────────────────────

    private void buildRecipeRows() {
        recipeListBox.getChildren().clear();
        PlayerComponent pc = player.getComponent(PlayerComponent.class);

        List<CraftingRecipe> recipes = (activeTab == TAB_HAND)
                ? CraftingRecipe.getHandRecipes()
                : CraftingRecipe.getTableRecipes();

        boolean tableAvailable = (activeTab == TAB_TABLE) && hasCraftingTable();

        for (CraftingRecipe recipe : recipes) {
            // On the table tab without a table: all recipes are greyed out / unclickable
            boolean canCraft = (activeTab == TAB_HAND)
                    ? canCraft(pc, recipe)
                    : tableAvailable && canCraft(pc, recipe);

            // Dim further if table tab is locked
            boolean locked = (activeTab == TAB_TABLE) && !tableAvailable;

            HBox row = buildRecipeRow(recipe, canCraft, locked);
            recipeListBox.getChildren().add(row);
        }
    }

    private HBox buildRecipeRow(CraftingRecipe recipe, boolean canCraft, boolean locked) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(5, 8, 5, 8));
        row.setPrefHeight(RECIPE_ROW_H);

        String baseBg;
        if (locked)       baseBg = "rgba(30,20,20,0.5)";
        else if (canCraft) baseBg = "rgba(35,55,35,0.85)";
        else               baseBg = "rgba(38,38,38,0.65)";

        row.setStyle("-fx-background-color: " + baseBg + "; -fx-background-radius: 3;");

        // Result icon
        InventoryItem phantom = makePhantomItem(recipe.getResultName(), recipe.getResultCount());
        if (phantom != null) {
            Texture icon = phantom.getIcon();
            icon.setFitWidth(SLOT_SIZE);
            icon.setFitHeight(SLOT_SIZE);
            // Dim icon when locked
            if (locked) icon.setOpacity(0.3);
            row.getChildren().add(icon);
        } else {
            Rectangle box = new Rectangle(SLOT_SIZE, SLOT_SIZE, Color.web("#444"));
            row.getChildren().add(box);
        }

        // Name + ingredients
        VBox textCol = new VBox(3);
        String nameStr = prettyName(recipe.getResultName())
                + (recipe.getResultCount() > 1 ? "  ×" + recipe.getResultCount() : "");

        String nameColor   = locked ? "#444" : (canCraft ? "#e5e5e5" : "#666");
        String ingColor    = locked ? "#333" : (canCraft ? "#9ac89a" : "#505050");

        Label name = new Label(nameStr);
        name.setStyle("-fx-text-fill: " + nameColor + "; -fx-font-size: 13; -fx-font-weight: bold;");

        Label ings = new Label(ingredientSummary(recipe));
        ings.setStyle("-fx-text-fill: " + ingColor + "; -fx-font-size: 11;");

        textCol.getChildren().addAll(name, ings);
        HBox.setHgrow(textCol, Priority.ALWAYS);
        row.getChildren().add(textCol);

        // Badge
        if (canCraft) {
            Label badge = new Label("✔");
            badge.setStyle("-fx-text-fill: #5ab85a; -fx-font-size: 12;");
            row.getChildren().add(badge);
        } else if (locked) {
            Label badge = new Label("🔒");
            badge.setStyle("-fx-font-size: 12;");
            row.getChildren().add(badge);
        }

        // Hover / click — disabled when locked
        if (!locked) {
            row.setOnMouseClicked(e -> selectRecipe(recipe, row, canCraft));
            row.setOnMouseEntered(e -> {
                if (row != selectedRow)
                    row.setStyle("-fx-background-color: rgba(55,75,55,0.9); -fx-background-radius: 3;");
            });
            row.setOnMouseExited(e -> {
                if (row != selectedRow)
                    row.setStyle("-fx-background-color: " + baseBg + "; -fx-background-radius: 3;");
            });
        }

        return row;
    }

    // ── Selection & detail ────────────────────────────────────────────────────

    private void selectRecipe(CraftingRecipe recipe, HBox row, boolean canCraft) {
        if (selectedRow != null) {
            PlayerComponent pc = player.getComponent(PlayerComponent.class);
            boolean prev = canCraft(pc, selectedRecipe);
            String prevBg = prev ? "rgba(35,55,35,0.85)" : "rgba(38,38,38,0.65)";
            selectedRow.setStyle("-fx-background-color: " + prevBg + "; -fx-background-radius: 3;");
        }
        selectedRecipe = recipe;
        selectedRow    = row;
        row.setStyle("-fx-background-color: rgba(75,100,75,0.9); -fx-background-radius: 3;");
        updateDetailBox(recipe, canCraft);
    }

    private void updateDetailBox(CraftingRecipe recipe, boolean canCraft) {
        ingredientBar.getChildren().clear();
        PlayerComponent pc = player.getComponent(PlayerComponent.class);

        for (Map.Entry<String, Integer> entry : recipe.getIngredients().entrySet()) {
            String ing    = entry.getKey();
            int    needed = entry.getValue();
            int    have   = countInInventory(pc, ing);
            boolean ok    = have >= needed;

            StackPane cell = new StackPane();
            cell.setPrefSize(40, 40);
            cell.setStyle("-fx-background-color: #2a2a2a; -fx-border-color: "
                    + (ok ? "#5ab85a" : "#b85a5a") + "; -fx-border-width: 1;");

            InventoryItem phantom = makePhantomItem(ing, 1);
            if (phantom != null) {
                Texture icon = phantom.getIcon();
                icon.setFitWidth(30);
                icon.setFitHeight(30);
                cell.getChildren().add(icon);
            }

            Label qty = new Label(have + "/" + needed);
            qty.setStyle("-fx-text-fill: " + (ok ? "#5ab85a" : "#e05555") + "; -fx-font-size: 9;");
            StackPane.setAlignment(qty, Pos.BOTTOM_RIGHT);
            cell.getChildren().add(qty);
            ingredientBar.getChildren().add(cell);
        }

        // If table tab: also show crafting_table requirement chip
        if (activeTab == TAB_TABLE) {
            boolean hasTable = hasCraftingTable();
            StackPane chip = new StackPane();
            chip.setPrefSize(40, 40);
            chip.setStyle("-fx-background-color: #2a2a2a; -fx-border-color: "
                    + (hasTable ? "#5ab85a" : "#b85a5a") + "; -fx-border-width: 1;");
            InventoryItem tableIcon = makePhantomItem("crafting_table", 1, InventoryItem.ItemCategory.BLOCK);
            if (tableIcon != null) {
                Texture ti = tableIcon.getIcon();
                ti.setFitWidth(30);
                ti.setFitHeight(30);
                chip.getChildren().add(ti);
            }
            Label tl = new Label(hasTable ? "✔" : "✗");
            tl.setStyle("-fx-text-fill: " + (hasTable ? "#5ab85a" : "#e05555") + "; -fx-font-size: 9;");
            StackPane.setAlignment(tl, Pos.BOTTOM_RIGHT);
            chip.getChildren().add(tl);
            ingredientBar.getChildren().add(chip);
        }

        String produces = prettyName(recipe.getResultName())
                + (recipe.getResultCount() > 1 ? " ×" + recipe.getResultCount() : "");
        resultLabel.setText(canCraft
                ? "Produces: " + produces
                : "Produces: " + produces + "  (not enough materials)");
        resultLabel.setStyle("-fx-text-fill: "
                + (canCraft ? "#b8e8b8" : "#d07070") + "; -fx-font-size: 12;");
    }

    // ── Crafting logic ────────────────────────────────────────────────────────

    private void tryCraft() {
        if (selectedRecipe == null) return;
        PlayerComponent pc = player.getComponent(PlayerComponent.class);

        // Extra guard: table recipes need a crafting table
        if (selectedRecipe.requiresCraftingTable() && !hasCraftingTable()) return;
        if (!canCraft(pc, selectedRecipe)) return;

        for (Map.Entry<String, Integer> entry : selectedRecipe.getIngredients().entrySet()) {
            consumeItem(pc, entry.getKey(), entry.getValue());
        }

        InventoryItem result = makePhantomItem(
                selectedRecipe.getResultName(), selectedRecipe.getResultCount(), selectedRecipe.getResultCategory());
        if (result != null) addToInventory(pc, result);

        refresh();

        if (selectedRecipe != null) {
            boolean can = (activeTab == TAB_HAND)
                    ? canCraft(pc, selectedRecipe)
                    : hasCraftingTable() && canCraft(pc, selectedRecipe);
            updateDetailBox(selectedRecipe, can);
        }
    }

    // ── Inventory helpers ─────────────────────────────────────────────────────

    private int countInInventory(PlayerComponent pc, String itemName) {
        int total = 0;
        for (InventoryItem it : pc.getInventory()) {
            if (it != null && it.getName().equalsIgnoreCase(itemName)) total += it.getCount();
        }
        for (InventoryItem it : pc.getHotbar()) {
            if (it != null && it.getName().equalsIgnoreCase(itemName)) total += it.getCount();
        }
        return total;
    }

    private boolean canCraft(PlayerComponent pc, CraftingRecipe recipe) {
        for (Map.Entry<String, Integer> entry : recipe.getIngredients().entrySet()) {
            if (countInInventory(pc, entry.getKey()) < entry.getValue()) return false;
        }
        return true;
    }

    private void consumeItem(PlayerComponent pc, String itemName, int amount) {
        amount = consumeFromList(pc.getInventory(), itemName, amount);
        if (amount > 0) consumeFromList(pc.getHotbar(), itemName, amount);
    }

    private int consumeFromList(List<InventoryItem> list, String itemName, int amount) {
        for (int i = 0; i < list.size() && amount > 0; i++) {
            InventoryItem it = list.get(i);
            if (it != null && it.getName().equalsIgnoreCase(itemName)) {
                int take = Math.min(it.getCount(), amount);
                amount -= take;
                if (it.getCount() - take <= 0) list.set(i, null);
                else it.setCount(it.getCount() - take);
            }
        }
        return amount;
    }

    private void addToInventory(PlayerComponent pc, InventoryItem newItem) {
        List<InventoryItem> inv = pc.getInventory();
        for (InventoryItem it : inv) {
            if (it != null && it.getName().equalsIgnoreCase(newItem.getName())) {
                it.setCount(it.getCount() + newItem.getCount());
                return;
            }
        }
        for (int i = 0; i < inv.size(); i++) {
            if (inv.get(i) == null) { inv.set(i, newItem); return; }
        }
        List<InventoryItem> hotbar = pc.getHotbar();
        for (int i = 0; i < hotbar.size(); i++) {
            if (hotbar.get(i) == null) { hotbar.set(i, newItem); return; }
        }
        System.out.println("[Crafting] Inventory full, could not add " + newItem.getName());
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    // Replace your single makePhantomItem with these two:

    private InventoryItem makePhantomItem(String name, int count, InventoryItem.ItemCategory category) {
        try {
            InventoryItem item = new InventoryItem(name, count, category, null);
            item.initTexture();
            return item;
        } catch (Exception e) {
            return null;
        }
    }

    // Overload that infers category automatically from CraftingRecipe.categoryOf()
    private InventoryItem makePhantomItem(String name, int count) {
        return makePhantomItem(name, count, CraftingRecipe.categoryOf(name));
    }

    private String prettyName(String raw) {
        StringBuilder sb = new StringBuilder();
        for (String p : raw.replace("_", " ").split(" ")) {
            if (!p.isEmpty())
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    private String ingredientSummary(CraftingRecipe recipe) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : recipe.getIngredients().entrySet()) {
            sb.append(prettyName(e.getKey())).append(" ×").append(e.getValue()).append("  ");
        }
        return sb.toString().trim();
    }
}
