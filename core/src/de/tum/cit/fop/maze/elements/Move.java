package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.math.Vector2;

public interface Move {
    float globalSpeedFactor = 64f;

    /**
     * Perform movement without checking collision
     *
     * @param delta vector denoting the distance and direction of the movement
     */
    void performMovement(Vector2 delta);

    /** Returns the distance to move on x- or y-axis. */
    float getMoveDistance(float deltaTime);
}
