package com.almasb.game;

import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.physics.PhysicsComponent;

import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class PlayerComponent extends Component {

    private PhysicsComponent physics;

    private boolean jumping = false;

    // PlayerComponent.java mein
    private List<InventoryItem> inventory = new ArrayList<>(Collections.nCopies(40, null));
// 40 slots (Terraria style 4 rows x 10 cols)

    public List<InventoryItem> getInventory() { return inventory; }

    public boolean addItem(InventoryItem newItem) {
        // Pehle existing stack dhundo
        for (InventoryItem item : inventory) {
            if (item != null && item.getName().equals(newItem.getName())) {
                item.setCount(item.getCount() + newItem.getCount());
                return true;
            }
        }
        // Nahi mila toh empty slot mein daalo
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.get(i) == null) {
                inventory.set(i, newItem);
                return true;
            }
        }
        return false; // inventory full
    }
    @Override
    public void onUpdate(double tpf) {
        if(isGrounded() && jumping)
        {
            physics.setVelocityX(0);
            jumping = false;
        }
    }

    public void left() {
        physics.setVelocityX(-100);
    }

    public void right() {
        physics.setVelocityX(100);
    }

    public void stop() {
        physics.setVelocityX(0);
    }

    public void jump() {

        if (isGrounded()) {
            jumping = true;
            physics.setVelocityY(-200);
        }
    }

    public boolean isGrounded() {
        return Math.abs(physics.getVelocityY()) < 0.1;
    }
}
