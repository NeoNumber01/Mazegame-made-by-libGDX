package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/** The game screen that appears when player dies. */
public class GameOverScreen implements Screen {

    private final MazeRunnerGame game;
    private final Stage stage;
    private Music afterDeath;
    private Texture backgroundTexture;
    private Texture skullTexture;
    private Texture vignetteTexture;
    private Texture particleTexture;

    // Animation state
    private float stateTime = 0f;
    private float[] particleX;
    private float[] particleY;
    private float[] particleSpeed;
    private float[] particleAlpha;
    private static final int PARTICLE_COUNT = 50;

    public GameOverScreen(MazeRunnerGame game) {
        this.game = game;
        game.stopMusic();
        afterDeath = Gdx.audio.newMusic(Gdx.files.internal("after death.mp3"));
        afterDeath.setLooping(true);
        afterDeath.setVolume(game.getVolume());
        afterDeath.play();

        // Create visual textures
        createBackgroundTexture();
        createVignetteTexture();
        createParticleTexture();
        initParticles();

        // Load skull texture
        skullTexture = new Texture(Gdx.files.internal("Skull.png"));

        var camera = new OrthographicCamera();
        camera.zoom = 1.5f;

        Viewport viewport = new ScreenViewport(camera);
        stage = new Stage(viewport, game.getSpriteBatch());
        Gdx.input.setInputProcessor(stage);

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        // Add skull image with pulsing animation
        Image skullImage = new Image(skullTexture);
        skullImage.setOrigin(64, 64);
        skullImage.addAction(Actions.forever(
            Actions.sequence(
                Actions.scaleTo(1.1f, 1.1f, 1.5f, Interpolation.sine),
                Actions.scaleTo(1.0f, 1.0f, 1.5f, Interpolation.sine)
            )
        ));
        // Add subtle rotation
        skullImage.addAction(Actions.forever(
            Actions.sequence(
                Actions.rotateBy(3f, 2f, Interpolation.sine),
                Actions.rotateBy(-6f, 4f, Interpolation.sine),
                Actions.rotateBy(3f, 2f, Interpolation.sine)
            )
        ));
        table.add(skullImage).size(128, 128).padBottom(30).row();

        // Title: "YOU ARE DEAD" with dramatic styling and animation
        Label titleLabel = new Label("YOU ARE DEAD", game.getSkin(), "title");
        titleLabel.setColor(new Color(0.9f, 0.1f, 0.1f, 1f));
        // Add color pulsing effect
        titleLabel.addAction(Actions.forever(
            Actions.sequence(
                Actions.color(new Color(0.9f, 0.1f, 0.1f, 1f), 0.8f),
                Actions.color(new Color(0.6f, 0.0f, 0.0f, 1f), 0.8f)
            )
        ));
        table.add(titleLabel).padBottom(20).row();

        // Subtitle message with fade animation
        Label subtitleLabel = new Label("The maze claims another soul...", game.getSkin(), "title");
        subtitleLabel.setFontScale(0.6f);
        subtitleLabel.setColor(new Color(0.6f, 0.6f, 0.6f, 0f));
        subtitleLabel.addAction(Actions.sequence(
            Actions.delay(0.5f),
            Actions.fadeIn(1.5f)
        ));
        table.add(subtitleLabel).padBottom(60).row();

        // Restart Button with hover effect (restart current level without cutscene)
        TextButton restartButton = new TextButton("Try Again", game.getSkin());
        restartButton.getColor().a = 0;
        restartButton.addAction(Actions.sequence(
            Actions.delay(1.0f),
            Actions.fadeIn(0.5f)
        ));
        table.add(restartButton).width(400).height(60).padBottom(20).row();
        restartButton.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        afterDeath.stop();
                        game.startNewGame();
                    }
                });

        // Start New Game Button (starts game with cutscene)
        TextButton startNewGameButton = new TextButton("Start New Game", game.getSkin());
        startNewGameButton.getColor().a = 0;
        startNewGameButton.addAction(Actions.sequence(
            Actions.delay(1.1f),
            Actions.fadeIn(0.5f)
        ));
        table.add(startNewGameButton).width(400).height(60).padBottom(20).row();
        startNewGameButton.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        afterDeath.stop();
                        game.startNewGameWithCutscene();
                    }
                });

        // Exit Button with hover effect
        TextButton exitButton = new TextButton("Return to Menu", game.getSkin());
        exitButton.getColor().a = 0;
        exitButton.addAction(Actions.sequence(
            Actions.delay(1.3f),
            Actions.fadeIn(0.5f)
        ));
        table.add(exitButton).width(400).height(60).padBottom(20).row();
        exitButton.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        afterDeath.stop();
                        game.goToMenu();
                    }
                });

        // Add fade-in animation for entire stage
        stage.getRoot().getColor().a = 0;
        stage.getRoot().addAction(Actions.fadeIn(1.0f));
    }

    /** Creates a dark red gradient background texture. */
    private void createBackgroundTexture() {
        int width = 1;
        int height = 512;
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);

        // Create vertical gradient from dark red at top to black at bottom
        for (int y = 0; y < height; y++) {
            float ratio = (float) y / height;
            // Interpolate from dark red (top) to almost black (bottom)
            float r = 0.25f * (1 - ratio * ratio);  // Smoother falloff
            float g = 0.02f * (1 - ratio);
            float b = 0.05f * (1 - ratio);
            pixmap.setColor(r, g, b, 1f);
            pixmap.drawLine(0, y, width, y);
        }

        backgroundTexture = new Texture(pixmap);
        backgroundTexture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        pixmap.dispose();
    }

    /** Creates a vignette texture for darkening screen edges. */
    private void createVignetteTexture() {
        int size = 512;
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);

        float center = size / 2f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = (x - center) / center;
                float dy = (y - center) / center;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                // Create smooth vignette falloff
                float alpha = dist * dist * 0.8f;
                alpha = Math.min(1f, alpha);

                pixmap.setColor(0, 0, 0, alpha);
                pixmap.drawPixel(x, y);
            }
        }

        vignetteTexture = new Texture(pixmap);
        vignetteTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
    }

    /** Creates a simple particle texture for floating ash/ember effect. */
    private void createParticleTexture() {
        int size = 8;
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);

        float center = size / 2f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = x - center;
                float dy = y - center;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                float alpha = 1f - (dist / center);
                if (alpha < 0) alpha = 0;

                pixmap.setColor(0.8f, 0.2f, 0.1f, alpha);
                pixmap.drawPixel(x, y);
            }
        }

        particleTexture = new Texture(pixmap);
        particleTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
    }

    /** Initialize floating particle positions and speeds. */
    private void initParticles() {
        particleX = new float[PARTICLE_COUNT];
        particleY = new float[PARTICLE_COUNT];
        particleSpeed = new float[PARTICLE_COUNT];
        particleAlpha = new float[PARTICLE_COUNT];

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            resetParticle(i);
            particleY[i] = MathUtils.random(0f, 1f); // Randomize initial Y position
        }
    }

    /** Reset a particle to bottom of screen with random properties. */
    private void resetParticle(int index) {
        particleX[index] = MathUtils.random(0f, 1f);
        particleY[index] = -0.05f;
        particleSpeed[index] = MathUtils.random(0.02f, 0.08f);
        particleAlpha[index] = MathUtils.random(0.3f, 0.8f);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        stateTime += delta;

        // Update particles
        updateParticles(delta);

        // Clear screen with dark color
        Gdx.gl.glClearColor(0.03f, 0.0f, 0.02f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        SpriteBatch batch = game.getSpriteBatch();
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        batch.getProjectionMatrix().setToOrtho2D(0, 0, screenWidth, screenHeight);
        batch.begin();

        // Draw gradient background
        batch.draw(backgroundTexture, 0, 0, screenWidth, screenHeight);

        // Draw floating particles (ash/embers)
        batch.setColor(1f, 1f, 1f, 1f);
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            float x = particleX[i] * screenWidth;
            float y = particleY[i] * screenHeight;
            float size = 4 + particleSpeed[i] * 40;
            batch.setColor(0.9f, 0.3f, 0.1f, particleAlpha[i] * 0.6f);
            batch.draw(particleTexture, x - size/2, y - size/2, size, size);
        }

        // Draw vignette overlay
        batch.setColor(1f, 1f, 1f, 0.7f);
        batch.draw(vignetteTexture, 0, 0, screenWidth, screenHeight);

        // Reset batch color
        batch.setColor(1f, 1f, 1f, 1f);
        batch.end();

        stage.act(Math.min(delta, 1 / 30f));
        stage.draw();
    }

    /** Update floating particles. */
    private void updateParticles(float delta) {
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            // Move particle upward
            particleY[i] += particleSpeed[i] * delta;
            // Add slight horizontal drift
            particleX[i] += MathUtils.sin(stateTime * 2 + i) * 0.001f;

            // Reset particle when it goes off screen
            if (particleY[i] > 1.1f) {
                resetParticle(i);
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
        }
        if (skullTexture != null) {
            skullTexture.dispose();
        }
        if (vignetteTexture != null) {
            vignetteTexture.dispose();
        }
        if (particleTexture != null) {
            particleTexture.dispose();
        }
        if (afterDeath != null) {
            afterDeath.dispose();
        }
    }
}
