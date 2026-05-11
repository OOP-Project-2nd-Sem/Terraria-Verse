package com.almasb.game;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.physics.PhysicsComponent;
import com.almasb.fxgl.texture.AnimatedTexture;
import com.almasb.fxgl.texture.AnimationChannel;
import com.almasb.fxgl.texture.Texture;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.util.Duration;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class PlayerComponent extends Component {
    private PhysicsComponent physics;

    private AnimatedTexture texture;
    private AnimationChannel animIdle, animWalk, animHurt, animDeath;

    private int maxHealth = 100;
    private double currentHealth = maxHealth;
    private DoubleProperty hpProperty = new SimpleDoubleProperty(maxHealth);

    private boolean jumping = false, isDead = false;

    private double stunTimer = 0;

    public PlayerComponent() {
        animIdle = new AnimationChannel(FXGL.image("Soldier-Idle.png"), 6, 90/6, 19, Duration.seconds(0.6), 0, 5);
        animWalk = new AnimationChannel(FXGL.image("Soldier-Walk.png"), 8, 120/8, 18, Duration.seconds(0.8), 0, 7);
        animHurt = new AnimationChannel(FXGL.image("Soldier-Hurt.png"), 4, 64/4, 18, Duration.seconds(0.4), 0, 3);
        animDeath = new AnimationChannel(FXGL.image("Soldier-Death.png"), 4, 65/4, 18, Duration.seconds(0.4), 0, 3);

        texture  = new AnimatedTexture(animIdle);
        texture.loop();

        // Start the idle animation once the hurt animation finishes
        texture.setOnCycleFinished(() -> {
            if(texture.getAnimationChannel() == animHurt) {
                if (!isDead) { texture.loopAnimationChannel(animIdle); }
            }
        });
    }

    @Override
    public void onAdded() {
        entity.getViewComponent().addChild(texture);
    }

    // PlayerComponent.java mein
    private List<InventoryItem> inventory = new ArrayList<>(Collections.nCopies(Config.MAX_INVENTORY_SIZE, null));

    // Hotbar - 10 slots (quick access)
    private List<InventoryItem> hotbar = new ArrayList<>(Collections.nCopies(10, null));
    private int selectedHotbarSlot = 0;

    // Armor slots - 4 slots (0=helmet, 1=chestplate, 2=leggings, 3=boots)
    private List<InventoryItem> armor = new ArrayList<>(Collections.nCopies(4, null));

    // Walking sound timing
    private double walkingSoundTimer = 0;
    private static final double WALKING_SOUND_INTERVAL = 0.4; // Play walking sound every 0.4 seconds

    // ─── Getters ───────────────────────────────────────────────────────────────

    public List<InventoryItem> getInventory() { return inventory; }
    public List<InventoryItem> getHotbar() { return hotbar; }
    public List<InventoryItem> getArmor() { return armor; }
    public int getSelectedHotbarSlot() { return selectedHotbarSlot; }
    public void setSelectedHotbarSlot(int slot) { selectedHotbarSlot = slot; }
    public InventoryItem getSelectedItem() { return hotbar.get(selectedHotbarSlot); }

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

        // Handle walking sound
        if (isGrounded() && Math.abs(physics.getVelocityX()) > 10 && texture.getAnimationChannel() == animWalk) {
            walkingSoundTimer -= tpf;
            if (walkingSoundTimer <= 0) {
                SoundManager.playWalkingSound();
                walkingSoundTimer = WALKING_SOUND_INTERVAL;
            }
        } else {
            walkingSoundTimer = 0; // Reset timer when not walking
        }
    }

    public void attack(InventoryItem heldItem) {
        if (isDead || stunTimer > 0) return;

        double damage = getDamageForItem(heldItem);
        double playerScaleX = entity.getViewComponent().getParent().getScaleX();
        boolean facingRight = playerScaleX > 0;
        double attackRange = 35;

        //Calculate the X cordinate to spawn the attack hitbox
        double hitX = facingRight ? entity.getRightX() : entity.getX() - attackRange;
        Rectangle2D hitBox = new Rectangle2D(hitX, entity.getY(), attackRange, entity.getHeight());

        //If the attacks hitbox intersects with an enemy
        for (Entity e : FXGL.getGameWorld().getEntitiesInRange(hitBox)) {
            if (e.isType(EntityType.ENEMY)) {
                double knockback = (playerScaleX > 0) ? 150 : -150;
                e.getComponent(EnemyComponent.class).takeDamage(damage, knockback);
            }
        }

        //Code for rotating the item held for attack
        if (heldItem != null && heldItem.getIcon() != null) {


            Texture weaponTexture = heldItem.getIcon().copy();


            // Spawn a temporary visual entity at the player's hand location
            double spawnX = facingRight ? entity.getRightX() - 4 : entity.getX() - 4;
            Entity weaponEntity = FXGL.entityBuilder()
                    .at(spawnX, entity.getY())
                    .view(weaponTexture)
                    .zIndex(100) // Make sure it renders in front of the player
                    .buildAndAttach();

            // Set the "Pivot Point" to the bottom of the item so it swings from the handle
            // Assuming a 16x16 icon: Bottom-Left (0, 16) for Right swing, Bottom-Right (16, 16) for Left swing
            weaponEntity.getTransformComponent().setRotationOrigin(new Point2D(facingRight ? 0 : 16, 16));

            // Create the smooth rotation animation using FXGL's AnimationBuilder
            FXGL.animationBuilder()
                    .duration(Duration.seconds(0.15))
                    .rotate(weaponEntity)
                    .from(facingRight ? -45 : 45)                 // Start slightly behind the head
                    .to(facingRight ? 135 : -135)                 // Swing downwards
                    .buildAndPlay();

            //Delete the weapon entity the exact moment the swing finishes
            FXGL.getGameTimer().runOnceAfter(() -> {
                weaponEntity.removeFromWorld();
            }, Duration.seconds(0.15));
        }
    }

    public void takeDamage(double damage, double knockbackDirX) {takeDamage(damage, knockbackDirX, -80);}
    public void takeDamage(double damage, double knockbackDirX, double knockbackDirY) {
        // Prevent the player for taking damage every frame of the hit
        if (stunTimer > 0) return;

        currentHealth -= damage;
        hpProperty.set(currentHealth);

        if (texture.getAnimationChannel() != animHurt) { texture.playAnimationChannel(animHurt); }

        if (currentHealth <= 0) {
            currentHealth = 0;
            die();
        } else {
            knockback(knockbackDirX, knockbackDirY); // Only knockback if the player is aliive
        }
    }

    private void die() {
        isDead = true;
        if (texture.getAnimationChannel() != animDeath) { texture.playAnimationChannel(animDeath); }

        // Display the death screen after a delay so the death animation is visible
        FXGL.getGameTimer().runOnceAfter(() -> {
            entity.removeFromWorld();
            displayDeathScreen();
        }, Duration.seconds(0.8));
    }

    public void knockback(double directionX, double directionY) {
        //Stun the player during knockback so cant cancel the knockback with movement keys
        stunTimer = 0.3;

        if (isDead) return;

        physics.setVelocityY(directionY);
        physics.setVelocityX(directionX);
    }

    public void left() {
        if (isDead || stunTimer > 0) return;
        physics.setVelocityX(-100);

        //Turn the sprite in the direction of the movement
        entity.getViewComponent().getParent().setScaleX(-1);
        if (texture.getAnimationChannel() != animWalk) { texture.loopAnimationChannel(animWalk); }
    }

    public void right() {
        if (isDead || stunTimer > 0) return;
        physics.setVelocityX(100);

        //Turn the sprite in the direction of the movement
        entity.getViewComponent().getParent().setScaleX(1);
        if (texture.getAnimationChannel() != animWalk) { texture.loopAnimationChannel(animWalk); }
    }

    public void stop() {
        if (stunTimer > 0) return;
        physics.setVelocityX(0);

        if (texture.getAnimationChannel() != animIdle) { texture.loopAnimationChannel(animIdle); }
    }

    public void jump() {
        if (isDead || stunTimer > 0) return;

        if (isGrounded()) {
            jumping = true;
            physics.setVelocityY(-200);
            SoundManager.playJumpSound();
        }
    }

    public boolean isGrounded() {
        return Math.abs(physics.getVelocityY()) < 0.1;
    }

    public int getMaxHealth() {return maxHealth;}
    public double getCurrentHealth() {return currentHealth;}
    public DoubleProperty getHpProperty() {return hpProperty;}
    public boolean isDead() {return isDead;}
    public double getDamageForItem(InventoryItem heldItem) {
        if (heldItem == null) return 2.0; // Default fist damage

        String name = heldItem.getName().toLowerCase();

        if (name.contains("sword") || name.contains("axe")) {
            if (name.contains("wood")) return 10.0;
            if (name.contains("stone")) return 15.0;
            if (name.contains("iron")) return 20.0;
            if (name.contains("diamond")) return 30.0;
            return 15.0;
        } else if (name.contains("pickaxe")) {
            if (name.contains("wood")) return 4.0;
            if (name.contains("iron")) return 8.0;
            if (name.contains("diamond")) return 12.0;
            return 5.0;
        }
        return 2.0;
    }

    private void displayDeathScreen() {
        StackPane overlay = new StackPane();
        overlay.setPrefSize(FXGL.getAppWidth(), FXGL.getAppHeight());
        overlay.setStyle("-fx-background-color: rgba(100, 0, 0, 0.5);");

        VBox deathMenu = new VBox(20);
        deathMenu.setAlignment(Pos.CENTER);

        Label deathText = new Label("YOU DIED");
        deathText.setStyle(
                "-fx-text-fill: #ff3333;" +
                        "-fx-font-size: 80;" +
                        "-fx-font-weight: bold;" +
                        "-fx-effect: dropshadow(gaussian, black, 15, 0.6, 0, 0);"
        );

        Button menuBtn = new Button("Return to Main Manu");
        String btnIdStyle = "-fx-font-size: 18; -fx-background-color: #222; -fx-text-fill: white; -fx-border-color: #ff3333; -fx-border-width: 2; -fx-padding: 10 20 10 20;";
        String btnHoverStyle = "-fx-font-size: 18; -fx-background-color: #442222; -fx-text-fill: white; -fx-border-color: #ff3333; -fx-border-width: 2; -fx-padding: 10 20 10 20;";

        menuBtn.setStyle(btnIdStyle);
        menuBtn.setOnMouseEntered(e -> menuBtn.setStyle(btnHoverStyle));
        menuBtn.setOnMouseExited(e -> menuBtn.setStyle(btnIdStyle));

        menuBtn.setOnAction(e -> ((GameApp) FXGL.getApp()).resetToMainMenu());

        deathMenu.getChildren().addAll(deathText, menuBtn);
        overlay.getChildren().add(deathMenu);

        FXGL.getGameScene().addUINode(overlay);
    }
}