package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.video.VideoPlayer;
import com.badlogic.gdx.video.VideoPlayerCreator;


/**
 * A reusable cutscene video screen using gdx-video.
 * Plays a video file and transitions to the next screen when finished or skipped.
 *
 * Usage:
 * <pre>
 * game.setScreen(new CutsceneVideoScreen(
 *     game,
 *     "videos/intro.webm",
 *     () -> game.setScreen(new GameScreen(game)),
 *     true // allowSkip
 * ));
 * </pre>
 */
@SuppressWarnings("deprecation")
public class CutsceneVideoScreen implements Screen {

    private static final String TAG = "CutsceneVideoScreen";

    private final MazeRunnerGame game;
    private final String videoPath;
    private final Runnable onFinished;
    private final boolean allowSkip;

    private SpriteBatch batch;
    private BitmapFont font;
    private OrthographicCamera camera;
    private Viewport viewport;

    private VideoPlayer videoPlayer;
    private boolean videoReady = false;
    private boolean videoFinished = false;
    private boolean transitioned = false;

    // Video dimensions (may differ from texture dimensions)
    private int videoWidth = 0;
    private int videoHeight = 0;

    // For skip delay (prevent accidental skip)
    private float skipTimer = 0f;
    private static final float SKIP_DELAY = 0.5f;

    // Fallback timeout if video never produces frames
    private float fallbackTimer = 0f;
    private static final float FALLBACK_TIMEOUT = 5.0f;

    /**
     * Creates a new CutsceneVideoScreen.
     *
     * @param game       The main game instance
     * @param videoPath  Path to the video file (relative to assets, e.g., "videos/intro.webm")
     * @param onFinished Callback executed when video finishes or is skipped
     * @param allowSkip  Whether the player can skip the video with ESC/SPACE
     */
    public CutsceneVideoScreen(MazeRunnerGame game, String videoPath, Runnable onFinished, boolean allowSkip) {
        this.game = game;
        this.videoPath = videoPath;
        this.onFinished = onFinished;
        this.allowSkip = allowSkip;
    }

