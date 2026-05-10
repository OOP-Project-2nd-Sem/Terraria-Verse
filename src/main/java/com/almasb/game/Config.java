package com.almasb.game;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.texture.Texture;
import javafx.geometry.Rectangle2D;

public final class Config {
    //Prevent instantiation
    private Config() {}

    public static final int TILE_SIZE = 16;

    public static final int INVENTORY_ROWS = 4;
    public static final int INVENTORY_COLS = 10;
    public static final int INVENTORY_SLOT_SIZE = 36;
    public static final int MAX_INVENTORY_SIZE = INVENTORY_ROWS * INVENTORY_COLS;

    /**
     * Canonical world block variants used by terrain generation, items and placement.
     * Keeping these in one place avoids texture mismatches (e.g. mined dirt placing as stone).
     */
    public static final BlockType SURFACE_GRASS_BLOCK = BlockType.GRASS_TOP_LAYER_21;
    public static final BlockType DEFAULT_DIRT_BLOCK = BlockType.DIRT_BLOCK;
    public static final BlockType DEFAULT_STONE_BLOCK = BlockType.GENERIC_STONE_1;
    public static final BlockType DEFAULT_TREE_TRUNK_BLOCK = BlockType.OAK_TREE_BOTTOM;
    public static final BlockType DEFAULT_LEAVES_BLOCK = BlockType.LEAF_VARIANT_1;
    public static final BlockType DEFAULT_PLANK_BLOCK = BlockType.HOUSE_WOOD_12;


    public enum BlockType {

        // -------------------------------------------------------------------------
        // Row 0: Stones
        // -------------------------------------------------------------------------
        DARK_DEEP_SLATE_1(0, 0), DARK_DEEP_SLATE_2(1, 0), DARK_DEEP_SLATE_3(2, 0),
        DARK_DEEP_SLATE_4(3, 0), DARK_DEEP_SLATE_5(4, 0),
        LIGHT_SLATE_1(5, 0), LIGHT_SLATE_2(6, 0), LIGHT_SLATE_3(7, 0),
        DEEP_SLATE_1(8, 0), DEEP_SLATE_2(9, 0), DEEP_SLATE_3(10, 0), DEEP_SLATE_4(11, 0),
        BRONZE_BLOCK_1(12, 0), BRONZE_BLOCK_2(13, 0), BRONZE_EARTH_BLOCK(14, 0),
        SILVER_BLOCK_1(15, 0), SILVER_BLOCK_2(16, 0), SILVER_BLOCK_3(17, 0),
        TUFF_STONE_1(18, 0), TUFF_STONE_2(19, 0), TUFF_STONE_3(20, 0),
        TUFF_STONE_4(21, 0), TUFF_STONE_5(22, 0),

        // -------------------------------------------------------------------------
        // Row 1: Bottom/Mix
        // -------------------------------------------------------------------------
        ROCKY_SLATE_1(0, 1), ROCKY_SLATE_2(1, 1), MOSSY_SLATE_1(2, 1),
        ROCKY_SLATE_3(3, 1), ROCKY_SLATE_4(4, 1), ROUGH_SLATE(5, 1), RIGID_SLATE(6, 1),
        MOSSY_SLATE_2(7, 1), MOSSY_SLATE_3(8, 1), DIRTY_SLATE(9, 1), COBBLE_SLATE(10, 1),
        BRONZE_BOTTOM(11, 1), DEEP_SLATE_BOTTOM(12, 1), DIRT_BLOCK(13, 1),
        DEEP_SLATE_BOTTOM_2(14, 1), DIRT_BOTTOM_1(15, 1), DIRT_BOTTOM_2(16, 1),
        SILVER_BOTTOM(17, 1), PRISMARINE(18, 1),
        GRAVEL(19, 1), GRAVEL_2(20, 1), GRAVEL_3(21, 1),
        // NOTE: Original names replaced — see bottom of file for explanation
        VOID_SLATE_1(22, 1), VOID_SLATE_2(23, 1),

