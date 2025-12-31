package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import java.util.ArrayList;
import java.util.List;

/** The MenuScreen class is responsible for displaying the main menu of the game. */
public class MenuScreen implements Screen {

    // --- Configuration ---
    public static class Config {
        // Gradient Colors
        public static final Color TOP_LEFT = new Color(0.05f, 0.05f, 0.2f, 1f);
        public static final Color TOP_RIGHT = new Color(0.1f, 0.0f, 0.15f, 1f);
        public static final Color BOTTOM_LEFT = new Color(0.0f, 0.1f, 0.2f, 1f);
        public static final Color BOTTOM_RIGHT = new Color(0.05f, 0.0f, 0.1f, 1f);
        
        // Particles
        public static final int PARTICLE_COUNT = 150;
        public static final float PARTICLE_BASE_SPEED = 30f;
        public static final float MOUSE_REPEL_RADIUS = 200f;
        public static final float MOUSE_REPEL_FORCE = 500f;
        public static final float CONNECTION_DISTANCE = 150f;
    }
    // ---------------------

    private final MazeRunnerGame game;
    private final Stage stage;
    private final boolean pauseMode;
    private final Music menuMusic;
    
    // Visual Effects
    private ShapeRenderer shapeRenderer;
    private List<MenuParticle> particles;
    private float stateTime = 0f;

    // Inner class for background particles
    private class MenuParticle {
        float x, y;
        float vx, vy;
        float size;
        float z; // Depth: 0.5 (far) to 1.5 (close)
        Color color;
        float baseVx, baseVy;

        MenuParticle(float w, float h) {
            reset(w, h, true);
        }

        void reset(float w, float h, boolean randomY) {
            this.z = MathUtils.random(0.5f, 1.5f);
            this.size = MathUtils.random(2f, 5f) * z;
            
            this.x = MathUtils.random(0, w);
            this.y = randomY ? MathUtils.random(0, h) : -10;
            
            float angle = MathUtils.random(0f, 360f);
            float speed = Config.PARTICLE_BASE_SPEED * z; // Parallax speed
            this.baseVx = MathUtils.cosDeg(angle) * speed * 0.2f; // Slight horizontal drift
            this.baseVy = MathUtils.random(speed * 0.5f, speed); // Mostly upwards
            
            this.vx = baseVx;
            this.vy = baseVy;

            // Colors based on depth
            float bright = 0.3f + 0.5f * (z / 1.5f);
            this.color = new Color(0.2f * bright, 0.6f * bright, 1.0f * bright, MathUtils.random(0.3f, 0.8f));
        }

        void update(float delta, float mouseX, float mouseY) {
            // Apply Mouse Repulsion
            float dx = x - mouseX;
            float dy = y - mouseY;
            float dist = (float)Math.sqrt(dx*dx + dy*dy);
            
            if (dist < Config.MOUSE_REPEL_RADIUS) {
                float force = (1f - dist / Config.MOUSE_REPEL_RADIUS) * Config.MOUSE_REPEL_FORCE;
                vx += (dx / dist) * force * delta;
                vy += (dy / dist) * force * delta;
            } else {
                // Return to base velocity
                vx = MathUtils.lerp(vx, baseVx, delta);
                vy = MathUtils.lerp(vy, baseVy, delta);
            }

            x += vx * delta;
            y += vy * delta;

            int w = Gdx.graphics.getWidth();
            int h = Gdx.graphics.getHeight();

            // Wrap around with margin
            if (y > h + 20) {
                reset(w, h, false);
                y = -10;
            }
            if (x < -20) x = w + 10;
            if (x > w + 20) x = -10;
        }
    }

    public MenuScreen(MazeRunnerGame game, boolean pauseMode) {
        this.game = game;
        this.pauseMode = pauseMode;

        var camera = new OrthographicCamera();
        camera.zoom = 1.0f; // Reset zoom for standard UI scaling

        Viewport viewport = new ScreenViewport(camera);
        stage = new Stage(viewport, game.getSpriteBatch());
        Gdx.input.setInputProcessor(stage);

        // Setup Audio
        game.stopMusic();
        menuMusic = Gdx.audio.newMusic(Gdx.files.internal("menu.ogg"));
        menuMusic.setLooping(true);
        menuMusic.setVolume(game.getVolume());
        menuMusic.play();

        // Setup Visuals
        shapeRenderer = new ShapeRenderer();
        particles = new ArrayList<>();
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        for (int i = 0; i < Config.PARTICLE_COUNT; i++) {
            particles.add(new MenuParticle(w, h));
        }

        setupUI();
    }

