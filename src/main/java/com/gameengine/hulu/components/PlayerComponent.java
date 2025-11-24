package com.gameengine.hulu.components;

import com.gameengine.components.PhysicsComponent;
import com.gameengine.components.RenderComponent;
import com.gameengine.components.TransformComponent;
import com.gameengine.core.Component;
import com.gameengine.core.GameObject;
import com.gameengine.hulu.HuluEntityFactory;
import com.gameengine.hulu.scene.HuluMainScene;
import com.gameengine.input.InputManager;
import com.gameengine.math.Vector2;

import java.util.List;

public class PlayerComponent extends Component<PlayerComponent> {
    private static final int SCREEN_WIDTH = 800;
    private static final int SCREEN_HEIGHT = 600;
    private static final float HULUWA_SPEED = 200.0f;
    private static final float HULUWA_MELEE_RANGE = 80.0f;
    private static final float HULUWA_MELEE_COOLDOWN = 0.1f;
    private static final float HULUWA_SHOOT_COOLDOWN = 0.5f;

    private int health = 100;
    private float lastMeleeAttackTime = 0.0f;
    private float lastRangedAttackTime = 0.0f;
    private boolean isBlocking = false;
    private final InputManager inputManager = InputManager.getInstance();
    private final HuluMainScene scene;

    public PlayerComponent(HuluMainScene scene) {
        this.scene = scene;
    }

    public int getHealth() { return health; }
    public void changeHealth(int delta) { this.health += delta; }
    public boolean isBlocking() { return isBlocking; }

    @Override
    public void initialize() {}

    @Override
    public void update(float deltaTime) {
        lastMeleeAttackTime += deltaTime;
        lastRangedAttackTime += deltaTime;

        handleMovement(deltaTime);

        // K键 (GLFW 75)
        isBlocking = inputManager.isKeyPressed(75);

        RenderComponent render = owner.getComponent(RenderComponent.class);
        if (render != null) {
            if (isBlocking) {
                render.setColor(0.0f, 0.0f, 1.0f, 1.0f); // 蓝色防御
            } else {
                render.setColor(0.0f, 1.0f, 0.0f, 1.0f); // 绿色正常
            }
        }

        // J键 (GLFW 74)
        boolean isMeleeAttacking = inputManager.isKeyPressed(74);
        if (isMeleeAttacking && lastMeleeAttackTime >= HULUWA_MELEE_COOLDOWN) {
            performMeleeAttack();
            lastMeleeAttackTime = 0.0f;
        }

        // L键 (GLFW 76)
        boolean isRangedAttacking = inputManager.isKeyJustPressed(76);
        if (isRangedAttacking && lastRangedAttackTime >= HULUWA_SHOOT_COOLDOWN) {
            if (isEnemyInMeleeRange()) {
                System.out.println("近战范围内有敌人，无法使用射击！");
            } else {
                Vector2 targetDirection = findNearestMonsterDirection();
                if (targetDirection != null) {
                    performRangedAttack(targetDirection);
                } else {
                    System.out.println("没有可瞄准的敌人！");
                }
            }
            lastRangedAttackTime = 0.0f;
        }
    }

    @Override
    public void render() {}

    private void handleMovement(float deltaTime) {
        TransformComponent transform = owner.getComponent(TransformComponent.class);
        PhysicsComponent physics = owner.getComponent(PhysicsComponent.class);
        if (transform == null || physics == null) return;

        Vector2 movement = new Vector2();
        // W(87), S(83), A(65), D(68), Up(265), Down(264), Left(263), Right(262)
        if (inputManager.isKeyPressed(87) || inputManager.isKeyPressed(265)) movement.y -= 1;
        if (inputManager.isKeyPressed(83) || inputManager.isKeyPressed(264)) movement.y += 1;
        if (inputManager.isKeyPressed(65) || inputManager.isKeyPressed(263)) movement.x -= 1;
        if (inputManager.isKeyPressed(68) || inputManager.isKeyPressed(262)) movement.x += 1;

        if (movement.magnitude() > 0) {
            physics.setVelocity(movement.normalize().multiply(HULUWA_SPEED));
        } else {
            physics.setVelocity(new Vector2(0, 0));
        }

        Vector2 pos = transform.getPosition();
        RenderComponent render = owner.getComponent(RenderComponent.class);
        float size = (render != null) ? render.getSize().x / 2 : 15;

        if (pos.x - size < 0) pos.x = size;
        if (pos.y - size < 0) pos.y = size;
        if (pos.x + size > SCREEN_WIDTH) pos.x = SCREEN_WIDTH - size;
        if (pos.y + size > SCREEN_HEIGHT) pos.y = SCREEN_HEIGHT - size;
        transform.setPosition(pos);
    }

