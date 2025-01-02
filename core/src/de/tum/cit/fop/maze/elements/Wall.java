package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public class Wall extends Block {
    public Wall(Maze maze, TextureRegion texture, Vector2 position) {
        super(maze, texture, position, true);
    }
}
