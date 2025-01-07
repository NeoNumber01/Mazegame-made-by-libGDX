package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public class Player extends Entity {

    private final MoveAnimation walkAnimation, sprintAnimation;
    private Vector2 position;

    public Player(Maze maze, Vector2 position) {
        // TextureRegion cut from assets is 16x32
        // However, actual visible part 16x22 in walk animation, which we define as the hitbox size
        // of player
        super(maze, position, new Vector2(16f, 22f), new Vector2(0f, -5f));
        walkAnimation = game.getResourcePack().getPlayerWalkAnimation();
        sprintAnimation = game.getResourcePack().getPlayerSprintAnimation();
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
}
