package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
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

    private final ShapeRenderer shapeRenderer; // For rendering shapes/stencil
    private Texture gradientTexture; // Used for the circular gradient

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
        this.game = game;

        // Get the font from the game's skin
        font = game.getSkin().getFont("font");

        // Load map properties
        Properties mapProperties = new Properties();
        try {
            mapProperties.load(Gdx.files.internal(mapFilePath).read());
        } catch (IOException err) {
            // Fallback if map file is missing
            mapProperties.put("0,0", "0");
            mapProperties.put("1,0", "1");
        }

        // Initialize Maze, Player, and camera
        maze = new Maze(game, new Vector2(0, 0), mapProperties);
        player = new Player(game, maze, maze.getEntry().getPosition());
        camera = new MazeRunnerCamera(game, player.getPosition());
        hud = new HUD(game.getSpriteBatch());

        shapeRenderer = new ShapeRenderer();
        createGradientTexture(); // Create the gradient texture for masking
    }

    /** Creates a circular gradient texture that will be used for the stencil effect. */
    private void createGradientTexture() {
        int size = 1024;
        Pixmap pix = new Pixmap(size, size, Pixmap.Format.RGBA8888);

        float center = size / 2f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = x - center;
                float dy = y - center;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                float t = dist / center; // range 0..1
                if (t > 1f) t = 1f;

                // center (0) => fully transparent, edge (1) => fully opaque black
                float alpha = t;
                pix.setColor(0, 0, 0, alpha);
                pix.drawPixel(x, y);
            }
        }
        gradientTexture = new Texture(pix);
        pix.dispose();
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

        // Refresh camera (updates position, etc.)
        camera.refresh();

        // Render the game world using spriteBatch
        game.getSpriteBatch().begin();
        renderGameElements();
        game.getSpriteBatch().end();

        // ULTIMATE SPAGHETTI!
        // Before war fog code, SpriteBatch::draw() uses in-game position,
        // after war fog code, it uses relative-to-window position instead

        // ----------------- War Fog Rendering with Stencil Buffer -----------------
        Gdx.gl.glEnable(GL20.GL_STENCIL_TEST);
        Gdx.gl.glClearStencil(0);
        Gdx.gl.glClear(GL20.GL_STENCIL_BUFFER_BIT);

        // Step 1: Write to stencil buffer with a circle (set stencil=1 in circle area)
        Gdx.gl.glColorMask(false, false, false, false);
        Gdx.gl.glStencilFunc(GL20.GL_ALWAYS, 1, 0xFF);
        Gdx.gl.glStencilOp(GL20.GL_KEEP, GL20.GL_KEEP, GL20.GL_REPLACE);

        shapeRenderer.setProjectionMatrix(camera.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        float playerX = player.getPosition().x;
        float playerY = player.getPosition().y;
        float circleRadius = 125f; // Keep the circle radius fixed, independent of zoom

        shapeRenderer.circle(playerX, playerY, circleRadius);
        shapeRenderer.end();

        // Step 2: Render black overlay where stencil != 1
        Gdx.gl.glColorMask(true, true, true, true);
        Gdx.gl.glStencilFunc(GL20.GL_EQUAL, 0, 0xFF);
        Gdx.gl.glStencilOp(GL20.GL_KEEP, GL20.GL_KEEP, GL20.GL_KEEP);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 1f);
        shapeRenderer.rect(
                playerX - Gdx.graphics.getWidth(),
                playerY - Gdx.graphics.getHeight(),
                Gdx.graphics.getWidth() * 2,
                Gdx.graphics.getHeight() * 2);
        shapeRenderer.end();

        // Step 3: Draw gradient light texture over the circular visible area
        Gdx.gl.glStencilFunc(GL20.GL_EQUAL, 1, 0xFF);
        Gdx.gl.glStencilOp(GL20.GL_KEEP, GL20.GL_KEEP, GL20.GL_KEEP);

        game.getSpriteBatch().setProjectionMatrix(camera.getCamera().combined);
        game.getSpriteBatch().begin();

        float gradientSize = circleRadius * 2f; // Keep the gradient size fixed, independent of zoom

        game.getSpriteBatch()
                .draw(
                        gradientTexture,
                        playerX - gradientSize / 2f,
                        playerY - gradientSize / 2f,
                        gradientSize,
                        gradientSize);
        game.getSpriteBatch().end();

        Gdx.gl.glDisable(GL20.GL_STENCIL_TEST);

        // Update and render HUD
        hud.update(
                (int) player.getHealth(),
                player.hasKey(),
                player.getSpeedFactor(),
                player.hasShield(),
                maze.findNearestExitDirection(player.getCenter()));
        hud.render();
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
        // Dispose resources if needed
    }

    /** Restores the player's state, for example when returning from another screen. */
    public void restorePlayerState(Player player, Exit exit) {
        Vector2 exitPosition = exit.getPosition();
        player.getHitbox().setPosition(exitPosition.x, exitPosition.y);
        setPaused(false); // ensure unpaused
    }
}
