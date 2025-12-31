package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import de.tum.cit.fop.maze.CutsceneVideoScreen;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.SpaceCruisesMiniGameScreen;
import de.tum.cit.fop.maze.StoryScreen;

/** Exit of the maze, where the victory may be triggered. */
public class Exit extends Path {

    // Ensure the spaceship cutscene only triggers once
    private boolean spaceshipCutsceneTriggered = false;

    public Exit(Maze maze, TextureRegion texture, Vector2 position) {
        super(maze, texture, position);
    }

    @Override
    public void onArrival(MazeObject other) {
        // Prevent multiple triggers
        if (spaceshipCutsceneTriggered) {
            return;
        }

        if (other instanceof Player player) {
            if (!player.hasKey()) {
                Gdx.app.log("Exit", "You haven't got the key");
                StoryScreen.getInstance().showMessage("You need a key to exit!");
                return;
            }

            // Mark as triggered immediately to prevent re-entry
            spaceshipCutsceneTriggered = true;

            // Stop all looping sounds before transitioning to cutscene
            player.stopAllSounds();

            Gdx.app.log("Exit", "Player arrived at the exit with key!");
            MazeRunnerGame game = other.getGame();

            long elapsedTime = game.getElapsedTime();
            int score = game.calculateTotalScore(elapsedTime);

            // Play spaceship cutscene, then transition to mini game
            game.setScreen(new CutsceneVideoScreen(
                game,
                MazeRunnerGame.getSpaceshipVideoPath(),
                () -> game.setScreen(new SpaceCruisesMiniGameScreen(game, score, elapsedTime)),
                true // allow skip
            ));
        }
    }

    /**
     * Resets the cutscene trigger flag (call when restarting the level).
     */
    public void resetCutsceneTrigger() {
        spaceshipCutsceneTriggered = false;
    }
}
