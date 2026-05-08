package com.almasb.game;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.physics.PhysicsComponent;

public class EnemyComponent extends Component {
    private PhysicsComponent physics;

    private int maxHealth;
    private double currentHealth;
    private double moveSpeed;

    public EnemyComponent(int maxHealth, double moveSpeed) {
        this.maxHealth = maxHealth;
        this.currentHealth = maxHealth;
        this.moveSpeed = moveSpeed;
    }

    public void takeDamage(double amount) {
        currentHealth -= amount;
        if (currentHealth <= 0) {
            entity.removeFromWorld();
        }
    }

    @Override
    public void onUpdate(double tpf) {
        Entity player = FXGL.getGameWorld().getSingleton(EntityType.PLAYER);

        //Move towards the player
        if (player != null) {
            physics.setVelocityX(player.getX() > entity.getX() ? moveSpeed : -moveSpeed);
        }

        if(physics.getVelocityX() == 0 && isGrounded()) {
            physics.setVelocityY(-150);
        }
    }

    public boolean isGrounded() {
        return Math.abs(physics.getVelocityY()) < 0.1;
    }
}