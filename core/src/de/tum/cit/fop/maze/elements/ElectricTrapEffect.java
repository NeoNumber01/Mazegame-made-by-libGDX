package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

/**
 * Electric arc effect for traps: irregular, flickering, ground-hugging pseudo lightning.
 *
 * Design goals:
 * - Not a regular grid animation
 * - Short jagged arc segments that jump/fork slightly
 * - Flicker intensity and occasional near-invisible frames
 * - Pixel-ish look (no smooth curves)
 *
 * Performance:
 * - No per-frame allocations
 * - Fixed-size arrays, regenerated on a timer (0.05~0.15s)
 */
public class ElectricTrapEffect {

    // ===== Tunables =====
    /** How many independent arcs (2..5 feels good) */
    private static final int ARC_MIN = 2;
    private static final int ARC_MAX = 5;

    /** How many points per arc polyline (jagged) */
    private static final int POINTS_PER_ARC = 6;

    /** Regeneration interval in seconds */
    private static final float REGEN_MIN = 0.05f;
    private static final float REGEN_MAX = 0.15f;

    /** Flicker speed. Higher = more unstable */
    private static final float FLICKER_FREQ = 28f;

    /** Chance that a frame becomes very dim */
    private static final float DIM_PULSE_CHANCE = 0.12f;

    /** Base alpha and max alpha */
    private static final float ALPHA_MIN = 0.05f;
    private static final float ALPHA_MAX = 0.95f;

    /** Arc thickness in pixels (1..2 for pixel art) */
    private static final float THICKNESS_MIN = 1f;
    private static final float THICKNESS_MAX = 2f;

    /** How far arcs can wander from their anchor, within the tile */
    private static final float WANDER = 6f;

    /** How much vertical movement (keep it ground-hugging -> small) */
    private static final float Y_WANDER_SCALE = 0.45f;

    /** -1..1 random jitter applied each regen, to "jump" */
    private static final float JUMP = 2f;

    /** Branch chance per arc */
    private static final float BRANCH_CHANCE = 0.35f;

    // ===================

    private static Texture pixelTexture;
    private static int refCount = 0;

    private final float tileSize;

    // world position of tile (bottom-left)
    private float baseX;
    private float baseY;

    private int arcCount;

    // Arc point arrays: [arcIndex][pointIndex]
    private final float[][] px;
    private final float[][] py;

    // Optional branch: one short branch per arc
    private final boolean[] hasBranch;
    private final float[][] bx;
    private final float[][] by;

    private final float[] arcThickness;

    private float regenTimer;

    private float flickerTime;
    private float flickerSeed;
    private float dimPulseTimer;

    private final Color tmpColor = new Color();

    public ElectricTrapEffect(float tileSize) {
        this.tileSize = tileSize;

        px = new float[ARC_MAX][POINTS_PER_ARC];
        py = new float[ARC_MAX][POINTS_PER_ARC];

        hasBranch = new boolean[ARC_MAX];
        bx = new float[ARC_MAX][3];
        by = new float[ARC_MAX][3];

        arcThickness = new float[ARC_MAX];

        initSharedPixel();
        resetTimers();

        // start with a shape
        regenerate();
    }

    public void setPosition(float x, float y) {
        this.baseX = x;
        this.baseY = y;
    }

    public void update(float dt) {
        flickerTime += dt;

        regenTimer -= dt;
        if (regenTimer <= 0f) {
            regenerate();
            resetTimers();
        }

        // occasional dim pulses (almost disappear)
        dimPulseTimer -= dt;
        if (dimPulseTimer <= 0f) {
            // schedule next pulse check
            dimPulseTimer = MathUtils.random(0.12f, 0.35f);
            if (MathUtils.random() < DIM_PULSE_CHANCE) {
                // force a temporary dim by adjusting seed
                flickerSeed = MathUtils.random(10f, 1000f);
            }
        }
    }

