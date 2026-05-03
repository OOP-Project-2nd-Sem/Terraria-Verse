package com.almasb.game;

public class CharacterData {
    public String name;

    // Visual/Appearance
    public String skinColor = "#FFD700";
    public String hairColor = "#8B4513";
    public String shirtColor = "#4169E1";
    public int hairStyle = 0;

    // Stats
    public int health = 100;
    public int maxHealth = 100;
    public int mana = 20;
    public int maxMana = 20;

    // Inventory - 40 slots (like Terraria)
    public InventoryItem[] inventory = new InventoryItem[40];
    public InventoryItem[] hotbar = new InventoryItem[10];
    public InventoryItem[] armor = new InventoryItem[4]; // head, chest, legs, boots

    // Constructor
    public CharacterData(String name) {
        this.name = name;
    }
    // Load hone ke baad textures reload karo
    public void initAllTextures() {
        reloadTextures(inventory);
        reloadTextures(hotbar);
        reloadTextures(armor);
    }

    private void reloadTextures(InventoryItem[] slots) {
        if (slots == null) return;
        for (InventoryItem item : slots) {
            if (item != null) item.initTexture();  // TextureRegistry se milegi
        }
    }
}