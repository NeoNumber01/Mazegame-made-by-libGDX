package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.math.Vector2;

public class
Skeleton extends Mob {
    public Skeleton(Maze maze, Vector2 position) {
        super(
                maze,
                position,
                new Vector2(10f, 15f),
                new Vector2(-3f, -1f),
                maze.game.getResourcePack().getSkeletonMoveAnimation());
    }
}
