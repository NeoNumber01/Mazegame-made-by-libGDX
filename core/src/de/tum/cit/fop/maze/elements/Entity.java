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

    @Override
    public void performMovement(Vector2 delta) {
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
}
