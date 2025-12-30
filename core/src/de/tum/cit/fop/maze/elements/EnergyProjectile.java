package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pool;

/**
 * A sci-fi energy projectile with tail flame.
 * - Moves as a fast projectile
 * - Bounces off walls + movable walls
 * - Kills mobs on contact
 * - Renders as a cyan energy core + additive glow + tapered exhaust trail
 *
 * Notes:
 * - No per-frame allocations (reuses rectangles/vectors and ring buffers).
 */
public class EnergyProjectile implements Pool.Poolable {

    // Shared 1x1 pixel texture
    private static Texture whitePixel;

    // State
    private float x, y;
    private float vx, vy; // normalized direction
    private float speed;
    private float radius;
    private int bouncesLeft;
    private float lifeLeft;
    private boolean alive;

    // Visual time
    private float time;

    // Tail ring buffer (positions)
    private static final int TAIL_POINTS = 10;
    private final float[] tailX = new float[TAIL_POINTS];
    private final float[] tailY = new float[TAIL_POINTS];
    private int tailHead = 0;
    private float tailTimer = 0f;
    private float tailInterval = 0.015f;

    // Colors
    private final Color glowColor = new Color(0.10f, 0.85f, 1.0f, 1f);
    private final Color coreColor = new Color(1f, 1f, 1f, 1f);

    // --- Visual tuning (smaller + less boxy) ---
    /** Overall visual scale applied on top of radius. */
    private float visualScale = 0.78f;
    /** Glow/bloom intensity factor. */
    private float glowIntensity = 0.42f;
    /** Core intensity factor. */
    private float coreIntensity = 0.92f;

    /** Random scatter applied after a bounce (degrees). 0 = perfect mirror reflect. */
    private float bounceScatterDeg = 22f;

    // temp vectors for bounce compute (no alloc)
    private final Vector2 tmpTangent = new Vector2();

    /** Bounce flash timer (seconds) */
    private float bounceFlash = 0f;

    // Temp objects (no alloc)
    private final Rectangle projRect = new Rectangle();
    private final Rectangle tileRect = new Rectangle();
    private final Vector2 tmpV = new Vector2();
    private final Vector2 tmpTilePos = new Vector2();

    public EnergyProjectile() {
        ensurePixel();
    }

    public void init(float x, float y, float dirX, float dirY,
                     float speed, float radius, int maxBounces, float lifetime) {
        this.x = x;
        this.y = y;
        tmpV.set(dirX, dirY).nor();
        this.vx = tmpV.x;
        this.vy = tmpV.y;
        this.speed = speed;
        this.radius = radius;
        this.bouncesLeft = maxBounces;
        this.lifeLeft = lifetime;
        this.alive = true;
        this.time = 0f;
        this.bounceFlash = 0f;

        // init tail
        for (int i = 0; i < TAIL_POINTS; i++) {
            tailX[i] = x;
            tailY[i] = y;
        }
        tailHead = 0;
        tailTimer = 0f;
    }

    public void update(float dt, Maze maze) {
        if (!alive) return;

        time += dt;
        if (bounceFlash > 0f) bounceFlash = Math.max(0f, bounceFlash - dt);

        lifeLeft -= dt;
        if (lifeLeft <= 0f) {
            alive = false;
            return;
        }

        // Update tail buffer at fixed interval
        tailTimer += dt;
        while (tailTimer >= tailInterval) {
            tailTimer -= tailInterval;
            tailX[tailHead] = x;
            tailY[tailHead] = y;
            tailHead = (tailHead + 1) % TAIL_POINTS;
        }

        // Move with substeps to avoid tunneling
        float dist = speed * dt;
        int steps = MathUtils.ceil(dist / (maze.getBlockSize() * 0.20f));
        steps = MathUtils.clamp(steps, 1, 8);
        float stepDt = dt / steps;

        for (int i = 0; i < steps; i++) {
            if (!alive) break;
            integrate(stepDt, maze);
            checkMobHit(maze);
        }
    }

