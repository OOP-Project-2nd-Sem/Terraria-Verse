package com.almasb.game;

import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.physics.PhysicsComponent;

public class PlayerComponent extends Component {

    private PhysicsComponent physics;

    private boolean jumping = false;

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
