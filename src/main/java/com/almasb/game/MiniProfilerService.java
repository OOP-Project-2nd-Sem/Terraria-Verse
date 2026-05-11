package com.almasb.game;

import com.almasb.fxgl.core.EngineService;
import com.almasb.fxgl.dsl.FXGL;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public final class MiniProfilerService extends EngineService {

    private final Runtime runtime = Runtime.getRuntime();
    private final VBox root = new VBox(1);
    private final Text fpsText = new Text("FPS: 0");
    private final Text cpuText = new Text("CPU: 0.00 ms");
    private final Text ramText = new Text("RAM: 0.00 MB");
    private final Text blocksText = new Text("Blocks: 0/0");

    private double sampleTime;
    private int sampleFrames;
    private double sampleCpuMs;
    private boolean consoleVisible = true;

    @Override
    public void onInit() {
        Font font = Font.font("Consolas", 10);

        fpsText.setFont(font);
        cpuText.setFont(font);
        ramText.setFont(font);
        blocksText.setFont(font);

        fpsText.setFill(Color.WHITE);
        cpuText.setFill(Color.WHITE);
        ramText.setFill(Color.WHITE);
        blocksText.setFill(Color.WHITE);

        root.setMouseTransparent(true);
        root.setTranslateX(6);
        root.setTranslateY(6);
        root.setStyle("-fx-background-color: rgba(0,0,0,0.45); -fx-padding: 4;");
        root.getChildren().addAll(fpsText, cpuText, ramText, blocksText);
        root.setVisible(consoleVisible);
        root.setManaged(consoleVisible);

        FXGL.getSceneService().getOverlayRoot().getChildren().add(root);
    }

    public void toggleConsole() {
        consoleVisible = !consoleVisible;
        root.setVisible(consoleVisible);
        root.setManaged(consoleVisible);
    }

    @Override
    public void onUpdate(double tpf) {
        sampleTime += tpf;
        sampleFrames++;
        sampleCpuMs += FXGL.cpuNanoTime() / 1_000_000.0;

        if (sampleTime < 0.25) {
            return;
        }

        double fps = sampleFrames / sampleTime;
        double avgCpuMs = sampleCpuMs / sampleFrames;
        double usedRamMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024.0 * 1024.0);
        GameApp app = FXGL.getAppCast();

        fpsText.setText(String.format("FPS: %.0f", fps));
        cpuText.setText(String.format("CPU: %.2f ms", avgCpuMs));
        ramText.setText(String.format("RAM: %.2f MB", usedRamMb));
        blocksText.setText(String.format("Blocks: %d/%d", app.getActiveTerrainBlockCount(), app.getTotalTerrainBlockCount()));

        sampleTime = 0.0;
        sampleFrames = 0;
        sampleCpuMs = 0.0;
    }
}
