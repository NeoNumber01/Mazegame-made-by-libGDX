package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.math.Rectangle;

/**
 * Basic abstraction of collision. Provides a simple wrapper to detect overlapping, and triggers
 * event on collision.
 */
public interface Collision {
    /** Returns if the hitbox of current object overlaps with given rectangle. */
    boolean overlaps(Rectangle other);

    /**
     * Triggers events when moving object other collides into this. Nothing happens by default.
     *
     * @param other the moving object
     */
    default void onCollision(MazeObject other) {}
}
