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
    private double damage;

    private double stuckTimer = 0, stunTimer = 0;

    public EnemyComponent(int maxHealth, double moveSpeed, double damage) {
        this.maxHealth = maxHealth;
        this.currentHealth = maxHealth;
        this.moveSpeed = moveSpeed;
        this.damage = damage;
    }

    public void takeDamage(double amount, double knockback) {
        currentHealth -= amount;

        System.out.println("Enemy took a damage of " + amount + " Current health: " + currentHealth); // For debug
        if (currentHealth <= 0) {
            entity.removeFromWorld();
        }
        else {
            knockback(knockback);
        }
    }

    public void knockback(double directionX) {
        stunTimer = 0.2;

        physics.setVelocityX(directionX);
        physics.setVelocityY(-80);
    }

    @Override
    public void onUpdate(double tpf) {
        if (stunTimer > 0) stunTimer -= tpf;

        // Check if there is a player in the world (used singletonOptional as safety in case player is dead)
        var playerOptional = FXGL.getGameWorld().getSingletonOptional(EntityType.PLAYER);

        //Move towards the player
        if (playerOptional.isPresent()) {
            Entity player = playerOptional.get();

            //Stop movement if the player is dead
            if (player.getComponent(PlayerComponent.class).isDead()) {
                physics.setVelocityX(0);
                physics.setVelocityY(0);
                return;
            }

            if (stunTimer > 0) return;

            boolean isGrounded = Math.abs(physics.getVelocityY()) < 0.1;
            boolean isStuck = Math.abs(physics.getVelocityX()) < 0.1;

            //Tracks how long the enemy has been stuck
            if (isStuck) stuckTimer += tpf;
            else stuckTimer = 0;

            physics.setVelocityX(player.getX() > entity.getX() ? moveSpeed : -moveSpeed);

            //Prevent jumps due to single frame zero velocity
            if(stuckTimer > 0.2 && isGrounded) {
                physics.setVelocityY(-170);
                stuckTimer = 0;
            }
        } else {
            //Stop moving if there is no player
            physics.setVelocityX(0);
        }
    }

    public double getDamage() {return damage;}
}