    private boolean isEnemyInMeleeRange() {
        TransformComponent huluwaTransform = owner.getComponent(TransformComponent.class);
        if (huluwaTransform == null) return false;

        List<MonsterComponent> monsters = scene.getComponents(MonsterComponent.class);
        Vector2 huluwaPos = huluwaTransform.getPosition();

        for (MonsterComponent monster : monsters) {
            GameObject monsterOwner = monster.getOwner();
            if (!monsterOwner.isActive()) continue;

            TransformComponent monsterTransform = monsterOwner.getComponent(TransformComponent.class);
            RenderComponent monsterRender = monsterOwner.getComponent(RenderComponent.class);
            if (monsterTransform == null) continue;

            float monsterRadius = (monsterRender != null) ? monsterRender.getSize().x / 2 : 0;
            float distance = huluwaPos.distance(monsterTransform.getPosition());

            if (distance <= HULUWA_MELEE_RANGE + monsterRadius) {
                return true;
            }
        }
        return false;
    }

    private Vector2 findNearestMonsterDirection() {
        TransformComponent huluwaTransform = owner.getComponent(TransformComponent.class);
        if (huluwaTransform == null) return null;

        List<MonsterComponent> monsters = scene.getComponents(MonsterComponent.class);
        Vector2 huluwaPos = huluwaTransform.getPosition();

        float minDistanceSq = Float.MAX_VALUE;
        Vector2 nearestMonsterPos = null;

        for (MonsterComponent monster : monsters) {
            GameObject monsterOwner = monster.getOwner();
            if (!monsterOwner.isActive()) continue;

            TransformComponent monsterTransform = monsterOwner.getComponent(TransformComponent.class);
            if (monsterTransform == null) continue;

            Vector2 monsterPos = monsterTransform.getPosition();
            float distanceSq = (monsterPos.x - huluwaPos.x) * (monsterPos.x - huluwaPos.x) +
                               (monsterPos.y - huluwaPos.y) * (monsterPos.y - huluwaPos.y);

            if (distanceSq < minDistanceSq) {
                minDistanceSq = distanceSq;
                nearestMonsterPos = monsterPos;
            }
        }

        if (nearestMonsterPos != null) {
            return nearestMonsterPos.subtract(huluwaPos);
        }
        return null;
    }

    private void performMeleeAttack() {
        TransformComponent huluwaTransform = owner.getComponent(TransformComponent.class);
        if (huluwaTransform == null) return;

        List<MonsterComponent> monsters = scene.getComponents(MonsterComponent.class);
        GameObject defeatedMonster = null;

        for (MonsterComponent monster : monsters) {
            GameObject monsterOwner = monster.getOwner();
            if (!monsterOwner.isActive()) continue;

            TransformComponent monsterTransform = monsterOwner.getComponent(TransformComponent.class);
            if (monsterTransform == null) continue;

            float distance = huluwaTransform.getPosition().distance(monsterTransform.getPosition());
            RenderComponent monsterRender = monsterOwner.getComponent(RenderComponent.class);
            float monsterRadius = (monsterRender != null) ? monsterRender.getSize().x / 2 : 0;

            if (distance <= HULUWA_MELEE_RANGE + monsterRadius) {
                System.out.println("葫芦娃近战攻击命中妖精！");
                defeatedMonster = monsterOwner;
                if (health <= 95) {
                    this.changeHealth(5);
                }
                break;
            }
        }

        if (defeatedMonster != null) {
            defeatedMonster.destroy();
            scene.addScore(1);
            if (Math.random() < 0.5) {
                scene.spawnMonster();
            }
        }
    }

    private void performRangedAttack(Vector2 direction) {
        TransformComponent huluwaTransform = owner.getComponent(TransformComponent.class);
        if (huluwaTransform == null) return;

        Vector2 huluwaPos = huluwaTransform.getPosition();
        GameObject bullet = HuluEntityFactory.createBullet(scene, huluwaPos, direction);
        scene.addGameObject(bullet);
        System.out.println("葫芦娃发射子弹！");
    }
}
