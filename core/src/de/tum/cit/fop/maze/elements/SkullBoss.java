package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import com.badlogic.gdx.utils.Disposable;

/**
 * SkullBoss - A powerful boss that enters from outside the map.
 *
 * Attacks:
 * 1. Spinning charge attack - rotates while rushing toward player
 * 2. Homing laser from mouth - tracks the player
 *
 * Behavior:
 * - Enters from outside map boundary (ignores walls)
 * - Fights player, then exits
 * - Returns after 3 minutes (180 seconds)
 *
 * Player can attack with Q key (lightning bolt) when in range.
 */
public class SkullBoss implements Disposable {

    // Boss stats
    private static final float MAX_HEALTH = 200f;
    private static final float BOSS_SIZE = 120f;  // Much bigger skull
    private static final float HITBOX_SIZE = 90f;

    // Timing
    private static final float RESPAWN_TIME = 120f; // 2 minutes after leaving
    private static final float FIGHT_DURATION = 35f; // Fight for 35 seconds then exit
    private static final float FIRST_SPAWN_TIME = 10f; // First spawn after 10 seconds
    private static final float ENTER_SPEED = 60f;   // Slow approach
    private static final float EXIT_SPEED = 100f;

    // Health recovery on respawn (after exiting, not after death)
    private static final float RESPAWN_HEALTH_PERCENT = 0.30f; // Recover 30% health

    // Orbit around player settings
    private static final float ORBIT_DISTANCE = 150f; // Distance to orbit around player
    private static final float ORBIT_SPEED_MIN = 0.5f;   // Starting orbit speed (slow)
    private static final float ORBIT_SPEED_MAX = 2.5f;   // Max orbit speed before charge

    // Attack: Spinning charge (gradual acceleration)
    private static final float CHARGE_MIN_SPEED = 60f;   // Starting charge speed (very slow)
    private static final float CHARGE_MAX_SPEED = 200f;  // Max charge speed (not too fast)
    private static final float CHARGE_ROTATION_SPEED = 120f; // Slower rotation
    private static final float CHARGE_WINDUP_TIME = 2.0f;    // Longer windup - orbiting faster
    private static final float CHARGE_ACCEL_TIME = 1.5f;     // Time to accelerate during charge
    private static final float CHARGE_DURATION = 2.0f;
    private static final float CHARGE_COOLDOWN = 4.0f;
    private static final float CHARGE_DAMAGE = 15f;  // 15% of 100 HP = 15 damage

    // Attack: Mouth laser (thin red continuous beam)
    private static final float LASER_SPEED = 220f;
    private static final float LASER_TRACKING = 0.5f; // how much it homes (0-1)
    private static final float LASER_COOLDOWN = 1.5f;
    private static final float LASER_DAMAGE = 15f;
    private static final float LASER_WIDTH = 3f;     // Very thin
    private static final float LASER_LENGTH = 25f;   // Elongated for continuous look

    // Player lightning attack
    private static final float LIGHTNING_RANGE = 200f;  // Increased range
    private static final float LIGHTNING_DAMAGE = 5f;   // 5 damage per hit
    private static final float LIGHTNING_COOLDOWN = 0.4f;

    // Score
    private static final int SCORE_KILL_BOSS = 500;

    private final Maze maze;
    private final Texture bossTexture;
    private final TextureRegion bossRegion;
    private final Texture pixel;

    // Position & movement
    private float x, y;
    private float velX, velY;
    private float rotation = 0f;
    private float health;

    // State machine
    private enum BossState {
        INACTIVE,       // Not spawned yet / waiting to respawn
        ENTERING,       // Flying into the map
        IDLE,           // In combat, deciding next action
        CHARGING,       // Spinning charge attack
        FIRING_LASER,   // Shooting mouth laser
        DYING,          // Death animation with explosions
        EXITING         // Leaving the map
    }
    private BossState state = BossState.INACTIVE;

    // Timers
    private float stateTimer = 0f;
    private float respawnTimer = FIRST_SPAWN_TIME; // First spawn after 10 seconds
    private float fightTimer = 0f;
    private float chargeCooldown = 0f;
    private float laserCooldown = 0f;
    private float lightningCooldown = 0f;

    // Charge attack
    private float chargeTargetX, chargeTargetY;
    private float chargeRotation = 0f;
    private boolean chargeHitPlayer = false;  // Prevent multiple hits per charge

    // Orbit movement
    private float orbitAngle = 0f;
    private float currentOrbitSpeed = ORBIT_SPEED_MIN;  // Current orbit speed (accelerates before charge)

    // Lasers (projectiles from mouth)
    private static final int MAX_LASERS = 8;
    private final float[] laserX = new float[MAX_LASERS];
    private final float[] laserY = new float[MAX_LASERS];
    private final float[] laserVX = new float[MAX_LASERS];
    private final float[] laserVY = new float[MAX_LASERS];
    private int laserCount = 0;