    public void render(SpriteBatch batch) {
        if (pixelTexture == null) return;

        // Additive blending for glow
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

        float globalF = flicker();
        if (globalF <= 0.01f) {
            // still restore blend
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            return;
        }

        // Colors: blue/cyan outer + white core, pixel-ish
        // We'll draw each segment twice: outer (cyan) + inner (white)
        for (int a = 0; a < arcCount; a++) {
            float t = arcThickness[a];

            drawPolyline(batch, a, globalF, t, 0.35f, 0.75f, 1f);
            drawPolyline(batch, a, globalF, Math.max(1f, t - 0.6f), 1f, 1f, 1f);

            if (hasBranch[a]) {
                drawBranch(batch, a, globalF, t, 0.35f, 0.75f, 1f);
                drawBranch(batch, a, globalF, Math.max(1f, t - 0.6f), 1f, 1f, 1f);
            }
        }

        // restore
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(Color.WHITE);
    }

    private void drawPolyline(SpriteBatch batch, int arcIndex, float globalF, float thickness, float r, float g, float b) {
        for (int i = 0; i < POINTS_PER_ARC - 1; i++) {
            float x1 = baseX + px[arcIndex][i];
            float y1 = baseY + py[arcIndex][i];
            float x2 = baseX + px[arcIndex][i + 1];
            float y2 = baseY + py[arcIndex][i + 1];

            drawSegment(batch, x1, y1, x2, y2, thickness, r, g, b, segmentAlpha(globalF, i));
        }
    }

    private void drawBranch(SpriteBatch batch, int arcIndex, float globalF, float thickness, float r, float g, float b) {
        for (int i = 0; i < 2; i++) {
            float x1 = baseX + bx[arcIndex][i];
            float y1 = baseY + by[arcIndex][i];
            float x2 = baseX + bx[arcIndex][i + 1];
            float y2 = baseY + by[arcIndex][i + 1];
            drawSegment(batch, x1, y1, x2, y2, thickness, r, g, b, globalF * 0.65f);
        }
    }

