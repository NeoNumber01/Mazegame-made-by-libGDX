package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import de.tum.cit.fop.maze.EndScreen;
import de.tum.cit.fop.maze.MazeRunnerGame;

public class Exit extends Path {
    public Exit(Maze maze, TextureRegion texture, Vector2 position) {
        super(maze, texture, position);
    }

    @Override
    public void onArrival(MazeObject other) {

        if (other instanceof Player player) {
            if (!player.hasKey()) {
                System.out.println("You haven't got the key");
                return;
            }
            System.out.println("Player arrived at the exit!");
            MazeRunnerGame game = other.getGame(); // Get the main game object

            long elapsedTime = game.getElapsedTime(); // Get the total elapsed time
            int score = game.calculateScore(elapsedTime); // Use game's calculateScore method

            game.setScreen(new EndScreen(game, score)); // Pass the score to EndScreen
        }
    }
}
