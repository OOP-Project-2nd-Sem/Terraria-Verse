package com.almasb.game;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.input.Input;
import com.almasb.fxgl.input.UserAction;
import javafx.scene.input.KeyCode;

public class GameApp extends GameApplication {

    private Entity player;

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(16 * 80);
        settings.setHeight(16 * 60);
    }

    @Override
    protected void initGame() {
        FXGL.getGameWorld().addEntityFactory(new GameFactory());
        FXGL.setLevelFromMap("map1.tmx");

        player = FXGL.getGameWorld().getSingleton(EnitiyType.PLAYER);
    }

    @Override
    protected void initInput() {
        Input input = FXGL.getInput();

        input.addAction(new UserAction("Move Left") {
            @Override
            protected void onAction() {
                if (player != null) {
                    player.getComponent(PlayerComponent.class).left();
                }
            }
            @Override
            protected void onActionEnd() {
                if (player.getComponent(PlayerComponent.class).isGrounded()) {
                    player.getComponent(PlayerComponent.class).stop();
                }
            }
        }, KeyCode.A);

        input.addAction(new UserAction("Move Right") {
            @Override
            protected void onAction() {
                if (player != null) {
                    player.getComponent(PlayerComponent.class).right();
                }
            }

            @Override
            protected void onActionEnd() {
                if (player.getComponent(PlayerComponent.class).isGrounded()) {
                    player.getComponent(PlayerComponent.class).stop();
                }
            }
        }, KeyCode.D);

        input.addAction(new UserAction("Jump") {
            @Override
            protected void onAction() {
                if (player != null) {
                    player.getComponent(PlayerComponent.class).jump();
                }
            }
        }, KeyCode.SPACE);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