    // Lightning effect (enhanced)
    private static final int MAX_LIGHTNING_SEGMENTS = 16;  // More segments for jagged effect
    private static final int LIGHTNING_BRANCHES = 3;       // Number of branch bolts
    private final float[] lightningX = new float[MAX_LIGHTNING_SEGMENTS];
    private final float[] lightningY = new float[MAX_LIGHTNING_SEGMENTS];
    // Branch lightning arrays
    private final float[][] branchX = new float[LIGHTNING_BRANCHES][6];
    private final float[][] branchY = new float[LIGHTNING_BRANCHES][6];
    private float lightningTimer = 0f;

    // Death explosion effect (Mine-style)
    private static final float DEATH_DURATION = 2.0f;  // 2 seconds death animation
    private static final int MAX_ANIM_EXPLOSIONS = 8;  // Multiple animated explosions
    private final float[] animExplosionX = new float[MAX_ANIM_EXPLOSIONS];
    private final float[] animExplosionY = new float[MAX_ANIM_EXPLOSIONS];
    private final float[] animExplosionTime = new float[MAX_ANIM_EXPLOSIONS];  // State time for each
    private final float[] animExplosionSize = new float[MAX_ANIM_EXPLOSIONS];
    private int animExplosionCount = 0;
    private float deathTimer = 0f;
    private float nextExplosionTime = 0f;
    private boolean lightningActive = false;

    // Explosion animation (Mine-style)
    private Animation<TextureRegion> explosionAnimation;
    private final java.util.List<ExplosionDebris> debrisList = new java.util.ArrayList<>();

    // Explosion sound
    private final Sound explosionSound;
    private final Sound roarSound;
    private final Sound lightningSound;

    // Entry/exit points
    private float entryX, entryY;
    private float targetX, targetY;

    // Track spawn state for health recovery
    private boolean isFirstSpawn = true;
    private float savedHealth = MAX_HEALTH; // Health saved when exiting

    // Hit flash
    private float hitFlashTimer = 0f;

    public SkullBoss(Maze maze) {
        this.maze = maze;
        this.bossTexture = new Texture(Gdx.files.internal("skullboss.png"));
        this.bossRegion = new TextureRegion(bossTexture);
        this.health = MAX_HEALTH;

        // Create 1x1 white pixel for effects
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        this.pixel = new Texture(pm);
        pm.dispose();

        // Load explosion sound
        this.explosionSound = Gdx.audio.newSound(Gdx.files.internal("explode.ogg"));
        this.roarSound = Gdx.audio.newSound(Gdx.files.internal("The_roar_of_the_SkullBoss.wav"));
        this.lightningSound = Gdx.audio.newSound(Gdx.files.internal("The_sound_of_the_lightning_attack.wav"));

        // Load explosion animation (same as Mine)
        this.explosionAnimation = maze.getGame().getResourcePack().getExplosionAnimation();
    }

    public void update(float dt) {
        // Update lightning effect
        if (lightningActive) {
            lightningTimer -= dt;
            if (lightningTimer <= 0f) {
                lightningActive = false;
            }
        }

        // Hit flash decay
        if (hitFlashTimer > 0f) {
            hitFlashTimer -= dt;
        }

        switch (state) {
            case INACTIVE:
                updateInactive(dt);
                break;
            case ENTERING:
                updateEntering(dt);
                break;
            case IDLE:
                updateIdle(dt);
                break;
            case CHARGING:
                updateCharging(dt);
                break;
            case FIRING_LASER:
                updateFiringLaser(dt);
                break;
            case DYING:
                updateDying(dt);
                break;
            case EXITING:
                updateExiting(dt);
                break;
        }

        // Update lasers
        updateLasers(dt);

        // Cooldowns
        if (chargeCooldown > 0f) chargeCooldown -= dt;
        if (laserCooldown > 0f) laserCooldown -= dt;
        if (lightningCooldown > 0f) lightningCooldown -= dt;

        // Player lightning attack (Q key)
        if (state != BossState.INACTIVE && state != BossState.ENTERING && state != BossState.EXITING) {
            handlePlayerLightningAttack();
        }
    }

    private void updateInactive(float dt) {
        respawnTimer -= dt;
        if (respawnTimer <= 0f) {
            startEntering();
        }
    }

