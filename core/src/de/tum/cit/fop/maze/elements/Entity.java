package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import de.tum.cit.fop.maze.Helper;

/** A game object, which can move, has a hitbox, and animation. */
public abstract class Entity extends MazeObject implements Move {
    protected Helper.Direction direction;

    public Entity(Maze maze, Vector2 position, Vector2 size, Vector2 visualOffset) {
        super(maze, position, size, visualOffset);
        direction = Helper.Direction.DOWN;
    }

    public void performForceDisplacement(Vector2 delta) {
        direction = Helper.Vector2Direction(delta);
        super.displace(delta);
    }

    /** Checks if it is possible to move an entity to some position */
    public boolean checkCollide(Vector2 position) {
        Rectangle rect = new Rectangle(position.x, position.y, getSize().x, getSize().y);
        for (MazeObject other : maze) {
            if (other != this && other.overlaps(rect)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void performDisplacement(Vector2 displacement) {
        // check the feasibility on x- and y-axis separately, this avoids the extremely complex
        // handling when moving with collision happening on the other axis
        Vector2 projectionX = new Vector2(displacement.x, 0f),
                projectionY = new Vector2(0f, displacement.y);
        if (!checkCollide(getPosition().add(projectionX))) {
            performForceDisplacement(projectionX);
        }
        if (!checkCollide(getPosition().add(projectionY))) {
            performForceDisplacement(projectionY);
        }
    }
}
