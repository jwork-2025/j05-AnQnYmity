package com.gameengine.hulu.scene;

import com.gameengine.core.GameEngine;
import com.gameengine.graphics.IRenderer;
import com.gameengine.input.InputManager;
import com.gameengine.math.Vector2;
import com.gameengine.scene.Scene;
import com.gameengine.recording.RecordingConfig;
import com.gameengine.recording.RecordingService;

import java.io.File;

public class HuluMenuScene extends Scene {
    public enum MenuOption {
        START_GAME,
        REPLAY,
        EXIT
    }

    private final GameEngine engine;
    private final IRenderer renderer;
    private final InputManager inputManager;
    private int selectedIndex = 0;
    private final MenuOption[] options = {MenuOption.START_GAME, MenuOption.REPLAY, MenuOption.EXIT};

    public HuluMenuScene(GameEngine engine) {
        super("HuluMenu");
        this.engine = engine;
        this.renderer = engine.getRenderer();
        this.inputManager = engine.getInputManager();
    }

    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);

        // Keyboard navigation
        if (inputManager.isKeyJustPressed(38) || inputManager.isKeyJustPressed(265)) { // Up
            selectedIndex = (selectedIndex - 1 + options.length) % options.length;
        } else if (inputManager.isKeyJustPressed(40) || inputManager.isKeyJustPressed(264)) { // Down
            selectedIndex = (selectedIndex + 1) % options.length;
        } else if (inputManager.isKeyJustPressed(10) || inputManager.isKeyJustPressed(32) || inputManager.isKeyJustPressed(257)) { // Enter/Space
            selectOption(options[selectedIndex]);
        }

        // Mouse interaction
        Vector2 mousePos = inputManager.getMousePosition();
        if (inputManager.isMouseButtonJustPressed(0)) {
            float centerX = renderer.getWidth() / 2.0f;
            float centerY = renderer.getHeight() / 2.0f;
            for (int i = 0; i < options.length; i++) {
                float y = centerY - 40 + i * 60;
                if (mousePos.x > centerX - 100 && mousePos.x < centerX + 100 &&
                    mousePos.y > y - 20 && mousePos.y < y + 20) {
                    selectOption(options[i]);
                }
            }
        }
    }

    private void selectOption(MenuOption option) {
        switch (option) {
            case START_GAME:
                startGame();
                break;
            case REPLAY:
                startReplay();
                break;
            case EXIT:
                engine.stop();
                break;
        }
    }

    private void startGame() {
        try {
            File recDir = new File("recordings");
            if (!recDir.exists()) recDir.mkdirs();
            String path = "recordings/hulu_session_" + System.currentTimeMillis() + ".jsonl";
            RecordingConfig cfg = new RecordingConfig(path);
            RecordingService svc = new RecordingService(cfg);
            engine.enableRecording(svc);
            System.out.println("Recording enabled: " + path);
        } catch (Exception e) {
            System.err.println("Failed to enable recording: " + e.getMessage());
        }
        engine.setScene(new HuluMainScene("HuluGame", engine));
    }

    private void startReplay() {
        engine.disableRecording();
        engine.setScene(new HuluReplayScene(engine, null));
    }

    @Override
    public void render() {
        renderer.drawRect(0, 0, renderer.getWidth(), renderer.getHeight(), 0.2f, 0.3f, 0.2f, 1.0f);

        float centerX = renderer.getWidth() / 2.0f;
        float centerY = renderer.getHeight() / 2.0f;

        String title = "HULUWA VS MONSTERS";
        renderer.drawText(centerX - (title.length() * 10), centerY - 100, title, 1.0f, 1.0f, 0.0f, 1.0f);

        for (int i = 0; i < options.length; i++) {
            String text = options[i].toString().replace("_", " ");
            float y = centerY - 40 + i * 60;
            float r = 0.8f, g = 0.8f, b = 0.8f;
            
            if (i == selectedIndex) {
                r = 1.0f; g = 1.0f; b = 0.0f;
                renderer.drawRect(centerX - 100, y - 20, 200, 40, 0.4f, 0.4f, 0.4f, 0.8f);
            }

            renderer.drawText(centerX - (text.length() * 8), y - 5, text, r, g, b, 1.0f);
        }
        
        String hint = "UP/DOWN to select, ENTER to confirm";
        renderer.drawText(centerX - (hint.length() * 6), renderer.getHeight() - 50, hint, 0.6f, 0.6f, 0.6f, 1.0f);
    }
}
