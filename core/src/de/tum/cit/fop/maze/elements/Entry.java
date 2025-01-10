package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import de.tum.cit.fop.maze.MazeRunnerGame;

public class Entry extends Path {
    public Entry(Maze maze, TextureRegion texture, Vector2 position) {
        super(maze, texture, position);
    }

    @Override
    public void onArrival(MazeObject other) {
        if (other instanceof Player) {
            System.out.println("Player entered the map!");
            MazeRunnerGame game = ((Player) other).getGame(); // Get the main game object
            game.startTimer(); // Start the timer when the player enters the map
        }
    }
}
