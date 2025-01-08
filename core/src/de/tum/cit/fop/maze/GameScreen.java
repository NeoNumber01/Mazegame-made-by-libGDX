package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;

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

    // 是否暂停
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

        // initialize map
        Properties mapProperties = new Properties();
        try {
            mapProperties.load(Gdx.files.internal(mapFilePath).read());
        } catch (IOException err) {
            mapProperties.put("0,0", "0");
            mapProperties.put("1,0", "1");
        }

        maze = new Maze(game, new Vector2(0, 0), mapProperties);
        player = new Player(maze, maze.getEntry().getPosition());
        camera = new MazeRunnerCamera(game, player.getPosition());
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public float getStateTime() {
        return stateTime;
    }

    // Screen interface methods with necessary functionality
    @Override
    public void render(float delta) {

        if (!paused) {
            stateTime += delta;
        }

        if (!paused) {
            handleInput(delta);
            triggerEvents(delta);
        }

        ScreenUtils.clear(0, 0, 0, 1); // Clear the screen

        camera.refresh();

        game.getSpriteBatch().begin();

        renderGameElements();

        game.getSpriteBatch().end();
    }

    /** Handle input for the game screen, should only be called by render() when not paused. */
    private void handleInput(float delta) {
        // Check for escape key press to go back to the menu
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            // 这里不直接退出游戏，而是暂停并切到菜单
            game.goToMenu(true);
            return;
        }

        Vector2 deltaPos = new Vector2();
        float deltaDist = player.getMoveDistance(delta);

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

        player.performDisplacement(deltaPos);
        camera.moveTowards(player.getPosition());
    }

    /** Trigger events in the game, should only be called by render() when not paused. */
    private void triggerEvents(float delta) {
        maze.onFrame(delta);
    }

    /** Render the game elements, should only be called by render(). */
    private void renderGameElements() {
        maze.render();
    }

    @Override
    public void resize(int width, int height) {
        camera.resize();
    }

    @Override
    public void pause() {
        this.paused = true;
    }

    @Override
    public void resume() {}

    @Override
    public void show() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        // 释放资源
    }
}
