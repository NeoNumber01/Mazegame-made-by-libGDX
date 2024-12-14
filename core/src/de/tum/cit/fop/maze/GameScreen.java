package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;

import de.tum.cit.fop.maze.elements.Block;
import de.tum.cit.fop.maze.elements.Maze;
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
    private final Maze maze;
    private float stateTime = 0f;

    /**
     * Constructor for GameScreen. Initializes all important elements.
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

        maze = new Maze(game, new Vector2(0, 0), 10, 10);
    }

    // Screen interface methods with necessary functionality
    @Override
    public void render(float delta) {
        stateTime += delta;

        handleInput(delta);
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
    private void handleInput(float delta) {
        // Check for escape key press to go back to the menu
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.goToMenu();
        }

        // Camera movement
        // Note: this is temporary implementation for testing purpose,
        // camera should follow the player but with inertia
        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            camera.translate(0, delta * 64);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            camera.translate(0, -delta * 64);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            camera.translate(-delta * 64, 0);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            camera.translate(delta * 64, 0);
        }

        // Player movement
        // ignore if opposite keys are pressed
        // but such handling seems unnecessary
        // TODO: refactor
        //        if (Gdx.input.isKeyPressed(Input.Keys.UP) !=
        // Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
        //            player.move(
        //                    Gdx.input.isKeyPressed(Input.Keys.UP) ? Direction.UP : Direction.DOWN,
        //                    delta * 64);
        //        }
        //        // ignores diagonal movement
        //        // TODO: decide go up/down or left/right by the current direction of player
        //        else if (Gdx.input.isKeyPressed(Input.Keys.LEFT)
        //                != Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
        //            player.move(
        //                    Gdx.input.isKeyPressed(Input.Keys.LEFT) ? Direction.LEFT :
        // Direction.RIGHT,
        //                    delta * 64);
        //        }

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
            Vector2 currentPos = new Vector2(player.getBox().getX(), player.getBox().getY());
            Rectangle
                    nextBoxX =
                            new Rectangle(
                                    currentPos.x + deltaPos.x,
                                    currentPos.y,
                                    player.getBoxSize(),
                                    player.getBoxSize()),
                    nextBoxY =
                            new Rectangle(
                                    currentPos.x,
                                    currentPos.y + deltaPos.y,
                                    player.getBoxSize(),
                                    player.getBoxSize());
            Array<Block> blocks = maze.getSurroundBlocks(currentPos);

            for (Block block : blocks) {
                if (!block.isWalkable() && block.getBox().overlaps(nextBoxX)) {
                    deltaPos.x = 0;
                    break;
                }
            }
            for (Block block : blocks) {
                if (!block.isWalkable() && block.getBox().overlaps(nextBoxY)) {
                    deltaPos.y = 0;
                    break;
                }
            }

            if (deltaPos.y != 0) {
                player.setDirection(deltaPos.y > 0 ? Helper.Direction.UP : Helper.Direction.DOWN);
            } else if (deltaPos.x != 0) {
                player.setDirection(
                        deltaPos.x > 0 ? Helper.Direction.RIGHT : Helper.Direction.LEFT);
            }

            player.getBox().setPosition(currentPos.x + deltaPos.x, currentPos.y + deltaPos.y);
        }
    }

    /** Trigger events in the game, should only be called by render(). */
    private void triggerEvents() {}

    /** Render the game elements, should only be called by render(). */
    private void renderGameElements() {
        // Layer 1: Maze blocks
        for (Block block : maze) {
            game.getSpriteBatch()
                    .draw(
                            block.getTexture(stateTime),
                            block.getBox().x,
                            block.getBox().y,
                            maze.getBlocksize(),
                            maze.getBlocksize());
        }

        // Layer 2: Entities

        // Layer 3: Player
        game.getSpriteBatch()
                .draw(
                        player.getTexture(stateTime),
                        player.getBox().x,
                        player.getBox().y - 8f,
                        player.getWidth(),
                        player.getHeight());
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
