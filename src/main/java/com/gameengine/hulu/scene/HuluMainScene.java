package com.gameengine.hulu.scene;

import com.gameengine.core.GameEngine;
import com.gameengine.core.GameObject;
import com.gameengine.graphics.IRenderer;
import com.gameengine.hulu.HuluEntityFactory;
import com.gameengine.hulu.components.MonsterComponent;
import com.gameengine.hulu.components.PlayerComponent;
import com.gameengine.math.Vector2;
import com.gameengine.scene.Scene;

import java.util.List;
import java.util.Random;

public class HuluMainScene extends Scene {
    private static final int SCREEN_WIDTH = 800;
    private static final int SCREEN_HEIGHT = 600;
    private static final int MAX_MONSTERS = 15;

    private final IRenderer renderer;
    private final GameEngine engine;
    private final Random random = new Random();
    private float spawntime = 0.0f;
    private int score = 0;

    public HuluMainScene(String name, GameEngine engine) {
        super(name);
        this.engine = engine;
        this.renderer = engine.getRenderer();
    }

    public IRenderer getRenderer() { return renderer; }
    public GameEngine getEngine() { return engine; }
    public void addScore(int delta) {
        this.score += delta;
        System.out.println("得分！当前得分: " + score);
    }

    @Override
    public void initialize() {
        Vector2 startPos = new Vector2(SCREEN_WIDTH / 2, SCREEN_HEIGHT / 2);
        addGameObject(HuluEntityFactory.createHuluwa(this, startPos));
        spawnMonster();
        super.initialize();
    }

    @Override
    public void update(float deltaTime) {
        // ESC (27) or P (80) to stop recording and return to menu
        if (engine.getInputManager().isKeyJustPressed(27) || engine.getInputManager().isKeyJustPressed(80)) {
            engine.disableRecording();
            engine.setScene(new HuluMenuScene(engine));
            return;
        }

        spawntime += deltaTime;
        if (spawntime >= 2.0f) {
            spawntime = 0.0f;
            List<MonsterComponent> currentMonsters = getComponents(MonsterComponent.class);
            if (currentMonsters.size() < MAX_MONSTERS) {
                spawnMonster();
            }
        }
        super.update(deltaTime);
    }

    public void spawnMonster() {
        float x = 50 + random.nextFloat() * (SCREEN_WIDTH - 100);
        float y = 50 + random.nextFloat() * (SCREEN_HEIGHT - 100);
        addGameObject(HuluEntityFactory.createMonster(this, new Vector2(x, y)));
    }

    @Override
    public void render() {
        renderer.drawRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, 0.5f, 0.8f, 0.5f, 1.0f);
        super.render();

        PlayerComponent huluwa = getHuluwaPlayerComponent();
        if (huluwa != null) {
            renderer.drawRect(10, 10, 150, 30, 0.0f, 0.0f, 0.0f, 0.8f);
            float hpRatio = huluwa.getHealth() / 100.0f;
            hpRatio = Math.max(0.0f, Math.min(1.0f, hpRatio));
            float barWidth = 140 * hpRatio;
            float r = (1.0f - hpRatio);
            float g = hpRatio;
            renderer.drawRect(15, 15, barWidth, 20, r, g, 0.0f, 1.0f);
            
            renderer.drawText(10, 50, "SCORE: " + score, 1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    public PlayerComponent getHuluwaPlayerComponent() {
        List<PlayerComponent> players = getComponents(PlayerComponent.class);
        return players.isEmpty() ? null : players.get(0);
    }
}
