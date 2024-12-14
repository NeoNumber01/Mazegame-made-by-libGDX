package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Path extends Block {
    public Path(Maze maze, TextureRegion texture, float x, float y) {
        super(maze, texture, x, y, true);
    }
}
