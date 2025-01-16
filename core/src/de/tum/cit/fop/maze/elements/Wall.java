package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public class Wall extends Block {
    public Wall(Maze maze, TextureRegion texture, Vector2 position) {
        super(maze, texture, position, true);
    }

    @Override
    public void onCollision(MazeObject other) {
        if (other instanceof Mob) {
            // If the mob still goes along the wall, this will again be triggered and eventually let
            // it leave through the opposite direction it comes.
            // TODO: mark collision events so that this is only called once.
            ((Mob) other).changeDirection();
        }
    }
}
