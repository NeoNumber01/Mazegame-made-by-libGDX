package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import de.tum.cit.fop.maze.Helper;

/** A special kind of obstacle. A wall but moves itself. */
public class MovableWall extends Entity {
    private static final float MOVE_SPEED = 30f;
    private final TextureRegion wallTexture;
    private float scale = 2.0f;

    public MovableWall(Maze maze, TextureRegion wallTexture, Vector2 position) {
        super(maze, position, new Vector2(32, 32), Vector2.Zero);
        this.wallTexture = wallTexture;
        direction = Helper.getRandomDirection();
    }

    @Override
    public void onFrame(float deltaTime) {
        super.onFrame(deltaTime);

        float distance = getMoveDistance(deltaTime);

        Vector2 displacement = direction.toVector2(distance);

        Vector2 newPosition = getPosition().cpy().add(displacement);
        Array<MazeObject> others = getCollision(newPosition);

        boolean collide = false;
        for (MazeObject other : others) {
            if (other instanceof Wall || other instanceof Entity) {
                collide = true;
                break;
            }
        }

        if (collide) {
            changeDirection();
        } else {
            performDisplacement(displacement);
        }
    }

    @Override
    public void render() {

        renderTextureV2(this.wallTexture, scale);
    }

    @Override
    public void performDisplacement(float deltaTime, Helper.Direction direction) {
        super.performDisplacement(deltaTime, direction);
    }

    @Override
    public float getMoveDistance(float deltaTime) {
        return MOVE_SPEED * deltaTime;
    }

    /** Changes current direction to its opposite. */
    public void changeDirection() {
        switch (direction) {
            case LEFT -> direction = Helper.Direction.RIGHT;
            case RIGHT -> direction = Helper.Direction.LEFT;
            case DOWN -> direction = Helper.Direction.UP;
            case UP -> direction = Helper.Direction.DOWN;
        }
    }

    @Override
    public void onCollision(MazeObject other) {
        if (other instanceof Mob mob) {
            mob.changeDirection();
        }
    }
}
