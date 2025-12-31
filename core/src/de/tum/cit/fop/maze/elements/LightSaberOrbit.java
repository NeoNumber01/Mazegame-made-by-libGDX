package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.Gdx;

/**
 * A timed rotating "light-saber" effect around the player.
 *
 * Contract:
 * - Triggered externally (e.g. by pressing R).
 * - Active for {@link #ACTIVE_DURATION} seconds.
 * - Then goes on cooldown for {@link #COOLDOWN_DURATION} seconds.
 * - While active, a thin glowing blade rotates around the player and instantly kills mobs hit.
 */
public class LightSaberOrbit {

    public static final float ACTIVE_DURATION = 5f;
    public static final float COOLDOWN_DURATION = 5f;

    // Visual / hit tuning
    private static final float ORBIT_RADIUS = 30f;
    private static final float BLADE_LENGTH = 12f;
    private static final float BLADE_THICKNESS = 1f;
    private static final float ROT_SPEED_DEG = 280f;

    // Afterimage trail
    private static final int TRAIL_SEGMENTS = 15;

    // Subtle particles (small sparks) trailing behind the blade
    private static final int MAX_PARTICLES = 180;
    private static final float PARTICLE_SPAWN_RATE = 420f; // particles per second
    private static final float PARTICLE_MIN_LIFE = 0.18f;
    private static final float PARTICLE_MAX_LIFE = 0.38f;

    // Damage tuning
    private static final float HIT_COOLDOWN = 0.08f; // seconds per mob hit gating
    private static final float DAMAGE_PER_HIT_BLADE = 99999f; // blade = instant kill
    private static final float DAMAGE_PER_HIT_TRAIL = 4f; // tail particles also hurt

    private final Maze maze;
    private final Texture pixel;
    private final Sound orbitSound;
    private long soundId = -1;

    private boolean active = false;
    private float activeTimer = 0f;
    private float cooldownTimer = 0f;

    private float angleDeg;
    private float dir = 1f;

    // Reuse objects/arrays (avoid GC)
    private final Rectangle bladeRect = new Rectangle();
    private final Vector2 tmpCenter = new Vector2();

    // Trail history (store previous orbit angles)
    private final float[] trailAnglesDeg = new float[TRAIL_SEGMENTS];
    private int trailIndex = 0;

    // Particle arrays
    private final float[] pX = new float[MAX_PARTICLES];
    private final float[] pY = new float[MAX_PARTICLES];
    private final float[] pPX = new float[MAX_PARTICLES]; // previous X (for short streaks)
    private final float[] pPY = new float[MAX_PARTICLES]; // previous Y
    private final float[] pVX = new float[MAX_PARTICLES];
    private final float[] pVY = new float[MAX_PARTICLES];
    private final float[] pLife = new float[MAX_PARTICLES];
    private final float[] pMaxLife = new float[MAX_PARTICLES];
    private final float[] pSize = new float[MAX_PARTICLES];
    private int pCount = 0;
    private float particleSpawnAcc = 0f;

    // Hit cooldown tracking (kept small and allocation-free)
    private final int[] hitEntityId = new int[64];
    private final float[] hitEntityTime = new float[64];
    private int hitEntityCount = 0;

    // Boss hit cooldown (separate from mob cooldown)
    private static final float BOSS_HIT_COOLDOWN = 0.25f; // 0.25 seconds between hits
    private float bossHitCooldown = 0f;

    // Particle damage AABB (union of particle blobs + streaks)
    private final Rectangle particleRect = new Rectangle();
    private boolean particleRectValid = false;

    private final Color cOuter = new Color(0.2f, 0.9f, 1f, 0.25f);
    private final Color cInner = new Color(0.85f, 1f, 1f, 0.85f);

    public LightSaberOrbit(Maze maze, Texture pixel) {
        this.maze = maze;
        this.pixel = pixel;
        this.orbitSound = Gdx.audio.newSound(Gdx.files.internal("The_sound_of_lightsaberorbit.wav"));
        this.angleDeg = MathUtils.random(0f, 360f);
        for (int i = 0; i < TRAIL_SEGMENTS; i++) {
            trailAnglesDeg[i] = angleDeg;
        }
    }

    public boolean canActivate() {
        return !active && cooldownTimer <= 0f;
    }

    public void tryActivate() {
        if (active) return; // already active
        if (cooldownTimer > 0f) return; // on cooldown

        active = true;
        activeTimer = ACTIVE_DURATION;
        cooldownTimer = COOLDOWN_DURATION;
        dir = (MathUtils.randomBoolean() ? 1f : -1f);
        angleDeg = MathUtils.random(0f, 360f);
        for (int i = 0; i < TRAIL_SEGMENTS; i++) {
            trailAnglesDeg[i] = angleDeg;
        }
        trailIndex = 0;

        // reset particles
        pCount = 0;
        particleSpawnAcc = 0f;
        hitEntityCount = 0;

        // Stop any lingering sound first, then play new loop
        if (soundId != -1) {
            orbitSound.stop(soundId);
            soundId = -1;
        }
        soundId = orbitSound.loop(0.6f); // Play at 60% volume
    }

