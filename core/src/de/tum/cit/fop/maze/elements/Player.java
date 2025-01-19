package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import de.tum.cit.fop.maze.GameOverScreen;
import de.tum.cit.fop.maze.Helper;
import de.tum.cit.fop.maze.MazeRunnerGame;

import java.util.Objects;

public class Player extends Entity implements Health {

    private final MoveAnimation walkAnimation, sprintAnimation, attackAnimation;
    private final float maxHealth;
    private final MazeRunnerGame game;
    private final float shieldDuration = 30f;
    private final float attackAnimationDuration = 0.4f;
    private Vector2 position;
    private float health;
    private float lastHitTimestamp;
    private boolean hasKey;
    private float speedFactor = 64f;
    private boolean hasShield = false;
    private float shieldStartTime = 0f;
    private float attackAnimationTimer = 0f;
    private boolean isRed = false;
    private float redEffectTimer = 0f;

    public Player(MazeRunnerGame game, Maze maze, Vector2 position) {
        // TextureRegion cut from assets is 16x32
        // However, actual visible part 16x22 in walk animation, which we define as the hitbox size
        // of player
        super(maze, position, new Vector2(16f, 22f), new Vector2(0f, -5f));
        maze.setPlayer(this);
        walkAnimation = game.getResourcePack().getPlayerWalkAnimation();
        sprintAnimation = game.getResourcePack().getPlayerSprintAnimation();
        attackAnimation = game.getResourcePack().getPlayerAttackAnimation();
        health = maxHealth = 100f;
        this.game = game;
        this.hasKey = false;
    }

    @Override
    public void render() {
        if (isRed) {
            maze.getGame().getSpriteBatch().setColor(1f, 0f, 0f, 1f);
        }
        if (attackAnimationTimer > 0f) {
            super.renderTextureV2(
                    attackAnimation.getTextureNoLoop(
                            direction, attackAnimationDuration - attackAnimationTimer),
                    1f,
                    new Vector2(0f, 0f));

        } else {
            MoveAnimation currentMoveAnimation = isSprinting() ? sprintAnimation : walkAnimation;
            TextureRegion texture =
                    currentMoveAnimation.getTexture(direction, super.game.getStateTime());
            super.renderTexture(texture);
        }
        maze.getGame().getSpriteBatch().setColor(0f, 0f, 0f, 1f);
    }

    private boolean isSprinting() {
        return getMotion() == Motion.SPRINT;
    }

    @Override
    public float getMoveDistance(float deltaTime) {
        return globalSpeedFactor * deltaTime * (isSprinting() ? 1.5f : 1f);
    }

    @Override
    public void modifyHealth(float delta) {
        // 如果是扣血（delta < 0），才执行冷却检查
        if (delta < 0) {
            if (game.getStateTime() - lastHitTimestamp < 1) {

                return;
            } else {
                lastHitTimestamp = game.getStateTime();
            }
            isRed = true;
            redEffectTimer = 1f;
        }
        if (delta < 0 && hasShield) {
            delta += 5; // Reduce damage by 5
            if (delta > 0) {
                delta = 0; // Ensure no health is gained from reduced damage
            }
        }
        if (hasShield && game.getStateTime() - shieldStartTime >= shieldDuration) {
            deactivateShield();
        }

        // 更新血量
        health += delta;

        // 不允许超过最大血量
        if (health > maxHealth) {
            health = maxHealth;
        }

        // 低于或等于 0，玩家死亡逻辑
        if (health <= 0) {
            onEmptyHealth();
        }
        // 日志输出：查看当前时间、改变前的血量以及改变值
        System.out.printf(
                "Time=%f, Health Before=%f, Delta=%f\n", game.getStateTime(), health, delta);
        // 日志输出：查看更新后的血量
        System.out.printf("Health After=%f\n", health);
    }

    @Override
    public void onEmptyHealth() {
        // TODO: end game
        System.out.println("Player has died!");
        game.setScreen(new GameOverScreen(game)); // 切换到 GameOverScreen
    }

    // Shield
    public void activateShield() {
        this.hasShield = true;
        this.shieldStartTime = game.getStateTime();
    }

    private void deactivateShield() {
        this.hasShield = false;
    }

    public MazeRunnerGame getGame() {
        return game;
    }

    public boolean hasKey() {
        return hasKey;
    }

    public void setHasKey(boolean hasKey) {
        this.hasKey = hasKey;
    }

    public float getHealth() {
        return health;
    }

    public float getSpeedFactor() {
        return speedFactor;
    }

    public void setSpeedFactor(float speedFactor) {
        this.speedFactor = speedFactor;
    }

    public boolean hasShield() {
        return this.hasShield;
    }

    @Override
    public void onFrame(float deltaTime) {
        if (isRed) {
            redEffectTimer -= deltaTime;
            if (redEffectTimer <= 0f) {
                isRed = false; // 红色效果结束
            }
        }
        if (Objects.requireNonNull(getMotion())
                == Motion.ATTACK) { // do nothing when currently performing attack animation
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            attack();
        } else {
            Vector2 deltaPos = new Vector2();
            float deltaDist = getMoveDistance(deltaTime);
            if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
                deltaPos.y += deltaDist;
            }
            if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
                deltaPos.y -= deltaDist;
            }
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
                deltaPos.x -= deltaDist;
            }
            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
                deltaPos.x += deltaDist;
            }
            this.performDisplacement(deltaPos);
            // force update player direction, so that player can attack to arbitrary direction in
            // case of collision
            direction = Helper.Vector2Direction(deltaPos);
        }

        handleTimers(deltaTime);
    }

    private void handleTimers(float deltaTime) {
        attackAnimationTimer = Math.max(0f, attackAnimationTimer - deltaTime);
    }

    public void attack() {
        if (getMotion() != Motion.ATTACK) { // only execute once during one attack animation
            attackAnimationTimer = attackAnimationDuration;

            Vector2 attackHitboxOffset =
                    switch (direction) {
                        case UP -> new Vector2(0f, getSize().y);
                        case DOWN -> new Vector2(0f, -getSize().y);
                        case LEFT -> new Vector2(-getSize().x, 0f);
                        case RIGHT -> new Vector2(getSize().x, 0f);
                    };
            Rectangle attackHitbox =
                    new Rectangle(
                            getPosition().x + attackHitboxOffset.x,
                            getPosition().y + attackHitboxOffset.y,
                            getSize().x,
                            getSize().y);
            for (MazeObject other : getCollision(attackHitbox)) {
                if (other instanceof Mob mob) {
                    System.out.println("Hit!");
                    mob.modifyHealth(-10f);
                }
            }
        }
    }

    private Motion getMotion() {
        if (attackAnimationTimer > 0) {
            return Motion.ATTACK;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
            return Motion.SPRINT;
        }
        return Motion.WALK;
    }

    private enum Motion {
        WALK,
        SPRINT,
        ATTACK
    }
}