        // -------------------------------------------------------------------------
        // Row 2: Polished Stones
        // -------------------------------------------------------------------------
        POLISHED_STONE_1(0, 2), SQUARE_POLISHED_STONE(1, 2),
        POLISHED_STONE_2(2, 2), SQUARE_POLISHED_STONE_2(3, 2),
        TILE_POLISHED_STONE_1(4, 2), TILE_POLISHED_STONE_2(5, 2),
        TILE_POLISHED_STONE_3(6, 2), TILE_POLISHED_STONE_4(7, 2),
        CHALK_POLISHED_STONE(8, 2), BUMPY_POLISHED_STONE(9, 2),
        BROWN_BRICK_STONE(10, 2), DARK_BRICK_STONE(11, 2), DARK_BRICK_STONE_2(12, 2),
        RED_BRICK_STONE_1(13, 2), RED_BRICK_STONE_2(14, 2),
        GENERIC_BRICK_STONE_1(15, 2), GENERIC_BRICK_STONE_2(16, 2), GENERIC_BRICK_STONE_3(17, 2),
        DITTO_BRICK_STONE_1(18, 2), DITTO_BRICK_STONE_2(19, 2), DITTO_BRICK_STONE_3(20, 2),
        DITTO_BRICK_STONE_4(21, 2), DITTO_BRICK_STONE_5(22, 2), DITTO_BRICK_STONE_6(23, 2),

        // -------------------------------------------------------------------------
        // Row 3: Slabby Stones
        // -------------------------------------------------------------------------
        SLAB_STONE_1(0, 3), SLAB_STONE_2(1, 3), SLAB_STONE_3(2, 3),
        MOSSY_SLAB_STONE_1(3, 3), CLEAN_SLAB_STONE_1(4, 3), CRACKED_SLAB_STONE(5, 3),
        MOSSY_SLAB_STONE_2(6, 3), ERODED_SLAB_STONE_1(7, 3), RIGID_SLAB_STONE(8, 3),
        DARK_SLAB_STONE(9, 3), CHOCO_SLAB_STONE(10, 3), WHITE_SLAB_STONE(11, 3),
        GENERIC_BRICK_SLAB_STONE(12, 3), GREY_SLAB_STONE(13, 3), HOUSE_SLAB_STONE(14, 3),
        BRITTLE_GREY_SLAB_STONE(15, 3), BRITTLE_RED_SLAB_STONE(16, 3), SANDY_SLAB_STONE(17, 3),
        BLACK_MARBLE_STONE(18, 3), MIX_MARBLE_STONE(19, 3), ROUGH_MARBLE_STONE(20, 3),
        SMOOTH_MARBLE_STONE(21, 3), GREEN_MARBLE_STONE_1(22, 3), GREEN_MARBLE_STONE_2(23, 3),

        // -------------------------------------------------------------------------
        // Row 4: Mixed Stuff
        // -------------------------------------------------------------------------
        CHECKERED_GREEN_MARBLE_STONE(0, 4), GREEN_SCRATCH_MARBLE_STONE(1, 4),
        CALCITE_1(2, 4), CALCITE_2(3, 4), GREY_CALCITE(4, 4),
        SAND_BOTTOM(5, 4), SAND_1(6, 4), SAND_2(7, 4), SAND_3(8, 4),
        FUDGY_DIRT(9, 4), MIX_DIRT(10, 4),
        // Named DIRT_BOTTOM_R4_x to avoid collision with Row 1's DIRT_BOTTOM_1/2
        DIRT_BOTTOM_R4_1(11, 4), DIRT_BOTTOM_R4_2(12, 4), DIRT_BOTTOM_R4_3(13, 4),
        DARK_DIRT(14, 4), LIGHT_DIRT(15, 4),
        GRAINY_DIRT_1(16, 4), GRAINY_DIRT_2(17, 4),
        STONY_DIRT_1(18, 4), STONY_DIRT_2(19, 4),
        DARK_BED_ROCK_1(20, 4), DARK_BED_ROCK_2(21, 4),
        LIGHT_BED_ROCK_1(22, 4), LIGHT_BED_ROCK_2(23, 4),

        // -------------------------------------------------------------------------
        // Row 5: Stone Variants and Ores
        // -------------------------------------------------------------------------
        GENERIC_STONE_1(0, 5), GENERIC_STONE_2(1, 5), GENERIC_STONE_3(2, 5), GENERIC_STONE_4(3, 5),
        ROUGH_STONE(4, 5), TILE_STONE(5, 5),
        COAL_BLOCK_1(6, 5), COAL_BLOCK_2(7, 5), COAL_BLOCK_3(8, 5),
        DIAMOND_BLOCK_1(9, 5), DIAMOND_BLOCK_2(10, 5), DIAMOND_BLOCK_3(11, 5),
        GOLD_BLOCK(12, 5), IRON_BLOCK_1(13, 5), IRON_BLOCK_2(14, 5),
        RED_STONE_BLOCK(15, 5), LAPIS_LAZULI_BLOCK(16, 5), EMERALD_BLOCK(17, 5),
        GLOW_STONE_1(18, 5), GLOW_STONE_2(19, 5), GLOW_STONE_3(20, 5),
        WATER_TEXTURE_1(21, 5), WATER_TEXTURE_2(22, 5), WATER_TEXTURE_3(23, 5),

