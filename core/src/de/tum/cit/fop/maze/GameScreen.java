package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;

import de.tum.cit.fop.maze.elements.Block;
import de.tum.cit.fop.maze.elements.Maze;
import de.tum.cit.fop.maze.elements.Player;

import java.io.IOException;
import java.util.Properties;

/**
 * The GameScreen class is responsible for rendering the gameplay screen. It handles the game logic
 * and rendering of the game elements.
 */
public class GameScreen implements Screen {

    private final MazeRunnerGame game;
    private final MazeRunnerCamera camera;
    private final BitmapFont font;
    private final Player player;
    private final Maze maze;
    private float stateTime = 0f;

    /**
     * Constructor for GameScreen. Initializes all important elements.
     *
     * @param game The main game class, used to access global resources and methods.
     */
    public GameScreen(MazeRunnerGame game) {
        this.game = game;

        // Get the font from the game's skin
        font = game.getSkin().getFont("font");

        // initialize map

        // TODO: pass map properties from frontend
        Properties mapProperties = new Properties();
        try {
            mapProperties.load(Gdx.files.internal("maps/level-1.properties").read());
        } catch (IOException err) {
            mapProperties.put("0,0", "0");
        }

        maze = new Maze(game, new Vector2(0, 0), mapProperties);

        // initialize player
        player = new Player(game, maze.getEntry().getPosition());

        // Create and configure the camera for the game view
        camera = new MazeRunnerCamera(game, player.getPosition());
    }

    public float getStateTime() {
        return stateTime;
    }

    // Screen interface methods with necessary functionality
    @Override
    public void render(float delta) {
        stateTime += delta;

        handleInput(delta);
        triggerEvents();

        ScreenUtils.clear(0, 0, 0, 1); // Clear the screen

        camera.refresh();

        game.getSpriteBatch().begin(); // Important to call this before drawing anything

        renderGameElements();

        game.getSpriteBatch().end(); // Important to call this after drawing everything
    }

    /** Handle input for the game screen, should only be called by render(). */
    private void handleInput(float delta) {
        // Check for escape key press to go back to the menu
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.goToMenu();
        }

        Vector2 deltaPos = new Vector2();
        float deltaDist = 64 * delta;

        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            deltaPos.y += deltaDist;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            deltaPos.y -= deltaDist;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            deltaPos.x -= deltaDist;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            deltaPos.x += deltaDist;
        }
        if (deltaPos.len() > 0) {
            // movement on X and Y axis should be handled separately,
            // this avoids the extremely complex handling when moving while being stuck to the wall
            Vector2 currentPos = player.getPosition();
            // nextBoxX emulates player's movement on X-axis, nextBoxY ditto.
            Rectangle
                    nextBoxX =
                            new Rectangle(
                                    currentPos.x + deltaPos.x,
                                    currentPos.y,
                                    player.getSize().x,
                                    player.getSize().y),
                    nextBoxY =
                            new Rectangle(
                                    currentPos.x,
                                    currentPos.y + deltaPos.y,
                                    player.getSize().x,
                                    player.getSize().y);
            Array<Block> blocks = maze.getSurroundBlocks(currentPos);

            for (Block block : blocks) {
                if (block.isObstacle() && block.overlaps(nextBoxX)) {
                    deltaPos.x = 0;
                    break;
                }
            }
            for (Block block : blocks) {
                if (block.isObstacle() && block.overlaps(nextBoxY)) {
                    deltaPos.y = 0;
                    break;
                }
            }

            player.performMovement(deltaPos);
            camera.moveTowards(player.getPosition());
        }
    }

    /** Trigger events in the game, should only be called by render(). */
    private void triggerEvents() {}

    /** Render the game elements, should only be called by render(). */
    private void renderGameElements() {
        // Layer 1: Maze blocks
        maze.render();

        // Layer 2: Entities

        // Layer 3: Player
        player.render();
    }

    @Override
    public void resize(int width, int height) {
        camera.resize();
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void show() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {}
    // Additional methods and logic can be added as needed for the game screen
}
