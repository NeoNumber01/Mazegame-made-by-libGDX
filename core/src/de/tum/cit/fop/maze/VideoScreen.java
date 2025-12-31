package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import org.jcodec.api.FrameGrab;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;

/**
 * A screen that plays a video and then transitions to another screen.
 * Used for story cutscenes, such as playing before the space mini-game.
 * Uses jcodec for MP4 decoding.
 */
public class VideoScreen implements Screen {

    private static final float SKIP_DELAY = 1.0f; // Allow skipping after 1 second
    private static final float START_TIMEOUT = 2.5f;
    private static final float FRAME_INTERVAL = 1f / 30f; // 30 FPS

    private final MazeRunnerGame game;
    private final Screen nextScreen;
    private final String videoPath;

    private SpriteBatch batch;
    private OrthographicCamera camera;
    private Viewport viewport;
    private BitmapFont font;

    private FrameGrab frameGrab;
    private SeekableByteChannel fileChannel;
    private Texture currentFrameTexture;
    private Pixmap pixmap;

    private boolean videoFinished = false;
    private boolean videoStarted = false;
    private float skipTimer = 0f;
    private float frameTimer = 0f;
    private float startTimer = 0f;

    private String statusText = "Loading video...";

    public VideoScreen(MazeRunnerGame game, String videoPath, Screen nextScreen) {
        this.game = game;
        this.videoPath = videoPath;
        this.nextScreen = nextScreen;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        camera = new OrthographicCamera();
        viewport = new FitViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), camera);
        camera.position.set(viewport.getWorldWidth() / 2, viewport.getWorldHeight() / 2, 0);

        game.pauseMusic();

        initializeVideo();
    }

    private void initializeVideo() {
        try {
            FileHandle videoFile = Gdx.files.internal(videoPath);
            File file = resolveFile(videoFile);

            if (file != null && file.exists()) {
                fileChannel = NIOUtils.readableChannel(file);
                frameGrab = FrameGrab.createFrameGrab(fileChannel);
                videoStarted = true;
                Gdx.app.log("VideoScreen", "Video loaded successfully: " + file.getAbsolutePath());
            } else {
                Gdx.app.error("VideoScreen", "Video file not found: " + videoPath);
                videoFinished = true;
            }
        } catch (Throwable e) {
            Gdx.app.error("VideoScreen", "Error initializing video player", e);
            videoFinished = true;
        }
    }

    private File resolveFile(FileHandle videoFile) {
        if (videoFile.type() == com.badlogic.gdx.Files.FileType.Internal) {
            File file = new File(videoFile.path());
            if (file.exists()) return file;
            
            file = new File(Gdx.files.getLocalStoragePath(), videoPath);
            if (file.exists()) return file;
            
            file = new File(videoPath);
            if (file.exists()) return file;
            
            // Fallback: try to find in assets folder relative to project root if running from IDE
            file = new File("assets/" + videoPath);
            if (file.exists()) return file;
            
            return null;
        }
        return videoFile.file();
    }

    @Override
    public void render(float delta) {
        startTimer += delta;
        skipTimer += delta;

        handleInput();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        boolean drewFrame = updateAndDrawVideo(delta);

        if (!drewFrame) {
            drawStatus();
        }

        checkTimeout();

        if (videoFinished) {
            transitionToNextScreen();
        }
    }

    private void handleInput() {
        if (skipTimer > SKIP_DELAY) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
                Gdx.input.isKeyJustPressed(Input.Keys.SPACE) ||
                Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                videoFinished = true;
            }
        }
    }

    private boolean updateAndDrawVideo(float delta) {
        if (frameGrab == null || !videoStarted) return false;

        frameTimer += delta;
        if (frameTimer >= FRAME_INTERVAL) {
            frameTimer -= FRAME_INTERVAL;
            decodeNextFrame();
        }

        if (currentFrameTexture != null) {
            drawFrame();
            return true;
        }
        return false;
    }

    private void decodeNextFrame() {
        try {
            Picture picture = frameGrab.getNativeFrame();
            if (picture != null) {
                updateTexture(picture);
                statusText = "";
            } else {
                videoFinished = true;
            }
        } catch (Exception e) {
            Gdx.app.error("VideoScreen", "Error decoding video frame", e);
            statusText = "Video decode failed (skip to continue)";
        }
    }

    private void drawFrame() {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        
        float videoWidth = currentFrameTexture.getWidth();
        float videoHeight = currentFrameTexture.getHeight();
        float screenWidth = viewport.getWorldWidth();
        float screenHeight = viewport.getWorldHeight();

        float scale = Math.min(screenWidth / videoWidth, screenHeight / videoHeight);
        float drawWidth = videoWidth * scale;
        float drawHeight = videoHeight * scale;
        float drawX = (screenWidth - drawWidth) / 2;
        float drawY = (screenHeight - drawHeight) / 2;

        batch.draw(currentFrameTexture, drawX, drawY, drawWidth, drawHeight);
        batch.end();
    }

    private void drawStatus() {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        if (statusText != null && !statusText.isEmpty()) {
            font.draw(batch, statusText, 20, viewport.getWorldHeight() - 20);
            font.draw(batch, "Press SPACE/ENTER/ESC to skip", 20, viewport.getWorldHeight() - 50);
        }
        batch.end();
    }

    private void checkTimeout() {
        if (currentFrameTexture == null && startTimer > START_TIMEOUT && !videoFinished) {
            Gdx.app.error("VideoScreen", "Video start timeout, skipping cutscene.");
            videoFinished = true;
        }
    }

    private void updateTexture(Picture picture) {
        try {
            BufferedImage bufferedImage = AWTUtil.toBufferedImage(picture);
            int width = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();

            if (pixmap == null || pixmap.getWidth() != width || pixmap.getHeight() != height) {
                if (pixmap != null) pixmap.dispose();
                if (currentFrameTexture != null) currentFrameTexture.dispose();
                pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
            }

            ByteBuffer buffer = pixmap.getPixels();
            buffer.clear();

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = bufferedImage.getRGB(x, y);
                    int a = (pixel >>> 24) & 0xFF;
                    int r = (pixel >>> 16) & 0xFF;
                    int g = (pixel >>> 8) & 0xFF;
                    int b = pixel & 0xFF;
                    buffer.put((byte) r);
                    buffer.put((byte) g);
                    buffer.put((byte) b);
                    buffer.put((byte) a);
                }
            }
            buffer.flip();

            if (currentFrameTexture != null) {
                currentFrameTexture.dispose();
            }
            currentFrameTexture = new Texture(pixmap);

        } catch (Exception e) {
            Gdx.app.error("VideoScreen", "Error converting frame to texture", e);
            statusText = "Video frame conversion failed (skip to continue)";
        }
    }

    private void transitionToNextScreen() {
        dispose();
        game.setScreen(nextScreen);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        camera.position.set(viewport.getWorldWidth() / 2, viewport.getWorldHeight() / 2, 0);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        if (currentFrameTexture != null) {
            currentFrameTexture.dispose();
            currentFrameTexture = null;
        }
        if (pixmap != null) {
            pixmap.dispose();
            pixmap = null;
        }
        if (batch != null) {
            batch.dispose();
            batch = null;
        }
        if (fileChannel != null) {
            try {
                fileChannel.close();
            } catch (Exception e) {
                Gdx.app.error("VideoScreen", "Error closing file channel", e);
            }
            fileChannel = null;
        }
        frameGrab = null;
        if (font != null) {
            font.dispose();
            font = null;
        }
    }
}
