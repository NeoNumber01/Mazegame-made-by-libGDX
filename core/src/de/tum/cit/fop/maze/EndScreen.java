package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** The game screen that appears when player wins. */
public class EndScreen implements Screen {

    private final MazeRunnerGame game;
    private final Stage stage;
    private final int targetScore;
    private final long elapsedTime;
    private Music victory;
    private Texture backgroundTexture;
    private Label scoreLabel;
    private int displayedScore = 0;
    private float scoreTimer = 0f;

    // Visual Effects
    private ShapeRenderer shapeRenderer;
    private List<Particle> particles;
    private List<Laser> lasers;
    private float stateTime = 0f;

    // Colors
    private static final Color NEON_BLUE = new Color(0.0f, 0.95f, 1.0f, 1.0f);
    private static final Color BRIGHT_PINK = new Color(1.0f, 0.1f, 0.6f, 1.0f);

    private class Laser {
        float x1, y1, x2, y2;
        float life, maxLife;
        float width;
        Color color;
        float speedFactor; // 0 to 1, for animation

        Laser(float width, float height) {
            this.maxLife = MathUtils.random(0.5f, 1.5f);
            this.life = 0; // Starts at 0, animates in and out
            this.width = MathUtils.random(2f, 8f);
            this.color = MathUtils.randomBoolean() ? NEON_BLUE : BRIGHT_PINK;
            
            // Random points on edges
            Vector2 start = getRandomEdgePoint(width, height);
            Vector2 end = getRandomEdgePoint(width, height);
            
            // Ensure some distance
            while(start.dst(end) < Math.min(width, height) / 2) {
                end = getRandomEdgePoint(width, height);
            }

            this.x1 = start.x;
            this.y1 = start.y;
            this.x2 = end.x;
            this.y2 = end.y;
        }

        private Vector2 getRandomEdgePoint(float w, float h) {
            int side = MathUtils.random(3); // 0: top, 1: bottom, 2: left, 3: right
            switch (side) {
                case 0: return new Vector2(MathUtils.random(0, w), h);
                case 1: return new Vector2(MathUtils.random(0, w), 0);
                case 2: return new Vector2(0, MathUtils.random(0, h));
                case 3: return new Vector2(w, MathUtils.random(0, h));
                default: return new Vector2(0,0);
            }
        }
    }

    private class Particle {
        float x, y;
        float vx, vy;
        float life, maxLife;
        float size;
        Color color;
        boolean twinkling;

        Particle(float x, float y, Color baseColor) {
            this.x = x;
            this.y = y;
            // High speed chaos
            float speed = MathUtils.random(50f, 200f);
            float angle = MathUtils.random(0f, 360f);
            this.vx = MathUtils.cosDeg(angle) * speed;
            this.vy = MathUtils.sinDeg(angle) * speed;
            
            this.maxLife = MathUtils.random(1f, 4f);
            this.life = maxLife;
            this.size = MathUtils.random(1f, 4f);
            this.color = new Color(baseColor);
            // Slight color variation
            this.color.a = MathUtils.random(0.5f, 1f);
            this.twinkling = MathUtils.randomBoolean(0.3f);
        }
    }

    public EndScreen(MazeRunnerGame game, int score, long elapsedTime) {
        this.game = game;
        this.targetScore = score;
        this.elapsedTime = elapsedTime;

        game.stopMusic();
        victory = Gdx.audio.newMusic(Gdx.files.internal("victory.mp3"));
        victory.setLooping(true);
        victory.setVolume(game.getVolume());
        victory.play();

        var camera = new OrthographicCamera();
        camera.zoom = 1.0f; 
        stage = new Stage(new ScreenViewport(camera), game.getSpriteBatch());
        Gdx.input.setInputProcessor(stage);

        // Load background texture
        backgroundTexture = new Texture(Gdx.files.internal("floor01.png"));
        backgroundTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

        shapeRenderer = new ShapeRenderer();
        particles = new ArrayList<>();
        lasers = new ArrayList<>();

        setupUI();
    }