    private void integrate(float dt, Maze maze) {
        float nx = x + vx * speed * dt;
        float ny = y + vy * speed * dt;

        boolean bounced = false;

        // Test collision and approximate normal using axis separation.
        // We also include MovableWall in the collision check.
        if (collidesWallOrMovableWall(nx, y, maze)) {
            vx = -vx;
            bounced = true;
            bouncesLeft--;
            if (bouncesLeft < 0) { alive = false; return; }
            nx = x + vx * speed * dt;
        }

        if (collidesWallOrMovableWall(nx, ny, maze)) {
            vy = -vy;
            bounced = true;
            bouncesLeft--;
            if (bouncesLeft < 0) { alive = false; return; }
            ny = y + vy * speed * dt;
        }

        x = nx;
        y = ny;

        if (bounced) {
            // Advanced bounce scatter:
            // - Build a normal from the axis flip result by looking at which component changed.
            // - Compute tangent and deflect along tangent by a random amount.
            // This yields natural diagonal ricochets without overly chaotic reversals.
            applyAdvancedBounceScatter();

            // Small push forward to avoid repeated collision on the same surface
            x += vx * 0.5f;
            y += vy * 0.5f;
        }
    }

    private boolean collidesWallOrMovableWall(float cx, float cy, Maze maze) {
        // 1) Tile obstacles
        if (collidesWall(cx, cy, maze)) return true;

        // 2) MovableWall entities
        projRect.set(cx - radius, cy - radius, radius * 2f, radius * 2f);
        for (Entity e : maze.getEntities()) {
            if (!(e instanceof MovableWall mw)) continue;
            if (projRect.overlaps(mw.getHitbox())) return true;
        }
        return false;
    }

    private boolean collidesWall(float cx, float cy, Maze maze) {
        projRect.set(cx - radius, cy - radius, radius * 2f, radius * 2f);

        float tile = maze.getBlockSize();
        int minX = (int) Math.floor(projRect.x / tile);
        int minY = (int) Math.floor(projRect.y / tile);
        int maxX = (int) Math.floor((projRect.x + projRect.width) / tile);
        int maxY = (int) Math.floor((projRect.y + projRect.height) / tile);

        for (int tx = minX; tx <= maxX; tx++) {
            for (int ty = minY; ty <= maxY; ty++) {
                if (tx < 0 || ty < 0 || tx >= maze.getWidth() || ty >= maze.getHeight()) return true;

                tmpTilePos.set(tx * tile, ty * tile);
                Block b = maze.getBlock(tmpTilePos);
                if (b == null || !b.isObstacle()) continue;

                tileRect.set(tx * tile, ty * tile, tile, tile);
                if (tileRect.overlaps(projRect)) return true;
            }
        }
        return false;
    }

    private void applyAdvancedBounceScatter() {
        if (bounceScatterDeg <= 0.01f) {
            tmpV.set(vx, vy).nor();
            vx = tmpV.x;
            vy = tmpV.y;
            return;
        }

        // Treat current velocity as the reflected direction already.
        // We now deflect along tangent (perpendicular) by a random amount.
        tmpV.set(vx, vy).nor();

        // tangent vector
        tmpTangent.set(-tmpV.y, tmpV.x);

        // Scale randomness based on how "head-on" the bounce is.
        // More head-on => more noticeable scatter; grazing => subtle.
        float headOn = Math.abs(tmpV.dot(tmpTangent)); // ~1 for diagonal, ~0 for axis
        float base = 1f - headOn;

        float jitterDeg = MathUtils.random(-bounceScatterDeg, bounceScatterDeg) * (0.35f + 0.65f * base);
        float jitter = MathUtils.sinDeg(jitterDeg);

        // deflect along tangent
        tmpV.mulAdd(tmpTangent, jitter).nor();

        vx = tmpV.x;
        vy = tmpV.y;
    }