        // -------------------------------------------------------------------------
        // Row 6: Water and Magma
        // -------------------------------------------------------------------------
        GENERIC_WATER_TEXTURE(0, 6), SEA_GREEN_WATER_TEXTURE(1, 6),
        DEEP_WATER_TEXTURE_1(2, 6), DEEP_WATER_TEXTURE_2(3, 6), CAVE_WATER_TEXTURE(4, 6),
        SHALLOW_WATER_TEXTURE_1(5, 6), SHALLOW_WATER_TEXTURE_2(6, 6),
        SHALLOW_WATER_TEXTURE_3(7, 6), SHALLOW_WATER_TEXTURE_4(8, 6),
        WATER_FALL_TEXTURE_1(9, 6), WATER_FALL_TEXTURE_2(10, 6),
        LAVA_TEXTURE_1(11, 6), LAVA_TEXTURE_2(12, 6),
        GENERIC_NETHER_WOOD(13, 6),
        FLOWING_LAVA_1(14, 6), FLOWING_LAVA_2(15, 6),
        MAGMA_BLOCK_1(16, 6), MAGMA_BLOCK_2(17, 6),
        FLOWING_LAVA_3(18, 6), FLOWING_LAVA_4(19, 6), FLOWING_LAVA_5(20, 6),
        DARK_MAGMA_BLOCK_1(21, 6), DARK_MAGMA_BLOCK_2(22, 6), DARK_MAGMA_BLOCK_3(23, 6),

        // -------------------------------------------------------------------------
        // Row 7: Magma and Other Stuff
        // -------------------------------------------------------------------------
        CORE_MAGMA_BLOCK(0, 7),
        FILLED_MAGMA_BLOCK_1(1, 7), FILLED_MAGMA_BLOCK_2(2, 7),
        FILLED_MAGMA_BLOCK_3(3, 7), FILLED_MAGMA_BLOCK_4(4, 7),
        BURNING_MAGMA_BLOCK_1(5, 7), BURNING_MAGMA_BLOCK_2(6, 7),
        DARK_AND_RED_NETHER_WOOD(7, 7), REDDY_NETHER_WOOD(8, 7), BLACK_NETHERWOOD(9, 7),
        NETHER_SOIL_1(10, 7), NETHER_SOIL_2(11, 7),
        BLACK_NETHER_WOOD_BOTTOM(12, 7), REDDY_NETHER_WOOD_BOTTOM(13, 7),
        NETHER_SOIL_BOTTOM_1(14, 7), NETHER_SOIL_BOTTOM_2(15, 7),
        GREEN_NETHER_LEAVE(16, 7), CYAN_NETHER_LEAVE(17, 7), MIX_NETHER_LEAVE(18, 7),
        GREEN_NETHER_WOOD(19, 7), MIX_NETHER_WOOD_1(20, 7), MIX_NETHER_WOOD_2(21, 7),
        PINK_NETHER_WOOD_1(22, 7), PINK_NETHER_WOOD_2(23, 7),

        // -------------------------------------------------------------------------
        // Row 8: Nether Soil and Other Stuff
        // -------------------------------------------------------------------------
        NETHER_SOIL_3(0, 8), OBSIDIAN_1(1, 8), OBSIDIAN_2(2, 8),
        NETHER_PORTAL(3, 8), NETHER_PORTAL_SWIRL(4, 8),
        BUBBLES(5, 8), CAVE_BUBBLES(6, 8), CAVE_BUBBLE(7, 8),
        PURE_DIAMOND_BLOCK(8, 8),
        // 7 smooth soil placeholder blocks
        SMOOTH_SOIL_1(9, 8), SMOOTH_SOIL_2(10, 8), SMOOTH_SOIL_3(11, 8), SMOOTH_SOIL_4(12, 8),
        SMOOTH_SOIL_5(13, 8), SMOOTH_SOIL_6(14, 8), SMOOTH_SOIL_7(15, 8),
        // 8 farm-soil placeholder blocks
        FARM_SOIL_1(16, 8), FARM_SOIL_2(17, 8), FARM_SOIL_3(18, 8), FARM_SOIL_4(19, 8),
        FARM_SOIL_5(20, 8), FARM_SOIL_6(21, 8), FARM_SOIL_7(22, 8), FARM_SOIL_8(23, 8),

