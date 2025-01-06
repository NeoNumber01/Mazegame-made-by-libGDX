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
 * The GameScreen class is responsible for rendering the gameplay screen.
 * It handles the game logic and rendering of the game elements.
 */
public class GameScreen implements Screen {

    private final MazeRunnerGame game;
    private final MazeRunnerCamera camera;
    private final BitmapFont font;
    private final Player player;
    private final Maze maze;
    private float stateTime = 0f;

    // 新增：用于控制是否暂停
    private boolean paused = false;

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
        Properties mapProperties = new Properties();
        try {
            mapProperties.load(Gdx.files.internal("maps/level-1.properties").read());
        } catch (IOException err) {
            // 如果加载失败，给一个简易默认地图
            mapProperties.put("0,0", "0");
            mapProperties.put("1,0", "1");
        }

        maze = new Maze(game, new Vector2(0, 0), mapProperties);

        // initialize player
        player = new Player(game, maze.getEntry().getPosition());

        // Create and configure the camera for the game view
        camera = new MazeRunnerCamera(game, player.getPosition());
    }

    /** 设置游戏是否暂停 */
    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean isPaused() {
        return paused;
    }

    public float getStateTime() {
        return stateTime;
    }

    // Screen interface methods with necessary functionality
    @Override
    public void render(float delta) {
        // 如果没暂停，才累加时间，用于动画等
        if (!paused) {
            stateTime += delta;
        }

        // 如果游戏未暂停，才处理输入和逻辑更新
        if (!paused) {
            handleInput(delta);
            triggerEvents();
        }

        ScreenUtils.clear(0, 0, 0, 1); // Clear the screen

        camera.refresh();

        game.getSpriteBatch().begin(); // Important to call this before drawing anything

        // 渲染游戏元素(玩家、地图等)
        renderGameElements();

        game.getSpriteBatch().end(); // Important to call this after drawing everything
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
        float deltaDist = 64 * delta;

        // 按键移动
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

        // 简易的碰撞检测
        if (deltaPos.len() > 0) {
            Vector2 currentPos = player.getPosition();
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

    /** Trigger events in the game, should only be called by render() when not paused. */
    private void triggerEvents() {
        // 这里可以处理各种事件、机关、NPC互动等
    }

    /** Render the game elements, should only be called by render(). */
    private void renderGameElements() {
        // Layer 1: Maze blocks
        maze.render();

        // Layer 2: Entities (后续若有敌人或NPC可以放这里)

        // Layer 3: Player
        player.render();
    }

    @Override
    public void resize(int width, int height) {
        camera.resize();
    }

    @Override
    public void pause() {
        // 当 LibGDX 发生 APP 切换时也会调用
        this.paused = true;
    }

    @Override
    public void resume() {
        // 从后台切回前台
        // 这里是否继续保持暂停取决于需求
    }

    @Override
    public void show() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        // 释放资源
    }
}