    private void startEntering() {
        state = BossState.ENTERING;
        stateTimer = 0f;

        // Set health: full on first spawn, recover 30% after exiting
        if (isFirstSpawn) {
            health = MAX_HEALTH;
            isFirstSpawn = false;
        } else {
            // Recover 30% of max health from saved health
            health = Math.min(savedHealth + MAX_HEALTH * RESPAWN_HEALTH_PERCENT, MAX_HEALTH);
        }

        // Choose random entry point from outside map
        Rectangle border = maze.getBorder();
        int side = MathUtils.random(0, 3);
        switch (side) {
            case 0: // Top
                entryX = MathUtils.random(border.x, border.x + border.width);
                entryY = border.y + border.height + BOSS_SIZE * 2;
                break;
            case 1: // Bottom
                entryX = MathUtils.random(border.x, border.x + border.width);
                entryY = border.y - BOSS_SIZE * 2;
                break;
            case 2: // Left
                entryX = border.x - BOSS_SIZE * 2;
                entryY = MathUtils.random(border.y, border.y + border.height);
                break;
            case 3: // Right
                entryX = border.x + border.width + BOSS_SIZE * 2;
                entryY = MathUtils.random(border.y, border.y + border.height);
                break;
        }

        x = entryX;
        y = entryY;

        // Target: player position (find the player!)
        Player player = maze.getPlayer();
        if (player != null) {
            targetX = player.getCenter().x;
            targetY = player.getCenter().y;
        } else {
            // Fallback to center of map if player not found
            targetX = border.x + border.width * 0.5f;
            targetY = border.y + border.height * 0.5f;
        }

        // Calculate velocity toward target
        float dx = targetX - x;
        float dy = targetY - y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        velX = (dx / dist) * ENTER_SPEED;
        velY = (dy / dist) * ENTER_SPEED;

        rotation = 0f;
    }

    private void updateEntering(float dt) {
        x += velX * dt;
        y += velY * dt;

        // Slow rotation while entering
        rotation += 45f * dt;

        // Keep tracking player while entering
        Player player = maze.getPlayer();
        if (player != null) {
            targetX = player.getCenter().x;
            targetY = player.getCenter().y;

            // Adjust velocity toward player
            float dx = targetX - x;
            float dy = targetY - y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist > 0.001f) {
                velX = (dx / dist) * ENTER_SPEED;
                velY = (dy / dist) * ENTER_SPEED;
            }
        }