    private void setupUI() {
        Table rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        // Title
        Label titleLabel = new Label(pauseMode ? "PAUSED" : "MAZE RUNNER", game.getSkin(), "title");
        titleLabel.setFontScale(1.5f);
        titleLabel.setAlignment(Align.center);
        
        // Title Animation: Pulse and Float
        titleLabel.addAction(Actions.forever(Actions.sequence(
            Actions.moveBy(0, 10, 2f, Interpolation.sine),
            Actions.moveBy(0, -10, 2f, Interpolation.sine)
        )));
        
        // Entrance Animation for Title
        titleLabel.getColor().a = 0f;
        titleLabel.addAction(Actions.fadeIn(2f, Interpolation.fade));

        rootTable.add(titleLabel).padBottom(60).row();

        // Button container
        Table buttonTable = new Table();
        rootTable.add(buttonTable);

        // Create buttons
        if (pauseMode) {
            addButton(buttonTable, "Resume Game", () -> {
                menuMusic.stop();
                game.goToGame();
            }, 0.2f);
            addButton(buttonTable, "Start New Game", () -> {
                menuMusic.stop();
                game.startNewGameWithCutscene();
            }, 0.3f);
        } else {
            addButton(buttonTable, "Start New Game", () -> {
                menuMusic.stop();
                game.startNewGameWithCutscene();
            }, 0.2f);
        }

        addButton(buttonTable, "Load Map", this::showLoadMapDialog, pauseMode ? 0.5f : 0.4f);
        addButton(buttonTable, "Volume", this::showVolumeDialog, pauseMode ? 0.7f : 0.6f);
        addButton(buttonTable, "Tutorial", this::showTutorialDialog, pauseMode ? 0.9f : 0.8f);
        addButton(buttonTable, "Exit", () -> Gdx.app.exit(), pauseMode ? 1.1f : 1.0f);
    }