        // -------------------------------------------------------------------------
        // Row 9: Woods
        // -------------------------------------------------------------------------
        DESERT_SOIL_1(0, 9), DESERT_SOIL_2(1, 9),
        GENERIC_TREE_TOP(2, 9), GENERIC_TREE_BOTTOM(3, 9),
        LIGHTER_TREE_TOP(4, 9), LIGHTER_TREE_BOTTOM(5, 9),
        LIGHT_TREE_TOP(6, 9), LIGHT_TREE_BOTTOM(7, 9),
        MUDDY_TREE_TOP(8, 9), MUDDY_TREE_BOTTOM(9, 9),
        OAK_TREE_TOP(10, 9), OAK_TREE_BOTTOM(11, 9),
        BIRCH_TREE_TOP(12, 9), BIRCH_TREE_BOTTOM(13, 9),
        SMOOTH_TREE_TOP(14, 9), SMOOTH_TREE_BOTTOM(15, 9),
        CHINESE_TREE_TOP(16, 9), CHINESE_TREE_BOTTOM(17, 9),
        // 6 various colour tree bottom placeholder blocks
        TREE_BOTTOM_VAR_1(18, 9), TREE_BOTTOM_VAR_2(19, 9), TREE_BOTTOM_VAR_3(20, 9),
        TREE_BOTTOM_VAR_4(21, 9), TREE_BOTTOM_VAR_5(22, 9), TREE_BOTTOM_VAR_6(23, 9),

        // -------------------------------------------------------------------------
        // Row 10: Various Housewoods (placeholder names)
        // -------------------------------------------------------------------------
        HOUSE_WOOD_1(0, 10), HOUSE_WOOD_2(1, 10), HOUSE_WOOD_3(2, 10), HOUSE_WOOD_4(3, 10),
        HOUSE_WOOD_5(4, 10), HOUSE_WOOD_6(5, 10), HOUSE_WOOD_7(6, 10), HOUSE_WOOD_8(7, 10),
        HOUSE_WOOD_9(8, 10), HOUSE_WOOD_10(9, 10), HOUSE_WOOD_11(10, 10), HOUSE_WOOD_12(11, 10),
        HOUSE_WOOD_13(12, 10), HOUSE_WOOD_14(13, 10), HOUSE_WOOD_15(14, 10), HOUSE_WOOD_16(15, 10),
        HOUSE_WOOD_17(16, 10), HOUSE_WOOD_18(17, 10), HOUSE_WOOD_19(18, 10), HOUSE_WOOD_20(19, 10),
        HOUSE_WOOD_21(20, 10), HOUSE_WOOD_22(21, 10), HOUSE_WOOD_23(22, 10), HOUSE_WOOD_24(23, 10),

        // -------------------------------------------------------------------------
        // Row 11: Crates / Shelves etc.
        // -------------------------------------------------------------------------
        CRATE_1(0, 11), CRATE_2(1, 11), CRATE_3(2, 11), CRATE_4(3, 11),
        BOOK_SHELVE_1(4, 11), BOOK_SHELVE_2(5, 11), BOOK_SHELVE_3(6, 11),
        DOOR_BOTTOM(7, 11), DOOR_TOP(8, 11), TRAP_DOOR(9, 11),
        DARK_LEAVE_1(10, 11), DARK_LEAVE_2(11, 11),
        HAY_BALE_BLOCK(12, 11),
        // 5 plant placeholder blocks
        PLANT_1(13, 11), PLANT_2(14, 11), PLANT_3(15, 11), PLANT_4(16, 11), PLANT_5(17, 11),
        HAY_BALE(18, 11), CACTUS_BOTTOM(19, 11), CACTUS_TOP_1(20, 11), CACTUS_TOP_2(21, 11),
        LUSH_LEAF_1(22, 11), LUSH_LEAF_2(23, 11),