    /**
     * Convenience constructor with skip enabled by default.
     */
    public CutsceneVideoScreen(MazeRunnerGame game, String videoPath, Runnable onFinished) {
        this(game, videoPath, onFinished, true);
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.setColor(Color.WHITE);

        camera = new OrthographicCamera();
        viewport = new FitViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), camera);
        viewport.apply(true);

        // Pause game music during cutscene
        game.pauseMusic();

        // Initialize video player
        try {
            videoPlayer = VideoPlayerCreator.createVideoPlayer();

            if (videoPlayer == null) {
                Gdx.app.error(TAG, "VideoPlayerCreator returned null - video backend not available");
                finishAndTransition();
                return;
            }

            // Set completion listener
            videoPlayer.setOnCompletionListener(new VideoPlayer.CompletionListener() {
                @Override
                public void onCompletionListener(FileHandle file) {
                    Gdx.app.log(TAG, "Video playback completed");
                    videoFinished = true;
                }
            });

            // Set video size listener to get actual video dimensions
            videoPlayer.setOnVideoSizeListener(new VideoPlayer.VideoSizeListener() {
                @Override
                public void onVideoSize(float width, float height) {
                    Gdx.app.log(TAG, "Video size: " + width + "x" + height);
                    videoWidth = (int) width;
                    videoHeight = (int) height;
                }
            });

            // Load and play video
            FileHandle videoFile = Gdx.files.internal(videoPath);
            if (!videoFile.exists()) {
                Gdx.app.error(TAG, "Video file not found: " + videoPath);
                finishAndTransition();
                return;
            }

            videoPlayer.play(videoFile);
            videoReady = true;
            Gdx.app.log(TAG, "Video playback started: " + videoPath);

        } catch (Exception e) {
            Gdx.app.error(TAG, "Failed to initialize video player: " + e.getMessage(), e);
            finishAndTransition();
        }
    }

    @Override
    public void render(float delta) {
        // Prevent rendering after transition
        if (transitioned) return;

        skipTimer += delta;
        fallbackTimer += delta;

        // Handle skip input
        if (allowSkip && skipTimer > SKIP_DELAY) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) ||
                Gdx.input.isKeyJustPressed(Input.Keys.SPACE) ||
                Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                Gdx.app.log(TAG, "Video skipped by user");
                finishAndTransition();
                return;
            }
        }

        // Check for video completion
        if (videoFinished) {
            finishAndTransition();
            return;
        }

        // Fallback timeout
        if (fallbackTimer > FALLBACK_TIMEOUT && !videoReady) {
            Gdx.app.error(TAG, "Video failed to start within timeout, skipping");
            finishAndTransition();
            return;
        }

        // Clear screen to black (letterbox color)
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Update video player
        if (videoPlayer != null && videoReady) {
            try {
                if (!videoPlayer.update()) {
                    // Video may have finished or failed
                    if (videoPlayer.isBuffered()) {
                        // Still buffering, continue
                    }
                }

                // Get current video frame texture
                Texture frameTexture = videoPlayer.getTexture();

                if (frameTexture != null) {
                    // Reset fallback timer when we get frames
                    fallbackTimer = 0f;

                    // Calculate draw dimensions to maintain aspect ratio (letterbox)
                    float screenWidth = viewport.getWorldWidth();
                    float screenHeight = viewport.getWorldHeight();

                    // Use actual video dimensions if available, otherwise use texture size
                    float texWidth = videoWidth > 0 ? videoWidth : frameTexture.getWidth();
                    float texHeight = videoHeight > 0 ? videoHeight : frameTexture.getHeight();

                    // Calculate scale to fit screen while maintaining aspect ratio
                    float scaleX = screenWidth / texWidth;
                    float scaleY = screenHeight / texHeight;
                    float scale = Math.min(scaleX, scaleY);

                    float drawWidth = texWidth * scale;
                    float drawHeight = texHeight * scale;
                    float drawX = (screenWidth - drawWidth) / 2f;
                    float drawY = (screenHeight - drawHeight) / 2f;

                    // Create TextureRegion to handle potential size mismatch
                    // (texture may be larger than video due to power-of-two padding)
                    TextureRegion region;
                    if (videoWidth > 0 && videoHeight > 0 &&
                        (frameTexture.getWidth() != videoWidth || frameTexture.getHeight() != videoHeight)) {
                        // Crop to actual video dimensions
                        region = new TextureRegion(frameTexture, 0, 0, videoWidth, videoHeight);
                    } else {
                        region = new TextureRegion(frameTexture);
                    }

                    // Draw video frame
                    batch.setProjectionMatrix(camera.combined);
                    batch.begin();
                    batch.draw(region, drawX, drawY, drawWidth, drawHeight);

                    // Draw skip hint
                    if (allowSkip && skipTimer > SKIP_DELAY) {
                        font.getData().setScale(0.8f);
                        font.setColor(1f, 1f, 1f, 0.6f);
                        font.draw(batch, "Press SPACE or ESC to skip", 20f, 40f);
                        font.getData().setScale(1f);
                        font.setColor(Color.WHITE);
                    }

                    batch.end();
                } else {
                    // No texture yet, show loading indicator
                    drawLoadingScreen();
                }

            } catch (Exception e) {
                Gdx.app.error(TAG, "Error during video playback: " + e.getMessage(), e);
                finishAndTransition();
            }
        } else {
            // Video not ready, show loading
            drawLoadingScreen();
        }
    }

    private void drawLoadingScreen() {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        font.setColor(1f, 1f, 1f, 0.8f);
        font.draw(batch, "Loading video...", viewport.getWorldWidth() / 2f - 60f, viewport.getWorldHeight() / 2f);

        if (allowSkip && skipTimer > SKIP_DELAY) {
            font.getData().setScale(0.8f);
            font.setColor(1f, 1f, 1f, 0.5f);
            font.draw(batch, "Press SPACE or ESC to skip", 20f, 40f);
            font.getData().setScale(1f);
        }

        font.setColor(Color.WHITE);
        batch.end();
    }

    private void finishAndTransition() {
        if (transitioned) return;
        transitioned = true;

        Gdx.app.log(TAG, "Transitioning from video screen");

        // Dispose video player first
        disposeVideoPlayer();

        // Resume music
        game.resumeMusic();

        // Execute callback
        if (onFinished != null) {
            Gdx.app.postRunnable(onFinished);
        }
    }

    private void disposeVideoPlayer() {
        if (videoPlayer != null) {
            try {
                videoPlayer.stop();
            } catch (Exception ignored) {
                // Ignore stop errors
            }
            try {
                videoPlayer.dispose();
            } catch (Exception ignored) {
                // Ignore dispose errors
            }
            videoPlayer = null;
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        camera.position.set(viewport.getWorldWidth() / 2f, viewport.getWorldHeight() / 2f, 0f);
        camera.update();
    }

    @Override
    public void pause() {
        if (videoPlayer != null) {
            try {
                videoPlayer.pause();
            } catch (Exception ignored) {
                // Ignore pause errors
            }
        }
    }

    @Override
    public void resume() {
        if (videoPlayer != null && !videoFinished) {
            try {
                videoPlayer.resume();
            } catch (Exception ignored) {
                // Ignore resume errors
            }
        }
    }

    @Override
    public void hide() {
        // Don't dispose here - dispose() will be called separately
    }

    @Override
    public void dispose() {
        Gdx.app.log(TAG, "Disposing CutsceneVideoScreen");

        disposeVideoPlayer();

        if (batch != null) {
            batch.dispose();
            batch = null;
        }

        if (font != null) {
            font.dispose();
            font = null;
        }
    }
}

