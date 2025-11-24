package com.gameengine.hulu;

import com.gameengine.core.GameEngine;
import com.gameengine.graphics.RenderBackend;
import com.gameengine.hulu.scene.HuluReplayScene;

public class HuluReplayLauncher {
    public static void main(String[] args) {
        String path = null;
        if (args != null && args.length > 0) {
            path = args[0];
        }
        
        GameEngine engine = new GameEngine(800, 600, "Hulu Replay", RenderBackend.GPU);
        // Initialize engine explicitly if needed, or constructor does it?
        // Based on HuluGame, it calls initialize().
        // Based on ReplayLauncher, it doesn't call initialize() explicitly before setScene?
        // Let's check GameEngine constructor.
        
        if (engine.initialize()) {
             HuluReplayScene scene = new HuluReplayScene(engine, path);
             engine.setScene(scene);
             engine.run();
        }
        engine.cleanup();
    }
}
