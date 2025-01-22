package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import de.tum.cit.fop.maze.GameOverScreen;
import de.tum.cit.fop.maze.Helper;
import de.tum.cit.fop.maze.MazeRunnerGame;

import java.util.Objects;

/** The main characters. */
public class Player extends Entity implements Health {

    private final MoveAnimation walkAnimation, sprintAnimation, attackAnimation;
    private final float maxHealth;
    private final MazeRunnerGame game;
    private final float shieldDuration = 10f;
    private final float attackAnimationDuration = 0.4f;
    private final Sound playerOnHit;
    private final Sound swing;
    private final Sound monsterHit;
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
        this.playerOnHit = Gdx.audio.newSound(Gdx.files.internal("playeronHit.wav"));
        this.swing = Gdx.audio.newSound(Gdx.files.internal("swing.wav"));
        this.monsterHit = Gdx.audio.newSound(Gdx.files.internal("monster.wav"));
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

    /** checks if player is sprinting. Compatibility code. */
    private boolean isSprinting() {
        return getMotion() == Motion.SPRINT;
    }

    @Override
    public float getMoveDistance(float deltaTime) {
        return globalSpeedFactor * deltaTime * (isSprinting() ? 1.5f : 1f);
    }

    @Override
    public void modifyHealth(float delta) {
        if (delta < 0) {
            if (game.getStateTime() - lastHitTimestamp < 1) {

                return;
            } else {
                lastHitTimestamp = game.getStateTime();
            }
            if (playerOnHit != null) {
                playerOnHit.play(); // 播放音效
            }
            isRed = true;
            redEffectTimer = 1f;
        }
        if (delta < 0 && hasShield) {
            delta += 10; // Reduce damage by 5
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
                "Time=%f, Health=%f, Delta=%f\n", game.getStateTime(), health, delta);
    }

    @Override
    public void onEmptyHealth() {
        System.out.println("Player has died!");
        game.setScreen(new GameOverScreen(game)); // 切换到 GameOverScreen
    }

    /** triggers shield effect. */
    public void activateShield() {
        this.hasShield = true;
        this.shieldStartTime = game.getStateTime();
    }

    /** Ends shield effect. */
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
            // only move 1 unit instead of \sqrt(2) unit when moving diagonally
            if (deltaPos.len() > deltaDist) {
                deltaPos.x /= (float) Math.sqrt(2);
                deltaPos.y /= (float) Math.sqrt(2);
            }

            this.performDisplacement(deltaPos);
            // force update player direction, so that player can attack to arbitrary direction in
            // case of collision
            if (deltaPos.len2() > 0f) {
                direction = Helper.Vector2Direction(deltaPos);
            }
        }

        handleTimers(deltaTime);
    }

    /** handles timers, should be called on every frame. */
    private void handleTimers(float deltaTime) {
        attackAnimationTimer = Math.max(0f, attackAnimationTimer - deltaTime);
    }

    /**
     * Tries to perform an attack. Checks if there is ongoing attack animation, and deals damage to
     * mobs.
     */
    public void attack() {

        if (getMotion() != Motion.ATTACK) { // only execute once during one attack animation
            attackAnimationTimer = attackAnimationDuration;

            // actual hit range is attackHitboxSizeW * attackHitboxSizeH
            // W is the edge vertical to player's direction
            float attackHitboxSizeW = 24f, attackHitboxSizeH = 16f;
            Rectangle attackHitbox =
                    switch (direction) {
                        case UP ->
                                new Rectangle(
                                        getCenter().x - attackHitboxSizeW / 2f,
                                        getPosition().y + getSize().y,
                                        attackHitboxSizeW,
                                        attackHitboxSizeH);
                        case DOWN ->
                                new Rectangle(
                                        getCenter().x - attackHitboxSizeW / 2f,
                                        getPosition().y - attackHitboxSizeH,
                                        attackHitboxSizeW,
                                        attackHitboxSizeH);
                        case LEFT ->
                                new Rectangle(
                                        getPosition().x - attackHitboxSizeH,
                                        getCenter().y - attackHitboxSizeW / 2f,
                                        attackHitboxSizeH,
                                        attackHitboxSizeW);
                        case RIGHT ->
                                new Rectangle(
                                        getPosition().x + getSize().x,
                                        getCenter().y - attackHitboxSizeW / 2f,
                                        attackHitboxSizeH,
                                        attackHitboxSizeW);
                    };
            for (MazeObject other : getCollision(attackHitbox)) {
                if (other instanceof Mob mob) {
                    System.out.println("Hit!");
                    mob.modifyHealth(-10f);
                    monsterHit.play();
                }
            }
            swing.play();
        }
    }

    /** Get the status of the player. We require the player to stop before attack. */
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
