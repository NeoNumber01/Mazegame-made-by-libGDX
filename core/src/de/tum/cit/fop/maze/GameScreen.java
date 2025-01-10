package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
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
    private final SpriteBatch batch;
    private float stateTime = 0f;
    private FrameBuffer fogBuffer;
    private Texture lightTexture;

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
        player = new Player(game, maze, maze.getEntry().getPosition());
        camera = new MazeRunnerCamera(game, player.getPosition());

        batch = new SpriteBatch();
        createFogBuffer();
        createLightTexture();
    }

    private void createFogBuffer() {
        fogBuffer =
                new FrameBuffer(
                        Pixmap.Format.RGBA8888,
                        Gdx.graphics.getWidth(),
                        Gdx.graphics.getHeight(),
                        false);
    }

    private void createLightTexture() {
        int size = 128;
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float distance = Vector2.dst(x, y, size / 2f, size / 2f);
                float alpha = Math.max(0, 1 - (distance / (size / 2f)));
                pixmap.setColor(1, 1, 1, alpha);
                pixmap.drawPixel(x, y);
            }
        }

        lightTexture = new Texture(pixmap);
        pixmap.dispose();
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

        // 绘制战争迷雾
        fogBuffer.begin();
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.getCamera().combined);

        // 计算屏幕中心位置
        float centerX = player.getPosition().x;
        float centerY = player.getPosition().y;

        // 在屏幕中央绘制光照
        batch.begin();
        //        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        //        batch.setBlendFunction(GL20.GL_DST_COLOR, GL20.GL_ZERO);

        fogBuffer.end();

        // 将战争迷雾绘制到屏幕

        float w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
        batch.draw(fogBuffer.getColorBufferTexture(), centerX - w / 2, centerY - h / 2, w, h);
        batch.draw(
                lightTexture,
                centerX - lightTexture.getWidth() / 2f,
                centerY - lightTexture.getHeight() / 2f);
        batch.end();
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