        // -------------------------------------------------------------------------
        // Row 12: Further Leaf Variants (placeholder names)
        // -------------------------------------------------------------------------
        LEAF_VARIANT_1(0, 12), LEAF_VARIANT_2(1, 12), LEAF_VARIANT_3(2, 12), LEAF_VARIANT_4(3, 12),
        LEAF_VARIANT_5(4, 12), LEAF_VARIANT_6(5, 12), LEAF_VARIANT_7(6, 12), LEAF_VARIANT_8(7, 12),
        LEAF_VARIANT_9(8, 12), LEAF_VARIANT_10(9, 12), LEAF_VARIANT_11(10, 12), LEAF_VARIANT_12(11, 12),
        LEAF_VARIANT_13(12, 12), LEAF_VARIANT_14(13, 12), LEAF_VARIANT_15(14, 12), LEAF_VARIANT_16(15, 12),
        LEAF_VARIANT_17(16, 12), LEAF_VARIANT_18(17, 12), LEAF_VARIANT_19(18, 12), LEAF_VARIANT_20(19, 12),
        LEAF_VARIANT_21(20, 12), LEAF_VARIANT_22(21, 12), LEAF_VARIANT_23(22, 12), LEAF_VARIANT_24(23, 12),

        // -------------------------------------------------------------------------
        // Row 13: Fruits and Leaves (placeholder names)
        // -------------------------------------------------------------------------
        FRUIT_LEAF_1(0, 13), FRUIT_LEAF_2(1, 13), FRUIT_LEAF_3(2, 13), FRUIT_LEAF_4(3, 13),
        FRUIT_LEAF_5(4, 13), FRUIT_LEAF_6(5, 13), FRUIT_LEAF_7(6, 13), FRUIT_LEAF_8(7, 13),
        FRUIT_LEAF_9(8, 13), FRUIT_LEAF_10(9, 13), FRUIT_LEAF_11(10, 13), FRUIT_LEAF_12(11, 13),
        FRUIT_LEAF_13(12, 13), FRUIT_LEAF_14(13, 13), FRUIT_LEAF_15(14, 13), FRUIT_LEAF_16(15, 13),
        FRUIT_LEAF_17(16, 13), FRUIT_LEAF_18(17, 13), FRUIT_LEAF_19(18, 13), FRUIT_LEAF_20(19, 13),
        FRUIT_LEAF_21(20, 13), FRUIT_LEAF_22(21, 13), FRUIT_LEAF_23(22, 13), FRUIT_LEAF_24(23, 13),

        // -------------------------------------------------------------------------
        // Row 14: Mushrooms / Sweets (placeholder names)
        // -------------------------------------------------------------------------
        MUSHROOM_BLOCK_1(0, 14), MUSHROOM_BLOCK_2(1, 14), MUSHROOM_BLOCK_3(2, 14), MUSHROOM_BLOCK_4(3, 14),
        MUSHROOM_BLOCK_5(4, 14), MUSHROOM_BLOCK_6(5, 14), MUSHROOM_BLOCK_7(6, 14), MUSHROOM_BLOCK_8(7, 14),
        MUSHROOM_BLOCK_9(8, 14), MUSHROOM_BLOCK_10(9, 14), MUSHROOM_BLOCK_11(10, 14), MUSHROOM_BLOCK_12(11, 14),
        MUSHROOM_BLOCK_13(12, 14), MUSHROOM_BLOCK_14(13, 14), MUSHROOM_BLOCK_15(14, 14), MUSHROOM_BLOCK_16(15, 14),
        MUSHROOM_BLOCK_17(16, 14), MUSHROOM_BLOCK_18(17, 14), MUSHROOM_BLOCK_19(18, 14), MUSHROOM_BLOCK_20(19, 14),
        MUSHROOM_BLOCK_21(20, 14), MUSHROOM_BLOCK_22(21, 14), MUSHROOM_BLOCK_23(22, 14), MUSHROOM_BLOCK_24(23, 14),

        // -------------------------------------------------------------------------
        // Row 15: Some Sweets and Wool (placeholder names)
        // -------------------------------------------------------------------------
        SWEET_WOOL_1(0, 15), SWEET_WOOL_2(1, 15), SWEET_WOOL_3(2, 15), SWEET_WOOL_4(3, 15),
        SWEET_WOOL_5(4, 15), SWEET_WOOL_6(5, 15), SWEET_WOOL_7(6, 15), SWEET_WOOL_8(7, 15),
        SWEET_WOOL_9(8, 15), SWEET_WOOL_10(9, 15), SWEET_WOOL_11(10, 15), SWEET_WOOL_12(11, 15),
        SWEET_WOOL_13(12, 15), SWEET_WOOL_14(13, 15), SWEET_WOOL_15(14, 15), SWEET_WOOL_16(15, 15),
        SWEET_WOOL_17(16, 15), SWEET_WOOL_18(17, 15), SWEET_WOOL_19(18, 15), SWEET_WOOL_20(19, 15),
        SWEET_WOOL_21(20, 15), SWEET_WOOL_22(21, 15), SWEET_WOOL_23(22, 15), SWEET_WOOL_24(23, 15),

