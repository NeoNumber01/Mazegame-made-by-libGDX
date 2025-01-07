package de.tum.cit.fop.maze.elements;

public class MazeObject extends GameObject {
    protected final Maze maze;

    public MazeObject(Maze maze) {
        super(maze.game);
        this.maze = maze;
    }
}
