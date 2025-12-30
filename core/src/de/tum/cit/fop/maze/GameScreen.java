package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;

import de.tum.cit.fop.maze.elements.Exit;
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
    private final HUD hud;
    private final FogOfWar fogOfWar;
    private final ShapeRenderer shapeRenderer;

    private float stateTime = 0f;
    private boolean paused = false;

    /**
     * Constructor for GameScreen. Initializes all important elements.
     *
     * @param game The main game class, used to access global resources and methods.
     */
    public GameScreen(MazeRunnerGame game) {
        this(game, "maps/level-1.properties");
    }

    public GameScreen(MazeRunnerGame game, String mapFilePath) {
        Maze currentMaze = null; // loaded map file maybe invalid, use this to temporarily store it
        this.game = game;

        // Get the font from the game's skin
        font = game.getSkin().getFont("font");

        // Initialize Maze, Player, and camera
        try {
            // Load map properties
            Properties mapProperties = new Properties();
            try {
                mapProperties.load(Gdx.files.internal(mapFilePath).read());
            } catch (IOException err) {
                // Fallback if map file is missing
                mapProperties.put("0,0", "0");
                mapProperties.put("1,0", "1");
            }
            currentMaze = new Maze(game, new Vector2(0, 0), mapProperties);
        } catch (InvalidMaze err) {
            System.out.println(err);
            System.out.println("Falling back to default map.");
            try {
                Properties defaultMapProperties = new Properties();
                defaultMapProperties.load(Gdx.files.internal("maps/level-1.properties").read());
                currentMaze = new Maze(game, new Vector2(0, 0), defaultMapProperties);
            } catch (IOException ignored) {
            }
        }
        maze = currentMaze;
        player = new Player(game, maze, maze.getEntry().getPosition());
        camera = new MazeRunnerCamera(game, player.getPosition());
        maze.setCamera(camera);
        hud = new HUD(game.getSpriteBatch());
        fogOfWar = new FogOfWar();
        shapeRenderer = new ShapeRenderer();
    }

    /** Whether the game is currently paused. */
    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    /** Returns the current state time of the game screen, typically used for animations. */
    public float getStateTime() {
        return stateTime;
    }

    @Override
    public void render(float delta) {
        if (!paused) {
            stateTime += delta;
            handleInput(delta);
            triggerEvents(delta);
        }

        // Clear the screen
        ScreenUtils.clear(0, 0, 0, 1);

        // Update camera (shake logic, etc.)
        camera.update(delta);

        // Refresh camera (updates position, etc.)
        camera.refresh();

        // Render background if spaceship mode
        if (player.isSpaceshipMode()) {
            renderSpaceBackground();
        }

        // Render the game world using spriteBatch
        game.getSpriteBatch().begin();
        renderGameElements();
        game.getSpriteBatch().end();

        // Render fog of war effect around the player
        fogOfWar.render(game.getSpriteBatch(), camera.getCamera(), player.getPosition());

        // Update and render HUD
        float compassDeg = player.hasKey()
                ? maze.findNearestExitDirection(player.getCenter())
                : maze.findNearestKeyDirection(player.getCenter());

        hud.update(
                (int) player.getHealth(),
                player.hasKey(),
                player.getSpeedFactor(),
                player.hasShield(),
                compassDeg);
        hud.onFrame(delta);
        hud.render();

        StoryScreen.getInstance().update(delta);
        StoryScreen.getInstance().render(game.getSpriteBatch());
    }

    /** Handle input for the game screen, should only be called by render() when not paused. */
    private void handleInput(float delta) {
        // Check for escape key press to go back to the menu
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            // Pause and switch to menu instead of exiting
            game.goToMenu(true);
            return;
        }

        // Player input is now handled in Player::onFrame()

        camera.moveTowards(player.getPosition());

        if (Gdx.input.isKeyPressed(Input.Keys.Z)) {
            camera.zoom(delta, 1f);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.X)) {
            camera.zoom(delta, -1f);
        }
    }

    /** Trigger events in the game, should only be called by render() when not paused. */
    private void triggerEvents(float delta) {
        maze.onFrame(delta);
    }

    /** Render the game elements, should only be called by render(). */
    private void renderGameElements() {
        maze.render();
    }

    /**
     * Called when the window is resized. We update the FitViewport for the game world and also
     * resize the HUD accordingly.
     */
    @Override
    public void resize(int width, int height) {
        // Resize the HUD
        hud.resize(width, height);

        // Make sure camera's logic is still correct
        camera.resize();
    }

    @Override
    public void pause() {
        this.paused = true;
    }

    @Override
    public void resume() {
        // No special logic here
    }

    @Override
    public void show() {
        // Not used
    }

    @Override
    public void hide() {
        // Not used
    }

    @Override
    public void dispose() {
        fogOfWar.dispose();
        if (player != null) {
            player.dispose();
        }
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
    }

    private void renderSpaceBackground() {
        shapeRenderer.setProjectionMatrix(camera.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        float w = maze.getWidth() * maze.getBlockSize();
        float h = maze.getHeight() * maze.getBlockSize();

        // Draw some planets
        shapeRenderer.setColor(0.2f, 0.1f, 0.4f, 1f); // Dark Purple
        shapeRenderer.circle(w * 0.2f, h * 0.8f, 120f);

        shapeRenderer.setColor(0.1f, 0.3f, 0.5f, 1f); // Blueish
        shapeRenderer.circle(w * 0.7f, h * 0.3f, 90f);

        shapeRenderer.setColor(0.4f, 0.2f, 0.1f, 1f); // Reddish
        shapeRenderer.circle(w * 0.5f, h * 0.6f, 60f);

        // Draw some stars
        shapeRenderer.setColor(Color.WHITE);
        for(int i=0; i<100; i++) {
            // Pseudo-random based on index
            float x = (i * 137.5f * 7f) % w;
            float y = (i * 293.3f * 3f) % h;
            shapeRenderer.circle(x, y, 2f);
        }

        shapeRenderer.end();
    }

    /** Restores the player's state, for example when returning from another screen. */
    public void restorePlayerState(Player player, Exit exit) {
        Vector2 exitPosition = exit.getPosition();
        player.getHitbox().setPosition(exitPosition.x, exitPosition.y);
        setPaused(false); // ensure unpaused
    }
}