    private void setupUI() {
        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        // Victory Title with Animation
        Label victoryLabel = new Label("VICTORY!", game.getSkin(), "title");
        victoryLabel.setFontScale(2.5f);
        victoryLabel.setColor(NEON_BLUE);
        victoryLabel.addAction(Actions.sequence(
            Actions.alpha(0),
            Actions.parallel(
                Actions.fadeIn(1f),
                Actions.scaleTo(1.2f, 1.2f, 1f, Interpolation.elasticOut)
            ),
            Actions.forever(Actions.sequence(
                Actions.color(NEON_BLUE, 1f),
                Actions.color(BRIGHT_PINK, 1f)
            ))
        ));
        table.add(victoryLabel).padBottom(50).center().row();

        // Message
        Label messageLabel = new Label("You escaped the maze!", game.getSkin());
        messageLabel.addAction(Actions.sequence(Actions.delay(0.5f), Actions.fadeIn(1f)));
        table.add(messageLabel).padBottom(40).center().row();

        // Dynamic Score Display
        scoreLabel = new Label("Score: 0", game.getSkin(), "title");
        scoreLabel.setFontScale(1.2f);
        scoreLabel.addAction(Actions.sequence(Actions.delay(1f), Actions.fadeIn(1f)));
        table.add(scoreLabel).padBottom(20).center().row();

        // Time Taken
        long seconds = elapsedTime / 1000;
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        String timeString = String.format("Time Taken: %02d:%02d", minutes, remainingSeconds);
        
        Label timeLabel = new Label(timeString, game.getSkin());
        timeLabel.addAction(Actions.sequence(Actions.delay(1.5f), Actions.fadeIn(1f)));
        table.add(timeLabel).padBottom(60).center().row();

        // Buttons
        Table buttonTable = new Table();
        
        TextButton returnButton = new TextButton("Return to Menu", game.getSkin());
        returnButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                victory.stop();
                game.goToMenu();
            }
        });

        buttonTable.add(returnButton).width(300).height(60).padRight(20);
        buttonTable.addAction(Actions.sequence(Actions.alpha(0), Actions.delay(2f), Actions.fadeIn(1f)));

        table.add(buttonTable).center().row();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Draw tiled background
        stage.getBatch().begin();
        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();
        stage.getBatch().draw(backgroundTexture, 0, 0, width, height, 0, 0, (float)width / backgroundTexture.getWidth(), (float)height / backgroundTexture.getHeight());
        stage.getBatch().end();

        // --- Visual Effects ---
        stateTime += delta;
        float centerX = width / 2f;
        float centerY = height / 2f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(stage.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Rhythm for spawning
        float rhythm = (MathUtils.sin(stateTime * 3f) + 1f) / 2f; // 0 to 1 oscillation

        // 1. Lasers
        // Spawn lasers based on rhythm
        if (MathUtils.random() < (0.05f + rhythm * 0.1f)) { 
            lasers.add(new Laser(width, height));
        }

        Iterator<Laser> laserIter = lasers.iterator();
        while (laserIter.hasNext()) {
            Laser l = laserIter.next();
            l.life += delta;
            
            // Fade in and out
            float alpha;
            if (l.life < l.maxLife * 0.2f) {
                alpha = l.life / (l.maxLife * 0.2f);
            } else if (l.life > l.maxLife * 0.8f) {
                alpha = 1f - ((l.life - l.maxLife * 0.8f) / (l.maxLife * 0.2f));
            } else {
                alpha = 1f;
            }

            if (l.life >= l.maxLife) {
                laserIter.remove();
                continue;
            }

            shapeRenderer.setColor(l.color.r, l.color.g, l.color.b, alpha * 0.8f);
            // Draw line with width (simulated by rectLine)
            shapeRenderer.rectLine(l.x1, l.y1, l.x2, l.y2, l.width);

            // Spawn particles along the laser
            if (MathUtils.randomBoolean(0.3f)) {
                 float t = MathUtils.random();
                 float px = l.x1 + (l.x2 - l.x1) * t;
                 float py = l.y1 + (l.y2 - l.y1) * t;
                 particles.add(new Particle(px, py, l.color));
            }
        }

        // 2. Particles
        // Global spawn
        if (particles.size() < 300) {
             float x = MathUtils.random(0, width);
             float y = MathUtils.random(0, height);
             Color baseColor = MathUtils.randomBoolean() ? NEON_BLUE : BRIGHT_PINK;
             particles.add(new Particle(x, y, baseColor));
        }
        
        Iterator<Particle> pIter = particles.iterator();
        while (pIter.hasNext()) {
            Particle p = pIter.next();
            p.life -= delta;
            p.x += p.vx * delta;
            p.y += p.vy * delta;
            
            // Bounce off screen edges for more chaotic flow
            if (p.x < 0 || p.x > width) p.vx *= -1;
            if (p.y < 0 || p.y > height) p.vy *= -1;

            if (p.life <= 0) {
                pIter.remove();
                continue;
            }

            float alpha = p.life / p.maxLife;
            if (p.twinkling) {
                alpha *= MathUtils.random(0.5f, 1f);
            }
            
            shapeRenderer.setColor(p.color.r, p.color.g, p.color.b, alpha);
            shapeRenderer.circle(p.x, p.y, p.size);
        }

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        // ----------------------

        // Animate score
        if (displayedScore < targetScore) {
            scoreTimer += delta;
            if (scoreTimer > 0.05f) { // Update every 0.05s
                displayedScore += Math.max(1, (targetScore - displayedScore) / 10);
                scoreLabel.setText("Score: " + displayedScore);
                scoreTimer = 0;
            }
        } else {
            scoreLabel.setText("Score: " + targetScore);
        }

        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
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
        victory.dispose();
        backgroundTexture.dispose();
        shapeRenderer.dispose();
    }
}