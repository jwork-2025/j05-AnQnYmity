package com.gameengine.hulu;

import com.gameengine.core.GameEngine;
import com.gameengine.graphics.RenderBackend;
import com.gameengine.hulu.scene.HuluMenuScene;

public class HuluGame {
    private static final int SCREEN_WIDTH = 800;
    private static final int SCREEN_HEIGHT = 600;

    public static void main(String[] args) {
        GameEngine engine = new GameEngine(SCREEN_WIDTH, SCREEN_HEIGHT, "葫芦娃大战妖精 - 重制版", RenderBackend.GPU);
        if (engine.initialize()) {
            HuluMenuScene scene = new HuluMenuScene(engine);
            engine.setScene(scene);
            engine.run();
        }
    }
}
