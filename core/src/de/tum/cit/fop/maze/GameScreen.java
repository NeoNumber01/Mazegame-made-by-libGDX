package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;

import de.tum.cit.fop.maze.Helper.Direction;
import de.tum.cit.fop.maze.elements.Block;
import de.tum.cit.fop.maze.elements.Player;

/**
 * The GameScreen class is responsible for rendering the gameplay screen. It handles the game logic
 * and rendering of the game elements.
 */
public class GameScreen implements Screen {

    private final MazeRunnerGame game;
    private final OrthographicCamera camera;
    private final BitmapFont font;
    private final Player player;
    private Block testBlock;
    private float stateTime = 0f;

    /**
     * Constructor for GameScreen. Sets up the camera and font.
     *
     * @param game The main game class, used to access global resources and methods.
     */
    public GameScreen(MazeRunnerGame game) {
        this.game = game;

        // Create and configure the camera for the game view
        camera = new OrthographicCamera();
        camera.setToOrtho(false);
        camera.zoom = 0.75f;

        // Get the font from the game's skin
        font = game.getSkin().getFont("font");

        // Initialize the player
        player =
                new Player(
                        new Vector2(camera.position.x / 2, camera.position.y / 2),
                        game.getResourcePack().getPlayerWalkAnimation());

        testBlock = new Block(game.getResourcePack().getBlockTexture(), 0, 0);
    }

    // Screen interface methods with necessary functionality
    @Override
    public void render(float delta) {
        stateTime += delta;

        handleInput();
        triggerEvents();

        ScreenUtils.clear(0, 0, 0, 1); // Clear the screen

        camera.update(); // Update the camera

        // Set up and begin drawing with the sprite batch
        game.getSpriteBatch().setProjectionMatrix(camera.combined);

        game.getSpriteBatch().begin(); // Important to call this before drawing anything

        renderGameElements();

        game.getSpriteBatch().end(); // Important to call this after drawing everything
    }

    /** Handle input for the game screen, should only be called by render(). */
    private void handleInput() {
        // Check for escape key press to go back to the menu
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.goToMenu();
        }

        // Player movement
        // ignore if opposite keys are pressed
        if (Gdx.input.isKeyPressed(Input.Keys.UP) != Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            player.move(Gdx.input.isKeyPressed(Input.Keys.UP) ? Direction.UP : Direction.DOWN, 1f);
        }
        // ignores diagonal movement
        // TODO: decide go up/down or left/right by the current direction of player
        else if (Gdx.input.isKeyPressed(Input.Keys.LEFT)
                != Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            player.move(
                    Gdx.input.isKeyPressed(Input.Keys.LEFT) ? Direction.LEFT : Direction.RIGHT, 1f);
        }
    }

    /** Trigger events in the game, should only be called by render(). */
    private void triggerEvents() {}

    /** Render the game elements, should only be called by render(). */
    private void renderGameElements() {
        game.getSpriteBatch()
                .draw(
                        testBlock.getTexture(stateTime),
                        testBlock.getBox().x,
                        testBlock.getBox().y,
                        Constants.BLOCK_SIZE,
                        Constants.BLOCK_SIZE);

        game.getSpriteBatch()
                .draw(player.getTexture(stateTime), player.getPosition().x, player.getPosition().y);
    }

    @Override
    public void resize(int width, int height) {
        camera.setToOrtho(false);
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
