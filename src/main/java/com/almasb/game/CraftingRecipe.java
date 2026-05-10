package com.almasb.game;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents a single crafting recipe.
 *
 * requiresCraftingTable = false  →  craftable anywhere (hand-crafting)
 * requiresCraftingTable = true   →  only craftable when player has a crafting_table in inventory/hotbar
 */

public class CraftingRecipe {

    private final String               resultName;
    private final int                  resultCount;
    private final Map<String, Integer> ingredients;
    private final boolean              requiresCraftingTable;

    public CraftingRecipe(String resultName, int resultCount,
                          Map<String, Integer> ingredients,
                          boolean requiresCraftingTable) {
        this.resultName            = resultName;
        this.resultCount           = resultCount;
        this.ingredients           = ingredients;
        this.requiresCraftingTable = requiresCraftingTable;
    }

    public String               getResultName()         { return resultName; }
    public int                  getResultCount()        { return resultCount; }
    public Map<String, Integer> getIngredients()        { return ingredients; }
    public boolean              requiresCraftingTable() { return requiresCraftingTable; }

    /** Returns the category of the crafted result. */
    public InventoryItem.ItemCategory getResultCategory() {
        return categoryOf(resultName);
    }

    public static InventoryItem.ItemCategory categoryOf(String name) {
        return switch (name.toLowerCase()) {
            case "iron_helmet", "iron_chestplate", "iron_leggings", "iron_boots",
                 "diamond_helmet", "diamond_chestplate", "diamond_leggings", "diamond_boots"
                    -> InventoryItem.ItemCategory.ARMOR;

            case "wood_pickaxe", "wood_axe",
                 "stone_pickaxe", "stone_axe", "stone_shovel",
                 "iron_pickaxe", "iron_axe", "iron_shovel",
                 "diamond_pickaxe", "diamond_axe", "diamond_shovel",
                 "gold_pickaxe"
                    -> InventoryItem.ItemCategory.TOOL;

            case "grass", "dirt", "stone", "wood", "plank",
                 "leaves", "coal", "iron", "gold", "diamond",
                 "emerald", "lapis", "crafting_table"
                    -> InventoryItem.ItemCategory.BLOCK;

            default -> InventoryItem.ItemCategory.ITEM;
        };
    }

    // ── Filtered views ────────────────────────────────────────────────────────

    public static List<CraftingRecipe> getHandRecipes() {
        return ALL_RECIPES.stream()
                .filter(r -> !r.requiresCraftingTable)
                .collect(Collectors.toList());
    }

    public static List<CraftingRecipe> getTableRecipes() {
        return ALL_RECIPES.stream()
                .filter(CraftingRecipe::requiresCraftingTable)
                .collect(Collectors.toList());
    }

    // ── Static registry ───────────────────────────────────────────────────────

    public static final List<CraftingRecipe> ALL_RECIPES;

    static {
        ALL_RECIPES = new ArrayList<>();

        // ════════════════════════════════════════════════════════════════════
        //  HAND CRAFTING
        // ════════════════════════════════════════════════════════════════════

        ALL_RECIPES.add(new CraftingRecipe("plank", 4,
                Map.of("wood", 1), false));

        ALL_RECIPES.add(new CraftingRecipe("stick", 4,
                Map.of("plank", 1), false));

        ALL_RECIPES.add(new CraftingRecipe("crafting_table", 1,
                Map.of("plank", 4), false));

        ALL_RECIPES.add(new CraftingRecipe("torch", 4,
                of("stick", 1, "coal", 1), false));

        // ════════════════════════════════════════════════════════════════════
        //  CRAFTING TABLE RECIPES
        // ════════════════════════════════════════════════════════════════════

        // ── Wood tools ───────────────────────────────────────────────────────
        ALL_RECIPES.add(new CraftingRecipe("wooden_pickaxe", 1,
                of("plank", 3, "stick", 2), true));

        ALL_RECIPES.add(new CraftingRecipe("wood_axe", 1,
                of("plank", 3, "stick", 2), true));

        // ── Stone tools ──────────────────────────────────────────────────────
        ALL_RECIPES.add(new CraftingRecipe("stone_pickaxe", 1,
                of("stone", 3, "stick", 2), true));

        ALL_RECIPES.add(new CraftingRecipe("stone_axe", 1,
                of("stone", 3, "stick", 2), true));

        ALL_RECIPES.add(new CraftingRecipe("stone_shovel", 1,
                of("stone", 1, "stick", 2), true));

        // ── Iron tools ───────────────────────────────────────────────────────
        ALL_RECIPES.add(new CraftingRecipe("iron_pickaxe", 1,
                of("iron", 3, "stick", 2), true));

        ALL_RECIPES.add(new CraftingRecipe("iron_axe", 1,
                of("iron", 3, "stick", 2), true));

        ALL_RECIPES.add(new CraftingRecipe("iron_shovel", 1,
                of("iron", 1, "stick", 2), true));

        // ── Diamond tools ────────────────────────────────────────────────────
        ALL_RECIPES.add(new CraftingRecipe("diamond_pickaxe", 1,
                of("diamond", 3, "stick", 2), true));

        ALL_RECIPES.add(new CraftingRecipe("diamond_axe", 1,
                of("diamond", 3, "stick", 2), true));

        ALL_RECIPES.add(new CraftingRecipe("diamond_shovel", 1,
                of("diamond", 1, "stick", 2), true));

        // ── Gold tools ───────────────────────────────────────────────────────
        ALL_RECIPES.add(new CraftingRecipe("gold_pickaxe", 1,
                of("gold", 3, "stick", 2), true));

        // ── Iron armor ───────────────────────────────────────────────────────
        ALL_RECIPES.add(new CraftingRecipe("iron_helmet", 1,
                Map.of("iron", 5), true));

        ALL_RECIPES.add(new CraftingRecipe("iron_chestplate", 1,
                Map.of("iron", 8), true));

        ALL_RECIPES.add(new CraftingRecipe("iron_leggings", 1,
                Map.of("iron", 7), true));

        ALL_RECIPES.add(new CraftingRecipe("iron_boots", 1,
                Map.of("iron", 4), true));

        // ── Diamond armor ────────────────────────────────────────────────────
        ALL_RECIPES.add(new CraftingRecipe("diamond_helmet", 1,
                Map.of("diamond", 5), true));

        ALL_RECIPES.add(new CraftingRecipe("diamond_chestplate", 1,
                Map.of("diamond", 8), true));

        ALL_RECIPES.add(new CraftingRecipe("diamond_leggings", 1,
                Map.of("diamond", 7), true));

        ALL_RECIPES.add(new CraftingRecipe("diamond_boots", 1,
                Map.of("diamond", 4), true));

        // ── Misc ─────────────────────────────────────────────────────────────
        ALL_RECIPES.add(new CraftingRecipe("coal", 9,
                Map.of("coal", 9), true));

        ALL_RECIPES.add(new CraftingRecipe("iron", 9,
                Map.of("iron", 9), true));

        ALL_RECIPES.add(new CraftingRecipe("stone", 2,
                Map.of("stone", 4), true));

        ALL_RECIPES.add(new CraftingRecipe("emerald", 1,
                of("emerald", 4, "gold", 1), true));
    }

    // ── Builder helpers ───────────────────────────────────────────────────────

    private static Map<String, Integer> of(String k1, int v1, String k2, int v2) {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        return m;
    }

    private static Map<String, Integer> of(String k1, int v1, String k2, int v2,
                                           String k3, int v3) {
        Map<String, Integer> m = of(k1, v1, k2, v2);
        m.put(k3, v3);
        return m;
    }
}