package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.math.Rectangle;

public interface Collision {
    boolean overlaps(Rectangle other);

    /**
     * Triggers events when moving object other collides into this. Nothing happens by default.
     *
     * @param other the moving object
     */
    default void onCollision(MazeObject other) {}
}
