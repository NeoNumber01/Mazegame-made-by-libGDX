package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.math.Vector2;

public interface Move {
    /**
     * Perform movement without checking collision
     *
     * @param delta vector denoting the distance and direction of the movement
     */
    public void performMovement(Vector2 delta);
}
