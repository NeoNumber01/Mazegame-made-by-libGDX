package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * A small "Space Cruises"-like mini game that gates the real victory screen.
 *
 * Updated rules:
 * - Player ship can move + shoot.
 * - Enemy ships spawn from top, drift/strafe, and shoot back.
 * - Boss spawns after {@link #BOSS_SPAWN_TIME} seconds; defeat the boss to WIN.
 */
public class SpaceCruisesMiniGameScreen implements Screen, Disposable {

    // Difficulty / tuning
    private static final float BOSS_SPAWN_TIME = 20.0f; // Boss spawns after 20 seconds
    private static final float SHIP_SPEED = 380f;

    // Make the game easier: slower spawns
    private static final float SPAWN_INTERVAL_MIN = 0.55f;
    private static final float SPAWN_INTERVAL_MAX = 0.95f;

    // Boss settings
    private static final float BOSS_HP = 150f;
    private static final float BOSS_LASER_SPEED = 480f;
    private static final float BOSS_LASER_W = 8f;
    private static final float BOSS_LASER_H = 28f;

    // Shooting
    private static final float FIRE_COOLDOWN = 0.12f;
    private static final float BULLET_SPEED = 760f;
    private static final float BULLET_W = 6f;
    private static final float BULLET_H = 14f;

    // Overheat
    private static final float MAX_HEAT = 3.0f; // 3 seconds continuous fire
    private static final float HEAT_COOLDOWN_TIME = 0.5f; // 0.5 seconds penalty
    private float shipHeat = 0f;
    private boolean shipOverheated = false;
    private float shipOverheatTimer = 0f;

    // Enemy shooting
    private static final float ENEMY_FIRE_INTERVAL_MIN = 0.55f;
    private static final float ENEMY_FIRE_INTERVAL_MAX = 1.05f;
    private static final float ENEMY_BULLET_SPEED = 420f;
    private static final float ENEMY_BULLET_W = 6f;
    private static final float ENEMY_BULLET_H = 14f;

    // Enemy strafe
    private static final float ENEMY_STRAFE_SPEED = 120f;

    // Playfield size (virtual)
    private static final float WORLD_W = 960f;
    private static final float WORLD_H = 540f;

    // Visual sizes
    // Remove old fixed ship sizes; we use pixel-aware playerDrawW/H instead.
// (kept earlier constants caused confusion and were unused)

//    private static final float SHIP_W = 28f;
//    private static final float SHIP_H = 40f;

    // Enemies (replaces passive obstacles)
    private static final float ENEMY_SPEED_MIN = 160f;
    private static final float ENEMY_SPEED_MAX = 320f;
    private static final float ENEMY_W = 22f;
    private static final float ENEMY_H = 22f;

    // Simple energy/shield for the mini game
    private static final int MAX_HP = 15;

    // Assets
    private static final String PLAYER_SHIP_PATH = "mainspaceship.png";
    private static final String ENEMY_SHIP_PATH = "enemyship.png";
    private static final String ROCKS_PATH = "rocks.png";
    private static final String BOSS_PATH = "boss.png";

    private Texture playerShipTex;
    private Texture enemyShipTex;
    private Texture rocksTex;
    private Texture bossTex;

    private TextureRegion shipRegion;
    private TextureRegion enemyRegion;
    private TextureRegion rocksRegion;
    private TextureRegion bossRegion;
    private Animation<TextureRegion> explosionAnim;

    private final MazeRunnerGame game;
    private final long elapsedTime;
    private final int score;

    private final OrthographicCamera camera;
    private final Viewport viewport;

    private final SpriteBatch batch;
    private final BitmapFont font;

    private ShapeRenderer shapes;
    private Texture pixel;

    // ship
    private float shipX;
    private float shipY;

    // Bullets (fixed arrays)
    private static final int MAX_BULLETS = 96;
    private final float[] bX = new float[MAX_BULLETS];
    private final float[] bY = new float[MAX_BULLETS];
    private int bCount = 0;
    private float fireCd = 0f;

    // Enemies (ships)
    private static final int MAX_ENEMIES = 22; // reduce on-screen clutter
    private final float[] eX = new float[MAX_ENEMIES];
    private final float[] eY = new float[MAX_ENEMIES];
    private final float[] eVY = new float[MAX_ENEMIES];
    private final float[] eVX = new float[MAX_ENEMIES];
    private final float[] eFireT = new float[MAX_ENEMIES];
    private final float[] eNextFire = new float[MAX_ENEMIES];
    private int eCount = 0;

    // Enemy bullets
    private static final int MAX_EB = 80;
    private final float[] ebX = new float[MAX_EB];
    private final float[] ebY = new float[MAX_EB];
    private int ebCount = 0;

    // Explosions (fixed arrays)
    private static final int MAX_EXPL = 32;
    private final float[] exX = new float[MAX_EXPL];
    private final float[] exY = new float[MAX_EXPL];
    private final float[] exT = new float[MAX_EXPL];
    private int exCount = 0;

    // Asteroids (fixed arrays)
    private static final int MAX_ROCKS = 26;
    private final float[] rX = new float[MAX_ROCKS];
    private final float[] rY = new float[MAX_ROCKS];
    private final float[] rVY = new float[MAX_ROCKS];
    private final float[] rVX = new float[MAX_ROCKS];
    private int rCount = 0;

    // Boss state
    private boolean bossActive = false;
    private boolean bossSpawned = false;
    private float bossX;
    private float bossY;
    private float bossHp;
    private float bossFireTimer = 0f;
    private float bossDrawW;
    private float bossDrawH;
    private float bossMoveDir = 1f;

    // Boss AI state machine
    private enum BossState { ENTERING, CHARGING, BURST_FIRING, COOLDOWN, DRIFTING }
    private BossState bossState = BossState.ENTERING;
    private float bossStateTimer = 0f;
    private float bossTargetX = 0f; // Target X position when charging
    private int bossBurstCount = 0; // Number of shots fired in current burst
    private static final int BOSS_BURST_SHOTS = 8; // Shots per burst
    private static final float BOSS_BURST_INTERVAL = 0.08f; // Fast firing during burst
    private static final float BOSS_CHARGE_SPEED = 450f; // Fast charge speed
    private static final float BOSS_DRIFT_SPEED = 40f; // Slow drift speed
    private static final float BOSS_COOLDOWN_TIME = 2.0f; // 2 seconds no firing

    // Randomized delay before charging; sampled once per DRIFTING phase (prevents per-frame re-randomization issues)
    private float bossNextChargeDelay = 2.0f;

    // Boss lasers (fixed arrays)
    private static final int MAX_BOSS_LASERS = 32;
    private final float[] blX = new float[MAX_BOSS_LASERS];
    private final float[] blY = new float[MAX_BOSS_LASERS];
    private int blCount = 0;

    // Space particles (background stars, nebula dust)
    private static final int MAX_STARS = 120; // More stars for richer sky
    private final float[] starX = new float[MAX_STARS];
    private final float[] starY = new float[MAX_STARS];
    private final float[] starSpeed = new float[MAX_STARS];
    private final float[] starSize = new float[MAX_STARS];
    private final float[] starAlpha = new float[MAX_STARS];
    private final int[] starLayer = new int[MAX_STARS]; // 0=far, 1=mid, 2=near

    // Nebula / space dust clouds
    private static final int MAX_NEBULA = 12;
    private final float[] nebulaX = new float[MAX_NEBULA];
    private final float[] nebulaY = new float[MAX_NEBULA];
    private final float[] nebulaSize = new float[MAX_NEBULA];
    private final float[] nebulaAlpha = new float[MAX_NEBULA];
    private final float[] nebulaR = new float[MAX_NEBULA];
    private final float[] nebulaG = new float[MAX_NEBULA];
    private final float[] nebulaB = new float[MAX_NEBULA];
    private final float[] nebulaSpeed = new float[MAX_NEBULA];

    // Shooting stars / meteors
    private static final int MAX_METEORS = 6;
    private final float[] meteorX = new float[MAX_METEORS];
    private final float[] meteorY = new float[MAX_METEORS];
    private final float[] meteorVX = new float[MAX_METEORS];
    private final float[] meteorVY = new float[MAX_METEORS];
    private final float[] meteorLife = new float[MAX_METEORS];
    private final float[] meteorMaxLife = new float[MAX_METEORS];
    private int meteorCount = 0;
    private float meteorSpawnTimer = 0f;

    // Comets (large, slow, spectacular with long glowing tails)
    private static final int MAX_COMETS = 3;
    private final float[] cometX = new float[MAX_COMETS];
    private final float[] cometY = new float[MAX_COMETS];
    private final float[] cometVX = new float[MAX_COMETS];
    private final float[] cometVY = new float[MAX_COMETS];
    private final float[] cometLife = new float[MAX_COMETS];
    private final float[] cometMaxLife = new float[MAX_COMETS];
    private final float[] cometSize = new float[MAX_COMETS]; // Head size
    private final float[] cometR = new float[MAX_COMETS]; // Tail color
    private final float[] cometG = new float[MAX_COMETS];
    private final float[] cometB = new float[MAX_COMETS];
    private int cometCount = 0;
    private float cometSpawnTimer = 0f;

    // Background Planets (always visible, parallax scrolling)
    // (removed)

    // Player bullet trails (for beautiful laser effect)
    private static final int TRAIL_LENGTH = 5;

    // Sparkle/hit flash particles
    private static final int MAX_SPARKS = 64;
    private final float[] spX = new float[MAX_SPARKS];
    private final float[] spY = new float[MAX_SPARKS];
    private final float[] spVX = new float[MAX_SPARKS];
    private final float[] spVY = new float[MAX_SPARKS];
    private final float[] spLife = new float[MAX_SPARKS];
    private final float[] spMaxLife = new float[MAX_SPARKS];
    private int spCount = 0;

    // Screen shake
    private float shakeTime = 0f;
    private float shakeIntensity = 0f;

    private int hp = MAX_HP;

    private float surviveTimer = 0f;
    private float spawnTimer = 0f;
    private float nextSpawn = 0.3f;
    private float rockSpawnTimer = 0f;
    private float nextRockSpawn = 0.5f;

    private float flicker = 0f;

    private enum State { RUNNING, PAUSED, WIN, FAIL }
    private State state = State.RUNNING;
    private boolean disposed = false; // Track if resources have been disposed

    // Pause menu
    private int pauseMenuSelection = 0; // 0 = Resume, 1 = Quit
    private static final int PAUSE_MENU_RESUME = 0;
    private static final int PAUSE_MENU_QUIT = 1;

    private Music miniMusic;
    private Sound laserShotSound;
    private Sound explosionSound;

    // Asteroids
    private static final float ASTEROID_SPAWN_INTERVAL_MIN = 0.90f;
    private static final float ASTEROID_SPAWN_INTERVAL_MAX = 1.45f;
    private static final float ASTEROID_SPEED_MIN = 90f;
    private static final float ASTEROID_SPEED_MAX = 190f;

    // Compute draw sizes based on pixel size
    private float playerDrawW;
    private float playerDrawH;

    private float enemyDrawW;
    private float enemyDrawH;

    private float rockDrawW;
    private float rockDrawH;


    // Soft glow texture (generated once) to avoid visible rectangular boxes
    private Texture glowTex;
    private TextureRegion glowRegion;

    // Scoring (added to the score passed from the maze)
    private static final int SCORE_KILL_ASTEROID = 10;
    private static final int SCORE_KILL_ENEMY = 50;
    private static final int SCORE_KILL_BOSS = 500;

    private int currentScore;

    // Boss death cinematic (1s rhythmic chain explosions)
    private boolean bossDying = false;
    private float bossDeathTimer = 0f;
    private float bossDeathBurstTimer = 0f;
    private int bossDeathBurstsDone = 0;
    private static final float BOSS_DEATH_DURATION = 3.0f;
    private static final float BOSS_DEATH_BURST_INTERVAL = 0.12f; // rhythmic bursts
    private static final int BOSS_DEATH_BURSTS = 25;

    public SpaceCruisesMiniGameScreen(MazeRunnerGame game, int score, long elapsedTime) {
        this.game = game;
        this.score = score;
        this.currentScore = score;
        this.elapsedTime = elapsedTime;

        this.batch = game.getSpriteBatch();
        this.font = game.getSkin().getFont("font");

        camera = new OrthographicCamera();
        viewport = new ScreenViewport(camera);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        camera.setToOrtho(false, WORLD_W, WORLD_H);
        camera.update();

        createPixel();
        createGlow();
        shapes = new ShapeRenderer();

        shipX = WORLD_W * 0.5f;
        shipY = 64f;

        // Load mini-game sprites from assets (dedicated textures)
        playerShipTex = new Texture(Gdx.files.internal(PLAYER_SHIP_PATH));
        enemyShipTex = new Texture(Gdx.files.internal(ENEMY_SHIP_PATH));
        rocksTex = new Texture(Gdx.files.internal(ROCKS_PATH));
        bossTex = new Texture(Gdx.files.internal(BOSS_PATH));

        shipRegion = new TextureRegion(playerShipTex);
        enemyRegion = new TextureRegion(enemyShipTex);
        rocksRegion = new TextureRegion(rocksTex);
        bossRegion = new TextureRegion(bossTex);

        // Compute draw sizes based on pixel size
        float targetShipH = 120f; // slightly smaller player ship (was 148f)
        playerDrawH = targetShipH;
        playerDrawW = targetShipH * shipRegion.getRegionWidth() / (float) shipRegion.getRegionHeight();

        enemyDrawH = 44f;
        enemyDrawW = enemyDrawH * enemyRegion.getRegionWidth() / (float) enemyRegion.getRegionHeight();

        rockDrawH = 34f;
        rockDrawW = rockDrawH * rocksRegion.getRegionWidth() / (float) rocksRegion.getRegionHeight();

        // Boss size (large)
        bossDrawH = 120f;
        bossDrawW = bossDrawH * bossRegion.getRegionWidth() / (float) bossRegion.getRegionHeight();

        // Initialize background stars
        initStars();

        // Explosion: reuse in-game explosion animation frames
        explosionAnim = game.getResourcePack().getExplosionAnimation();

        // optional: stop current music and play a short loop
        game.stopMusic();
        try {
            miniMusic = Gdx.audio.newMusic(Gdx.files.internal("menu.ogg"));
            miniMusic.setLooping(true);
            miniMusic.setVolume(game.getVolume());
            miniMusic.play();
        } catch (Exception ignored) {
            miniMusic = null;
        }

        try {
            laserShotSound = Gdx.audio.newSound(Gdx.files.internal("Laser Shot.wav"));
        } catch (Exception ignored) {
            laserShotSound = null;
        }

        try {
            // Reuse maze mine explosion SFX (stackable; we play it multiple times for multi-blast boss death)
            explosionSound = Gdx.audio.newSound(Gdx.files.internal("explode.ogg"));
        } catch (Exception ignored) {
            explosionSound = null;
        }

        Gdx.input.setInputProcessor(null);

        // first spawn soon
        nextSpawn = MathUtils.random(SPAWN_INTERVAL_MIN, SPAWN_INTERVAL_MAX);
        nextRockSpawn = MathUtils.random(ASTEROID_SPAWN_INTERVAL_MIN, ASTEROID_SPAWN_INTERVAL_MAX);
    }

    private void createPixel() {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        pixel = new Texture(pm);
        pm.dispose();
    }

    private void createGlow() {
        // Creates a small radial alpha texture used for glow underlays without a visible rectangle.
        int size = 64;
        Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pm.setBlending(Pixmap.Blending.None);

        float cx = (size - 1) * 0.5f;
        float cy = (size - 1) * 0.5f;
        float r = size * 0.5f;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = (x - cx) / r;
                float dy = (y - cy) / r;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                float a = 1f - MathUtils.clamp(d, 0f, 1f);
                // soften edge
                a = a * a;
                pm.drawPixel(x, y, Color.rgba8888(1f, 1f, 1f, a));
            }
        }

        glowTex = new Texture(pm);
        glowRegion = new TextureRegion(glowTex);
        pm.dispose();
    }

    private void initStars() {
        // Initialize multi-layer stars
        for (int i = 0; i < MAX_STARS; i++) {
            starX[i] = MathUtils.random(0f, WORLD_W);
            starY[i] = MathUtils.random(0f, WORLD_H);
            starLayer[i] = MathUtils.random(0, 2); // 0=far, 1=mid, 2=near

            // Speed and size based on layer (parallax effect)
            switch (starLayer[i]) {
                case 0: // Far - slow, small, dim
                    starSpeed[i] = MathUtils.random(15f, 35f);
                    starSize[i] = MathUtils.random(0.5f, 1.2f);
                    starAlpha[i] = MathUtils.random(0.15f, 0.35f);
                    break;
                case 1: // Mid - medium
                    starSpeed[i] = MathUtils.random(40f, 80f);
                    starSize[i] = MathUtils.random(1f, 2f);
                    starAlpha[i] = MathUtils.random(0.35f, 0.6f);
                    break;
                case 2: // Near - fast, large, bright
                    starSpeed[i] = MathUtils.random(90f, 150f);
                    starSize[i] = MathUtils.random(1.5f, 3.5f);
                    starAlpha[i] = MathUtils.random(0.6f, 1f);
                    break;
            }
        }

        // Initialize nebula clouds
        for (int i = 0; i < MAX_NEBULA; i++) {
            nebulaX[i] = MathUtils.random(0f, WORLD_W);
            nebulaY[i] = MathUtils.random(0f, WORLD_H);
            nebulaSize[i] = MathUtils.random(80f, 200f);
            nebulaAlpha[i] = MathUtils.random(0.03f, 0.08f);
            nebulaSpeed[i] = MathUtils.random(8f, 25f);

            // Random nebula colors (purple, blue, cyan, pink tones)
            int colorType = MathUtils.random(0, 3);
            switch (colorType) {
                case 0: // Purple
                    nebulaR[i] = 0.6f; nebulaG[i] = 0.2f; nebulaB[i] = 0.8f;
                    break;
                case 1: // Blue
                    nebulaR[i] = 0.2f; nebulaG[i] = 0.4f; nebulaB[i] = 0.9f;
                    break;
                case 2: // Cyan
                    nebulaR[i] = 0.1f; nebulaG[i] = 0.7f; nebulaB[i] = 0.8f;
                    break;
                case 3: // Pink
                    nebulaR[i] = 0.8f; nebulaG[i] = 0.3f; nebulaB[i] = 0.6f;
                    break;
            }
        }
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void render(float delta) {
        // Don't render if disposed
        if (disposed) return;

        // update
        if (state == State.RUNNING) {
            updateRunning(delta);
        } else if (state == State.PAUSED) {
            updatePaused();
        } else {
            // WIN or FAIL - allow quick continue
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                routeResult();
                return;
            }
        }

        // draw
        Gdx.gl.glClearColor(0.02f, 0.03f, 0.05f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        drawBackground();
        drawObstacles();
        drawShip();
        drawHUD();
        batch.end();

        if (state == State.PAUSED) {
            drawPauseMenu();
        } else if (state != State.RUNNING) {
            drawEndOverlay();
        }
    }

    private void updatePaused() {
        // Navigate menu with UP/DOWN or W/S
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            pauseMenuSelection = PAUSE_MENU_RESUME;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            pauseMenuSelection = PAUSE_MENU_QUIT;
        }

        // Confirm selection with ENTER or SPACE
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            if (pauseMenuSelection == PAUSE_MENU_RESUME) {
                state = State.RUNNING;
            } else if (pauseMenuSelection == PAUSE_MENU_QUIT) {
                // Return to main menu - don't dispose here, let the screen transition handle it
                if (miniMusic != null) {
                    miniMusic.stop();
                    miniMusic.dispose();
                    miniMusic = null;
                }
                game.goToMenu();
                return;
            }
        }

        // ESC to resume
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            state = State.RUNNING;
        }
    }

    private void drawPauseMenu() {
        if (shapes == null) return;

        // Darken background
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.7f);
        shapes.rect(0f, 0f, WORLD_W, WORLD_H);

        // Menu panel
        float panelW = 400f;
        float panelH = 220f;
        float panelX = (WORLD_W - panelW) * 0.5f;
        float panelY = (WORLD_H - panelH) * 0.5f;

        // Outer glow border
        shapes.setColor(0.1f, 0.7f, 1f, 0.4f);
        shapes.rect(panelX - 4f, panelY - 4f, panelW + 8f, panelH + 8f);

        // Panel background
        shapes.setColor(0.02f, 0.05f, 0.1f, 0.95f);
        shapes.rect(panelX, panelY, panelW, panelH);

        // Inner border
        shapes.setColor(0.15f, 0.8f, 1f, 0.3f);
        shapes.rect(panelX + 2f, panelY + 2f, panelW - 4f, panelH - 4f);
        shapes.setColor(0.02f, 0.05f, 0.1f, 0.95f);
        shapes.rect(panelX + 4f, panelY + 4f, panelW - 8f, panelH - 8f);

        // Selection highlight
        float btnW = 200f;
        float btnH = 40f;
        float btnX = (WORLD_W - btnW) * 0.5f;
        float resumeY = panelY + panelH - 90f;
        float quitY = panelY + panelH - 150f;

        float selectedY = (pauseMenuSelection == PAUSE_MENU_RESUME) ? resumeY : quitY;

        // Highlight glow
        shapes.setColor(0.1f, 0.8f, 1f, 0.25f);
        shapes.rect(btnX - 10f, selectedY - 5f, btnW + 20f, btnH + 10f);

        // Highlight box
        shapes.setColor(0.15f, 0.85f, 1f, 0.4f);
        shapes.rect(btnX, selectedY, btnW, btnH);

        shapes.end();

        // Draw text
        batch.begin();
        batch.setProjectionMatrix(camera.combined);

        // Title
        font.setColor(0.2f, 0.95f, 1f, 1f);
        font.getData().setScale(1.4f);
        font.draw(batch, "PAUSED", (WORLD_W - 100f) * 0.5f, panelY + panelH - 25f);

        // Menu options
        font.getData().setScale(1.1f);

        // Resume option
        if (pauseMenuSelection == PAUSE_MENU_RESUME) {
            font.setColor(1f, 1f, 1f, 1f);
        } else {
            font.setColor(0.6f, 0.7f, 0.8f, 0.8f);
        }
        font.draw(batch, "RESUME", (WORLD_W - 80f) * 0.5f, resumeY + 28f);

        // Quit option
        if (pauseMenuSelection == PAUSE_MENU_QUIT) {
            font.setColor(1f, 1f, 1f, 1f);
        } else {
            font.setColor(0.6f, 0.7f, 0.8f, 0.8f);
        }
        font.draw(batch, "QUIT TO MENU", (WORLD_W - 150f) * 0.5f, quitY + 28f);

        // Instructions
        font.setColor(0.5f, 0.6f, 0.7f, 0.7f);
        font.getData().setScale(0.7f);
        font.draw(batch, "↑↓ Navigate   ENTER Select   ESC Resume", (WORLD_W - 340f) * 0.5f, panelY + 25f);

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    private void updateRunning(float dt) {
        // Pause game with ESC
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            state = State.PAUSED;
            pauseMenuSelection = PAUSE_MENU_RESUME; // Default to Resume
            return;
        }

        // Update screen shake
        if (shakeTime > 0f) {
            shakeTime -= dt;
        }

        // Update background stars
        updateStars(dt);

        // Update spark particles
        updateSparks(dt);

        // movement
        float input = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) input -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) input += 1f;

        shipX += input * SHIP_SPEED * dt;
        shipX = MathUtils.clamp(shipX, playerDrawW * 0.5f, WORLD_W - playerDrawW * 0.5f);

        // shooting
        fireCd = Math.max(0f, fireCd - dt);

        // Overheat logic
        if (shipOverheated) {
            shipOverheatTimer -= dt;
            if (shipOverheatTimer <= 0f) {
                shipOverheated = false;
                shipHeat = 0f;
            }
        } else {
            // Cool down if not firing
            if (!(Gdx.input.isKeyPressed(Input.Keys.SPACE) || Gdx.input.isButtonPressed(Input.Buttons.LEFT))) {
                shipHeat -= dt; // Cool down at 1.0 rate (3 seconds to cool from max)
                if (shipHeat < 0f) shipHeat = 0f;
            }
        }

        if ((Gdx.input.isKeyPressed(Input.Keys.SPACE) || Gdx.input.isButtonPressed(Input.Buttons.LEFT)) && !shipOverheated) {
            // Heat up
            shipHeat += dt;
            if (shipHeat >= MAX_HEAT) {
                shipOverheated = true;
                shipOverheatTimer = HEAT_COOLDOWN_TIME;
                // Play overheat sound or effect?
                triggerShake(0.1f, 2f);
            } else if (fireCd <= 0f) {
                fireCd = FIRE_COOLDOWN;
                spawnBullet(shipX, shipY + playerDrawH * 0.55f);
                // Play laser sound
                if (laserShotSound != null) {
                    laserShotSound.play(0.5f); // 50% volume
                }
                // Muzzle spark effect
                spawnSparks(shipX, shipY + playerDrawH * 0.5f, 3, 0.15f);
            }
        }

        // Before boss spawn: spawn enemies and asteroids
        if (!bossSpawned) {
            // spawn enemies
            spawnTimer += dt;
            if (spawnTimer >= nextSpawn) {
                spawnTimer = 0f;
                nextSpawn = MathUtils.random(SPAWN_INTERVAL_MIN, SPAWN_INTERVAL_MAX);
                spawnEnemyShip();
            }

            // spawn asteroids
            rockSpawnTimer += dt;
            if (rockSpawnTimer >= nextRockSpawn) {
                rockSpawnTimer = 0f;
                nextRockSpawn = MathUtils.random(ASTEROID_SPAWN_INTERVAL_MIN, ASTEROID_SPAWN_INTERVAL_MAX);
                spawnRock();
            }
        }

        // update player bullets
        for (int i = 0; i < bCount; ) {
            bY[i] += BULLET_SPEED * dt;
            if (bY[i] > WORLD_H + 32f) {
                removeBullet(i);
                continue;
            }
            i++;
        }

        // update enemy bullets
        for (int i = 0; i < ebCount; ) {
            ebY[i] -= ENEMY_BULLET_SPEED * dt;

            // collide with player
            if (Math.abs(ebX[i] - shipX) < (ENEMY_BULLET_W + playerDrawW) * 0.4f
                    && Math.abs(ebY[i] - shipY) < (ENEMY_BULLET_H + playerDrawH) * 0.4f) {
                spawnExplosion(ebX[i], ebY[i]);
                spawnSparks(ebX[i], ebY[i], 8, 0.3f);
                triggerShake(0.15f, 3f);
                removeEnemyBullet(i);
                hp--;
                if (hp <= 0) {
                    state = State.FAIL;
                    return;
                }
                continue;
            }

            if (ebY[i] < -ENEMY_BULLET_H - 24f) {
                removeEnemyBullet(i);
                continue;
            }
            i++;
        }

        // update enemy ships + collisions
        for (int i = 0; i < eCount; ) {
            // movement
            eY[i] -= eVY[i] * dt;
            eX[i] += eVX[i] * dt;
            if (eX[i] < ENEMY_W * 0.5f) {
                eX[i] = ENEMY_W * 0.5f;
                eVX[i] = Math.abs(eVX[i]);
            } else if (eX[i] > WORLD_W - ENEMY_W * 0.5f) {
                eX[i] = WORLD_W - ENEMY_W * 0.5f;
                eVX[i] = -Math.abs(eVX[i]);
            }

            // enemy shooting
            eFireT[i] += dt;
            if (eFireT[i] >= eNextFire[i]) {
                eFireT[i] = 0f;
                eNextFire[i] = MathUtils.random(ENEMY_FIRE_INTERVAL_MIN, ENEMY_FIRE_INTERVAL_MAX);
                spawnEnemyBullet(eX[i], eY[i] - ENEMY_H * 0.55f);
            }

            // collision with ship (ram)
            if (Math.abs(eX[i] - shipX) < (enemyDrawW + playerDrawW) * 0.4f
                    && Math.abs(eY[i] - shipY) < (enemyDrawH + playerDrawH) * 0.4f) {
                spawnExplosion(eX[i], eY[i]);
                spawnSparks(eX[i], eY[i], 12, 0.4f);
                triggerShake(0.2f, 5f);
                removeEnemy(i);
                hp--;
                if (hp <= 0) {
                    state = State.FAIL;
                    return;
                }
                continue;
            }

            // bullet hits enemy
            boolean hit = false;
            for (int b = 0; b < bCount; b++) {
                if (Math.abs(eX[i] - bX[b]) < (enemyDrawW + BULLET_W) * 0.5f
                        && Math.abs(eY[i] - bY[b]) < (enemyDrawH + BULLET_H) * 0.5f) {
                    spawnExplosion(eX[i], eY[i]);
                    spawnSparks(eX[i], eY[i], 10, 0.35f);
                    removeBullet(b);
                    hit = true;
                    break;
                }
            }
            if (hit) {
                currentScore += SCORE_KILL_ENEMY;
                removeEnemy(i);
                continue;
            }

            // remove if off-screen
            if (eY[i] < -ENEMY_H - 12f) {
                removeEnemy(i);
                continue;
            }

            i++;
        }

        // update explosions
        if (exCount > 0) {
            float animDur = explosionAnim.getAnimationDuration();
            for (int i = 0; i < exCount; ) {
                exT[i] += dt;
                if (exT[i] >= animDur) {
                    removeExplosion(i);
                    continue;
                }
                i++;
            }
        }

        // update rocks + collisions
        for (int i = 0; i < rCount; ) {
            rY[i] -= rVY[i] * dt;
            rX[i] += rVX[i] * dt;

            // bounce softly on side bounds
            if (rX[i] < rockDrawW * 0.5f) {
                rX[i] = rockDrawW * 0.5f;
                rVX[i] = Math.abs(rVX[i]);
            } else if (rX[i] > WORLD_W - rockDrawW * 0.5f) {
                rX[i] = WORLD_W - rockDrawW * 0.5f;
                rVX[i] = -Math.abs(rVX[i]);
            }

            // collide with player
            if (Math.abs(rX[i] - shipX) < (rockDrawW + playerDrawW) * 0.4f
                    && Math.abs(rY[i] - shipY) < (rockDrawH + playerDrawH) * 0.4f) {
                spawnExplosion(rX[i], rY[i]);
                spawnSparks(rX[i], rY[i], 8, 0.3f);
                triggerShake(0.15f, 4f);
                removeRock(i);
                hp--;
                if (hp <= 0) {
                    state = State.FAIL;
                    return;
                }
                continue;
            }

            // player bullet vs rock
            boolean rockHit = false;
            for (int b = 0; b < bCount; b++) {
                if (Math.abs(rX[i] - bX[b]) < (rockDrawW + BULLET_W) * 0.5f
                        && Math.abs(rY[i] - bY[b]) < (rockDrawH + BULLET_H) * 0.5f) {
                    spawnExplosion(rX[i], rY[i]);
                    spawnSparks(rX[i], rY[i], 6, 0.25f);
                    removeBullet(b);
                    rockHit = true;
                    break;
                }
            }
            if (rockHit) {
                currentScore += SCORE_KILL_ASTEROID;
                removeRock(i);
                continue;
            }

            // offscreen
            if (rY[i] < -rockDrawH - 12f) {
                removeRock(i);
                continue;
            }

            i++;
        }

        surviveTimer += dt;

        // Boss spawn logic: after BOSS_SPAWN_TIME seconds, spawn boss
        if (!bossSpawned && surviveTimer >= BOSS_SPAWN_TIME) {
            spawnBoss();
        }

        // Boss active: update boss
        if (bossActive) {
            if (bossDying) {
                // play death show; delay WIN overlay until it ends
                updateBossDeathCinematic(dt);
            } else {
                updateBoss(dt);

                // Trigger death show (do NOT end the mini game immediately)
                if (bossHp <= 0) {
                    currentScore += SCORE_KILL_BOSS;
                    startBossDeathCinematic();
                }
            }
        }

        // subtle flicker
        flicker += dt;
    }

    // ===== BOSS DEATH CINEMATIC =====
    private void startBossDeathCinematic() {
        bossDying = true;
        bossDeathTimer = 0f;
        bossDeathBurstTimer = 0f;
        bossDeathBurstsDone = 0;

        // stop boss attacks immediately
        blCount = 0;

        // initial shock
        triggerShake(0.4f, 14f);

        // immediate pops to sell the start
        for (int i = 0; i < 3; i++) {
            float ox = bossX + MathUtils.random(-bossDrawW * 0.25f, bossDrawW * 0.25f);
            float oy = bossY + MathUtils.random(-bossDrawH * 0.2f, bossDrawH * 0.2f);
            spawnExplosion(ox, oy);
            spawnSparks(ox, oy, 14, 0.55f);
            if (explosionSound != null) explosionSound.play(0.65f);
        }
    }

    private void updateBossDeathCinematic(float dt) {
        bossDeathTimer += dt;
        bossDeathBurstTimer += dt;

        // Emit rhythmic bursts over ~1 second
        while (bossDeathBurstTimer >= BOSS_DEATH_BURST_INTERVAL && bossDeathBurstsDone < BOSS_DEATH_BURSTS) {
            bossDeathBurstTimer -= BOSS_DEATH_BURST_INTERVAL;
            bossDeathBurstsDone++;

            int localExplosions = 2 + MathUtils.random(0, 2);
            for (int i = 0; i < localExplosions; i++) {
                float ox = bossX + MathUtils.random(-bossDrawW * 0.48f, bossDrawW * 0.48f);
                float oy = bossY + MathUtils.random(-bossDrawH * 0.42f, bossDrawH * 0.42f);
                spawnExplosion(ox, oy);
                spawnSparks(ox, oy, 12 + MathUtils.random(0, 10), 0.55f);
                if (explosionSound != null) explosionSound.play(0.55f);
            }

            // rhythm shake per burst
            triggerShake(0.08f, 6f);
        }

        // Final detonation at the end of timeline
        if (bossDeathTimer >= BOSS_DEATH_DURATION) {
            spawnExplosion(bossX, bossY);
            spawnExplosion(bossX + MathUtils.random(-25f, 25f), bossY + MathUtils.random(-18f, 18f));
            spawnExplosion(bossX + MathUtils.random(-40f, 40f), bossY + MathUtils.random(-28f, 28f));
            spawnSparks(bossX, bossY, 48, 0.85f);

            if (explosionSound != null) {
                explosionSound.play(0.9f);
                explosionSound.play(0.9f);
            }

            triggerShake(0.55f, 18f);

            // Remove boss and end the round
            bossActive = false;
            bossDying = false;
            state = State.WIN;
        }
    }

    private void spawnBullet(float x, float y) {
        if (bCount >= MAX_BULLETS) return;
        bX[bCount] = x;
        bY[bCount] = y;
        bCount++;
    }

    private void removeBullet(int idx) {
        int last = bCount - 1;
        bX[idx] = bX[last];
        bY[idx] = bY[last];
        bCount--;
    }

    private void spawnEnemyShip() {
        if (eCount >= MAX_ENEMIES) return;
        eX[eCount] = MathUtils.random(ENEMY_W * 0.5f, WORLD_W - ENEMY_W * 0.5f);
        eY[eCount] = WORLD_H + ENEMY_H;
        eVY[eCount] = MathUtils.random(ENEMY_SPEED_MIN, ENEMY_SPEED_MAX);

        // strafe
        float dir = MathUtils.randomBoolean() ? 1f : -1f;
        eVX[eCount] = dir * MathUtils.random(ENEMY_STRAFE_SPEED * 0.6f, ENEMY_STRAFE_SPEED);

        // fire timing
        eFireT[eCount] = MathUtils.random(0f, 0.35f);
        eNextFire[eCount] = MathUtils.random(ENEMY_FIRE_INTERVAL_MIN, ENEMY_FIRE_INTERVAL_MAX);

        eCount++;
    }

    private void spawnEnemyBullet(float x, float y) {
        if (ebCount >= MAX_EB) return;
        ebX[ebCount] = x;
        ebY[ebCount] = y;
        ebCount++;
    }

    private void removeEnemyBullet(int idx) {
        int last = ebCount - 1;
        ebX[idx] = ebX[last];
        ebY[idx] = ebY[last];
        ebCount--;
    }

    private void spawnExplosion(float x, float y) {
        if (exCount >= MAX_EXPL) return;
        exX[exCount] = x;
        exY[exCount] = y;
        exT[exCount] = 0f;
        exCount++;
    }

    private void removeExplosion(int idx) {
        int last = exCount - 1;
        exX[idx] = exX[last];
        exY[idx] = exY[last];
        exT[idx] = exT[last];
        exCount--;
    }

    private void spawnRock() {
        if (rCount >= MAX_ROCKS) return;
        rX[rCount] = MathUtils.random(rockDrawW * 0.5f, WORLD_W - rockDrawW * 0.5f);
        rY[rCount] = WORLD_H + rockDrawH;
        rVY[rCount] = MathUtils.random(ASTEROID_SPEED_MIN, ASTEROID_SPEED_MAX);
        float dir = MathUtils.randomBoolean() ? 1f : -1f;
        rVX[rCount] = dir * MathUtils.random(20f, 90f);
        rCount++;
    }

    private void removeRock(int idx) {
        int last = rCount - 1;
        rX[idx] = rX[last];
        rY[idx] = rY[last];
        rVY[idx] = rVY[last];
        rVX[idx] = rVX[last];
        rCount--;
    }

    private void removeEnemy(int idx) {
        int last = eCount - 1;
        eX[idx] = eX[last];
        eY[idx] = eY[last];
        eVY[idx] = eVY[last];
        eVX[idx] = eVX[last];
        eFireT[idx] = eFireT[last];
        eNextFire[idx] = eNextFire[last];
        eCount--;
    }

    // ========== BOSS METHODS ==========
    private void spawnBoss() {
        bossSpawned = true;
        bossActive = true;
        bossX = WORLD_W * 0.5f;
        bossY = WORLD_H + bossDrawH;
        bossHp = BOSS_HP;
        bossFireTimer = 0f;
        bossMoveDir = MathUtils.randomBoolean() ? 1f : -1f;
        bossState = BossState.ENTERING;
        bossStateTimer = 0f;
        bossBurstCount = 0;
        bossNextChargeDelay = MathUtils.random(1.5f, 3f);

        // Clear remaining enemies/rocks for boss fight
        eCount = 0;
        rCount = 0;
    }

    private void updateBoss(float dt) {
        bossStateTimer += dt;

        // Define bounds once
        float minX = bossDrawW * 0.5f + 30f;
        float maxX = WORLD_W - bossDrawW * 0.5f - 30f;

        // Fix move direction if boss is at edge (prevents getting stuck)
        if (bossX <= minX + 5f && bossMoveDir < 0) {
            bossMoveDir = 1f;
        } else if (bossX >= maxX - 5f && bossMoveDir > 0) {
            bossMoveDir = -1f;
        }

        switch (bossState) {
            case ENTERING:
                // Slide into position from top
                if (bossY > WORLD_H - bossDrawH * 0.7f) {
                    bossY -= 100f * dt;
                } else {
                    // Finished entering, start attack cycle
                    bossState = BossState.DRIFTING;
                    bossStateTimer = 0f;
                    // Always start moving toward center
                    bossMoveDir = (bossX < WORLD_W * 0.5f) ? 1f : -1f;
                    bossNextChargeDelay = MathUtils.random(1.0f, 2.0f);
                }
                break;

            case DRIFTING:
                // Slow drift left/right, preparing for next attack
                bossX += BOSS_DRIFT_SPEED * bossMoveDir * dt;

                // Bounce off edges
                if (bossX <= minX) {
                    bossX = minX;
                    bossMoveDir = 1f;
                } else if (bossX >= maxX) {
                    bossX = maxX;
                    bossMoveDir = -1f;
                }

                // After drifting for a sampled delay, start charging toward player
                if (bossStateTimer >= bossNextChargeDelay) {
                    bossState = BossState.CHARGING;
                    bossStateTimer = 0f;
                    // Target player but clamp to valid range
                    bossTargetX = MathUtils.clamp(shipX, minX + 50f, maxX - 50f);
                    spawnSparks(bossX, bossY, 8, 0.3f);
                }
                break;

            case CHARGING:
                // Rapidly move toward the locked target position
                float chargeDiff = bossTargetX - bossX;

                // Use a timeout to prevent infinite charging
                if (Math.abs(chargeDiff) > 15f && bossStateTimer < 2.0f) {
                    float chargeDir = chargeDiff > 0 ? 1f : -1f;
                    bossX += chargeDir * BOSS_CHARGE_SPEED * dt;

                    // Clamp to screen bounds
                    bossX = MathUtils.clamp(bossX, minX, maxX);

                    // Spawn trail sparks while charging
                    if (MathUtils.randomBoolean(0.4f)) {
                        spawnSparks(bossX, bossY + bossDrawH * 0.3f, 2, 0.15f);
                    }
                } else {
                    // Reached target or timeout, start burst firing
                    bossState = BossState.BURST_FIRING;
                    bossStateTimer = 0f;
                    bossFireTimer = 0f;
                    bossBurstCount = 0;
                    // Warning flash
                    triggerShake(0.1f, 3f);
                }
                break;

            case BURST_FIRING:
                // Rapid fire burst
                bossFireTimer += dt;
                if (bossFireTimer >= BOSS_BURST_INTERVAL && bossBurstCount < BOSS_BURST_SHOTS) {
                    bossFireTimer = 0f;
                    bossBurstCount++;

                    // Fire from both wing cannons
                    float laserOffset = bossDrawW * 0.22f; // was 0.35f; smaller = beams closer
                    spawnBossLaser(bossX - laserOffset, bossY - bossDrawH * 0.4f);
                    spawnBossLaser(bossX + laserOffset, bossY - bossDrawH * 0.4f);

                    // Intense muzzle flash sparks
                    spawnSparks(bossX - laserOffset, bossY - bossDrawH * 0.35f, 5, 0.15f);
                    spawnSparks(bossX + laserOffset, bossY - bossDrawH * 0.35f, 5, 0.15f);

                    // Small shake per shot
                    triggerShake(0.03f, 1.5f);
                }

                // After burst complete, enter cooldown
                if (bossBurstCount >= BOSS_BURST_SHOTS) {
                    bossState = BossState.COOLDOWN;
                    bossStateTimer = 0f;
                    // Move toward center after firing
                    bossMoveDir = (bossX < WORLD_W * 0.5f) ? 1f : -1f;
                }
                break;

            case COOLDOWN:
                // 2 second cooldown - no firing, drift toward center
                bossX += BOSS_DRIFT_SPEED * 0.8f * bossMoveDir * dt;

                // Bounce off edges
                if (bossX <= minX) {
                    bossX = minX;
                    bossMoveDir = 1f;
                } else if (bossX >= maxX) {
                    bossX = maxX;
                    bossMoveDir = -1f;
                }

                if (bossStateTimer >= BOSS_COOLDOWN_TIME) {
                    bossState = BossState.DRIFTING;
                    bossStateTimer = 0f;
                    bossNextChargeDelay = MathUtils.random(1.0f, 2.0f);
                    // Ensure moving toward center
                    bossMoveDir = (bossX < WORLD_W * 0.5f) ? 1f : -1f;
                }
                break;
        }

        // Final safety clamp
        bossX = MathUtils.clamp(bossX, minX, maxX);

        // Update boss lasers (always runs regardless of state)
        for (int i = 0; i < blCount; ) {
            blY[i] -= BOSS_LASER_SPEED * dt;

            // Collide with player
            if (Math.abs(blX[i] - shipX) < (BOSS_LASER_W + playerDrawW) * 0.4f
                    && Math.abs(blY[i] - shipY) < (BOSS_LASER_H + playerDrawH) * 0.4f) {
                spawnExplosion(blX[i], blY[i]);
                spawnSparks(blX[i], blY[i], 10, 0.35f);
                triggerShake(0.2f, 6f);
                removeBossLaser(i);
                hp -= 2; // Boss lasers hurt more
                if (hp <= 0) {
                    state = State.FAIL;
                    return;
                }
                continue;
            }

            // Off screen
            if (blY[i] < -BOSS_LASER_H - 20f) {
                removeBossLaser(i);
                continue;
            }
            i++;
        }

        // Player bullets vs Boss (always runs)
        for (int b = 0; b < bCount; ) {
            if (Math.abs(bX[b] - bossX) < bossDrawW * 0.45f
                    && Math.abs(bY[b] - bossY) < bossDrawH * 0.45f) {
                bossHp -= 1f;
                spawnSparks(bX[b], bY[b], 5, 0.2f);
                removeBullet(b);
                // Small flash on hit
                if (MathUtils.randomBoolean(0.3f)) {
                    triggerShake(0.05f, 2f);
                }
                continue;
            }
            b++;
        }
    }

    private void spawnBossLaser(float x, float y) {
        if (blCount >= MAX_BOSS_LASERS) return;
        blX[blCount] = x;
        blY[blCount] = y;
        blCount++;
    }

    private void removeBossLaser(int idx) {
        int last = blCount - 1;
        blX[idx] = blX[last];
        blY[idx] = blY[last];
        blCount--;
    }

    // ========== PARTICLE / EFFECT METHODS ==========
    private void updateStars(float dt) {
        // Update multi-layer stars
        for (int i = 0; i < MAX_STARS; i++) {
            starY[i] -= starSpeed[i] * dt;
            if (starY[i] < -5f) {
                starY[i] = WORLD_H + 5f;
                starX[i] = MathUtils.random(0f, WORLD_W);
            }
        }

        // Update nebula clouds
        for (int i = 0; i < MAX_NEBULA; i++) {
            nebulaY[i] -= nebulaSpeed[i] * dt;
            if (nebulaY[i] < -nebulaSize[i]) {
                nebulaY[i] = WORLD_H + nebulaSize[i];
                nebulaX[i] = MathUtils.random(-nebulaSize[i] * 0.5f, WORLD_W + nebulaSize[i] * 0.5f);
            }
        }

        // Spawn meteors occasionally
        meteorSpawnTimer += dt;
        if (meteorSpawnTimer > MathUtils.random(1.5f, 4f) && meteorCount < MAX_METEORS) {
            meteorSpawnTimer = 0f;
            spawnMeteor();
        }

        // Update meteors
        for (int i = 0; i < meteorCount; ) {
            meteorX[i] += meteorVX[i] * dt;
            meteorY[i] += meteorVY[i] * dt;
            meteorLife[i] -= dt;

            if (meteorLife[i] <= 0f || meteorY[i] < -50f || meteorX[i] > WORLD_W + 50f) {
                // Remove meteor
                int last = meteorCount - 1;
                meteorX[i] = meteorX[last];
                meteorY[i] = meteorY[last];
                meteorVX[i] = meteorVX[last];
                meteorVY[i] = meteorVY[last];
                meteorLife[i] = meteorLife[last];
                meteorMaxLife[i] = meteorMaxLife[last];
                meteorCount--;
                continue;
            }
            i++;
        }

        // Update comets
        updateComets(dt);
    }

    private void spawnMeteor() {
        if (meteorCount >= MAX_METEORS) return;
        // Spawn from top-left area, move to bottom-right
        meteorX[meteorCount] = MathUtils.random(-30f, WORLD_W * 0.3f);
        meteorY[meteorCount] = WORLD_H + MathUtils.random(10f, 50f);
        meteorVX[meteorCount] = MathUtils.random(180f, 350f);
        meteorVY[meteorCount] = MathUtils.random(-280f, -180f);
        meteorLife[meteorCount] = MathUtils.random(0.8f, 1.5f);
        meteorMaxLife[meteorCount] = meteorLife[meteorCount];
        meteorCount++;
    }

    private void updateComets(float dt) {
        // Spawn comets occasionally (less frequent than meteors)
        cometSpawnTimer += dt;
        if (cometSpawnTimer > MathUtils.random(5f, 12f) && cometCount < MAX_COMETS) {
            cometSpawnTimer = 0f;
            spawnComet();
        }

        // Update existing comets
        for (int i = 0; i < cometCount; ) {
            cometX[i] += cometVX[i] * dt;
            cometY[i] += cometVY[i] * dt;
            cometLife[i] -= dt;

            // Remove if expired or off-screen
            if (cometLife[i] <= 0f || cometY[i] < -100f || cometX[i] > WORLD_W + 100f || cometX[i] < -100f) {
                int last = cometCount - 1;
                cometX[i] = cometX[last];
                cometY[i] = cometY[last];
                cometVX[i] = cometVX[last];
                cometVY[i] = cometVY[last];
                cometLife[i] = cometLife[last];
                cometMaxLife[i] = cometMaxLife[last];
                cometSize[i] = cometSize[last];
                cometR[i] = cometR[last];
                cometG[i] = cometG[last];
                cometB[i] = cometB[last];
                cometCount--;
                continue;
            }
            i++;
        }
    }

    private void spawnComet() {
        if (cometCount >= MAX_COMETS) return;

        // Comets can come from different directions for variety
        int dir = MathUtils.random(0, 2);
        switch (dir) {
            case 0: // From top-left to bottom-right
                cometX[cometCount] = MathUtils.random(-50f, WORLD_W * 0.2f);
                cometY[cometCount] = WORLD_H + MathUtils.random(30f, 80f);
                cometVX[cometCount] = MathUtils.random(60f, 120f);
                cometVY[cometCount] = MathUtils.random(-100f, -60f);
                break;
            case 1: // From top-right to bottom-left
                cometX[cometCount] = WORLD_W + MathUtils.random(30f, 80f);
                cometY[cometCount] = WORLD_H + MathUtils.random(30f, 80f);
                cometVX[cometCount] = MathUtils.random(-120f, -60f);
                cometVY[cometCount] = MathUtils.random(-100f, -60f);
                break;
            case 2: // From top, slightly angled
                cometX[cometCount] = MathUtils.random(WORLD_W * 0.2f, WORLD_W * 0.8f);
                cometY[cometCount] = WORLD_H + MathUtils.random(30f, 80f);
                cometVX[cometCount] = MathUtils.random(-40f, 40f);
                cometVY[cometCount] = MathUtils.random(-80f, -50f);
                break;
        }

        // Comet properties
        cometLife[cometCount] = MathUtils.random(4f, 8f); // Long-lasting
        cometMaxLife[cometCount] = cometLife[cometCount];
        cometSize[cometCount] = MathUtils.random(6f, 11f); // Head size

        // Random comet tail colors (cyan, blue, white-blue, purple-blue)
        int colorType = MathUtils.random(0, 3);
        switch (colorType) {
            case 0: // Cyan-white
                cometR[cometCount] = 0.6f; cometG[cometCount] = 0.9f; cometB[cometCount] = 1f;
                break;
            case 1: // Blue-white
                cometR[cometCount] = 0.4f; cometG[cometCount] = 0.6f; cometB[cometCount] = 1f;
                break;
            case 2: // Ice blue
                cometR[cometCount] = 0.7f; cometG[cometCount] = 0.85f; cometB[cometCount] = 1f;
                break;
            case 3: // Purple-blue
                cometR[cometCount] = 0.6f; cometG[cometCount] = 0.5f; cometB[cometCount] = 1f;
                break;
        }

        cometCount++;
    }

    private void spawnSparks(float x, float y, int count, float life) {
        for (int i = 0; i < count && spCount < MAX_SPARKS; i++) {
            float angle = MathUtils.random(0f, MathUtils.PI2);
            float speed = MathUtils.random(60f, 180f);
            spX[spCount] = x;
            spY[spCount] = y;
            spVX[spCount] = MathUtils.cos(angle) * speed;
            spVY[spCount] = MathUtils.sin(angle) * speed;
            spLife[spCount] = life + MathUtils.random(-0.05f, 0.1f);
            spMaxLife[spCount] = spLife[spCount];
            spCount++;
        }
    }

    private void updateSparks(float dt) {
        for (int i = 0; i < spCount; ) {
            spX[i] += spVX[i] * dt;
            spY[i] += spVY[i] * dt;
            spLife[i] -= dt;
            if (spLife[i] <= 0f) {
                // swap-remove
                int last = spCount - 1;
                spX[i] = spX[last];
                spY[i] = spY[last];
                spVX[i] = spVX[last];
                spVY[i] = spVY[last];
                spLife[i] = spLife[last];
                spMaxLife[i] = spMaxLife[last];
                spCount--;
                continue;
            }
            i++;
        }
    }

    private void triggerShake(float duration, float intensity) {
        shakeTime = Math.max(shakeTime, duration);
        shakeIntensity = Math.max(shakeIntensity, intensity);
    }

    private void routeResult() {
        if (miniMusic != null) {
            miniMusic.stop();
            miniMusic.dispose();
            miniMusic = null;
        }

        if (state == State.WIN) {
            game.setScreen(new EndScreen(game, currentScore, elapsedTime));
        } else {
            game.setScreen(new GameOverScreen(game));
        }

        dispose();
    }

    private void drawBackground() {
        // Reset camera to default position first
        camera.position.set(WORLD_W * 0.5f, WORLD_H * 0.5f, 0f);

        // Apply screen shake offset (temporary, will be reset next frame)
        if (shakeTime > 0f) {
            float ox = MathUtils.random(-shakeIntensity, shakeIntensity);
            float oy = MathUtils.random(-shakeIntensity, shakeIntensity);
            camera.position.add(ox, oy, 0f);
            // Decay shake intensity
            shakeIntensity *= 0.9f;
        }

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        // ===== LAYER 1: Deep space gradient (dark blue to black) =====
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

        // ===== LAYER 2: Nebula clouds (soft colored fog) =====
        for (int i = 0; i < MAX_NEBULA; i++) {
            float pulse = 0.8f + 0.2f * MathUtils.sin(flicker * 0.5f + i * 1.2f);
            batch.setColor(nebulaR[i], nebulaG[i], nebulaB[i], nebulaAlpha[i] * pulse);
            float s = nebulaSize[i];
            batch.draw(glowRegion, nebulaX[i] - s * 0.5f, nebulaY[i] - s * 0.5f, s, s);
        }

        // ===== LAYER 3: Far stars (tiny, slow, dim - white/blue) =====
        for (int i = 0; i < MAX_STARS; i++) {
            if (starLayer[i] != 0) continue;
            float twinkle = 0.6f + 0.4f * MathUtils.sin(flicker * 3f + i * 0.7f);
            batch.setColor(0.7f, 0.8f, 1f, starAlpha[i] * twinkle);
            batch.draw(glowRegion, starX[i] - starSize[i], starY[i] - starSize[i],
                    starSize[i] * 2f, starSize[i] * 2f);
        }


        // ===== LAYER 4: Mid stars =====
        for (int i = 0; i < MAX_STARS; i++) {
            if (starLayer[i] != 1) continue;
            float twinkle = 0.7f + 0.3f * MathUtils.sin(flicker * 5f + i * 0.5f);
            // Slight color variation
            float r = 0.8f + 0.2f * MathUtils.sin(i * 0.3f);
            float g = 0.85f + 0.15f * MathUtils.cos(i * 0.4f);
            float b = 0.95f + 0.05f * MathUtils.sin(i * 0.2f);
            batch.setColor(r, g, b, starAlpha[i] * twinkle);
            batch.draw(glowRegion, starX[i] - starSize[i], starY[i] - starSize[i],
                    starSize[i] * 2f, starSize[i] * 2f);
        }

        // ===== LAYER 5: Near stars (large, bright, fast - with glow) =====
        for (int i = 0; i < MAX_STARS; i++) {
            if (starLayer[i] != 2) continue;
            float twinkle = 0.8f + 0.2f * MathUtils.sin(flicker * 8f + i * 0.3f);
            // Bright white/cyan
            batch.setColor(0.9f, 0.95f, 1f, starAlpha[i] * twinkle);
            // Draw larger glow halo
            batch.draw(glowRegion, starX[i] - starSize[i] * 1.5f, starY[i] - starSize[i] * 1.5f,
                    starSize[i] * 3f, starSize[i] * 3f);
            // Draw bright core
            batch.setColor(1f, 1f, 1f, starAlpha[i] * twinkle * 0.9f);
            batch.draw(glowRegion, starX[i] - starSize[i] * 0.5f, starY[i] - starSize[i] * 0.5f,
                    starSize[i], starSize[i]);
        }

        // ===== LAYER 6: Shooting stars / meteors =====
        for (int i = 0; i < meteorCount; i++) {
            float lifeRatio = meteorLife[i] / meteorMaxLife[i];
            float alpha = lifeRatio * 0.9f;

            // Meteor trail (multiple segments fading)
            float trailLen = 60f;
            float dx = -meteorVX[i] / 300f * trailLen;
            float dy = -meteorVY[i] / 300f * trailLen;

            // Outer glow trail
            batch.setColor(0.4f, 0.6f, 1f, alpha * 0.3f);
            for (int t = 0; t < 5; t++) {
                float tt = t / 5f;
                float tx = meteorX[i] + dx * tt;
                float ty = meteorY[i] + dy * tt;
                float ts = (1f - tt) * 12f;
                batch.draw(glowRegion, tx - ts, ty - ts, ts * 2f, ts * 2f);
            }

            // Core trail (bright)
            batch.setColor(0.9f, 0.95f, 1f, alpha * 0.8f);
            for (int t = 0; t < 3; t++) {
                float tt = t / 3f;
                float tx = meteorX[i] + dx * tt * 0.5f;
                float ty = meteorY[i] + dy * tt * 0.5f;
                float ts = (1f - tt) * 5f;
                batch.draw(glowRegion, tx - ts, ty - ts, ts * 2f, ts * 2f);
            }

            // Meteor head (bright point)
            batch.setColor(1f, 1f, 1f, alpha);
            batch.draw(glowRegion, meteorX[i] - 3f, meteorY[i] - 3f, 6f, 6f);
        }

        // ===== LAYER 6.5: COMETS (spectacular long-tailed celestial bodies) =====
        for (int i = 0; i < cometCount; i++) {
            float lifeRatio = cometLife[i] / cometMaxLife[i];
            float alpha = Math.min(1f, lifeRatio * 1.5f); // Fade in/out
            float size = cometSize[i];

            // Calculate tail direction (opposite of velocity)
            float speed = (float) Math.sqrt(cometVX[i] * cometVX[i] + cometVY[i] * cometVY[i]);
            float tailDirX = -cometVX[i] / speed;
            float tailDirY = -cometVY[i] / speed;

            // ---- OUTER DIFFUSE TAIL (wide, faint) ----
            float outerTailLen = 180f;
            for (int t = 0; t < 12; t++) {
                float tt = t / 12f;
                float tailX = cometX[i] + tailDirX * outerTailLen * tt;
                float tailY = cometY[i] + tailDirY * outerTailLen * tt;
                float tailAlpha = (1f - tt) * alpha * 0.12f;
                float tailSize = size * (2.5f + tt * 3f);

                // Slight wobble for organic feel
                float wobble = MathUtils.sin(flicker * 3f + t * 0.8f) * (4f + t * 0.5f);
                tailX += tailDirY * wobble; // Perpendicular wobble

                batch.setColor(cometR[i], cometG[i], cometB[i], tailAlpha);
                batch.draw(glowRegion, tailX - tailSize, tailY - tailSize, tailSize * 2f, tailSize * 2f);
            }

            // ---- MAIN GLOWING TAIL (medium width, brighter) ----
            float mainTailLen = 120f;
            for (int t = 0; t < 16; t++) {
                float tt = t / 16f;
                float tailX = cometX[i] + tailDirX * mainTailLen * tt;
                float tailY = cometY[i] + tailDirY * mainTailLen * tt;
                float tailAlpha = (1f - tt * tt) * alpha * 0.35f; // Quadratic falloff
                float tailSize = size * (1.2f + tt * 1.5f);

                batch.setColor(cometR[i] * 1.1f, cometG[i] * 1.1f, cometB[i], tailAlpha);
                batch.draw(glowRegion, tailX - tailSize, tailY - tailSize, tailSize * 2f, tailSize * 2f);
            }

            // ---- INNER BRIGHT CORE TAIL (narrow, bright) ----
            float innerTailLen = 60f;
            for (int t = 0; t < 10; t++) {
                float tt = t / 10f;
                float tailX = cometX[i] + tailDirX * innerTailLen * tt;
                float tailY = cometY[i] + tailDirY * innerTailLen * tt;
                float tailAlpha = (1f - tt) * alpha * 0.7f;
                float tailSize = size * (0.6f + tt * 0.5f);

                batch.setColor(1f, 1f, 1f, tailAlpha);
                batch.draw(glowRegion, tailX - tailSize, tailY - tailSize, tailSize * 2f, tailSize * 2f);
            }

            // ---- DUST PARTICLES along tail ----
            for (int p = 0; p < 8; p++) {
                float pt = p / 8f;
                float px = cometX[i] + tailDirX * mainTailLen * pt;
                float py = cometY[i] + tailDirY * mainTailLen * pt;

                // Random offset for dust scatter
                float scatter = 15f + pt * 25f;
                px += MathUtils.sin(flicker * 5f + p * 1.5f + i) * scatter * tailDirY;
                py += MathUtils.cos(flicker * 5f + p * 1.5f + i) * scatter * (-tailDirX);

                float dustAlpha = (1f - pt) * alpha * 0.4f;
                float dustSize = 2f + MathUtils.random(0f, 2f);

                batch.setColor(cometR[i], cometG[i], cometB[i], dustAlpha);
                batch.draw(glowRegion, px - dustSize, py - dustSize, dustSize * 2f, dustSize * 2f);
            }

            // ---- COMET HEAD (coma - glowing nucleus) ----
            // Outer coma glow
            batch.setColor(cometR[i], cometG[i], cometB[i], alpha * 0.4f);
            batch.draw(glowRegion, cometX[i] - size * 1.5f, cometY[i] - size * 1.5f, size * 3f, size * 3f);

            // Inner coma
            batch.setColor(cometR[i] * 1.2f, cometG[i] * 1.2f, 1f, alpha * 0.6f);
            batch.draw(glowRegion, cometX[i] - size * 0.9f, cometY[i] - size * 0.9f, size * 1.8f, size * 1.8f);

            // Bright nucleus core - FIXED: Using glowRegion instead of square pixel for a rounder look
            batch.setColor(1f, 1f, 1f, alpha * 0.9f);
            batch.draw(glowRegion, cometX[i] - size * 0.35f, cometY[i] - size * 0.35f, size * 0.7f, size * 0.7f);
        }

        // ===== LAYER 7: Spark particles =====
        for (int i = 0; i < spCount; i++) {
            float alpha = spLife[i] / spMaxLife[i];
            // Orange/yellow sparks
            batch.setColor(1f, 0.7f +  0.3f * alpha, 0.3f, alpha * 0.9f);
            float s = 4f + 4f * alpha;
            batch.draw(glowRegion, spX[i] - s * 0.5f, spY[i] - s * 0.5f, s, s);
        }

        // ===== LAYER 8: Subtle scan lines / grid (sci-fi atmosphere) =====
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        float pulse = 0.015f + 0.008f * MathUtils.sin(flicker * 4f);
        batch.setColor(0.15f, 0.7f, 1f, pulse);

        // Subtle vertical lines
        for (int x = 0; x <= 32; x++) {
            float px = x * (WORLD_W / 32f);
            batch.draw(pixel, px, 0, 1f, WORLD_H);
        }
        // Subtle horizontal lines
        for (int y = 0; y <= 18; y++) {
            float py = y * (WORLD_H / 18f);
            batch.draw(pixel, 0, py, WORLD_W, 1f);
        }

        // reset
        batch.setColor(Color.WHITE);
    }

    private void drawShip() {
        float w = playerDrawW;
        float h = playerDrawH;
        float x = shipX - w * 0.5f;
        float y = shipY - h * 0.5f;

        // Thruster effect (behind ship)
        float thrusterY = y + 30f; // Near bottom of ship
        float thrusterW = w * 0.12f; // Narrower thrusters
        float thrusterH = h * 0.45f;
        float flickerScale = 0.8f + 0.2f * MathUtils.sin(flicker * 20f);

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        
        // --- ENHANCED PLAYER THRUSTER (DUAL ENGINE) ---
        // Player ship now has 2 distinct engine trails like the boss
        // Reduced distance between thrusters (from 0.2f to 0.12f)
        float[] pThrusterOffsets = {-0.10f, 0.10f}; // Left and right engines

        for(float offset : pThrusterOffsets) {
            float tx = shipX + offset * w;
            
            // 1. Outer blue glow (wide)
            // Moved closer to ship body (added +10f to Y)
            batch.setColor(0.1f, 0.4f, 1f, 0.4f);
            batch.draw(glowRegion, tx - thrusterW * 1.5f, thrusterY - thrusterH * 1.2f * flickerScale + 10f, thrusterW * 3f, thrusterH * 1.5f * flickerScale);
            
            // 2. Main cyan flame
            batch.setColor(0.2f, 0.8f, 1f, 0.7f); 
            batch.draw(glowRegion, tx - thrusterW * 0.8f, thrusterY - thrusterH * flickerScale + 10f, thrusterW * 1.6f, thrusterH * flickerScale);

            // 3. Inner white core (hot)
            batch.setColor(0.8f, 0.95f, 1f, 0.9f);
            batch.draw(glowRegion, tx - thrusterW * 0.4f, thrusterY - thrusterH * 0.6f * flickerScale + 10f, thrusterW * 0.8f, thrusterH * 0.6f * flickerScale);
        }

        // glow underlay (soft radial)
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        batch.setColor(0.2f, 0.9f, 1f, 0.22f);
        float gw = w * 1.55f;
        float gh = h * 1.45f;
        batch.draw(glowRegion, shipX - gw * 0.5f, shipY - gh * 0.5f, gw, gh);

        // sprite
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        if (shipOverheated) {
            // Pulse red when overheated
            float pulse = 0.5f + 0.5f * MathUtils.sin(flicker * 15f);
            batch.setColor(1f, pulse, pulse, 1f);
        } else {
            batch.setColor(Color.WHITE);
        }
        batch.draw(shipRegion, x, y, w, h);

        // muzzle flash when firing
        if (fireCd > FIRE_COOLDOWN * 0.5f) {
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            batch.setColor(0.9f, 1f, 1f, 0.35f);
            batch.draw(glowRegion, shipX - 10f, shipY + h * 0.35f, 20f, 26f);
        }

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(Color.WHITE);
    }

    private void drawObstacles() {
        // Asteroids
        for (int i = 0; i < rCount; i++) {
            float x = rX[i] - rockDrawW * 0.5f;
            float y = rY[i] - rockDrawH * 0.5f;

            // glow (soft)
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            batch.setColor(0.9f, 0.65f, 0.25f, 0.10f);
            float gw = rockDrawW * 1.6f;
            float gh = rockDrawH * 1.6f;
            batch.draw(glowRegion, rX[i] - gw * 0.5f, rY[i] - gh * 0.5f, gw, gh);

            // sprite
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            batch.setColor(1f, 1f, 1f, 1f);
            batch.draw(rocksRegion, x, y, rockDrawW, rockDrawH);
        }

        // Enemy ships
        for (int i = 0; i < eCount; i++) {
            float x = eX[i] - enemyDrawW * 0.5f;
            float y = eY[i] - enemyDrawH * 0.5f;

            // Thruster effect (above ship, facing up/backwards relative to screen movement)
            // Enemy moves down, so thruster is at top (y + h)
            float thrusterY = y + enemyDrawH - 5f;
            float thrusterW = enemyDrawW * 0.5f;
            float thrusterH = enemyDrawH * 0.6f;
            float flickerScale = 0.8f + 0.2f * MathUtils.sin(flicker * 15f + i);

            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            
            // --- ENHANCED ENEMY THRUSTER ---
            // 1. Outer red/orange glow
            batch.setColor(1f, 0.2f, 0.1f, 0.4f);
            batch.draw(glowRegion, eX[i] - thrusterW * 0.8f, thrusterY, thrusterW * 1.6f, thrusterH * 1.4f * flickerScale);
            
            // 2. Main orange flame
            batch.setColor(1f, 0.5f, 0.1f, 0.7f); 
            batch.draw(glowRegion, eX[i] - thrusterW * 0.5f, thrusterY, thrusterW, thrusterH * flickerScale);
            
            // 3. Inner yellow core
            batch.setColor(1f, 0.9f, 0.6f, 0.8f);
            batch.draw(glowRegion, eX[i] - thrusterW * 0.25f, thrusterY, thrusterW * 0.5f, thrusterH * 0.5f * flickerScale);

            // glow (soft)
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            batch.setColor(1f, 0.25f, 0.35f, 0.10f);
            float gw = enemyDrawW * 1.55f;
            float gh = enemyDrawH * 1.55f;
            batch.draw(glowRegion, eX[i] - gw * 0.5f, eY[i] - gh * 0.5f, gw, gh);

            // sprite
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            batch.setColor(1f, 1f, 1f, 1f);
            batch.draw(enemyRegion, x, y, enemyDrawW, enemyDrawH);
        }

        // BOSS
        if (bossActive || bossDying) {
            float bx = bossX - bossDrawW * 0.5f;
            float by = bossY - bossDrawH * 0.5f;

            // --- ENHANCED BOSS THRUSTERS ---
            // Boss has 4 engines now, closer to the body
            // Move thrusters down a bit (closer to body center/engine nozzles)
            float bossThrusterY = by + bossDrawH - 55f;
            float bossThrusterW = bossDrawW * 0.12f;
            float bossThrusterH = bossDrawH * 0.45f;
            float bossFlicker = 0.9f + 0.1f * MathUtils.sin(flicker * 12f);
            
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            
            // Draw 4 main engine thrusters
            float[] thrusterOffsets = {-0.22f, -0.1f, 0.1f, 0.22f};
            
            for(float offset : thrusterOffsets) {
                float tx = bossX + offset * bossDrawW;
                
                // Outer glow
                batch.setColor(1f, 0.1f, 0.3f, 0.5f);
                batch.draw(glowRegion, tx - bossThrusterW, bossThrusterY, bossThrusterW * 2f, bossThrusterH * 1.5f * bossFlicker);
                
                // Main flame
                batch.setColor(1f, 0.4f, 0.1f, 0.8f);
                batch.draw(glowRegion, tx - bossThrusterW * 0.6f, bossThrusterY, bossThrusterW * 1.2f, bossThrusterH * bossFlicker);
                
                // Core
                batch.setColor(1f, 0.9f, 0.7f, 0.9f);
                batch.draw(glowRegion, tx - bossThrusterW * 0.3f, bossThrusterY, bossThrusterW * 0.6f, bossThrusterH * 0.6f * bossFlicker);
            }

            // Boss warning glow (pulsing red/orange)
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            float pulseAlpha = 0.15f + 0.1f * MathUtils.sin(flicker * 10f);
            batch.setColor(1f, 0.3f, 0.1f, pulseAlpha);
            float bgw = bossDrawW * 1.8f;
            float bgh = bossDrawH * 1.6f;
            batch.draw(glowRegion, bossX - bgw * 0.5f, bossY - bgh * 0.5f, bgw, bgh);

            // Secondary glow (cyan highlight)
            batch.setColor(0.2f, 0.8f, 1f, 0.08f);
            batch.draw(glowRegion, bossX - bgw * 0.4f, bossY - bgh * 0.4f, bgw * 0.8f, bgh * 0.8f);

            // Boss sprite
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            batch.setColor(Color.WHITE);
            batch.draw(bossRegion, bx, by, bossDrawW / 2f, bossDrawH / 2f, bossDrawW, bossDrawH, 1f, 1f, 180f);

            // Boss HP bar
            float hpRatio = Math.max(0f, bossHp / BOSS_HP);
            float barW = bossDrawW * 0.8f;
            float barH = 6f;
            float barX = bossX - barW * 0.5f;
            float barY = bossY + bossDrawH * 0.55f;

            // BG
            batch.setColor(0.1f, 0.1f, 0.1f, 0.7f);
            batch.draw(pixel, barX - 2f, barY - 2f, barW + 4f, barH + 4f);
            // HP fill
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            batch.setColor(1f, 0.2f, 0.2f, 0.8f);
            batch.draw(pixel, barX, barY, barW * hpRatio, barH);
            // Glow on HP bar
            batch.setColor(1f, 0.5f, 0.3f, 0.3f);
            batch.draw(glowRegion, barX, barY - 4f, barW * hpRatio, barH + 8f);
        }

        // Boss lasers (cyan/white energy beams - ENHANCED)
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (int i = 0; i < blCount; i++) {
            float bx = blX[i];
            float by = blY[i];

            // Layer 1: Wide outer glow (cyan/blue haze)
            batch.setColor(0.1f, 0.6f, 1f, 0.25f);
            batch.draw(glowRegion, bx - 15f, by - 20f, 30f, 40f);

            // Layer 2: Energy trail
            for (int t = 1; t <= 3; t++) {
                float trailAlpha = (1f - t / 4f) * 0.4f;
                float trailY = by + t * 10f; // Trail goes up (laser goes down)
                float trailW = BOSS_LASER_W * (1f - t * 0.2f);

                batch.setColor(0.2f, 0.8f, 1f, trailAlpha);
                batch.draw(glowRegion, bx - trailW * 1.5f, trailY - 6f, trailW * 3f, 12f);
            }

            // Layer 3: Main beam glow
            batch.setColor(0.2f, 0.9f, 1f, 0.6f);
            batch.draw(glowRegion, bx - BOSS_LASER_W * 2f, by - BOSS_LASER_H * 0.8f,
                    BOSS_LASER_W * 4f, BOSS_LASER_H * 1.6f);

            // Layer 4: Bright core
            batch.setColor(0.8f, 1f, 1f, 0.95f);
            batch.draw(pixel, bx - BOSS_LASER_W * 0.5f, by - BOSS_LASER_H * 0.5f,
                    BOSS_LASER_W, BOSS_LASER_H);

            // Layer 5: Hot tip
            batch.setColor(1f, 1f, 1f, 1f);
            batch.draw(glowRegion, bx - 4f, by - BOSS_LASER_H * 0.5f - 4f, 8f, 12f);
        }

        // Enemy bullets (red energy bolts - ENHANCED)
        for (int i = 0; i < ebCount; i++) {
            float ex = ebX[i];
            float ey = ebY[i];

            // Layer 1: Red glow
            batch.setColor(1f, 0.2f, 0.2f, 0.3f);
            batch.draw(glowRegion, ex - 12f, ey - 12f, 24f, 24f);

            // Layer 2: Trail
            batch.setColor(1f, 0.4f, 0.2f, 0.4f);
            batch.draw(glowRegion, ex - 6f, ey + 6f, 12f, 16f);

            // Layer 3: Core
            batch.setColor(1f, 0.6f, 0.6f, 0.9f);
            batch.draw(pixel, ex - ENEMY_BULLET_W * 0.5f, ey - ENEMY_BULLET_H * 0.5f,
                    ENEMY_BULLET_W, ENEMY_BULLET_H);
        }

        // Player bullets (BEAUTIFUL LASER EFFECT - multi-layer energy beams)
        for (int i = 0; i < bCount; i++) {
            float bx = bX[i];
            float by = bY[i];

            // Layer 1: Wide outer glow (cyan/blue haze)
            batch.setColor(0.1f, 0.5f, 0.9f, 0.15f);
            batch.draw(glowRegion, bx - 20f, by - 25f, 40f, 50f);

            // Layer 2: Energy trail (fading segments behind bullet)
            for (int t = 1; t <= 4; t++) {
                float trailAlpha = (1f - t / 5f) * 0.4f;
                float trailY = by - t * 12f;
                float trailW = BULLET_W * (1f - t * 0.15f);

                // Trail outer glow
                batch.setColor(0.2f, 0.7f, 1f, trailAlpha * 0.5f);
                batch.draw(glowRegion, bx - trailW * 2f, trailY - 8f, trailW * 4f, 16f);

                // Trail core
                batch.setColor(0.4f, 0.9f, 1f, trailAlpha);
                batch.draw(pixel, bx - trailW * 0.5f, trailY - 4f, trailW, 8f);
            }

            // Layer 3: Main bullet glow (bright cyan aura)
            batch.setColor(0.2f, 0.85f, 1f, 0.5f);
            batch.draw(glowRegion, bx - BULLET_W * 2.5f, by - BULLET_H * 1.2f,
                    BULLET_W * 5f, BULLET_H * 2.4f);

            // Layer 4: Inner glow (white-cyan hot core)
            batch.setColor(0.5f, 0.95f, 1f, 0.7f);
            batch.draw(glowRegion, bx - BULLET_W * 1.2f, by - BULLET_H * 0.7f,
                    BULLET_W * 2.4f, BULLET_H * 1.4f);

            // Layer 5: Bullet core (bright white center)
            batch.setColor(0.9f, 1f, 1f, 0.95f);
            batch.draw(pixel, bx - BULLET_W * 0.5f, by - BULLET_H * 0.5f, BULLET_W, BULLET_H);

            // Layer 6: Hot tip (brightest point at front)
            batch.setColor(1f, 1f, 1f, 1f);
            batch.draw(glowRegion, bx - 3f, by + BULLET_H * 0.3f, 6f, 8f);

            // Layer 7: Side energy wisps (animated)
            // Reduced distance between wisps (from 8f/2f to 5f/1f)
            float wispOffset = MathUtils.sin(flicker * 15f + i * 2f) * 2f;
            batch.setColor(0.3f, 0.8f, 1f, 0.3f);
            batch.draw(glowRegion, bx - 5f + wispOffset, by - 6f, 4f, 10f);
            batch.draw(glowRegion, bx + 1f - wispOffset, by - 6f, 4f, 10f);
        }

        // Explosions
        if (exCount > 0) {
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            for (int i = 0; i < exCount; i++) {
                TextureRegion frame = explosionAnim.getKeyFrame(exT[i], false);
                float w = 50f;
                float h = 50f;
                // Outer flash
                float progress = exT[i] / explosionAnim.getAnimationDuration();
                float flash = 1f - progress;
                batch.setColor(1f, 0.8f, 0.4f, flash * 0.5f);
                batch.draw(glowRegion, exX[i] - w * 0.8f, exY[i] - h * 0.8f, w * 1.6f, h * 1.6f);
                // Explosion sprite
                batch.setColor(1f, 0.7f, 0.9f, 0.85f);
                batch.draw(frame, exX[i] - w * 0.5f, exY[i] - h * 0.5f, w, h);
            }
        }

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(Color.WHITE);
    }

    /** Draws the mini-game HUD (HP, boss info, overheat). */
    private void drawHUD() {
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Title
        font.setColor(new Color(0.2f, 0.95f, 1f, 0.95f));
        font.getData().setScale(1.2f);
        font.draw(batch, "SPACE CRUISES", 24f, WORLD_H - 24f);

        // Controls
        font.setColor(new Color(0.85f, 0.95f, 1f, 0.75f));
        font.getData().setScale(0.8f);
        font.draw(batch, "A/D or ←/→: move   SPACE/LMB: fire", 24f, WORLD_H - 54f);

        // Status line depends on boss state
        if (bossActive) {
            float bossHpPct = Math.max(0f, bossHp / BOSS_HP) * 100f;
            font.setColor(new Color(1f, 0.4f, 0.3f, 0.95f));
            font.draw(batch,
                    "!! BOSS FIGHT !!   Boss HP: " + String.format("%.0f%%", bossHpPct) + "   Your HP: " + hp + "/" + MAX_HP,
                    24f,
                    WORLD_H - 76f);
        } else if (bossSpawned) {
            font.setColor(new Color(0.85f, 0.95f, 1f, 0.9f));
            font.draw(batch, "Boss defeated!   HP: " + hp + "/" + MAX_HP, 24f, WORLD_H - 76f);
        } else {
            float remain = Math.max(0f, BOSS_SPAWN_TIME - surviveTimer);
            font.setColor(new Color(0.85f, 0.95f, 1f, 0.9f));
            font.draw(batch, "Boss in: " + String.format("%.1fs", remain) + "   HP: " + hp + "/" + MAX_HP, 24f, WORLD_H - 76f);
        }

        // Overheat indicator
        if (shipOverheated) {
            font.setColor(new Color(1f, 0.3f, 0.3f, 0.95f));
            font.draw(batch, "OVERHEATED", 24f, WORLD_H - 98f);
        }

        font.getData().setScale(1f);
        batch.setColor(Color.WHITE);
    }

    /** Draws the win/lose overlay shown when the mini game ends. */
    private void drawEndOverlay() {
        if (shapes == null) return;

        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        shapes.setColor(0f, 0f, 0f, 0.6f);
        shapes.rect(0f, 0f, WORLD_W, WORLD_H);

        // neon frame
        shapes.setColor(0.15f, 0.9f, 1f, 0.35f);
        float w = 520f;
        float h = 180f;
        float x = (WORLD_W - w) * 0.5f;
        float y = (WORLD_H - h) * 0.5f;
        shapes.rect(x, y, w, h);

        shapes.setColor(0f, 0f, 0.02f, 0.85f);
        shapes.rect(x + 3f, y + 3f, w - 6f, h - 6f);

        shapes.end();

        batch.begin();
        batch.setProjectionMatrix(camera.combined);

        String msg = state == State.WIN ? "MINI GAME SUCCESS" : "MINI GAME FAILED";
        String msg2 = state == State.WIN ? "Press ENTER to claim victory" : "Press ENTER to accept defeat";

        font.setColor(state == State.WIN
                ? new Color(0.2f, 0.95f, 1f, 0.95f)
                : new Color(1f, 0.25f, 0.35f, 0.95f));
        font.getData().setScale(1.3f);
        font.draw(batch, msg, (WORLD_W - 320f) * 0.5f, WORLD_H * 0.5f + 32f);

        font.setColor(new Color(0.9f, 0.95f, 1f, 0.75f));
        font.getData().setScale(0.9f);
        font.draw(batch, msg2, (WORLD_W - 420f) * 0.5f, WORLD_H * 0.5f - 8f);

        font.getData().setScale(1f);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        camera.setToOrtho(false, WORLD_W, WORLD_H);
        camera.update();
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        disposed = true; // Mark as disposed first to prevent rendering

        if (playerShipTex != null) {
            playerShipTex.dispose();
            playerShipTex = null;
        }
        if (enemyShipTex != null) {
            enemyShipTex.dispose();
            enemyShipTex = null;
        }
        if (rocksTex != null) {
            rocksTex.dispose();
            rocksTex = null;
        }
        if (bossTex != null) {
            bossTex.dispose();
            bossTex = null;
        }

        // don't dispose shared textures from ResourcePack
        if (shapes != null) {
            shapes.dispose();
            shapes = null;
        }
        if (pixel != null) {
            pixel.dispose();
            pixel = null;
        }
        // regions are owned by this class or ResourcePack
        shipRegion = null;
        enemyRegion = null;
        explosionAnim = null;
        rocksRegion = null;
        bossRegion = null;

        if (glowTex != null) {
            glowTex.dispose();
            glowTex = null;
            glowRegion = null;
        }
        if (laserShotSound != null) {
            laserShotSound.dispose();
            laserShotSound = null;
        }
        if (explosionSound != null) {
            explosionSound.dispose();
            explosionSound = null;
        }
    }
}
