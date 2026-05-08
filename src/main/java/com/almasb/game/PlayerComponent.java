package com.almasb.game;

import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.physics.PhysicsComponent;

import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class PlayerComponent extends Component {
    private PhysicsComponent physics;

    private int maxHealth = 100;
    private double currentHealth = maxHealth;

    private boolean jumping = false;

    private double stunTimer = 0;

    // PlayerComponent.java mein
    private List<InventoryItem> inventory = new ArrayList<>(Collections.nCopies(Config.MAX_INVENTORY_SIZE, null));

    // Hotbar - 10 slots (quick access)
    private List<InventoryItem> hotbar = new ArrayList<>(Collections.nCopies(10, null));
    private int selectedHotbarSlot = 0;

    // Armor slots - 4 slots (0=helmet, 1=chestplate, 2=leggings, 3=boots)
    private List<InventoryItem> armor = new ArrayList<>(Collections.nCopies(4, null));

    // ─── Getters ───────────────────────────────────────────────────────────────

    public List<InventoryItem> getInventory() { return inventory; }
    public List<InventoryItem> getHotbar() { return hotbar; }
    public List<InventoryItem> getArmor() { return armor; }
    public int getSelectedHotbarSlot() { return selectedHotbarSlot; }
    public void setSelectedHotbarSlot(int slot) { selectedHotbarSlot = slot; }

    public InventoryItem getSelectedItem() {
        return hotbar.get(selectedHotbarSlot);
    }

    // ─── Add Item ──────────────────────────────────────────────────────────────

    public boolean addItem(InventoryItem newItem) {
        // Pehle hotbar mein stack dhundo
        for (InventoryItem item : hotbar) {
            if (item != null && item.getName().equals(newItem.getName())) {
                item.setCount(item.getCount() + newItem.getCount());
                return true;
            }
        }
        // Phir inventory mein stack dhundo
        for (InventoryItem item : inventory) {
            if (item != null && item.getName().equals(newItem.getName())) {
                item.setCount(item.getCount() + newItem.getCount());
                return true;
            }
        }
        // Hotbar mein empty slot dhundo pehle
        for (int i = 0; i < hotbar.size(); i++) {
            if (hotbar.get(i) == null) {
                hotbar.set(i, newItem);
                return true;
            }
        }
        // Phir inventory mein empty slot
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.get(i) == null) {
                inventory.set(i, newItem);
                return true;
            }
        }
        return false; // sab full
    }

    // ─── Remove Item ───────────────────────────────────────────────────────────

    public boolean removeItem(String itemName, int count) {
        // Hotbar mein dhundo pehle
        for (int i = 0; i < hotbar.size(); i++) {
            InventoryItem item = hotbar.get(i);
            if (item != null && item.getName().equals(itemName)) {
                if (item.getCount() <= count) {
                    hotbar.set(i, null);
                } else {
                    item.setCount(item.getCount() - count);
                }
                return true;
            }
        }
        // Phir inventory mein
        for (int i = 0; i < inventory.size(); i++) {
            InventoryItem item = inventory.get(i);
            if (item != null && item.getName().equals(itemName)) {
                if (item.getCount() <= count) {
                    inventory.set(i, null);
                } else {
                    item.setCount(item.getCount() - count);
                }
                return true;
            }
        }
        return false;
    }

    // ─── Armor ─────────────────────────────────────────────────────────────────

    public boolean equipArmor(InventoryItem item, int slot) {
        if (slot < 0 || slot >= 4) return false;
        armor.set(slot, item);
        return true;
    }

    public InventoryItem unequipArmor(int slot) {
        if (slot < 0 || slot >= 4) return null;
        InventoryItem item = armor.get(slot);
        armor.set(slot, null);
        return item;
    }

    public void loadFromSave(CharacterData data) {
        // Inventory
        for (int i = 0; i < inventory.size(); i++) {
            inventory.set(i, (data.inventory != null && i < data.inventory.length) ? data.inventory[i] : null);
        }
        // Hotbar
        for (int i = 0; i < hotbar.size(); i++) {
            hotbar.set(i, (data.hotbar != null && i < data.hotbar.length) ? data.hotbar[i] : null);
        }
        // Armor
        for (int i = 0; i < armor.size(); i++) {
            armor.set(i, (data.armor != null && i < data.armor.length) ? data.armor[i] : null);
        }
    }

    public void saveToCharacterData(CharacterData data) {
        data.inventory = inventory.toArray(new InventoryItem[0]);
        data.hotbar = hotbar.toArray(new InventoryItem[0]);
        data.armor = armor.toArray(new InventoryItem[0]);
    }

    @Override
    public void onUpdate(double tpf) {
        //reduce the stuntimer
        if (stunTimer > 0) {
            stunTimer -= tpf;

            //Artificial friction for the duration of stun
            physics.setVelocityX(physics.getVelocityX() * 0.8);

            //Hard stop after stun is over
            if (stunTimer <= 0) {
                physics.setVelocityX(0);
            }
        }

        if(isGrounded() && jumping)
        {
            physics.setVelocityX(0);
            jumping = false;
        }
    }

    public void takeDamage(double damage, double knockbackDirX) {takeDamage(damage, knockbackDirX, -80);}
    public void takeDamage(double damage, double knockbackDirX, double knockbackDirY) {
        // Prevent the player for taking damage every frame of the hit
        if (stunTimer > 0) return;

        currentHealth -= damage;
        System.out.println("Player took " + damage + " damage! Health: " + currentHealth + "/" + maxHealth); //Temporarily print the damage and health on console

        if (currentHealth <= 0) {
            currentHealth = 0;
            System.out.println("PLAYER DIED!"); // Temporarily print the died message on the console
        } else {
            knockback(knockbackDirX, knockbackDirY); // Only knockback if the player is aliive
        }
    }

    public void knockback(double directionX, double directionY) {
        //Stun the player during knockback so cant cancel the knockback with movement keys
        stunTimer = 0.3;
        physics.setVelocityY(directionY);
        physics.setVelocityX(directionX);
    }

    public void left() {
        if (stunTimer > 0) return;
        physics.setVelocityX(-100);
    }

    public void right() {
        if (stunTimer > 0) return;
        physics.setVelocityX(100);
    }

    public void stop() {
        if (stunTimer > 0) return;
        physics.setVelocityX(0);
    }

    public void jump() {
        if (stunTimer > 0) return;

        if (isGrounded()) {
            jumping = true;
            physics.setVelocityY(-200);
        }
    }

    public boolean isGrounded() {
        return Math.abs(physics.getVelocityY()) < 0.1;
    }

    public int getMaxHealth() {return maxHealth;}
    public double getCurrentHealth() {return currentHealth;}
}