    public void update(float dt, Vector2 center) {
        // Cooldown tick
        if (!active && cooldownTimer > 0f) {
            cooldownTimer -= dt;
        }

        // Always stop sound if not active
        if (!active && soundId != -1) {
            orbitSound.stop(soundId);
            soundId = -1;
        }

        if (!active) {
            // Continue updating particles so they fade out properly
            if (pCount > 0) {
                updateParticles(dt);
            }
            return;
        }

        activeTimer -= dt;
        if (activeTimer <= 0f) {
            active = false;
            // Stop sound immediately when deactivating
            if (soundId != -1) {
                orbitSound.stop(soundId);
                soundId = -1;
            }
            // Update remaining particles so they fade out, then return
            if (pCount > 0) {
                updateParticles(dt);
            }
            return;
        }

        angleDeg += dir * ROT_SPEED_DEG * dt;
        angleDeg = (angleDeg % 360f + 360f) % 360f;

        // push most recent angle into ring buffer
        trailIndex = (trailIndex + 1) % TRAIL_SEGMENTS;
        trailAnglesDeg[trailIndex] = angleDeg;

        // Emit subtle particles from the "tail" of the blade (opposite tangent direction)
        emitTailParticles(dt, center);
        updateParticles(dt);

        computeBladeAabb(center);
        computeParticleDamageAabb();
        tickHitCooldowns(dt);

        Array<Entity> entities = maze.getEntities();
        for (int i = 0; i < entities.size; i++) {
            Entity e = entities.get(i);
            if (!(e instanceof Mob mob)) continue;

            boolean hitByBlade = mob.overlaps(bladeRect);
            boolean hitByTrail = particleRectValid && mob.overlaps(particleRect);

            if ((hitByBlade || hitByTrail) && canHitNow(mob)) {
                float dmg = hitByBlade ? -DAMAGE_PER_HIT_BLADE : -DAMAGE_PER_HIT_TRAIL;
                mob.modifyHealth(dmg);
                markHit(mob);

                // small hit puff on trail hits too
                if (hitByTrail && !hitByBlade) {
                    emitImpactParticles(mob.getCenter(), 4);
                }
            }
        }

        // Also check SkullBoss (with cooldown to prevent instant kill)
        if (bossHitCooldown > 0f) {
            bossHitCooldown -= dt;
        }

        SkullBoss boss = maze.getSkullBoss();
        if (boss != null && boss.isActive() && bossHitCooldown <= 0f) {
            Rectangle bossRect = new Rectangle(
                boss.getX() - 45f, boss.getY() - 45f, 90f, 90f
            );
            boolean hitByBlade = bladeRect.overlaps(bossRect);
            boolean hitByTrail = particleRectValid && particleRect.overlaps(bossRect);

            if (hitByBlade || hitByTrail) {
                // 5 damage per 0.25s contact tick
                boss.modifyHealth(-5f);
                bossHitCooldown = BOSS_HIT_COOLDOWN; // Start cooldown
            }
        }
    }

    private void tickHitCooldowns(float dt) {
        for (int i = 0; i < hitEntityCount; ) {
            hitEntityTime[i] -= dt;
            if (hitEntityTime[i] <= 0f) {
                int last = hitEntityCount - 1;
                hitEntityId[i] = hitEntityId[last];
                hitEntityTime[i] = hitEntityTime[last];
                hitEntityCount--;
                continue;
            }
            i++;
        }
    }

    private boolean canHitNow(Entity e) {
        int id = System.identityHashCode(e);
        for (int i = 0; i < hitEntityCount; i++) {
            if (hitEntityId[i] == id) return false;
        }
        return true;
    }

    private void markHit(Entity e) {
        int id = System.identityHashCode(e);
        // refresh existing
        for (int i = 0; i < hitEntityCount; i++) {
            if (hitEntityId[i] == id) {
                hitEntityTime[i] = HIT_COOLDOWN;
                return;
            }
        }
        if (hitEntityCount >= hitEntityId.length) return;
        hitEntityId[hitEntityCount] = id;
        hitEntityTime[hitEntityCount] = HIT_COOLDOWN;
        hitEntityCount++;
    }

