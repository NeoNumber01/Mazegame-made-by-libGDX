package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public class Entry extends Path {
    public Entry(Maze maze, TextureRegion texture, Vector2 position) {
        super(maze, texture, position);
    }
}
