package de.tum.cit.fop.maze;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

import de.tum.cit.fop.maze.elements.Exit;
import de.tum.cit.fop.maze.elements.Player;

import games.spooky.gdx.nativefilechooser.NativeFileChooser;

/**
 * The MazeRunnerGame class represents the core of the Maze Runner game. It manages the screens and
 * global resources like SpriteBatch and Skin.
 */
public class MazeRunnerGame extends Game {

    // 地图
    private static final String DEFAULT_MAP_PATH = "maps/level-1.properties";
    // Screens
    private MenuScreen menuScreen;
    private GameScreen gameScreen;
    // Sprite Batch for rendering
    private SpriteBatch spriteBatch;
    // UI Skin
    private Skin skin;
    private ResourcePack resourcePack;
    private Music backgroundMusic;
    // time recording
    private long startTime; // Record game start time
    // Time recorded when the game is paused
    private long pausedTime;
    private boolean timerStarted; // Flag to indicate if the timer has started
    // 是否暂停
    private boolean paused;
    private float volume = 0.5f;

    private Player player;

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
        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("background.ogg"));
        backgroundMusic.setLooping(true);
        backgroundMusic.setVolume(volume);
        backgroundMusic.play();

        // 一启动就显示菜单
        goToMenu(false);
    }

    // 停止音乐
    public void stopMusic() {
        if (backgroundMusic != null && backgroundMusic.isPlaying()) {
            backgroundMusic.stop();
        }
    }

    // 暂停音乐
    public void pauseMusic() {
        if (backgroundMusic != null && backgroundMusic.isPlaying()) {
            backgroundMusic.pause();
        }
    }

    // 回复音乐
    public void resumeMusic() {
        if (backgroundMusic != null && !backgroundMusic.isPlaying()) {
            backgroundMusic.play();
        }
    }

    // 切换音乐
    public void switchMusic(String filePath) {
        stopMusic();
        backgroundMusic.dispose();
        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal(filePath));
        backgroundMusic.setLooping(true);
        backgroundMusic.play();
    }

    public float getVolume() {
        return this.volume;
    }

    public void setVolume(float newVolume) {
        this.volume = Math.max(0.0f, Math.min(newVolume, 1.0f)); // 限制在 [0, 1]
        if (backgroundMusic != null) {
            backgroundMusic.setVolume(this.volume);
        }
    }

    // 回主菜单
    public void goToMenu(boolean pause) {

        // 如果是暂停，则仅暂停游戏，不dispose
        if (pause) {
            pauseGame(); // 设置 paused = true
            timerStarted = false; // Stop the timer
            pauseMusic();
        } else {

            // 不处于暂停模式，说明要么是刚启动，要么是要真正结束当前游戏
            // 可以安全 dispose 原有 gameScreen
            if (gameScreen != null) {
                gameScreen.dispose();
                gameScreen = null;
            }
            // switchMusic("background.mp3");
        }

        // 切换到主菜单
        menuScreen = new MenuScreen(this, pause);
        setScreen(menuScreen);
    }

    public void goToMenu() {
        goToMenu(false); // Call the existing goToMenu method with pause set to false
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

    // 新游戏
    public void startNewGame() {
        paused = false; // Reset the paused flag
        pausedTime = 0; // Reset paused time
        timerStarted = false; // Reset the timer flag
        startNewGame(DEFAULT_MAP_PATH);
        if (backgroundMusic != null) {
            backgroundMusic.stop();
            backgroundMusic.dispose();
        }
        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("background.ogg"));
        backgroundMusic.setLooping(true);
        backgroundMusic.setVolume(0.5f);
        backgroundMusic.play();
        showStorySequence();
    }

    public void startNewGame(String mapFilePath) {

        // Record the start time when the game begins
        startTime = System.currentTimeMillis();

        paused = false; // Game is not paused

        if (gameScreen != null) {
            gameScreen.dispose();
            gameScreen = null;
        }
        gameScreen = new GameScreen(this, mapFilePath);
        resumeGame();

        setScreen(gameScreen);

        if (menuScreen != null) {
            menuScreen.dispose();
            menuScreen = null;
        }

        if (backgroundMusic != null) {
            backgroundMusic.stop();
            backgroundMusic.dispose();
        }
        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("background.ogg"));
        backgroundMusic.setLooping(true);
        backgroundMusic.setVolume(0.5f);
        backgroundMusic.play();
        showStorySequence();
    }

    /** Show messages one by one */
    private void showStorySequence() {
        new Thread(
                        () -> {
                            try {
                                Gdx.app.postRunnable(
                                        () ->
                                                StoryScreen.getInstance()
                                                        .showMessage(
                                                                "\"Was it a vision,or a waking dream?\"\n"
                                                                        + "\"Do I wake,or sleep?\""));
                                Thread.sleep(5000);

                                Gdx.app.postRunnable(
                                        () ->
                                                StoryScreen.getInstance()
                                                        .showMessage(
                                                                "You wake up from a nightmare, only to find\n"
                                                                        + "yourself in an even deeper dream\n"));
                                Thread.sleep(5000);

                                Gdx.app.postRunnable(
                                        () ->
                                                StoryScreen.getInstance()
                                                        .showMessage(
                                                                "an unknown space, dim lighting, \n"
                                                                        + "and a labyrinth full\n"
                                                                        + "of twists and turns, with the occasional\n"
                                                                        + "sound of monsters echoing in the distance."));
                                Thread.sleep(5000);
                                Gdx.app.postRunnable(
                                        () ->
                                                StoryScreen.getInstance()
                                                        .showMessage(
                                                                "Could this be an even stranger dream?\n"
                                                                        + "However, everything around you feels so\n"
                                                                        + "real that it is hard to distinguish reality\n"
                                                                        + "from illusion."));
                                Thread.sleep(5000);

                                Gdx.app.postRunnable(
                                        () ->
                                                StoryScreen.getInstance()
                                                        .showMessage(
                                                                "No matter what, you must\n"
                                                                        + "find the key inside the labyrinth, locate\n"
                                                                        + "the path to the exit, and escape\n"
                                                                        + "as soon as possible.\n"));
                                Thread.sleep(5000);
                            } catch (InterruptedException e) {
                                Gdx.app.log("StoryScreen", "Error displaying story sequence", e);
                            }
                        })
                .start();
    }

    /** pause and resume game */
    public void pauseGame() {
        if (!paused && timerStarted) { // Only pause if the timer has started
            pausedTime = System.currentTimeMillis() - startTime; // Record the elapsed time
            paused = true;
            timerStarted = false; // Stop the timer
        }

        if (gameScreen != null) {
            gameScreen.setPaused(true);
        }
    }

    public void resumeGame() {
        if (paused) {
            startTime = System.currentTimeMillis() - pausedTime; // Adjust start time
            paused = false;
            timerStarted = true; // Restart the timer
        }

        if (gameScreen != null) {
            gameScreen.setPaused(false); // Resume the game screen
        }

        resumeMusic(); // Resume background music
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
        if (backgroundMusic != null) {
            backgroundMusic.dispose(); // 释放音乐资源
        }
        super.dispose();
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

    /** Start the timer when the player enters the map */
    public void startTimer() {
        if (!timerStarted) { // Only start the timer if it hasn't started yet
            startTime = System.currentTimeMillis(); // Record the start time
            timerStarted = true; // Set the flag to indicate the timer has started
        }
    }

    /** Returns the elapsed time since the game started in milliseconds */
    public long getElapsedTime() {
        if (!timerStarted) {
            return 0; // If timer hasn't started, return 0
        }
        if (paused) {
            return pausedTime; // If the game is paused, return the recorded paused time
        }
        return System.currentTimeMillis() - startTime;
    }

    /** Calculate score based on elapsed time in milliseconds */
    public int calculateScore(long elapsedTime) {
        long elapsedSeconds = elapsedTime / 1000; // Convert milliseconds to seconds
        int maxScore = 1000;
        int minScore = 100;
        int minTime = 10; // 100 seconds or less gives max score
        int maxTime = 3600; // 3600 seconds or more gives min score
        int scoreRange = maxScore - minScore;
        int timeRange = maxTime - minTime;

        // If elapsed time is 100 seconds or less, return max score
        if (elapsedSeconds <= minTime) {
            return maxScore;
        }

        // If elapsed time is 3600 seconds or more, return min score
        if (elapsedSeconds >= maxTime) {
            return minScore;
        }

        // Calculate score using linear interpolation
        int score =
                maxScore - (int) (((double) (elapsedSeconds - minTime) / timeRange) * scoreRange);

        return score;
    }

    public void resumeFromExit(Player player, Exit exit) {
        setScreen(gameScreen); // 切换回游戏界面
        gameScreen.restorePlayerState(player, exit); // 恢复玩家位置和状态
    }
}