        // -------------------------------------------------------------------------
        // Row 16: Wool and Decorative Blocks (placeholder names)
        // -------------------------------------------------------------------------
        WOOL_DECO_1(0, 16), WOOL_DECO_2(1, 16), WOOL_DECO_3(2, 16), WOOL_DECO_4(3, 16),
        WOOL_DECO_5(4, 16), WOOL_DECO_6(5, 16), WOOL_DECO_7(6, 16), WOOL_DECO_8(7, 16),
        WOOL_DECO_9(8, 16), WOOL_DECO_10(9, 16), WOOL_DECO_11(10, 16), WOOL_DECO_12(11, 16),
        WOOL_DECO_13(12, 16), WOOL_DECO_14(13, 16), WOOL_DECO_15(14, 16), WOOL_DECO_16(15, 16),
        WOOL_DECO_17(16, 16), WOOL_DECO_18(17, 16), WOOL_DECO_19(18, 16), WOOL_DECO_20(19, 16),
        WOOL_DECO_21(20, 16), WOOL_DECO_22(21, 16), WOOL_DECO_23(22, 16), WOOL_DECO_24(23, 16),

        // -------------------------------------------------------------------------
        // Row 17: More Decorative Blocks (placeholder names)
        // -------------------------------------------------------------------------
        DECO_BLOCK_A1(0, 17), DECO_BLOCK_A2(1, 17), DECO_BLOCK_A3(2, 17), DECO_BLOCK_A4(3, 17),
        DECO_BLOCK_A5(4, 17), DECO_BLOCK_A6(5, 17), DECO_BLOCK_A7(6, 17), DECO_BLOCK_A8(7, 17),
        DECO_BLOCK_A9(8, 17), DECO_BLOCK_A10(9, 17), DECO_BLOCK_A11(10, 17), DECO_BLOCK_A12(11, 17),
        DECO_BLOCK_A13(12, 17), DECO_BLOCK_A14(13, 17), DECO_BLOCK_A15(14, 17), DECO_BLOCK_A16(15, 17),
        DECO_BLOCK_A17(16, 17), DECO_BLOCK_A18(17, 17), DECO_BLOCK_A19(18, 17), DECO_BLOCK_A20(19, 17),
        DECO_BLOCK_A21(20, 17), DECO_BLOCK_A22(21, 17), DECO_BLOCK_A23(22, 17), DECO_BLOCK_A24(23, 17),

        // -------------------------------------------------------------------------
        // Row 18: Even More Decorative Blocks (placeholder names)
        // -------------------------------------------------------------------------
        DECO_BLOCK_B1(0, 18), DECO_BLOCK_B2(1, 18), DECO_BLOCK_B3(2, 18), DECO_BLOCK_B4(3, 18),
        DECO_BLOCK_B5(4, 18), DECO_BLOCK_B6(5, 18), DECO_BLOCK_B7(6, 18), DECO_BLOCK_B8(7, 18),
        DECO_BLOCK_B9(8, 18), DECO_BLOCK_B10(9, 18), DECO_BLOCK_B11(10, 18), DECO_BLOCK_B12(11, 18),
        DECO_BLOCK_B13(12, 18), DECO_BLOCK_B14(13, 18), DECO_BLOCK_B15(14, 18), DECO_BLOCK_B16(15, 18),
        DECO_BLOCK_B17(16, 18), DECO_BLOCK_B18(17, 18), DECO_BLOCK_B19(18, 18), DECO_BLOCK_B20(19, 18),
        DECO_BLOCK_B21(20, 18), DECO_BLOCK_B22(21, 18), DECO_BLOCK_B23(22, 18), DECO_BLOCK_B24(23, 18),

        // -------------------------------------------------------------------------
        // Row 19: More Decorative Blocks (placeholder names)
        // -------------------------------------------------------------------------
        DECO_BLOCK_C1(0, 19), DECO_BLOCK_C2(1, 19), DECO_BLOCK_C3(2, 19), DECO_BLOCK_C4(3, 19),
        DECO_BLOCK_C5(4, 19), DECO_BLOCK_C6(5, 19), DECO_BLOCK_C7(6, 19), DECO_BLOCK_C8(7, 19),
        DECO_BLOCK_C9(8, 19), DECO_BLOCK_C10(9, 19), DECO_BLOCK_C11(10, 19), DECO_BLOCK_C12(11, 19),
        DECO_BLOCK_C13(12, 19), DECO_BLOCK_C14(13, 19), DECO_BLOCK_C15(14, 19), DECO_BLOCK_C16(15, 19),
        DECO_BLOCK_C17(16, 19), DECO_BLOCK_C18(17, 19), DECO_BLOCK_C19(18, 19), DECO_BLOCK_C20(19, 19),
        DECO_BLOCK_C21(20, 19), DECO_BLOCK_C22(21, 19), DECO_BLOCK_C23(22, 19), DECO_BLOCK_C24(23, 19),

