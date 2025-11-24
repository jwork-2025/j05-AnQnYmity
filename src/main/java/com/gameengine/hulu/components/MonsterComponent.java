package com.gameengine.hulu.components;

import com.gameengine.components.PhysicsComponent;
import com.gameengine.components.RenderComponent;
import com.gameengine.components.TransformComponent;
import com.gameengine.core.Component;
import com.gameengine.core.GameObject;
import com.gameengine.hulu.scene.HuluMainScene;
import com.gameengine.math.Vector2;

public class MonsterComponent extends Component<MonsterComponent> {
    private static final float MONSTER_ATTACK_INTERVAL = 5.0f;
    private static final int MONSTER_DAMAGE = 10;
    private static final float MONSTER_SPEED = 50.0f;

    private float attackTimer = 0.0f;
    private final float attackRange;
    private final HuluMainScene scene;

    public MonsterComponent(float attackRange, HuluMainScene scene) {
        this.attackRange = attackRange;
        this.scene = scene;
    }

    @Override
    public void initialize() {}

    @Override
    public void update(float deltaTime) {
        TransformComponent monsterTransform = owner.getComponent(TransformComponent.class);
        PhysicsComponent monsterPhysics = owner.getComponent(PhysicsComponent.class);
        if (monsterTransform == null || monsterPhysics == null) return;

        PlayerComponent huluwa = scene.getHuluwaPlayerComponent();
        if (huluwa == null) return;

        GameObject huluwaOwner = huluwa.getOwner();
        TransformComponent huluwaTransform = huluwaOwner.getComponent(TransformComponent.class);
        if (huluwaTransform == null) return;

        Vector2 directionToPlayer = huluwaTransform.getPosition().subtract(monsterTransform.getPosition());
        if (directionToPlayer.magnitude() > attackRange) {
            monsterPhysics.setVelocity(directionToPlayer.normalize().multiply(MONSTER_SPEED));
        } else {
            monsterPhysics.setVelocity(new Vector2(0, 0));
        }

        float distance = monsterTransform.getPosition().distance(huluwaTransform.getPosition());
        RenderComponent monsterRender = owner.getComponent(RenderComponent.class);
        float monsterRadius = (monsterRender != null) ? monsterRender.getSize().x / 2 : 0;

        if (distance <= attackRange + monsterRadius) {
            attackTimer += deltaTime;
            if (monsterRender != null) {
                float ratio = Math.min(1.0f, attackTimer / MONSTER_ATTACK_INTERVAL);
                monsterRender.setColor(1.0f, 1.0f - ratio, 1.0f - ratio, 1.0f);
            }

            if (attackTimer >= MONSTER_ATTACK_INTERVAL) {
                if (huluwa.isBlocking()) {
                    System.out.println("妖精攻击！但葫芦娃格挡成功！");
                } else {
                    huluwa.changeHealth(-MONSTER_DAMAGE);
                    System.out.println("妖精攻击！葫芦娃受到伤害，HP: " + huluwa.getHealth());
                    if (huluwa.getHealth() <= 0) {
                        System.out.println("游戏结束！葫芦娃被打败了。");
                        scene.getEngine().stop();
                    }
                }
                attackTimer = 0.0f;
            }
        } else {
            attackTimer = 0.0f;
            if (monsterRender != null) {
                monsterRender.setColor(1.0f, 0.0f, 1.0f, 1.0f);
            }
        }
    }

    @Override
    public void render() {}
}
