package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import de.tum.cit.fop.maze.Helper;
import de.tum.cit.fop.maze.Helper.Direction;
import de.tum.cit.fop.maze.MazeRunnerGame;

public class Player extends Entity {

    private final MoveAnimation walkAnimation, sprintAnimation;
    private Vector2 position;
    private Direction direction;

    public Player(MazeRunnerGame game, Vector2 position) {
        // TextureRegion cut from assets is 16x32
        // However, actual visible part 16x22 in walk animation, which we define as the hitbox size
        // of player
        super(game, position, new Vector2(16f, 22f), new Vector2(0f, -5f));
        walkAnimation = game.getResourcePack().getPlayerWalkAnimation();
        sprintAnimation = game.getResourcePack().getPlayerSprintAnimation();
        direction = Direction.DOWN;
    }

    @Override
    public void performMovement(Vector2 delta) {
        direction = Helper.directionToVector2(delta);
        super.performMovement(delta);
    }

    @Override
    public void render() {
        MoveAnimation currentMoveAnimation = isSprinting() ? sprintAnimation : walkAnimation;
        TextureRegion texture =
                currentMoveAnimation.getTexture(direction, super.game.getStateTime());
        Vector2 visualPosition = super.getVisualPosition();
        super.game
                .getSpriteBatch()
                .draw(
                        texture,
                        visualPosition.x,
                        visualPosition.y,
                        texture.getRegionWidth(),
                        texture.getRegionHeight());
    }

    private boolean isSprinting() {
        return Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT);
    }

    @Override
    public float getMoveDistance(float deltaTime) {
        return globalSpeedFactor * deltaTime * (isSprinting() ? 1.5f : 1f);
    }
}