        // -------------------------------------------------------------------------
        // Row 20: Pumpkin and Decorations (placeholder names)
        // -------------------------------------------------------------------------
        PUMPKIN_DECO_1(0, 20), PUMPKIN_DECO_2(1, 20), PUMPKIN_DECO_3(2, 20), PUMPKIN_DECO_4(3, 20),
        PUMPKIN_DECO_5(4, 20), PUMPKIN_DECO_6(5, 20), PUMPKIN_DECO_7(6, 20), PUMPKIN_DECO_8(7, 20),
        PUMPKIN_DECO_9(8, 20), PUMPKIN_DECO_10(9, 20), PUMPKIN_DECO_11(10, 20), PUMPKIN_DECO_12(11, 20),
        PUMPKIN_DECO_13(12, 20), PUMPKIN_DECO_14(13, 20), PUMPKIN_DECO_15(14, 20), PUMPKIN_DECO_16(15, 20),
        PUMPKIN_DECO_17(16, 20), PUMPKIN_DECO_18(17, 20), PUMPKIN_DECO_19(18, 20), PUMPKIN_DECO_20(19, 20),
        PUMPKIN_DECO_21(20, 20), PUMPKIN_DECO_22(21, 20), PUMPKIN_DECO_23(22, 20), PUMPKIN_DECO_24(23, 20),

        // -------------------------------------------------------------------------
        // Row 21: Unending Decorations (placeholder names)
        // -------------------------------------------------------------------------
        UNENDING_DECO_1(0, 21), UNENDING_DECO_2(1, 21), UNENDING_DECO_3(2, 21), UNENDING_DECO_4(3, 21),
        UNENDING_DECO_5(4, 21), UNENDING_DECO_6(5, 21), UNENDING_DECO_7(6, 21), UNENDING_DECO_8(7, 21),
        UNENDING_DECO_9(8, 21), UNENDING_DECO_10(9, 21), UNENDING_DECO_11(10, 21), UNENDING_DECO_12(11, 21),
        UNENDING_DECO_13(12, 21), UNENDING_DECO_14(13, 21), UNENDING_DECO_15(14, 21), UNENDING_DECO_16(15, 21),
        UNENDING_DECO_17(16, 21), UNENDING_DECO_18(17, 21), UNENDING_DECO_19(18, 21), UNENDING_DECO_20(19, 21),
        UNENDING_DECO_21(20, 21), UNENDING_DECO_22(21, 21), UNENDING_DECO_23(22, 21), UNENDING_DECO_24(23, 21),

        // -------------------------------------------------------------------------
        // Row 22: Grass Top Layer Variants (placeholder names)
        // -------------------------------------------------------------------------
        GRASS_TOP_LAYER_1(0, 22), GRASS_TOP_LAYER_2(1, 22), GRASS_TOP_LAYER_3(2, 22), GRASS_TOP_LAYER_4(3, 22),
        GRASS_TOP_LAYER_5(4, 22), GRASS_TOP_LAYER_6(5, 22), GRASS_TOP_LAYER_7(6, 22), GRASS_TOP_LAYER_8(7, 22),
        GRASS_TOP_LAYER_9(8, 22), GRASS_TOP_LAYER_10(9, 22), GRASS_TOP_LAYER_11(10, 22), GRASS_TOP_LAYER_12(11, 22),
        GRASS_TOP_LAYER_13(12, 22), GRASS_TOP_LAYER_14(13, 22), GRASS_TOP_LAYER_15(14, 22), GRASS_TOP_LAYER_16(15, 22),
        GRASS_TOP_LAYER_17(16, 22), GRASS_TOP_LAYER_18(17, 22), GRASS_TOP_LAYER_19(18, 22), GRASS_TOP_LAYER_20(19, 22),
        GRASS_TOP_LAYER_21(20, 22), GRASS_TOP_LAYER_22(21, 22), GRASS_TOP_LAYER_23(22, 22), GRASS_TOP_LAYER_24(23, 22),

