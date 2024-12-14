package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Wall extends Block {
    public Wall(Maze maze, TextureRegion texture, float x, float y) {
        super(maze, texture, x, y, false);
    }
}
