package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
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
    private float stateTime = 0f;
    private final HUD hud;

    private ShapeRenderer shapeRenderer;// 专门用来往 FBO 上绘制的 SpriteBatch
    private Texture gradientTexture;//用来做圆形渐变纹理


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
        hud = new HUD(game.getSpriteBatch());
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

        shapeRenderer = new ShapeRenderer();
        // 创建渐变纹理
        createGradientTexture();
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

    //创建圆形渐变纹理
    private void createGradientTexture() {
        int size = 1024;
        Pixmap pix = new Pixmap(size, size, Pixmap.Format.RGBA8888);

        // 圆心在 (size/2, size/2)
        float center = size / 2f;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = x - center;
                float dy = y - center;
                float dist = (float)Math.sqrt(dx*dx + dy*dy);

                // dist=0 =>中心, dist>center =>边缘
                float t = dist / center; // 0..1

                if (t > 1f) t = 1f;      // 超过边缘就算1

                // 希望中心=0(透明), 边缘=1(不透明黑)
                float alpha = t;

                // 设置颜色(黑 + alpha)
                pix.setColor(0, 0, 0, alpha);
                pix.drawPixel(x,y);
            }
        }
        gradientTexture = new Texture(pix);
        pix.dispose();
    }

    @Override
    public void render(float delta) {

        if (!paused) {
            stateTime += delta;

            handleInput(delta);
            triggerEvents(delta);
        }

        ScreenUtils.clear(0, 0, 0, 1); // Clear the screen

        camera.refresh();

        game.getSpriteBatch().begin();

        renderGameElements();

        game.getSpriteBatch().end();


        // 使用 Stencil 做“圆形区域可见、外部不透明黑”
        Gdx.gl.glEnable(GL20.GL_STENCIL_TEST);
        Gdx.gl.glClearStencil(0);
        Gdx.gl.glClear(GL20.GL_STENCIL_BUFFER_BIT);

        // 写圆形 stencil=1
        Gdx.gl.glColorMask(false,false,false,false);
        Gdx.gl.glStencilFunc(GL20.GL_ALWAYS, 1, 0xFF);
        Gdx.gl.glStencilOp(GL20.GL_KEEP, GL20.GL_KEEP, GL20.GL_REPLACE);

        shapeRenderer.setProjectionMatrix(
            new Matrix4().setToOrtho2D(0,0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight())
        );
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        float cx = Gdx.graphics.getWidth()/2f;
        float cy = Gdx.graphics.getHeight()/2f;
        float circleRadius = 300; // 圆形半径
        shapeRenderer.circle(cx, cy, circleRadius);
        shapeRenderer.end();

        // 在 stencil=0 的地方画黑幕
        Gdx.gl.glColorMask(true,true,true,true);
        Gdx.gl.glStencilFunc(GL20.GL_EQUAL, 0, 0xFF);
        Gdx.gl.glStencilOp(GL20.GL_KEEP, GL20.GL_KEEP, GL20.GL_KEEP);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0,0,0,1f); // 不透明黑
        shapeRenderer.rect(0,0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapeRenderer.end();

        // 现在给“圆形区域(stencil=1)”叠加一张渐变纹理
        Gdx.gl.glStencilFunc(GL20.GL_EQUAL, 1, 0xFF);
        Gdx.gl.glStencilOp(GL20.GL_KEEP, GL20.GL_KEEP, GL20.GL_KEEP);

        // 注意：别关 stencil，需要它来限制只在圆形内画渐变
        // 我们用 spriteBatch 绘制纹理
        SpriteBatch batch = game.getSpriteBatch();
        batch.setProjectionMatrix(
            new Matrix4().setToOrtho2D(0,0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight())
        );
        batch.begin();

        // 在圆心放置渐变纹理
        // 建议给纹理一个比 circleRadius 稍大的尺寸
        // 以保证边缘有足够的渐变空间
        float gradientSize = 600; // 可自行调大或小
        batch.draw(
            gradientTexture,
            cx - gradientSize/2f,
            cy - gradientSize/2f,
            gradientSize,
            gradientSize
        );
        batch.end();

        // 关闭 Stencil
        Gdx.gl.glDisable(GL20.GL_STENCIL_TEST);
        hud.update((int) player.getHealth(), player.hasKey(), player.getSpeedFactor(),player.hasShield());
        hud.render();

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
        maze.render();
    }

    @Override
    public void resize(int width, int height) {
        camera.resize();
        hud.resize(width, height);
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
    public void restorePlayerState(Player player, Exit exit) {
        // 获取出口的位置
        Vector2 exitPosition = exit.getPosition();

        // 设置玩家的 hitbox 位置为出口的位置
        player.getHitbox().setPosition(exitPosition.x, exitPosition.y);

        setPaused(false); // 确保游戏未暂停
    }
}
