package com.almasb.game;

import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.physics.PhysicsComponent;

public class PlayerComponent extends Component {

    private PhysicsComponent physics;

    public void left() {
        physics.setVelocityX(-150);
    }

    public void right() {
        physics.setVelocityX(150);
    }

    public void jump() {
        physics.setVelocityY(-300);
    }
}