    /**
     * Computes a collision normal at the next position.
     * Writes the normal into outNormal and returns true if colliding.
     */
    private boolean getCollisionNormal(float cx, float cy, Maze maze, Vector2 outNormal) {
        // Projectile AABB
        projRect.set(cx - radius, cy - radius, radius * 2f, radius * 2f);

        // 1) MovableWall entities (treat as solid)
        for (Entity e : maze.getEntities()) {
            if (!(e instanceof MovableWall mw)) continue;
            Rectangle hw = mw.getHitbox();
            if (!projRect.overlaps(hw)) continue;
            computeAabbPushNormal(projRect, hw, outNormal);
            return true;
        }

        // 2) Tile obstacles
        float tile = maze.getBlockSize();
        int minX = (int) Math.floor(projRect.x / tile);
        int minY = (int) Math.floor(projRect.y / tile);
        int maxX = (int) Math.floor((projRect.x + projRect.width) / tile);
        int maxY = (int) Math.floor((projRect.y + projRect.height) / tile);

        for (int tx = minX; tx <= maxX; tx++) {
            for (int ty = minY; ty <= maxY; ty++) {
                // out of bounds = wall
                if (tx < 0 || ty < 0 || tx >= maze.getWidth() || ty >= maze.getHeight()) {
                    // approximate normal as pointing back toward center by clamping
                    float nx = (tx < 0) ? 1f : (tx >= maze.getWidth() ? -1f : 0f);
                    float ny = (ty < 0) ? 1f : (ty >= maze.getHeight() ? -1f : 0f);
                    if (nx == 0f && ny == 0f) {
                        // fallback
                        outNormal.set(-vx, -vy).nor();
                        return true;
                    }
                    outNormal.set(nx, ny).nor();
                    return true;
                }

                tmpTilePos.set(tx * tile, ty * tile);
                Block b = maze.getBlock(tmpTilePos);
                if (b == null || !b.isObstacle()) continue;

                tileRect.set(tx * tile, ty * tile, tile, tile);
                if (!tileRect.overlaps(projRect)) continue;

                computeAabbPushNormal(projRect, tileRect, outNormal);
                return true;
            }
        }

        return false;
    }

    /**
     * For two overlapping AABBs, compute a stable push normal (axis of minimum penetration).
     */
    private static void computeAabbPushNormal(Rectangle a, Rectangle b, Vector2 outNormal) {
        float axc = a.x + a.width * 0.5f;
        float ayc = a.y + a.height * 0.5f;
        float bxc = b.x + b.width * 0.5f;
        float byc = b.y + b.height * 0.5f;

        float dx = axc - bxc;
        float px = (a.width * 0.5f + b.width * 0.5f) - Math.abs(dx);

        float dy = ayc - byc;
        float py = (a.height * 0.5f + b.height * 0.5f) - Math.abs(dy);

        if (px < py) {
            outNormal.set(Math.signum(dx), 0f);
        } else {
            outNormal.set(0f, Math.signum(dy));
        }
        if (outNormal.isZero(0.0001f)) {
            outNormal.set(1f, 0f);
        }
    }

    private void checkMobHit(Maze maze) {
        // If projectile overlaps a mob hitbox -> kill
        projRect.set(x - radius, y - radius, radius * 2f, radius * 2f);
        for (Entity e : maze.getEntities()) {
            if (!(e instanceof Mob mob)) continue;
            if (projRect.overlaps(mob.getHitbox())) {
                mob.modifyHealth(-9999f);
                alive = false;
                return;
            }
        }

        // Check SkullBoss (20 damage per hit, doesn't kill projectile)
        SkullBoss boss = maze.getSkullBoss();
        if (boss != null && boss.isActive()) {
            Rectangle bossRect = new Rectangle(
                boss.getX() - 45f, boss.getY() - 45f, 90f, 90f
            );
            if (projRect.overlaps(bossRect)) {
                boss.modifyHealth(-20f);
                alive = false;  // Projectile is consumed
                return;
            }
        }
    }

