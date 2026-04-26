package com.almasb.game;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.input.Input;
import com.almasb.fxgl.input.UserAction;
import com.almasb.fxgl.time.TimerAction;
import javafx.geometry.Rectangle2D;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.util.Duration;

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

        player = FXGL.getGameWorld().getSingleton(EntityType.PLAYER);

        FXGL.getGameScene().getViewport().setBounds(-1500, 0, 1500, FXGL.getAppHeight());
        FXGL.getGameScene().getViewport().bindToEntity(player, FXGL.getAppWidth() / 2, FXGL.getAppHeight() / 2);
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

        input.addAction(new UserAction("Mine") {
            private TimerAction timer;

            @Override
            protected void onActionBegin() {
                double worldX = input.getMouseXWorld();
                double worldY = input.getMouseYWorld();

                Rectangle2D mouseBounds = new Rectangle2D(worldX, worldY, 1, 1);

                timer = FXGL.getGameTimer().runOnceAfter(() -> {
                    for (Entity e : FXGL.getGameWorld().getEntitiesInRange(mouseBounds)) {
                        if (e.isType(EntityType.BLOCK)) {
                            e.removeFromWorld();
                        }
                    }
                }, Duration.seconds(2));
            }

            @Override
            protected void onActionEnd() {
                if (timer != null)
                    timer.expire();
            }
        }, MouseButton.PRIMARY);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
