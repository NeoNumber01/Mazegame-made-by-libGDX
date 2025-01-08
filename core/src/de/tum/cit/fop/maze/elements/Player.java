package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public class Player extends Entity implements Health {

    private final MoveAnimation walkAnimation, sprintAnimation;
    private Vector2 position;
    private float maxHealth, health, lastHitTimestamp;

    public Player(Maze maze, Vector2 position) {
        // TextureRegion cut from assets is 16x32
        // However, actual visible part 16x22 in walk animation, which we define as the hitbox size
        // of player
        super(maze, position, new Vector2(16f, 22f), new Vector2(0f, -5f));
        maze.getEntities().add(this);
        walkAnimation = game.getResourcePack().getPlayerWalkAnimation();
        sprintAnimation = game.getResourcePack().getPlayerSprintAnimation();
        health = maxHealth = 100f;
    }

    @Override
    public void render() {
        MoveAnimation currentMoveAnimation = isSprinting() ? sprintAnimation : walkAnimation;
        TextureRegion texture =
                currentMoveAnimation.getTexture(direction, super.game.getStateTime());
        super.renderTexture(texture);
    }

    private boolean isSprinting() {
        return Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT);
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
        }
        System.out.printf("Time=%f, Health=%f\n", game.getStateTime(), health);

        health += delta;
        if (health > maxHealth) health = maxHealth;
        if (health <= 0) {
            onEmptyHealth();
        }
    }

    @Override
    public void onEmptyHealth() {
        // TODO: end game
    }
}
