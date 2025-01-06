package de.tum.cit.fop.maze;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

import games.spooky.gdx.nativefilechooser.NativeFileChooser;

/**
 * The MazeRunnerGame class represents the core of the Maze Runner game.
 * It manages the screens and global resources like SpriteBatch and Skin.
 */
public class MazeRunnerGame extends Game {

    // Screens
    private MenuScreen menuScreen;
    private GameScreen gameScreen;

    // Sprite Batch for rendering
    private SpriteBatch spriteBatch;

    // UI Skin
    private Skin skin;
    private ResourcePack resourcePack;
    private Music backgroundMusic;

    // 标记游戏是否处于暂停状态
    private boolean paused;

    /**
     * Constructor for MazeRunnerGame.
     *
     * @param fileChooser The file chooser for the game, typically used in desktop environment.
     */
    public MazeRunnerGame(NativeFileChooser fileChooser) {
        super();
    }

    public ResourcePack getResourcePack() {
        return resourcePack;
    }

    /** Called when the game is created. Initializes the SpriteBatch and Skin. */
    @Override
    public void create() {
        spriteBatch = new SpriteBatch(); // Create SpriteBatch
        skin = new Skin(Gdx.files.internal("craft/craftacular-ui.json")); // Load UI skin

        resourcePack = new ResourcePack();

        // Play some background music
        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("background.mp3"));
        backgroundMusic.setLooping(true);
        backgroundMusic.play();

        // 一启动就显示菜单
        goToMenu(false);
    }

    /**
     * 切换至主菜单。
     *
     * @param pause 是否处于“暂停”模式。如果 true，则不 dispose GameScreen，保留以便恢复。
     *              如果 false，则表示新开游戏或真正退出游戏，此时可以 dispose 原有 GameScreen。
     */
    public void goToMenu(boolean pause) {
        // 如果是暂停，则仅暂停游戏，不dispose
        if (pause) {
            pauseGame(); // 设置 paused = true
        } else {
            // 不处于暂停模式，说明要么是刚启动，要么是要真正结束当前游戏
            // 可以安全 dispose 原有 gameScreen
            if (gameScreen != null) {
                gameScreen.dispose();
                gameScreen = null;
            }
        }

        // 切换到主菜单
        menuScreen = new MenuScreen(this, pause);
        setScreen(menuScreen);
    }

    /** 真正切换到游戏界面，如果已存在 gameScreen，就resume；否则新建一个。 */
    public void goToGame() {
        if (gameScreen == null) {
            gameScreen = new GameScreen(this);
        }
        resumeGame(); // 取消暂停
        setScreen(gameScreen);

        // 菜单用完就dispose掉
        if (menuScreen != null) {
            menuScreen.dispose();
            menuScreen = null;
        }
    }

    /**
     * 开始新游戏。会强制dispose掉旧的 gameScreen，重新创建。
     * 调用后会自动进入游戏界面。
     */
    public void startNewGame() {
        if (gameScreen != null) {
            gameScreen.dispose();
            gameScreen = null;
        }
        gameScreen = new GameScreen(this);
        resumeGame();
        setScreen(gameScreen);

        if (menuScreen != null) {
            menuScreen.dispose();
            menuScreen = null;
        }
    }

    /** 游戏暂停（角色、敌人停止移动） */
    public void pauseGame() {
        paused = true;
        if (gameScreen != null) {
            gameScreen.setPaused(true);
        }
    }

    /** 游戏恢复 */
    public void resumeGame() {
        paused = false;
        if (gameScreen != null) {
            gameScreen.setPaused(false);
        }
    }

    public boolean isGamePaused() {
        return paused;
    }

    /** Cleans up resources when the game is disposed. */
    @Override
    public void dispose() {
        if (getScreen() != null) {
            getScreen().hide();
            getScreen().dispose();
        }
        spriteBatch.dispose();
        skin.dispose();
        if (backgroundMusic != null) {
            backgroundMusic.dispose();
        }
    }

    // Getter methods
    public Skin getSkin() {
        return skin;
    }

    public SpriteBatch getSpriteBatch() {
        return spriteBatch;
    }

    public float getStateTime() {
        if (gameScreen != null) {
            return gameScreen.getStateTime();
        }
        return 0f;
    }
}