    private void drawSegment(SpriteBatch batch,
                             float x1, float y1,
                             float x2, float y2,
                             float thickness,
                             float r, float g, float b,
                             float alpha) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.01f) return;

        float angle = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;

        // Pixel-snapping to reduce smooth look
        x1 = MathUtils.floor(x1);
        y1 = MathUtils.floor(y1);

        tmpColor.set(r, g, b, alpha);
        batch.setColor(tmpColor);

        // Draw a stretched 1x1 pixel with rotation
        batch.draw(
                pixelTexture,
                x1, y1,
                0f, thickness / 2f,
                len, thickness,
                1f, 1f,
                angle,
                0, 0,
                1, 1,
                false, false
        );
    }

    private float flicker() {
        // Composite flicker: fast sine + random noise-like component
        float s = 0.55f + 0.45f * MathUtils.sin((flickerTime + flickerSeed) * FLICKER_FREQ);
        float n = MathUtils.random(0.65f, 1.05f);

        float a = s * n;
        // occasional very dim
        if (MathUtils.random() < 0.05f) a *= 0.2f;

        return MathUtils.clamp(MathUtils.lerp(ALPHA_MIN, ALPHA_MAX, a), 0f, 1f);
    }

    private float segmentAlpha(float globalF, int segmentIndex) {
        // Slight variation along arc, avoid uniform look
        float local = 0.75f + 0.25f * MathUtils.sin((flickerTime * 12f) + segmentIndex * 2.2f + flickerSeed);
        return globalF * local;
    }

    private void regenerate() {
        arcCount = MathUtils.random(ARC_MIN, ARC_MAX);

        float cx = tileSize / 2f;
        float cy = tileSize / 2f;

        for (int a = 0; a < arcCount; a++) {
            arcThickness[a] = MathUtils.random(THICKNESS_MIN, THICKNESS_MAX);

            // pick a random "anchor" in the tile, keep inside with margins
            float ax = cx + MathUtils.random(-WANDER, WANDER) + MathUtils.random(-JUMP, JUMP);
            float ay = cy + MathUtils.random(-WANDER, WANDER) * Y_WANDER_SCALE + MathUtils.random(-JUMP, JUMP) * 0.4f;

            // pick a random direction mostly horizontal-ish to feel ground-crawling
            float dir = MathUtils.random(0f, 360f);
            float dirBias = (MathUtils.random() < 0.5f) ? 0f : 180f; // prefer left/right
            dir = MathUtils.lerpAngleDeg(dir, dirBias, 0.65f);

            float totalLen = MathUtils.random(tileSize * 0.45f, tileSize * 0.8f);
            float step = totalLen / (POINTS_PER_ARC - 1);

            float vx = MathUtils.cosDeg(dir);
            float vy = MathUtils.sinDeg(dir) * 0.35f; // flatten vertically

            float x = ax - vx * totalLen * 0.5f;
            float y = ay - vy * totalLen * 0.5f;

            for (int i = 0; i < POINTS_PER_ARC; i++) {
                // jagged offset each point
                float jx = MathUtils.random(-3f, 3f);
                float jy = MathUtils.random(-2f, 2f) * Y_WANDER_SCALE;

                float pxLocal = x + vx * step * i + jx;
                float pyLocal = y + vy * step * i + jy;

                // clamp inside tile a bit
                pxLocal = MathUtils.clamp(pxLocal, 2f, tileSize - 2f);
                pyLocal = MathUtils.clamp(pyLocal, 2f, tileSize - 2f);

                px[a][i] = MathUtils.floor(pxLocal); // snap for pixel-ish look
                py[a][i] = MathUtils.floor(pyLocal);
            }

            // Optional branch: start at a random mid point and shoot a short jagged sub-arc
            hasBranch[a] = MathUtils.random() < BRANCH_CHANCE;
            if (hasBranch[a]) {
                int baseIdx = MathUtils.random(1, POINTS_PER_ARC - 3);
                float sx = px[a][baseIdx];
                float sy = py[a][baseIdx];

                float bdir = dir + MathUtils.randomSign() * MathUtils.random(35f, 75f);
                float blen = MathUtils.random(tileSize * 0.15f, tileSize * 0.28f);

                float bvx = MathUtils.cosDeg(bdir);
                float bvy = MathUtils.sinDeg(bdir) * 0.35f;

                bx[a][0] = sx;
                by[a][0] = sy;

                for (int bi = 1; bi < 3; bi++) {
                    float t = bi / 2f;
                    float jx = MathUtils.random(-2f, 2f);
                    float jy = MathUtils.random(-2f, 2f) * Y_WANDER_SCALE;

                    float lx = sx + bvx * blen * t + jx;
                    float ly = sy + bvy * blen * t + jy;

                    bx[a][bi] = MathUtils.floor(MathUtils.clamp(lx, 2f, tileSize - 2f));
                    by[a][bi] = MathUtils.floor(MathUtils.clamp(ly, 2f, tileSize - 2f));
                }
            }
        }
    }

    private void resetTimers() {
        regenTimer = MathUtils.random(REGEN_MIN, REGEN_MAX);
        flickerSeed = MathUtils.random(0f, 1000f);
    }

    private static synchronized void initSharedPixel() {
        if (pixelTexture == null) {
            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(Color.WHITE);
            pixmap.fill();
            pixelTexture = new Texture(pixmap);
            pixelTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            pixmap.dispose();
        }
        refCount++;
    }

    /** Call when the owning Trap is disposed. */
    public void dispose() {
        disposeShared();
    }

    public static synchronized void disposeShared() {
        refCount--;
        if (refCount <= 0 && pixelTexture != null) {
            pixelTexture.dispose();
            pixelTexture = null;
            refCount = 0;
        }
    }
}

