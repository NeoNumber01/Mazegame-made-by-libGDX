package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.SpaceCruisesMiniGameScreen;
import de.tum.cit.fop.maze.StoryScreen;
import de.tum.cit.fop.maze.VideoScreen;

/** Exit of the maze, where the victory may be triggered. */
public class Exit extends Path {
    private static final String STORY_VIDEO_PATH = "spaceshipboard.mp4";

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

            // Create the mini game screen that will be shown after the video
            SpaceCruisesMiniGameScreen miniGameScreen = new SpaceCruisesMiniGameScreen(game, score, elapsedTime);

            try {
                // First play the story video, then transition to the mini game
                VideoScreen videoScreen = new VideoScreen(game, STORY_VIDEO_PATH, miniGameScreen);
                game.setScreen(videoScreen);
            } catch (Throwable e) {
                System.err.println("Failed to load video screen: " + e.getMessage());
                e.printStackTrace();
                // Fallback: start mini game directly
                game.setScreen(miniGameScreen);
            }
        }
    }
}