        // Check if close enough to player to start fighting
        float dx = targetX - x;
        float dy = targetY - y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist < ORBIT_DISTANCE + 30f) {
            state = BossState.IDLE;
            stateTimer = 0f;
            fightTimer = 0f;
            velX = 0f;
            velY = 0f;
            roarSound.play();
        }
    }

    private void updateIdle(float dt) {
        stateTimer += dt;
        fightTimer += dt;

        Player player = maze.getPlayer();

        // Orbit around player with current speed
        if (player != null) {
            orbitAngle += currentOrbitSpeed * dt;

            float px = player.getCenter().x;
            float py = player.getCenter().y;

            // Calculate orbit position
            float targetX = px + MathUtils.cos(orbitAngle) * ORBIT_DISTANCE;
            float targetY = py + MathUtils.sin(orbitAngle) * ORBIT_DISTANCE;

            // Smoothly move toward orbit position
            float moveSpeed = 100f + currentOrbitSpeed * 30f; // Move faster when orbiting faster
            float dx = targetX - x;
            float dy = targetY - y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            if (dist > 5f) {
                x += (dx / dist) * moveSpeed * dt;
                y += (dy / dist) * moveSpeed * dt;
            }

            // Add slight bobbing motion
            y += MathUtils.sin(stateTimer * 3f) * 5f * dt;
        }

        // Rotation - face the player, spin faster as orbit speeds up
        if (player != null) {
            float px = player.getCenter().x;
            float py = player.getCenter().y;
            float angleToPlayer = MathUtils.atan2(py - y, px - x) * MathUtils.radiansToDegrees - 90f;
            float rotDiff = ((angleToPlayer - rotation + 180f) % 360f) - 180f;
            // Rotate faster as orbit speed increases
            rotation += rotDiff * (1f + currentOrbitSpeed) * dt;
        } else {
            rotation += 15f * dt;
        }

        // Check collision with player (light contact damage in idle)
        if (player != null) {
            Rectangle bossRect = new Rectangle(x - HITBOX_SIZE * 0.5f, y - HITBOX_SIZE * 0.5f, HITBOX_SIZE, HITBOX_SIZE);
            if (player.overlaps(bossRect)) {
                player.modifyHealth(-5f * dt); // Light contact damage
            }
        }

        // Exit after fight duration
        if (fightTimer >= FIGHT_DURATION) {
            startExiting();
            return;
        }

        // Gradually increase orbit speed over time, then trigger charge
        if (chargeCooldown <= 0f) {
            // Accelerate orbit speed
            currentOrbitSpeed += 0.3f * dt;

            // When orbit speed reaches max, trigger charge attack!
            if (currentOrbitSpeed >= ORBIT_SPEED_MAX) {
                if (player != null) {
                    startCharge(player.getCenter().x, player.getCenter().y);
                }
            }
        } else {
            // Reset orbit speed while on cooldown
            currentOrbitSpeed = MathUtils.lerp(currentOrbitSpeed, ORBIT_SPEED_MIN, 2f * dt);
        }

        // Laser attack (independent of charge)
        if (player != null && laserCooldown <= 0f && stateTimer > 2.0f && MathUtils.randomBoolean(0.015f)) {
            startLaser();
        }
    }

    private void startCharge(float targetX, float targetY) {
        state = BossState.CHARGING;
        stateTimer = 0f;
        chargeTargetX = targetX;
        chargeTargetY = targetY;
        chargeRotation = rotation;
        chargeHitPlayer = false;  // Reset hit flag

        // Start with minimum speed, will accelerate gradually
        float dx = targetX - x;
        float dy = targetY - y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist > 0.001f) {
            velX = (dx / dist) * CHARGE_MIN_SPEED;
            velY = (dy / dist) * CHARGE_MIN_SPEED;
        }

        chargeCooldown = CHARGE_COOLDOWN;
        currentOrbitSpeed = ORBIT_SPEED_MIN;  // Reset orbit speed
    }

    private void updateCharging(float dt) {
        stateTimer += dt;

        Player player = maze.getPlayer();

        // Calculate current charge speed (gradually accelerates)
        float chargeProgress = Math.min(stateTimer / CHARGE_ACCEL_TIME, 1f);
        float currentSpeed = MathUtils.lerp(CHARGE_MIN_SPEED, CHARGE_MAX_SPEED, chargeProgress);

        // Re-aim toward player during early phase (first 0.5 seconds)
        if (stateTimer < 0.5f && player != null) {
            float px = player.getCenter().x;
            float py = player.getCenter().y;
            float dx = px - x;
            float dy = py - y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist > 0.001f) {
                // Smoothly adjust direction
                float targetVelX = (dx / dist) * currentSpeed;
                float targetVelY = (dy / dist) * currentSpeed;
                velX = MathUtils.lerp(velX, targetVelX, 3f * dt);
                velY = MathUtils.lerp(velY, targetVelY, 3f * dt);
            }
        } else {
            // After 0.5s, lock direction but keep accelerating
            float len = (float) Math.sqrt(velX * velX + velY * velY);
            if (len > 0.001f) {
                velX = (velX / len) * currentSpeed;
                velY = (velY / len) * currentSpeed;
            }
        }

        // Move
        x += velX * dt;
        y += velY * dt;

        // Rotation speeds up with charge
        chargeRotation += CHARGE_ROTATION_SPEED * (0.5f + chargeProgress * 0.5f) * dt;
        rotation = chargeRotation;

        // Check collision with player (single hit per charge, 15% HP damage)
        if (!chargeHitPlayer && player != null) {
            Rectangle bossRect = new Rectangle(x - HITBOX_SIZE * 0.5f, y - HITBOX_SIZE * 0.5f, HITBOX_SIZE, HITBOX_SIZE);
            if (player.overlaps(bossRect)) {
                player.modifyHealth(-CHARGE_DAMAGE);  // 15 damage = 15% of 100 HP
                chargeHitPlayer = true;  // Only hit once per charge
            }
        }

        // End charge after duration
        if (stateTimer >= CHARGE_DURATION) {
            state = BossState.IDLE;
            stateTimer = 0f;
            velX = 0f;
            velY = 0f;

            // Teleport back to map if too far out
            Rectangle border = maze.getBorder();
            float margin = BOSS_SIZE * 4;
            if (x < border.x - margin || x > border.x + border.width + margin ||
                y < border.y - margin || y > border.y + border.height + margin) {
                x = MathUtils.clamp(x, border.x + BOSS_SIZE, border.x + border.width - BOSS_SIZE);
                y = MathUtils.clamp(y, border.y + BOSS_SIZE, border.y + border.height - BOSS_SIZE);
            }
        }
    }

    private void startLaser() {
        state = BossState.FIRING_LASER;
        stateTimer = 0f;
        laserCooldown = LASER_COOLDOWN;
    }

    private void updateFiringLaser(float dt) {
        stateTimer += dt;

        // Fire multiple lasers in quick succession for continuous beam effect
        // Fire at 0.2s, 0.3s, 0.4s, 0.5s marks
        float[] fireTimes = {0.2f, 0.3f, 0.4f, 0.5f};
        for (float fireTime : fireTimes) {
            if (stateTimer >= fireTime && stateTimer < fireTime + dt * 1.5f) {
                spawnLaser();
            }
        }

        // Return to idle after animation
        if (stateTimer >= 0.8f) {
            state = BossState.IDLE;
            stateTimer = 0f;
        }
    }

    private void spawnLaser() {
        if (laserCount >= MAX_LASERS) return;

        Player player = maze.getPlayer();
        if (player == null) return;

        // Mouth position (near bottom of boss sprite)
        float mouthX = x;
        float mouthY = y - BOSS_SIZE * 0.35f;

        // Direction toward player
        float px = player.getCenter().x;
        float py = player.getCenter().y;
        float dx = px - mouthX;
        float dy = py - mouthY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist > 0.001f) {
            laserX[laserCount] = mouthX;
            laserY[laserCount] = mouthY;
            laserVX[laserCount] = (dx / dist) * LASER_SPEED;
            laserVY[laserCount] = (dy / dist) * LASER_SPEED;
            laserCount++;
        }
    }

    private void updateLasers(float dt) {
        Player player = maze.getPlayer();
        Rectangle border = maze.getBorder();

        for (int i = 0; i < laserCount; ) {
            // Homing: adjust velocity toward player
            if (player != null) {
                float px = player.getCenter().x;
                float py = player.getCenter().y;
                float dx = px - laserX[i];
                float dy = py - laserY[i];
                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                if (dist > 0.001f) {
                    float targetVX = (dx / dist) * LASER_SPEED;
                    float targetVY = (dy / dist) * LASER_SPEED;
                    laserVX[i] = MathUtils.lerp(laserVX[i], targetVX, LASER_TRACKING * dt * 3f);
                    laserVY[i] = MathUtils.lerp(laserVY[i], targetVY, LASER_TRACKING * dt * 3f);

                    // Normalize speed
                    float speed = (float) Math.sqrt(laserVX[i] * laserVX[i] + laserVY[i] * laserVY[i]);
                    if (speed > 0.001f) {
                        laserVX[i] = (laserVX[i] / speed) * LASER_SPEED;
                        laserVY[i] = (laserVY[i] / speed) * LASER_SPEED;
                    }
                }
            }

            // Move
            laserX[i] += laserVX[i] * dt;
            laserY[i] += laserVY[i] * dt;

            // Check collision with player
            boolean hit = false;
            if (player != null) {
                Rectangle laserRect = new Rectangle(laserX[i] - LASER_LENGTH * 0.5f, laserY[i] - LASER_WIDTH * 0.5f, LASER_LENGTH, LASER_WIDTH);
                if (player.overlaps(laserRect)) {
                    player.modifyHealth(-LASER_DAMAGE);
                    hit = true;
                }
            }

            // Check out of bounds
            boolean outOfBounds = laserX[i] < border.x - 50f || laserX[i] > border.x + border.width + 50f ||
                                  laserY[i] < border.y - 50f || laserY[i] > border.y + border.height + 50f;

            if (hit || outOfBounds) {
                // Remove laser (swap with last)
                int last = laserCount - 1;
                laserX[i] = laserX[last];
                laserY[i] = laserY[last];
                laserVX[i] = laserVX[last];
                laserVY[i] = laserVY[last];
                laserCount--;
                continue;
            }

            i++;
        }
    }

    private void startExiting() {
        state = BossState.EXITING;
        stateTimer = 0f;

        // Choose exit point (opposite of current position relative to center)
        Rectangle border = maze.getBorder();
        float cx = border.x + border.width * 0.5f;
        float cy = border.y + border.height * 0.5f;

        float dx = x - cx;
        float dy = y - cy;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist > 0.001f) {
            targetX = x + (dx / dist) * 500f;
            targetY = y + (dy / dist) * 500f;
            velX = (dx / dist) * EXIT_SPEED;
            velY = (dy / dist) * EXIT_SPEED;
        } else {
            // Default: exit to right
            targetX = border.x + border.width + 200f;
            targetY = y;
            velX = EXIT_SPEED;
            velY = 0f;
        }
    }

    private void updateExiting(float dt) {
        x += velX * dt;
        y += velY * dt;
        rotation += 30f * dt;

        // Check if far enough from map
        Rectangle border = maze.getBorder();
        boolean farEnough = x < border.x - BOSS_SIZE * 3 || x > border.x + border.width + BOSS_SIZE * 3 ||
                            y < border.y - BOSS_SIZE * 3 || y > border.y + border.height + BOSS_SIZE * 3;

        if (farEnough) {
            // Save current health before going inactive
            savedHealth = health;
            state = BossState.INACTIVE;
            respawnTimer = RESPAWN_TIME;
            laserCount = 0;
        }
    }

    private void handlePlayerLightningAttack() {
        if (lightningCooldown > 0f) return;
        if (!Gdx.input.isKeyJustPressed(Input.Keys.Q)) return;

        Player player = maze.getPlayer();
        if (player == null) return;

        float px = player.getCenter().x;
        float py = player.getCenter().y;
        float dist = Vector2.dst(px, py, x, y);

        if (dist <= LIGHTNING_RANGE) {
            // Deal damage
            modifyHealth(-LIGHTNING_DAMAGE);
            lightningCooldown = LIGHTNING_COOLDOWN;

            // Create lightning effect
            createLightningEffect(px, py, x, y);
            lightningSound.play();
        }
    }

    private void createLightningEffect(float startX, float startY, float endX, float endY) {
        lightningActive = true;
        lightningTimer = 0.4f;  // Longer duration

        // Generate jagged lightning path (main bolt)
        float dx = endX - startX;
        float dy = endY - startY;
        float totalDist = (float) Math.sqrt(dx * dx + dy * dy);

        for (int i = 0; i < MAX_LIGHTNING_SEGMENTS; i++) {
            float t = i / (float) (MAX_LIGHTNING_SEGMENTS - 1);
            lightningX[i] = startX + dx * t;
            lightningY[i] = startY + dy * t;

            // Add randomness (except for endpoints) - more jagged!
            if (i > 0 && i < MAX_LIGHTNING_SEGMENTS - 1) {
                float perpX = -dy;
                float perpY = dx;
                float len = (float) Math.sqrt(perpX * perpX + perpY * perpY);
                if (len > 0.001f) {
                    perpX /= len;
                    perpY /= len;
                }
                // More dramatic zigzag
                float offset = MathUtils.random(-25f, 25f);
                lightningX[i] += perpX * offset;
                lightningY[i] += perpY * offset;
            }
        }

        // Generate branch bolts (smaller lightning forks)
        for (int b = 0; b < LIGHTNING_BRANCHES; b++) {
            // Pick a random point along main bolt to branch from
            int branchStart = MathUtils.random(2, MAX_LIGHTNING_SEGMENTS - 4);
            float bx = lightningX[branchStart];
            float by = lightningY[branchStart];

            // Random branch direction
            float branchAngle = MathUtils.random(0f, MathUtils.PI2);
            float branchLen = totalDist * MathUtils.random(0.2f, 0.4f);

            for (int i = 0; i < 6; i++) {
                float t = i / 5f;
                branchX[b][i] = bx + MathUtils.cos(branchAngle) * branchLen * t;
                branchY[b][i] = by + MathUtils.sin(branchAngle) * branchLen * t;

                // Add zigzag to branches too
                if (i > 0 && i < 5) {
                    branchX[b][i] += MathUtils.random(-10f, 10f);
                    branchY[b][i] += MathUtils.random(-10f, 10f);
                }
            }
        }
    }

    public void modifyHealth(float delta) {
        if (state == BossState.DYING || state == BossState.INACTIVE) return;  // Can't damage during death

        health += delta;
        if (delta < 0) {
            hitFlashTimer = 0.1f;
        }

        if (health <= 0f) {
            onDeath();
        }
    }

    private void onDeath() {
        // Start death animation
        state = BossState.DYING;
        deathTimer = 0f;
        nextExplosionTime = 0f;
        animExplosionCount = 0;
        laserCount = 0;  // Clear lasers
        debrisList.clear();

        // Trigger screen shake
        if (maze.getCamera() != null) {
            maze.getCamera().shake(2.0f, 20f);
        }

        // Award score immediately
        maze.getGame().addBonusScore(SCORE_KILL_BOSS);
    }

    private void updateDying(float dt) {
        deathTimer += dt;

        // Update animated explosion timers
        for (int i = 0; i < animExplosionCount; i++) {
            animExplosionTime[i] += dt;
        }

        // Update debris
        java.util.Iterator<ExplosionDebris> iter = debrisList.iterator();
        while (iter.hasNext()) {
            ExplosionDebris debris = iter.next();
            debris.update(dt);
            if (debris.isFinished()) {
                iter.remove();
            }
        }

        // Spawn new explosions at intervals
        if (deathTimer >= nextExplosionTime && animExplosionCount < MAX_ANIM_EXPLOSIONS) {
            spawnDeathExplosion();
            // Next explosion in 0.15 to 0.3 seconds
            nextExplosionTime = deathTimer + MathUtils.random(0.15f, 0.3f);
        }

        // Shake and spin the boss during death
        rotation += 200f * dt;
        x += MathUtils.random(-3f, 3f);
        y += MathUtils.random(-3f, 3f);

        // After death duration and all effects done, go inactive
        if (deathTimer >= DEATH_DURATION && debrisList.isEmpty()) {
            state = BossState.INACTIVE;
            respawnTimer = RESPAWN_TIME;
            health = MAX_HEALTH;
            animExplosionCount = 0;
        }
    }

    private void spawnDeathExplosion() {
        if (animExplosionCount >= MAX_ANIM_EXPLOSIONS) return;

        // Random position around the boss
        float offsetX = MathUtils.random(-BOSS_SIZE * 0.5f, BOSS_SIZE * 0.5f);
        float offsetY = MathUtils.random(-BOSS_SIZE * 0.5f, BOSS_SIZE * 0.5f);

        animExplosionX[animExplosionCount] = x + offsetX;
        animExplosionY[animExplosionCount] = y + offsetY;
        animExplosionTime[animExplosionCount] = 0f;
        animExplosionSize[animExplosionCount] = MathUtils.random(64f, 96f);  // Bigger explosions
        animExplosionCount++;

        // Spawn debris for this explosion (like Mine)
        float debrisX = x + offsetX;
        float debrisY = y + offsetY;
        for (int i = 0; i < 15; i++) {
            debrisList.add(new ExplosionDebris(debrisX, debrisY));
        }

        // Play explosion sound with varying volume and pitch for variety
        float volume = MathUtils.random(0.3f, 0.6f);
        float pitch = MathUtils.random(0.8f, 1.2f);
        explosionSound.play(volume, pitch, 0f);
    }

    public void render(SpriteBatch batch) {
        if (state == BossState.INACTIVE) return;

        // Render lasers (not during death)
        if (state != BossState.DYING) {
            renderLasers(batch);
        }

        // Render boss (with fade during death)
        renderBoss(batch);

        // Render death explosions
        if (state == BossState.DYING) {
            renderDeathExplosions(batch);
        }

        // Render lightning
        if (lightningActive) {
            renderLightning(batch);
        }

        // Render HP bar
        renderHealthBar(batch);
    }

    private void renderBoss(SpriteBatch batch) {
        float drawW = BOSS_SIZE;
        float drawH = BOSS_SIZE * bossRegion.getRegionHeight() / (float) bossRegion.getRegionWidth();

        // Calculate alpha (fade out during death)
        float alpha = 1f;
        if (state == BossState.DYING) {
            alpha = 1f - (deathTimer / DEATH_DURATION);
            alpha = Math.max(0f, alpha);
        }

        // Hit flash or dying flash
        if (state == BossState.DYING) {
            // Flashing red/white during death
            float flash = MathUtils.sin(deathTimer * 30f) * 0.5f + 0.5f;
            batch.setColor(1f, flash, flash, alpha);
        } else if (hitFlashTimer > 0f) {
            batch.setColor(1f, 0.3f, 0.3f, alpha);
        } else {
            batch.setColor(1f, 1f, 1f, alpha);
        }

        // Draw with rotation
        batch.draw(
            bossRegion,
            x - drawW * 0.5f,
            y - drawH * 0.5f,
            drawW * 0.5f,
            drawH * 0.5f,
            drawW,
            drawH,
            1f,
            1f,
            rotation
        );

        batch.setColor(Color.WHITE);
    }

    private void renderLasers(SpriteBatch batch) {
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

        for (int i = 0; i < laserCount; i++) {
            float lx = laserX[i];
            float ly = laserY[i];

            // Direction for rotation
            float ang = MathUtils.atan2(laserVY[i], laserVX[i]) * MathUtils.radiansToDegrees;

            // Outer red glow (thin)
            batch.setColor(1f, 0f, 0f, 0.3f);
            drawRotRect(batch, lx, ly, LASER_LENGTH, LASER_WIDTH * 3f, ang);

            // Middle red beam
            batch.setColor(1f, 0.1f, 0.1f, 0.7f);
            drawRotRect(batch, lx, ly, LASER_LENGTH, LASER_WIDTH * 1.5f, ang);

            // Hot white-red core (very thin)
            batch.setColor(1f, 0.6f, 0.6f, 1f);
            drawRotRect(batch, lx, ly, LASER_LENGTH, LASER_WIDTH, ang);
        }

        batch.setColor(Color.WHITE);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void renderDeathExplosions(SpriteBatch batch) {
        // Render animated explosions (Mine-style)
        for (int i = 0; i < animExplosionCount; i++) {
            float stateTime = animExplosionTime[i];
            float size = animExplosionSize[i];
            float ex = animExplosionX[i];
            float ey = animExplosionY[i];

            // Draw explosion animation if not finished
            if (!explosionAnimation.isAnimationFinished(stateTime)) {
                TextureRegion explosionFrame = explosionAnimation.getKeyFrame(stateTime, false);
                batch.draw(
                    explosionFrame,
                    ex - size / 2f,
                    ey - size / 2f,
                    size,
                    size
                );
            }
        }

        // Render debris (like Mine)
        for (ExplosionDebris debris : debrisList) {
            debris.render(batch);
        }
    }

    private void renderLightning(SpriteBatch batch) {
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

        // Dramatic flickering - faster for more electric feel
        float flicker = 0.7f + 0.3f * MathUtils.sin(lightningTimer * 150f);
        float pulse = 0.85f + 0.15f * MathUtils.sin(lightningTimer * 250f);

        // === MAIN BOLT (thinner, more elegant) ===
        for (int i = 0; i < MAX_LIGHTNING_SEGMENTS - 1; i++) {
            float x1 = lightningX[i];
            float y1 = lightningY[i];
            float x2 = lightningX[i + 1];
            float y2 = lightningY[i + 1];

            float dx = x2 - x1;
            float dy = y2 - y1;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            float ang = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;

            float cx = (x1 + x2) * 0.5f;
            float cy = (y1 + y2) * 0.5f;

            // Soft outer electric glow (very subtle purple)
            batch.setColor(0.3f, 0.2f, 0.9f, 0.12f * flicker);
            drawRotRect(batch, cx, cy, len + 2f, 10f, ang);

            // Outer glow (soft cyan)
            batch.setColor(0.2f, 0.5f, 1f, 0.25f * flicker);
            drawRotRect(batch, cx, cy, len + 1f, 6f, ang);

            // Main bolt (bright cyan, thinner)
            batch.setColor(0.4f, 0.85f, 1f, 0.9f * pulse);
            drawRotRect(batch, cx, cy, len, 2.5f, ang);

            // Hot core (white, very thin)
            batch.setColor(1f, 1f, 1f, 1f * flicker);
            drawRotRect(batch, cx, cy, len, 1.2f, ang);
        }

        // === BRANCH BOLTS (even thinner) ===
        for (int b = 0; b < LIGHTNING_BRANCHES; b++) {
            for (int i = 0; i < 5; i++) {
                float x1 = branchX[b][i];
                float y1 = branchY[b][i];
                float x2 = branchX[b][i + 1];
                float y2 = branchY[b][i + 1];

                float dx = x2 - x1;
                float dy = y2 - y1;
                float len = (float) Math.sqrt(dx * dx + dy * dy);
                float ang = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;

                float cx = (x1 + x2) * 0.5f;
                float cy = (y1 + y2) * 0.5f;

                // Branch glow (subtle)
                batch.setColor(0.3f, 0.6f, 1f, 0.2f * flicker);
                drawRotRect(batch, cx, cy, len, 4f, ang);

                // Branch core
                batch.setColor(0.5f, 0.85f, 1f, 0.6f * pulse);
                drawRotRect(batch, cx, cy, len, 1.8f, ang);

                // Branch hot center
                batch.setColor(1f, 1f, 1f, 0.85f * flicker);
                drawRotRect(batch, cx, cy, len, 0.8f, ang);
            }
        }

        // === SPARK PARTICLES at impact point (smaller, more elegant) ===
        float impactX = lightningX[MAX_LIGHTNING_SEGMENTS - 1];
        float impactY = lightningY[MAX_LIGHTNING_SEGMENTS - 1];

        for (int s = 0; s < 12; s++) {
            float sparkAngle = (s / 12f) * MathUtils.PI2 + lightningTimer * 25f;
            float sparkDist = 8f + MathUtils.sin(lightningTimer * 80f + s * 0.5f) * 6f;
            float sx = impactX + MathUtils.cos(sparkAngle) * sparkDist * flicker;
            float sy = impactY + MathUtils.sin(sparkAngle) * sparkDist * flicker;

            // Small cyan glow
            batch.setColor(0.4f, 0.7f, 1f, 0.5f * flicker);
            batch.draw(pixel, sx - 2f, sy - 2f, 4f, 4f);

            // Tiny white core
            batch.setColor(1f, 1f, 1f, 0.9f * flicker);
            batch.draw(pixel, sx - 1f, sy - 1f, 2f, 2f);
        }

        // === Small glow at start point (player) ===
        float startX = lightningX[0];
        float startY = lightningY[0];
        batch.setColor(0.4f, 0.7f, 1f, 0.4f * flicker);
        batch.draw(pixel, startX - 4f, startY - 4f, 8f, 8f);
        batch.setColor(1f, 1f, 1f, 0.7f * flicker);
        batch.draw(pixel, startX - 2f, startY - 2f, 4f, 4f);

        batch.setColor(Color.WHITE);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void renderHealthBar(SpriteBatch batch) {
        if (state == BossState.INACTIVE || state == BossState.EXITING) return;

        float hpRatio = Math.max(0f, health / MAX_HEALTH);
        float barW = BOSS_SIZE * 1.2f;
        float barH = 6f;
        float barX = x - barW * 0.5f;
        float barY = y + BOSS_SIZE * 0.6f;

        // Background
        batch.setColor(0.1f, 0.1f, 0.1f, 0.8f);
        batch.draw(pixel, barX - 1f, barY - 1f, barW + 2f, barH + 2f);

        // HP fill
        batch.setColor(0.8f, 0.1f, 0.1f, 0.9f);
        batch.draw(pixel, barX, barY, barW * hpRatio, barH);

        batch.setColor(Color.WHITE);
    }

    private void drawRotRect(SpriteBatch batch, float cx, float cy, float w, float h, float rotationDeg) {
        batch.draw(
            pixel,
            cx - w * 0.5f,
            cy - h * 0.5f,
            w * 0.5f,
            h * 0.5f,
            w,
            h,
            1f,
            1f,
            rotationDeg,
            0, 0, 1, 1,
            false, false
        );
    }

    public boolean isActive() {
        return state != BossState.INACTIVE;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getHealth() { return health; }

    @Override
    public void dispose() {
        if (bossTexture != null) {
            bossTexture.dispose();
        }
        if (pixel != null) {
            pixel.dispose();
        }
        if (explosionSound != null) {
            explosionSound.dispose();
        }
        if (roarSound != null) {
            roarSound.dispose();
        }
        if (lightningSound != null) {
            lightningSound.dispose();
        }
    }
}