    private void addButton(Table table, String text, Runnable action, float delay) {
        TextButton button = new TextButton(text, game.getSkin());
        
        // Button hover animation
        button.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                button.addAction(Actions.scaleTo(1.1f, 1.1f, 0.1f));
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                button.addAction(Actions.scaleTo(1.0f, 1.0f, 0.1f));
            }
        });
        
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        });

        // Entrance animation
        button.setTransform(true);
        button.getColor().a = 0f;
        button.addAction(Actions.sequence(
            Actions.delay(delay),
            Actions.parallel(
                Actions.fadeIn(0.5f),
                Actions.moveBy(0, 20, 0.5f, Interpolation.pow2Out)
            )
        ));

        // Initial offset for sliding in
        button.moveBy(0, -20);
        
        table.add(button).width(300).height(50).padBottom(15).row();
    }

    private void showTutorialDialog() {
        Dialog tutorialDialog = new Dialog("", game.getSkin()) {
            @Override
            protected void result(Object object) {
                if ((boolean) object) this.hide();
            }
        };
        
        // Style the dialog
        tutorialDialog.getTitleLabel().setAlignment(Align.center);
        tutorialDialog.pad(20);

        String tutorialTextContent = loadTutorialFromFile("tutorial.txt");
        if (tutorialTextContent == null) tutorialTextContent = "Welcome to Maze Runner!\n\nUse Arrow Keys to move.\nAvoid monsters and traps.\nFind the Key to open the Exit.\n\nGood Luck!";

        Label tutorialText = new Label(tutorialTextContent, game.getSkin());
        tutorialText.setWrap(true);
        tutorialText.setAlignment(Align.center);

        ScrollPane scrollPane = new ScrollPane(tutorialText, game.getSkin());
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

        Table contentTable = new Table();
        contentTable.add(new Label("HOW TO PLAY", game.getSkin(), "title")).padBottom(20).row();
        contentTable.add(scrollPane).width(500).height(300).row();

        tutorialDialog.getContentTable().add(contentTable);
        
        TextButton closeBtn = new TextButton("Got it!", game.getSkin());
        tutorialDialog.button(closeBtn, true);

        tutorialDialog.show(stage, Actions.fadeIn(0.3f));
    }

    private String loadTutorialFromFile(String filePath) {
        try {
            return Gdx.files.internal(filePath).readString();
        } catch (Exception e) {
            return null;
        }
    }

    private void showVolumeDialog() {
        Dialog volumeDialog = new Dialog("", game.getSkin()) {
            @Override
            protected void result(Object object) {
                if ((boolean) object) this.hide();
            }
        };
        
        volumeDialog.pad(20);
        Table dialogContent = new Table();
        dialogContent.add(new Label("Audio Settings", game.getSkin(), "title")).padBottom(30).row();

        float initialValue = game.getVolume() * 100f;
        Slider volumeSlider = new Slider(0f, 100f, 1f, false, game.getSkin(), "default-horizontal");
        volumeSlider.setValue(initialValue);
        
        dialogContent.add(new Label("Master Volume", game.getSkin())).padBottom(10).row();
        dialogContent.add(volumeSlider).width(300).padBottom(30).row();

        volumeSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                float sliderValue = volumeSlider.getValue();
                game.setVolume(sliderValue / 100f);
                menuMusic.setVolume(sliderValue / 100f);
            }
        });

        volumeDialog.getContentTable().add(dialogContent);
        volumeDialog.button(new TextButton("Close", game.getSkin()), true);
        volumeDialog.show(stage, Actions.fadeIn(0.3f));
    }

    private void showLoadMapDialog() {
        // No result override needed since buttons handle logic
        Dialog loadMapDialog = new Dialog("", game.getSkin());
        
        loadMapDialog.pad(20);
        loadMapDialog.getContentTable().add(new Label("Select Level", game.getSkin(), "title")).padBottom(20).row();

        Table buttonTable = new Table();
        String[] mapNames = {"Tutorial", "The Beginning", "Dark Corridors", "The Maze", "Deep Descent", "The Core", "Final Challenge"};
        String[] mapPaths = {
            "maps/level-0.properties", "maps/level-1.properties", "maps/level-2.properties",
            "maps/level-3.properties", "maps/level-4.properties", "maps/level-5.properties", "maps/level-6.properties"
        };
        
        ScrollPane scrollPane = new ScrollPane(buttonTable, game.getSkin());
        scrollPane.setFadeScrollBars(false);

        for (int i = 0; i < mapNames.length; i++) {
            String mapName = mapNames[i];
            String mapPath = mapPaths[i];
            TextButton mapButton = new TextButton(mapName, game.getSkin());
            buttonTable.add(mapButton).width(250).padBottom(10).row();
            
            final String selectedMapPath = mapPath;
            mapButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    menuMusic.stop();
                    game.startNewGame(selectedMapPath);
                    loadMapDialog.hide();
                }
            });
        }
        
        loadMapDialog.getContentTable().add(scrollPane).height(300).width(300).row();
        
        TextButton cancelBtn = new TextButton("Cancel", game.getSkin());
        cancelBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                loadMapDialog.hide();
            }
        });
        loadMapDialog.button(cancelBtn); // Just adds to button table
        
        loadMapDialog.show(stage, Actions.fadeIn(0.3f));
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        // --- Dynamic Gradient Background ---
        stateTime += delta;
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        // Calculate Mouse Position for interactions
        float mouseX = Gdx.input.getX();
        float mouseY = h - Gdx.input.getY(); // Invert Y for world coords

        shapeRenderer.setProjectionMatrix(stage.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Pulsing Gradient
        float pulse = (MathUtils.sin(stateTime * 0.5f) + 1f) / 2f; // 0 to 1
        Color c1 = Config.TOP_LEFT.cpy().lerp(Config.BOTTOM_RIGHT, pulse * 0.2f);
        Color c2 = Config.TOP_RIGHT.cpy().lerp(Config.BOTTOM_LEFT, pulse * 0.2f);
        
        shapeRenderer.rect(0, 0, w, h, Config.BOTTOM_LEFT, Config.BOTTOM_RIGHT, c2, c1);
        shapeRenderer.end();
        
        // --- Particle System ---
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        // Update and draw particles
        for (MenuParticle p : particles) {
            p.update(delta, mouseX, mouseY);
            shapeRenderer.setColor(p.color);
            shapeRenderer.circle(p.x, p.y, p.size);
        }
        shapeRenderer.end();

        // Draw connections (Lines)
        // Optimized: Only check close particles to avoid O(N^2) heaviness if N is large.
        // For N=150, brute force is fine (22500 checks is trivial for modern CPUs).
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < particles.size(); i++) {
            MenuParticle p1 = particles.get(i);
            for (int j = i + 1; j < particles.size(); j++) {
                MenuParticle p2 = particles.get(j);
                
                float dst2 = Vector2.dst2(p1.x, p1.y, p2.x, p2.y);
                float maxDist2 = Config.CONNECTION_DISTANCE * Config.CONNECTION_DISTANCE;
                
                if (dst2 < maxDist2) {
                    float alpha = 1f - (dst2 / maxDist2);
                    // Fade line based on distance
                    shapeRenderer.setColor(0.5f, 0.8f, 1f, alpha * 0.3f); 
                    shapeRenderer.line(p1.x, p1.y, p2.x, p2.y);
                }
            }
        }
        shapeRenderer.end();
        
        Gdx.gl.glDisable(GL20.GL_BLEND);
        // -------------------------

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
        shapeRenderer.dispose();
    }
}