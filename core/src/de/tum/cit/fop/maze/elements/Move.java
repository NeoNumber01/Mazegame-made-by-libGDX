package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.math.Vector2;

import de.tum.cit.fop.maze.Helper;

/** Basic movement mechanics */
public interface Move {
    float globalSpeedFactor = 64f;

    /**
     * Perform movement, with collision considered
     *
     * @param delta vector denoting the distance and direction of the movement
     */
    void performDisplacement(Vector2 delta);

    /**
     * A QoL wrapper that calls performDisplacement() with default move distance towards given
     * direction.
     */
    default void performDisplacement(float deltaTime, Helper.Direction direction) {
        performDisplacement(direction.toVector2(getMoveDistance(deltaTime)));
    }

    /** Returns the default distance to move on x- or y-axis. */
    default float getMoveDistance(float deltaTime) {
        return globalSpeedFactor * deltaTime;
    }
}
