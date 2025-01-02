package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import de.tum.cit.fop.maze.Helper;
import de.tum.cit.fop.maze.Helper.Direction;
import de.tum.cit.fop.maze.MazeRunnerGame;

public class Player extends Entity {

    // TODO: REFACTORING!

    private final MoveAnimation walkAnimation; // TODO: add run animation etc.
    private Vector2 position;
    private Direction direction;

    public Player(MazeRunnerGame game, Vector2 position) {
        // TextureRegion cut from assets is 16x32
        // However, actual visible part 16x22 in walk animation, which we define as the hitbox size
        // of player
        super(game, position, new Vector2(16f, 22f), new Vector2(0f, -5f));
        walkAnimation = game.getResourcePack().getPlayerWalkAnimation();
        direction = Direction.DOWN;
    }

    @Override
    public void performMovement(Vector2 delta) {
        direction = Helper.directionToVector2(delta);
        super.performMovement(delta);
    }

    @Override
    public void render() {
        TextureRegion texture = walkAnimation.getTexture(direction, super.game.getStateTime());
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
}
