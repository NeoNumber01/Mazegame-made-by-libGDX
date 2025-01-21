package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.math.Vector2;

import de.tum.cit.fop.maze.Helper;

/** Interactive collectables, e.g. keys, power-ups. */
public abstract class InteractiveElements extends Entity {

    public InteractiveElements(Maze maze, Vector2 position, Vector2 size, Vector2 visualOffset) {
        super(maze, position, size, visualOffset);
    }

    @Override
    public void onCollision(MazeObject other) {
        super.onCollision(other);
    }

    @Override
    public void performDisplacement(float deltaTime, Helper.Direction direction) {
        super.performDisplacement(deltaTime, direction);
    }

    @Override
    public float getMoveDistance(float deltaTime) {
        return super.getMoveDistance(deltaTime);
    }
}
