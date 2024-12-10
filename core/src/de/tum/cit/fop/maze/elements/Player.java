package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import de.tum.cit.fop.maze.Helper.Direction;

public class Player extends GameObject implements Visible {

    private Vector2 position;
    private MoveAnimation walkAnimation;
    private Direction direction;

    public Player(Vector2 position, MoveAnimation walkAnimation) {
        this.position = position;
        this.walkAnimation = walkAnimation;
        direction = Direction.DOWN;
    }

    public Vector2 getPosition() {
        return position;
    }

    @Override
    public TextureRegion getTexture(float stateTime) {
        return walkAnimation.getTexture(direction, stateTime);
    }

    public void move(Direction direction, float distance) {
        this.direction = direction;
        position.add(direction.toVector2(distance));
    }
}
