package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

/** A basic type of block that the player cannot walk on. */
public class Wall extends Block {
    public Wall(Maze maze, TextureRegion texture, Vector2 position) {
        super(maze, texture, position, true);
    }
}
