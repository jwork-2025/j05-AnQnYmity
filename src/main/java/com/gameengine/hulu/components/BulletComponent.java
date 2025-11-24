package com.gameengine.hulu.components;

import com.gameengine.components.RenderComponent;
import com.gameengine.components.TransformComponent;
import com.gameengine.core.Component;
import com.gameengine.core.GameObject;
import com.gameengine.hulu.scene.HuluMainScene;
import com.gameengine.math.Vector2;

import java.util.List;

public class BulletComponent extends Component<BulletComponent> {
    private static final int SCREEN_WIDTH = 800;
    private static final int SCREEN_HEIGHT = 600;
    private static final float BULLET_SIZE = 10.0f;

    private final Vector2 velocity;
    private final HuluMainScene scene;

    public BulletComponent(Vector2 velocity, HuluMainScene scene) {
        this.velocity = velocity;
        this.scene = scene;
    }

    @Override
    public void initialize() {}

    @Override
    public void update(float deltaTime) {
        TransformComponent transform = owner.getComponent(TransformComponent.class);
        if (transform == null) return;

        // 1. 移动子弹
        Vector2 deltaMovement = velocity.multiply(deltaTime);
        transform.translate(deltaMovement);

        // 2. 检查边界
        Vector2 pos = transform.getPosition();
        if (pos.x < 0 || pos.x > SCREEN_WIDTH || pos.y < 0 || pos.y > SCREEN_HEIGHT) {
            owner.destroy();
            return;
        }

        // 3. 检查与妖精的碰撞
        RenderComponent bulletRender = owner.getComponent(RenderComponent.class);
        float bulletRadius = (bulletRender != null) ? bulletRender.getSize().x / 2 : BULLET_SIZE / 2;

        List<MonsterComponent> monsters = scene.getComponents(MonsterComponent.class);
        for (MonsterComponent monster : monsters) {
            GameObject monsterOwner = monster.getOwner();
            if (!monsterOwner.isActive()) continue;

            TransformComponent monsterTransform = monsterOwner.getComponent(TransformComponent.class);
            RenderComponent monsterRender = monsterOwner.getComponent(RenderComponent.class);

            if (monsterTransform == null) continue;

            float monsterRadius = (monsterRender != null) ? monsterRender.getSize().x / 2 : 0;
            float distance = transform.getPosition().distance(monsterTransform.getPosition());

            if (distance <= bulletRadius + monsterRadius) {
                System.out.println("子弹命中妖精！");
                owner.destroy();
                monsterOwner.destroy();

                PlayerComponent huluwa = scene.getHuluwaPlayerComponent();
                if (huluwa != null && huluwa.getHealth() <= 95) {
                    huluwa.changeHealth(5);
                }
                scene.addScore(1);
                scene.spawnMonster();
                return;
            }
        }
    }

    @Override
    public void render() {}
}