    public void render(SpriteBatch batch) {
        if (!alive) return;

        // Energy flicker/pulse
        float pulse = 0.85f + 0.15f * MathUtils.sin((time * 10.0f) * MathUtils.PI2);
        float flicker = (0.85f + 0.15f * MathUtils.random()) * pulse;

        // Some alpha modulation near death
        float lifeAlpha = MathUtils.clamp(lifeLeft * 0.65f, 0.2f, 1.0f);

        float vs = visualScale;

        // ===== Trail / Exhaust (additive) =====
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

        for (int i = 0; i < TAIL_POINTS; i++) {
            int idx = (tailHead + i) % TAIL_POINTS; // oldest -> newest
            float age = (float) i / (TAIL_POINTS - 1); // 0..1

            // Outer flame (wider) - smaller than before
            float a = lifeAlpha * (0.03f + 0.18f * age) * flicker;
            float size = radius * vs * (1.6f + 4.2f * age);
            batch.setColor(glowColor.r, glowColor.g, glowColor.b, a);
            batch.draw(whitePixel, tailX[idx] - size / 2f, tailY[idx] - size / 2f, size, size);

            // Inner hot streak (smaller & brighter)
            float a2 = lifeAlpha * (0.02f + 0.14f * age) * flicker;
            float size2 = radius * vs * (0.95f + 2.2f * age);
            batch.setColor(1f, 1f, 1f, a2);
            batch.draw(whitePixel, tailX[idx] - size2 / 2f, tailY[idx] - size2 / 2f, size2, size2);
        }

        // ===== Bounce flash (additive) =====
        if (bounceFlash > 0f) {
            float k = bounceFlash / 0.08f;
            float s = radius * vs * (7.5f + 11.0f * (1f - k));
            batch.setColor(0.65f, 1f, 1f, 0.50f * k);
            batch.draw(whitePixel, x - s / 2f, y - s / 2f, s, s);
        }

        // ===== Main glow bloom (additive) =====
        // smaller + softer glow to avoid a big square front
        float glowSize = radius * vs * (4.2f + 0.8f * pulse);
        batch.setColor(glowColor.r, glowColor.g, glowColor.b, glowIntensity * flicker * lifeAlpha);
        batch.draw(whitePixel, x - glowSize / 2f, y - glowSize / 2f, glowSize, glowSize);

        // ===== Core (normal alpha) =====
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        float rot = (time * 260f) % 360f;
        // shorter, thinner diamond -> less boxy
        float coreLen = radius * vs * 2.4f;
        float coreThk = Math.max(0.9f, radius * vs * 0.55f);

        // bright compact core
        batch.setColor(coreColor.r, coreColor.g, coreColor.b, coreIntensity * lifeAlpha);
        drawRotRect(batch, x, y, coreLen, coreThk, rot);
        drawRotRect(batch, x, y, coreLen * 0.70f, coreThk, rot + 90f);

        // subtle cyan rim overlay (very light)
        batch.setColor(glowColor.r, glowColor.g, glowColor.b, 0.18f * lifeAlpha);
        drawRotRect(batch, x, y, coreLen * 1.05f, coreThk * 1.25f, rot + 45f);

        // hot tip highlight in travel direction (tiny dot) to reduce "flat/square head"
        float tipX = x + vx * radius * vs * 1.35f;
        float tipY = y + vy * radius * vs * 1.35f;
        float tipSize = Math.max(1.2f, radius * vs * 0.85f);
        batch.setColor(1f, 1f, 1f, 0.70f * lifeAlpha);
        batch.draw(whitePixel, tipX - tipSize / 2f, tipY - tipSize / 2f, tipSize, tipSize);

        batch.setColor(Color.WHITE);
    }

    private void drawRotRect(SpriteBatch batch, float cx, float cy, float w, float h, float angleDeg) {
        batch.draw(
                whitePixel,
                cx - w / 2f,
                cy - h / 2f,
                w / 2f,
                h / 2f,
                w,
                h,
                1f,
                1f,
                angleDeg,
                0,
                0,
                1,
                1,
                false,
                false);
    }

    public boolean isAlive() {
        return alive;
    }

    @Override
    public void reset() {
        alive = false;
        lifeLeft = 0f;
        bouncesLeft = 0;
        x = y = 0f;
        vx = vy = 0f;
        time = 0f;
        bounceFlash = 0f;
        tailHead = 0;
        tailTimer = 0f;
    }

    private static synchronized void ensurePixel() {
        if (whitePixel == null) {
            Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pm.setColor(Color.WHITE);
            pm.fill();
            whitePixel = new Texture(pm);
            whitePixel.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            pm.dispose();
        }
    }
}
