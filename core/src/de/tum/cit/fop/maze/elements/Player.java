package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import de.tum.cit.fop.maze.Helper.Direction;

public class Player extends GameObject implements Visible {

    private final float width = 16f;
    private final float height = 32f; // match with ResourcePack
    private final MoveAnimation walkAnimation;
    private final Rectangle box;
    private final float boxSize = 16f;
    private Direction direction;

    public Player(Vector2 position, MoveAnimation walkAnimation) {
        this.walkAnimation = walkAnimation;
        direction = Direction.DOWN;

        box = new Rectangle(position.x, position.y, boxSize, boxSize);
    }

    public float getBoxSize() {
        return boxSize;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public Rectangle getBox() {
        return box;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    @Override
    public TextureRegion getTexture(float stateTime) {
        return walkAnimation.getTexture(direction, stateTime);
    }
}
