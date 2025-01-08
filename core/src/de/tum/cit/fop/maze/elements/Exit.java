package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public class Exit extends Path {
    public Exit(Maze maze, TextureRegion texture, Vector2 position) {
        super(maze, texture, position);
    }

    @Override
    public void onArrival(MazeObject other) {
        if (other instanceof Player) {
            System.out.println("Player arrived exit!");
        }
    }
}