        // -------------------------------------------------------------------------
        // Row 23: More Grass Top Layer Variants (placeholder names)
        // -------------------------------------------------------------------------
        GRASS_TOP_LAYER_25(0, 23), GRASS_TOP_LAYER_26(1, 23), GRASS_TOP_LAYER_27(2, 23), GRASS_TOP_LAYER_28(3, 23),
        GRASS_TOP_LAYER_29(4, 23), GRASS_TOP_LAYER_30(5, 23), GRASS_TOP_LAYER_31(6, 23), GRASS_TOP_LAYER_32(7, 23),
        GRASS_TOP_LAYER_33(8, 23), GRASS_TOP_LAYER_34(9, 23), GRASS_TOP_LAYER_35(10, 23), GRASS_TOP_LAYER_36(11, 23),
        GRASS_TOP_LAYER_37(12, 23), GRASS_TOP_LAYER_38(13, 23), GRASS_TOP_LAYER_39(14, 23), GRASS_TOP_LAYER_40(15, 23),
        GRASS_TOP_LAYER_41(16, 23), GRASS_TOP_LAYER_42(17, 23), GRASS_TOP_LAYER_43(18, 23), GRASS_TOP_LAYER_44(19, 23),
        GRASS_TOP_LAYER_45(20, 23), GRASS_TOP_LAYER_46(21, 23), GRASS_TOP_LAYER_47(22, 23), GRASS_TOP_LAYER_48(23, 23),

        // -------------------------------------------------------------------------
        // Row 24: Grass / Glass / Useable Stuff
        // -------------------------------------------------------------------------
        // 10 grass top layer blocks (placeholder names)
        GRASS_TOP_1(0, 24), GRASS_TOP_2(1, 24), GRASS_TOP_3(2, 24), GRASS_TOP_4(3, 24), GRASS_TOP_5(4, 24),
        GRASS_TOP_6(5, 24), GRASS_TOP_7(6, 24), GRASS_TOP_8(7, 24), GRASS_TOP_9(8, 24), GRASS_TOP_10(9, 24),
        GLASS_1(10, 24), GLASS_2(11, 24), GLASS_3(12, 24),
        STORAGE_CHEST(13, 24), STORAGE_CHEST_BACK(14, 24), STORAGE_CHEST_BOTTOM(15, 24),
        MIRROR(16, 24),
        CRAFTING_TABLE_1(17, 24), CRAFTING_TABLE_2(18, 24), CRAFTING_TABLE_3(19, 24),
        FURNACE(20, 24), FURNACE_BOTTOM(21, 24), FURNACE_BACK(22, 24),
        RED_STONE_LAMP(23, 24),

        // -------------------------------------------------------------------------
        // Row 25: Random Stuff
        // -------------------------------------------------------------------------
        RED_STONE_LAMP_BACK(0, 25), TARGET_BLOCK(1, 25), IRON_BARS(2, 25), HAZARD_SIGN(3, 25),
        PLASTIC_STORAGE_CHEST(4, 25), PLASTIC_STORAGE_CHEST_BACK(5, 25), LINE_HAZARD_SIGN(6, 25),
        // 2 ogre/slime placeholder blocks
        SLIME_THING_1(7, 25), SLIME_THING_2(8, 25);

        // =========================================================================

        public final int col, row;

        BlockType(int col, int row) {
            this.col = col;
            this.row = row;
        }
        public int getCol(){return col;}
        public int getRow(){return row;}

    }
    public static Texture getTexture(BlockType blockType) {
        return FXGL.texture("textures_02_08_25.png")
                .subTexture(new Rectangle2D(
                        blockType.getCol() * TILE_SIZE,
                        blockType.getRow() * TILE_SIZE,
                        TILE_SIZE,
                        TILE_SIZE
                ));
    }
    public enum ItemType {
        TORCH(720),
        STICK(1010),
        WOODEN_PICKAXE(21),
        EYE(1169)
        ;

        public final int i;
        ItemType(int i) {
            this.i=i;
        }
        public String getTexturePath() {
        return "items/item" + i + ".png";
        }
    }

public static Texture getItemTexture(ItemType itemType) {
    return FXGL.texture(itemType.getTexturePath());
}

    public static final Texture GRASS_TEX = getTexture(SURFACE_GRASS_BLOCK);
    public static final Texture STONE_TEX = getTexture(DEFAULT_STONE_BLOCK);
    public static final String SAVE_DIR = "saves/characters/";
}
