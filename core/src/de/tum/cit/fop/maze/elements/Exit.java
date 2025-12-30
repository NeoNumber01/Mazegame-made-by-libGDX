package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.SpaceCruisesMiniGameScreen;
import de.tum.cit.fop.maze.StoryScreen;

/** Exit of the maze, where the victory may be triggered. */
public class Exit extends Path {
    public Exit(Maze maze, TextureRegion texture, Vector2 position) {
        super(maze, texture, position);
    }

    @Override
    public void onArrival(MazeObject other) {

        if (other instanceof Player player) {
            if (!player.hasKey()) {
                System.out.println("You haven't got the key");
                StoryScreen.getInstance().showMessage("You need a key to exit!");
                return;
            }
            System.out.println("Player arrived at the exit!");
            MazeRunnerGame game = other.getGame();

            long elapsedTime = game.getElapsedTime();
            int score = game.calculateTotalScore(elapsedTime);

            // Gate victory behind the Space-Cruises mini game.
            game.setScreen(new SpaceCruisesMiniGameScreen(game, score, elapsedTime));
        }
    }
}
