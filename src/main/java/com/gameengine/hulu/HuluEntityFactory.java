package com.gameengine.hulu;

import com.gameengine.components.PhysicsComponent;
import com.gameengine.components.RenderComponent;
import com.gameengine.components.TransformComponent;
import com.gameengine.core.GameObject;
import com.gameengine.hulu.components.BulletComponent;
import com.gameengine.hulu.components.MonsterComponent;
import com.gameengine.hulu.components.PlayerComponent;
import com.gameengine.hulu.scene.HuluMainScene;
import com.gameengine.math.Vector2;

public class HuluEntityFactory {
    private static final float HULUWA_MELEE_RANGE = 80.0f;
    private static final float BULLET_SPEED = 600.0f;
    private static final float BULLET_SIZE = 10.0f;

    public static GameObject createHuluwa(HuluMainScene scene, Vector2 position) {
        GameObject huluwa = new GameObject("Huluwa");
        huluwa.addComponent(new TransformComponent(position));
        
        RenderComponent render = new RenderComponent(RenderComponent.RenderType.CIRCLE, new Vector2(30, 30), new RenderComponent.Color(0.0f, 1.0f, 0.0f, 1.0f));
        render.setRenderer(scene.getRenderer());
        huluwa.addComponent(render);

        PhysicsComponent physics = new PhysicsComponent();
        physics.setFriction(0.9f);
        huluwa.addComponent(physics);

        huluwa.addComponent(new PlayerComponent(scene));
        return huluwa;
    }

    public static GameObject createMonster(HuluMainScene scene, Vector2 position) {
        GameObject monster = new GameObject("Monster");
        monster.addComponent(new TransformComponent(position));

        RenderComponent render = new RenderComponent(RenderComponent.RenderType.RECTANGLE, new Vector2(25, 25), new RenderComponent.Color(1.0f, 0.0f, 1.0f, 1.0f));
        render.setRenderer(scene.getRenderer());
        monster.addComponent(render);

        PhysicsComponent physics = new PhysicsComponent(10.0f);
        physics.setFriction(0.5f);
        monster.addComponent(physics);

        monster.addComponent(new MonsterComponent(HULUWA_MELEE_RANGE * 0.9f, scene));
        return monster;
    }

    public static GameObject createBullet(HuluMainScene scene, Vector2 startPosition, Vector2 direction) {
        GameObject bullet = new GameObject("Bullet");
        Vector2 offset = direction.normalize().multiply(15.0f);
        bullet.addComponent(new TransformComponent(startPosition.add(offset)));

        RenderComponent render = new RenderComponent(RenderComponent.RenderType.CIRCLE, new Vector2(BULLET_SIZE, BULLET_SIZE), new RenderComponent.Color(1.0f, 1.0f, 0.0f, 1.0f));
        render.setRenderer(scene.getRenderer());
        bullet.addComponent(render);

        Vector2 velocity = direction.normalize().multiply(BULLET_SPEED);
        bullet.addComponent(new BulletComponent(velocity, scene));
        return bullet;
    }
}
