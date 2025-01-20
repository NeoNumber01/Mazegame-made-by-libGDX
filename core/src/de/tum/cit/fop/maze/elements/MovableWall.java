package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import de.tum.cit.fop.maze.Helper;

public class MovableWall extends Entity {
    private final TextureRegion wallTexture;
    private static final float MOVE_SPEED = 30f;
    private float scale = 2.0f;
    private Vector2 offset = new Vector2(0, 0);

    public MovableWall(Maze maze, TextureRegion wallTexture, Vector2 position) {
        super(maze, position, new Vector2(32, 32), Vector2.Zero);
        this.wallTexture = wallTexture;
        direction = Helper.getRandomDirection();
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public void setOffset(Vector2 offset) {
        this.offset = offset;
    }

    @Override
    public void onFrame(float deltaTime) {
        super.onFrame(deltaTime);

        float distance = getMoveDistance(deltaTime);

        Vector2 displacement = direction.toVector2(distance);

        Vector2 newPosition = getPosition().cpy().add(displacement);
        if (checkCollision(newPosition)) {

            changeDirection();
        } else {

            performDisplacement(displacement);
        }
    }

    @Override
    public void render() {

        renderTextureV2(this.wallTexture, scale, offset);
    }

    @Override
    public void onCollision(MazeObject other) {
        // 如果 other 是 Mobs 类型，那就让它掉血 10
        if (other instanceof Mob) {
            ((Mob) other).modifyHealth(-10);
        }
    }

    @Override
    public void performDisplacement(float deltaTime, Helper.Direction direction) {
        super.performDisplacement(deltaTime, direction);
    }

    @Override
    public float getMoveDistance(float deltaTime) {
        return MOVE_SPEED * deltaTime;
    }

    public void changeDirection() {
        switch (direction) {
            case LEFT -> direction = Helper.Direction.RIGHT;
            case RIGHT -> direction = Helper.Direction.LEFT;
            case DOWN -> direction = Helper.Direction.UP;
            case UP -> direction = Helper.Direction.DOWN;
        }
    }
}
