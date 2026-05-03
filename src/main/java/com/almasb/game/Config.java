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
    public static final int INVENTORY_SLOT_SIZE = 50;
    public static final int MAX_INVENTORY_SIZE = INVENTORY_ROWS * INVENTORY_COLS;

    public static final int GRASS_TEX_ROW = 20;
    public static final int GRASS_TEX_COL = 22;
    public static final int STONE_TEX_ROW = 21;
    public static final int STONE_TEX_COL = 22;

    public static final Texture GRASS_TEX = FXGL.texture("textures_02_08_25.png").subTexture(new Rectangle2D(GRASS_TEX_ROW * TILE_SIZE, GRASS_TEX_COL * TILE_SIZE, TILE_SIZE, TILE_SIZE));
    public static final Texture STONE_TEX = FXGL.texture("textures_02_08_25.png").subTexture(new Rectangle2D(STONE_TEX_ROW * TILE_SIZE, STONE_TEX_COL * TILE_SIZE, TILE_SIZE, TILE_SIZE));
    public static final String SAVE_DIR = "saves/characters/";
}