    private void computeParticleDamageAabb() {
        particleRectValid = false;
        if (pCount <= 0) return;

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < pCount; i++) {
            float s = pSize[i];
            // include blob area
            float x0 = pX[i] - s;
            float y0 = pY[i] - s;
            float x1 = pX[i] + s;
            float y1 = pY[i] + s;

            // include streak segment area (previous -> current)
            float sx0 = Math.min(pX[i], pPX[i]) - s;
            float sy0 = Math.min(pY[i], pPY[i]) - s;
            float sx1 = Math.max(pX[i], pPX[i]) + s;
            float sy1 = Math.max(pY[i], pPY[i]) + s;

            minX = Math.min(minX, Math.min(x0, sx0));
            minY = Math.min(minY, Math.min(y0, sy0));
            maxX = Math.max(maxX, Math.max(x1, sx1));
            maxY = Math.max(maxY, Math.max(y1, sy1));
        }

        particleRect.set(minX, minY, maxX - minX, maxY - minY);
        particleRectValid = true;
    }

    private void computeBladeAabb(Vector2 playerCenter) {
        tmpCenter.set(playerCenter);
        float orbitRad = angleDeg * MathUtils.degreesToRadians;

        float bx = tmpCenter.x + MathUtils.cos(orbitRad) * ORBIT_RADIUS;
        float by = tmpCenter.y + MathUtils.sin(orbitRad) * ORBIT_RADIUS;

        float bladeDeg = angleDeg + (dir > 0f ? 90f : -90f);
        float bladeRad = bladeDeg * MathUtils.degreesToRadians;

        float halfL = BLADE_LENGTH * 0.5f;
        float halfT = BLADE_THICKNESS * 0.5f;

        float cos = Math.abs(MathUtils.cos(bladeRad));
        float sin = Math.abs(MathUtils.sin(bladeRad));

        float ex = cos * halfL + sin * halfT;
        float ey = sin * halfL + cos * halfT;

        bladeRect.set(bx - ex, by - ey, ex * 2f, ey * 2f);
    }

    private void emitTailParticles(float dt, Vector2 playerCenter) {
        particleSpawnAcc += PARTICLE_SPAWN_RATE * dt;
        int toSpawn = (int) particleSpawnAcc;
        if (toSpawn <= 0) return;
        particleSpawnAcc -= toSpawn;

        float orbitRad = angleDeg * MathUtils.degreesToRadians;
        float bx = playerCenter.x + MathUtils.cos(orbitRad) * ORBIT_RADIUS;
        float by = playerCenter.y + MathUtils.sin(orbitRad) * ORBIT_RADIUS;

        float bladeDeg = angleDeg + (dir > 0f ? 90f : -90f);
        float bladeRad = bladeDeg * MathUtils.degreesToRadians;

        // tail is opposite of blade direction
        float tailX = bx - MathUtils.cos(bladeRad) * (BLADE_LENGTH * 0.5f);
        float tailY = by - MathUtils.sin(bladeRad) * (BLADE_LENGTH * 0.5f);

        for (int i = 0; i < toSpawn; i++) {
            if (pCount >= MAX_PARTICLES) break;

            // Wider cone behind the blade for a more visible trail
            float a = bladeRad + MathUtils.PI + MathUtils.random(-1.05f, 1.05f);
            float speed = MathUtils.random(45f, 150f);

            pX[pCount] = tailX + MathUtils.random(-2.5f, 2.5f);
            pY[pCount] = tailY + MathUtils.random(-2.5f, 2.5f);
            pPX[pCount] = pX[pCount];
            pPY[pCount] = pY[pCount];
            pVX[pCount] = MathUtils.cos(a) * speed;
            pVY[pCount] = MathUtils.sin(a) * speed;

            float life = MathUtils.random(PARTICLE_MIN_LIFE, PARTICLE_MAX_LIFE);
            pLife[pCount] = life;
            pMaxLife[pCount] = life;

            // Bigger particles so the trail is more visible
            pSize[pCount] = MathUtils.random(2.2f, 5.4f);
            pCount++;
        }
    }

    private void emitImpactParticles(Vector2 center, int count) {
        for (int i = 0; i < count; i++) {
            if (pCount >= MAX_PARTICLES) break;

            float a = MathUtils.random(0f, MathUtils.PI2);
            float speed = MathUtils.random(80f, 220f);

            pX[pCount] = center.x + MathUtils.random(-4f, 4f);
            pY[pCount] = center.y + MathUtils.random(-4f, 4f);
            pPX[pCount] = pX[pCount];
            pPY[pCount] = pY[pCount];
            pVX[pCount] = MathUtils.cos(a) * speed;
            pVY[pCount] = MathUtils.sin(a) * speed;

            float life = MathUtils.random(0.16f, 0.28f);
            pLife[pCount] = life;
            pMaxLife[pCount] = life;
            pSize[pCount] = MathUtils.random(3.0f, 6.5f);
            pCount++;
        }
    }

    private void updateParticles(float dt) {
        for (int i = 0; i < pCount; ) {
            pLife[i] -= dt;
            if (pLife[i] <= 0f) {
                int last = pCount - 1;
                pX[i] = pX[last];
                pY[i] = pY[last];
                pPX[i] = pPX[last];
                pPY[i] = pPY[last]; // FIX: copy previous Y, not current Y
                pVX[i] = pVX[last];
                pVY[i] = pVY[last];
                pLife[i] = pLife[last];
                pMaxLife[i] = pMaxLife[last];
                pSize[i] = pSize[last];
                pCount--;
                continue;
            }

            // store previous position for streak rendering
            pPX[i] = pX[i];
            pPY[i] = pY[i];

            pX[i] += pVX[i] * dt;
            pY[i] += pVY[i] * dt;
            pVX[i] *= 0.90f;
            pVY[i] *= 0.90f;
            i++;
        }
    }

    public void render(SpriteBatch batch, Vector2 playerCenter) {
        if (!active && pCount <= 0) return;

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

        // Particles first (still behind blade): now more visible with dual-layer glow
        if (pCount > 0) {
            for (int i = 0; i < pCount; i++) {
                float a = pLife[i] / pMaxLife[i];
                float s = pSize[i] * (0.85f + 0.35f * a);

                // ---- short streak line (previous -> current) ----
                float dx = pX[i] - pPX[i];
                float dy = pY[i] - pPY[i];
                float len = (float) Math.sqrt(dx * dx + dy * dy);
                if (len > 0.001f) {
                    float ang = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
                    // clamp streak length so it stays "short" even at high speed/framerate hiccups
                    float streakLen = Math.min(len * 3.0f, 14f);

                    // cyan halo streak
                    batch.setColor(0.2f, 0.8f, 1f, a * 0.14f);
                    drawRotRect(batch, pX[i], pY[i], streakLen, Math.max(1.2f, s * 0.22f), ang);

                    // warm core streak
                    batch.setColor(1f, 0.85f, 0.25f, a * 0.22f);
                    drawRotRect(batch, pX[i], pY[i], streakLen, Math.max(0.9f, s * 0.14f), ang);
                }

                // Outer cyan halo blob
                batch.setColor(0.2f, 0.8f, 1f, a * 0.22f);
                batch.draw(pixel, pX[i] - s * 0.75f, pY[i] - s * 0.75f, s * 1.5f, s * 1.5f);

                // Inner warm core blob
                batch.setColor(1f, 0.85f, 0.25f, a * 0.45f);
                batch.draw(pixel, pX[i] - s * 0.35f, pY[i] - s * 0.35f, s * 0.7f, s * 0.7f);
            }
        }

        if (active) {
            // ---- TRAIL (afterimages) ----
            // Newest is at trailIndex; older segments go backwards through ring buffer.
            for (int s = 0; s < TRAIL_SEGMENTS; s++) {
                int idx = (trailIndex - s);
                if (idx < 0) idx += TRAIL_SEGMENTS;

                // alpha decays with age
                float t = s / (float) (TRAIL_SEGMENTS - 1);
                float aOuter = MathUtils.lerp(0.20f, 0.02f, t);
                float aInner = MathUtils.lerp(0.55f, 0.05f, t);

                float orbitDeg = trailAnglesDeg[idx];
                float orbitRad = orbitDeg * MathUtils.degreesToRadians;

                float bx = playerCenter.x + MathUtils.cos(orbitRad) * ORBIT_RADIUS;
                float by = playerCenter.y + MathUtils.sin(orbitRad) * ORBIT_RADIUS;

                float bladeDeg = orbitDeg + (dir > 0f ? 90f : -90f);

                // Slightly scale older trail segments so they look softer
                float scale = 1f + t * 0.35f;

                batch.setColor(cOuter.r, cOuter.g, cOuter.b, aOuter);
                drawRotRect(batch, bx, by, BLADE_LENGTH * scale, BLADE_THICKNESS * 3.0f * scale, bladeDeg);

                batch.setColor(cInner.r, cInner.g, cInner.b, aInner);
                drawRotRect(batch, bx, by, BLADE_LENGTH * scale, BLADE_THICKNESS * scale, bladeDeg);
            }
        }

        batch.setColor(Color.WHITE);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
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
                0,
                0,
                1,
                1,
                false,
                false);
    }

    /**
     * Stops the looping sound effect immediately.
     * Call this when transitioning screens or when the player exits the level.
     */
    public void stopSound() {
        if (soundId != -1) {
            orbitSound.stop(soundId);
            soundId = -1;
        }
        active = false;
        activeTimer = 0f;
    }

    public void dispose() {
        stopSound();
        if (orbitSound != null) {
            orbitSound.stop();
            orbitSound.dispose();
        }
    }
}
