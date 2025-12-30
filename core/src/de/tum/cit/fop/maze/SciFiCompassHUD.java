package de.tum.cit.fop.maze;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

/**
 * Sci-Fi HUD compass / direction indicator.
 *
 * Visual style:
 * - Futuristic holographic dashboard (spaceship navigation HUD)
 * - Cyan / teal primary, white highlights
 * - Segmented outer ring with slow phase rotation
 * - Inner radar disc with sweep line
 * - Direction indicated by holographic energy arrow with trail
 * - Smooth interpolation, slight flicker/pulse for electronic feel
 *
 * Layers (bottom to top):
 * 1. Inner radar disc (very translucent)
 * 2. Radar sweep line (rotating)
 * 3. Segmented outer ring (slow counter-rotation)
 * 4. Cardinal markers (N/E/S/W as small HUD glyphs)
 * 5. Direction arrow with energy trail (glowing, points to target)
 * 6. Center dot / crosshair
 */
public class SciFiCompassHUD {

    // ========== TUNABLE PARAMETERS ==========

    /** Overall UI scale (1.0 = 64px base size) */
    public float scale = 2.4f;

    /** Margin from top-left corner in HUD world units */
    public float margin = 20f;

    /** Rotation smoothing speed (higher = snappier). Range: 8..20 */
    public float rotationSmoothing = 12f;

    /** Outer ring slow rotation speed (degrees/sec). Negative = counter-clockwise */
    public float outerRingRotationSpeed = -4f;

    /** Radar sweep rotation speed (degrees/sec) */
    public float sweepRotationSpeed = 45f;

    /** Flicker frequency (Hz). Higher = more unstable electronic feel */
    public float flickerFreq = 18f;

    /** Flicker intensity (0..1). 0 = no flicker, 0.15 = subtle, 0.3 = noticeable */
    public float flickerIntensity = 0.12f;

    /** Primary color: cyan/teal */
    public final Color primaryColor = new Color(0.2f, 0.85f, 0.9f, 1f);

    /** Secondary color: white highlight */
    public final Color highlightColor = new Color(1f, 1f, 1f, 1f);

    /** Arrow glow color: bright cyan */
    public final Color arrowGlowColor = new Color(0.3f, 0.95f, 1f, 1f);

    /** Inner disc alpha */
    public float discAlpha = 0.12f;

    /** Sweep line alpha */
    public float sweepAlpha = 0.35f;

    /** Outer ring segment alpha */
    public float ringAlpha = 0.7f;

    /** Direction arrow glow intensity */
    public float arrowGlow = 0.65f;

    // ========== ARROW/TRAIL PARAMETERS ==========
    /** Number of trail segments (ring buffer size) */
    private static final int TRAIL_COUNT = 6;
    /** Trail update interval in seconds */
    public float trailInterval = 0.025f;
    /** Arrow core length (relative to compass radius) */
    public float arrowLength = 0.58f;
    /** Arrow core width */
    public float arrowWidth = 2.5f;
    /** Glow layer width multiplier */
    public float glowWidthMult = 3.0f;
    /** Trail alpha start (most recent) */
    public float trailAlphaStart = 0.5f;
    /** Trail alpha end (oldest) */
    public float trailAlphaEnd = 0.05f;

    // ========================================

    private static final float BASE_SIZE = 64f;
    private static final int OUTER_SEGMENTS = 12;
    private static final float SEGMENT_GAP_DEG = 8f;

    // Shared 1x1 pixel texture
    private static Texture pixelTex;
    private static int pixelRefCount = 0;

    // Font for cardinal markers (use default bitmap font, small scale)
    private BitmapFont hudFont;
    private final GlyphLayout glyphLayout = new GlyphLayout();

    // State
    private float targetAngleDeg = 0f;
    private float currentAngleDeg = 0f;
    private float outerRingAngle = 0f;
    private float sweepAngle = 0f;
    private float time = 0f;

    // Trail ring buffer (fixed size, no allocation)
    private final float[] trailAngles = new float[TRAIL_COUNT];
    private int trailHead = 0;
    private float trailTimer = 0f;

    // Temp color to avoid allocation
    private final Color tmpColor = new Color();

    public SciFiCompassHUD() {
        initPixel();
        hudFont = new BitmapFont(); // default font, will scale down
        hudFont.getData().setScale(0.45f);
        hudFont.setColor(primaryColor);
        // Initialize trail buffer
        for (int i = 0; i < TRAIL_COUNT; i++) {
            trailAngles[i] = 0f;
        }
    }

    /**
     * Set target direction angle (degrees).
     * Convention: 0 = East (right), 90 = North (up), 180 = West (left), 270 = South (down)
     *
     * FIX: The input angle from Maze.findNearestExitDirection has a 180° offset bug.
     * We correct it here by adding 180° to point TOWARD the target instead of AWAY.
     */
    public void setTargetAngle(float degrees) {
        // FIX: Removed 180° offset as it was causing the pointer to be inverted
        targetAngleDeg = normalizeDeg(degrees);
    }

    /**
     * Update animations and smoothing. Call once per frame.
     */
    public void update(float dt) {
        time += dt;

        // Smooth rotation toward target (shortest path)
        float delta = shortestAngleDelta(currentAngleDeg, targetAngleDeg);
        float k = Math.max(0f, rotationSmoothing);
        float a = 1f - (float) Math.exp(-k * dt);
        currentAngleDeg = normalizeDeg(currentAngleDeg + delta * a);

        // Update trail buffer at fixed intervals
        trailTimer += dt;
        if (trailTimer >= trailInterval) {
            trailTimer = 0f;
            // Push current angle into ring buffer
            trailAngles[trailHead] = currentAngleDeg;
            trailHead = (trailHead + 1) % TRAIL_COUNT;
        }

        // Outer ring slow rotation
        outerRingAngle = normalizeDeg(outerRingAngle + outerRingRotationSpeed * dt);

        // Radar sweep rotation
        sweepAngle = normalizeDeg(sweepAngle + sweepRotationSpeed * dt);
    }

    /**
     * Render the HUD at top-left of viewport.
     * Assumes batch is NOT begun; will begin/end internally.
     */
    public void render(SpriteBatch batch, float viewportWidth, float viewportHeight) {
        float size = BASE_SIZE * scale;
        float x = margin;
        float y = viewportHeight - size - margin;

        batch.begin();
        renderAt(batch, x, y, size);
        batch.end();
    }

    /**
     * Render at specific position (bottom-left of HUD box). Assumes batch is begun.
     */
    public void renderAt(SpriteBatch batch, float x, float y, float size) {
        float cx = x + size / 2f;
        float cy = y + size / 2f;
        float r = size / 2f;

        // Compute flicker multiplier
        float flicker = 1f - flickerIntensity * 0.5f
                + flickerIntensity * 0.5f * MathUtils.sin(time * flickerFreq * MathUtils.PI2);
        // Occasional micro-dropout
        if (MathUtils.random() < 0.02f) flicker *= 0.7f;

        // === Layer 1: Inner radar disc ===
        drawDisc(batch, cx, cy, r * 0.75f, discAlpha * flicker);

        // === Layer 2: Radar sweep line ===
        drawSweep(batch, cx, cy, r * 0.72f, sweepAlpha * flicker);

        // === Layer 3: Segmented outer ring ===
        drawSegmentedRing(batch, cx, cy, r * 0.92f, r * 0.82f, ringAlpha * flicker);

        // === Layer 4: Cardinal markers (N/E/S/W) ===
        drawCardinals(batch, cx, cy, r * 0.62f, flicker);

        // === Layer 5: Direction arrow / light cone ===
        drawDirectionArrow(batch, cx, cy, r * 0.55f, flicker);

        // === Layer 6: Center crosshair / dot ===
        drawCenterCrosshair(batch, cx, cy, r * 0.08f, flicker);

        // Restore
        batch.setColor(Color.WHITE);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    // ==================== Drawing Helpers ====================

    private void drawDisc(SpriteBatch batch, float cx, float cy, float radius, float alpha) {
        // Draw as a filled circle approximation using segments
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        tmpColor.set(primaryColor.r * 0.3f, primaryColor.g * 0.4f, primaryColor.b * 0.5f, alpha);
        batch.setColor(tmpColor);

        // Simple: draw a square with rounded perception (pixel art friendly)
        // For true circle, you'd use a circle texture. Here we fake with concentric rings.
        int rings = 12;
        for (int i = 0; i < rings; i++) {
            float t = (float) i / rings;
            float rr = radius * (1f - t * 0.9f);
            float a = alpha * (0.3f + 0.7f * t); // inner brighter
            tmpColor.set(primaryColor.r * 0.25f, primaryColor.g * 0.35f, primaryColor.b * 0.4f, a * 0.5f);
            batch.setColor(tmpColor);
            drawRing(batch, cx, cy, rr, Math.max(1f, radius * 0.06f), 32);
        }
    }

    private void drawSweep(SpriteBatch batch, float cx, float cy, float radius, float alpha) {
        // Additive blend for glow
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

        // Draw a fading arc (sweep fan)
        int steps = 8;
        float fanAngle = 35f; // degrees
        for (int i = 0; i < steps; i++) {
            float t = (float) i / steps;
            float ang = sweepAngle - t * fanAngle;
            float a = alpha * (1f - t) * 0.7f;
            tmpColor.set(primaryColor.r, primaryColor.g, primaryColor.b, a);
            batch.setColor(tmpColor);

            float x1 = cx;
            float y1 = cy;
            float x2 = cx + MathUtils.cosDeg(ang) * radius;
            float y2 = cy + MathUtils.sinDeg(ang) * radius;
            drawSegment(batch, x1, y1, x2, y2, Math.max(1f, radius * 0.025f));
        }

        // Bright leading edge
        tmpColor.set(highlightColor.r, highlightColor.g, highlightColor.b, alpha * 0.9f);
        batch.setColor(tmpColor);
        float x2 = cx + MathUtils.cosDeg(sweepAngle) * radius;
        float y2 = cy + MathUtils.sinDeg(sweepAngle) * radius;
        drawSegment(batch, cx, cy, x2, y2, Math.max(1f, radius * 0.035f));

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void drawSegmentedRing(SpriteBatch batch, float cx, float cy,
                                   float outerR, float innerR, float alpha) {
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

        float segmentArc = (360f - OUTER_SEGMENTS * SEGMENT_GAP_DEG) / OUTER_SEGMENTS;
        float thickness = outerR - innerR;
        float midR = (outerR + innerR) / 2f;

        for (int i = 0; i < OUTER_SEGMENTS; i++) {
            float startAng = outerRingAngle + i * (segmentArc + SEGMENT_GAP_DEG);

            // Vary alpha slightly per segment for electronic feel
            float segAlpha = alpha * (0.7f + 0.3f * MathUtils.sin(time * 3f + i * 1.1f));
            tmpColor.set(primaryColor.r, primaryColor.g, primaryColor.b, segAlpha);
            batch.setColor(tmpColor);

            // Draw arc segment as multiple small lines
            int arcSteps = 6;
            float stepAng = segmentArc / arcSteps;
            for (int j = 0; j < arcSteps; j++) {
                float a1 = startAng + j * stepAng;
                float a2 = startAng + (j + 1) * stepAng;
                float x1 = cx + MathUtils.cosDeg(a1) * midR;
                float y1 = cy + MathUtils.sinDeg(a1) * midR;
                float x2 = cx + MathUtils.cosDeg(a2) * midR;
                float y2 = cy + MathUtils.sinDeg(a2) * midR;
                drawSegment(batch, x1, y1, x2, y2, thickness);
            }
        }

        // Outer glow pass
        tmpColor.set(primaryColor.r, primaryColor.g, primaryColor.b, alpha * 0.25f);
        batch.setColor(tmpColor);
        drawRing(batch, cx, cy, outerR + 1f, 2f, 48);

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void drawCardinals(SpriteBatch batch, float cx, float cy, float radius, float flicker) {
        String[] labels = {"E", "N", "W", "S"};
        float[] angles = {0f, 90f, 180f, 270f};

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

        for (int i = 0; i < 4; i++) {
            // Cardinals rotate opposite to direction (they're fixed in world, we rotate)
            // Actually for a HUD, cardinals are fixed on screen. Let's keep them static.
            float ang = angles[i];
            float lx = cx + MathUtils.cosDeg(ang) * radius;
            float ly = cy + MathUtils.sinDeg(ang) * radius;

            // Slight pulse
            float pulse = 0.7f + 0.3f * MathUtils.sin(time * 2.5f + i * 0.8f);
            tmpColor.set(primaryColor.r, primaryColor.g, primaryColor.b, 0.6f * flicker * pulse);
            hudFont.setColor(tmpColor);

            glyphLayout.setText(hudFont, labels[i]);
            hudFont.draw(batch, labels[i], lx - glyphLayout.width / 2f, ly + glyphLayout.height / 2f);
        }

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void drawDirectionArrow(SpriteBatch batch, float cx, float cy, float length, float flicker) {
        // === HOLOGRAPHIC ENERGY ARROW WITH TRAIL ===
        // Layers: Trail (oldest first) -> Outer Glow -> Inner Core -> Hot Tip

        float ang = currentAngleDeg;
        float len = length * (arrowLength / 0.55f); // Normalize to parameter

        // Arrow geometry
        float tipDist = len;
        float baseDist = len * 0.15f; // How far back the base is from center
        float wingSpread = 22f; // Angle spread for arrow wings

        // Calculate arrow points
        float tipX = cx + MathUtils.cosDeg(ang) * tipDist;
        float tipY = cy + MathUtils.sinDeg(ang) * tipDist;

        // Wing points (form a V shape behind the tip)
        float wingDist = len * 0.55f;
        float leftWingX = cx + MathUtils.cosDeg(ang + 180f - wingSpread) * wingDist;
        float leftWingY = cy + MathUtils.sinDeg(ang + 180f - wingSpread) * wingDist;
        float rightWingX = cx + MathUtils.cosDeg(ang + 180f + wingSpread) * wingDist;
        float rightWingY = cy + MathUtils.sinDeg(ang + 180f + wingSpread) * wingDist;

        // Tail point (energy exhaust origin)
        float tailX = cx + MathUtils.cosDeg(ang + 180f) * baseDist;
        float tailY = cy + MathUtils.sinDeg(ang + 180f) * baseDist;

        // === Layer 1: Energy Trail (additive, oldest first) ===
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

        for (int i = 0; i < TRAIL_COUNT; i++) {
            // Read from ring buffer, oldest first
            int idx = (trailHead + i) % TRAIL_COUNT;
            float trailAng = trailAngles[idx];

            // Age factor: 0 = oldest, 1 = newest
            float age = (float) i / (TRAIL_COUNT - 1);
            float trailAlpha = MathUtils.lerp(trailAlphaEnd, trailAlphaStart, age) * flicker * arrowGlow;

            // Trail gets shorter and thinner as it ages
            float trailLen = len * (0.3f + 0.4f * age);
            float trailWidth = arrowWidth * scale * (0.5f + 0.5f * age) * glowWidthMult * 0.6f;

            // Trail segment from center outward in trail direction
            float tTipX = cx + MathUtils.cosDeg(trailAng) * trailLen * 0.5f;
            float tTipY = cy + MathUtils.sinDeg(trailAng) * trailLen * 0.5f;
            float tTailX = cx + MathUtils.cosDeg(trailAng + 180f) * trailLen * 0.3f;
            float tTailY = cy + MathUtils.sinDeg(trailAng + 180f) * trailLen * 0.3f;

            tmpColor.set(arrowGlowColor.r, arrowGlowColor.g, arrowGlowColor.b, trailAlpha * 0.4f);
            batch.setColor(tmpColor);
            drawSegment(batch, tTailX, tTailY, tTipX, tTipY, trailWidth);
        }

        // === Layer 2: Outer Glow (wide, dim, additive) ===
        float glowAlpha = arrowGlow * flicker * 0.35f;
        float glowWidth = arrowWidth * scale * glowWidthMult;

        tmpColor.set(arrowGlowColor.r, arrowGlowColor.g * 0.9f, arrowGlowColor.b, glowAlpha);
        batch.setColor(tmpColor);

        // Glow: tip to wings
        drawSegment(batch, tipX, tipY, leftWingX, leftWingY, glowWidth);
        drawSegment(batch, tipX, tipY, rightWingX, rightWingY, glowWidth);
        // Glow: wings to tail (energy exhaust)
        drawSegment(batch, leftWingX, leftWingY, tailX, tailY, glowWidth * 0.7f);
        drawSegment(batch, rightWingX, rightWingY, tailX, tailY, glowWidth * 0.7f);

        // Extra glow bloom around tip
        tmpColor.set(arrowGlowColor.r, arrowGlowColor.g, arrowGlowColor.b, glowAlpha * 0.5f);
        batch.setColor(tmpColor);
        float bloomSize = glowWidth * 2f;
        batch.draw(pixelTex, tipX - bloomSize / 2f, tipY - bloomSize / 2f, bloomSize, bloomSize);

        // === Layer 3: Inner Core (solid, brighter) ===
        float coreAlpha = arrowGlow * flicker * 0.9f;
        float coreWidth = arrowWidth * scale;

        // Switch to normal blending for solid core
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        tmpColor.set(highlightColor.r, highlightColor.g * 0.98f, highlightColor.b * 0.95f, coreAlpha);
        batch.setColor(tmpColor);

        // Core: tip to wings
        drawSegment(batch, tipX, tipY, leftWingX, leftWingY, coreWidth);
        drawSegment(batch, tipX, tipY, rightWingX, rightWingY, coreWidth);
        // Core: wings to tail
        drawSegment(batch, leftWingX, leftWingY, tailX, tailY, coreWidth * 0.6f);
        drawSegment(batch, rightWingX, rightWingY, tailX, tailY, coreWidth * 0.6f);

        // === Layer 4: Hot Tip Highlight (additive) ===
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

        // Bright tip dot
        tmpColor.set(1f, 1f, 1f, coreAlpha);
        batch.setColor(tmpColor);
        float dotSize = coreWidth * 1.5f;
        batch.draw(pixelTex, tipX - dotSize / 2f, tipY - dotSize / 2f, dotSize, dotSize);

        // Small wing highlights
        tmpColor.set(arrowGlowColor.r, arrowGlowColor.g, arrowGlowColor.b, coreAlpha * 0.6f);
        batch.setColor(tmpColor);
        float wingDotSize = coreWidth * 0.8f;
        batch.draw(pixelTex, leftWingX - wingDotSize / 2f, leftWingY - wingDotSize / 2f, wingDotSize, wingDotSize);
        batch.draw(pixelTex, rightWingX - wingDotSize / 2f, rightWingY - wingDotSize / 2f, wingDotSize, wingDotSize);

        // Restore blending
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void drawCenterCrosshair(SpriteBatch batch, float cx, float cy, float size, float flicker) {
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

        // Small crosshair
        float len = size * 2.5f;
        float thick = Math.max(1f, size * 0.4f);
        float gap = size * 0.8f;

        tmpColor.set(primaryColor.r, primaryColor.g, primaryColor.b, 0.7f * flicker);
        batch.setColor(tmpColor);

        // Horizontal
        drawSegment(batch, cx - len, cy, cx - gap, cy, thick);
        drawSegment(batch, cx + gap, cy, cx + len, cy, thick);
        // Vertical
        drawSegment(batch, cx, cy - len, cx, cy - gap, thick);
        drawSegment(batch, cx, cy + gap, cx, cy + len, thick);

        // Center dot
        tmpColor.set(highlightColor.r, highlightColor.g, highlightColor.b, 0.9f * flicker);
        batch.setColor(tmpColor);
        float dotSize = Math.max(2f, size * 0.7f);
        batch.draw(pixelTex, cx - dotSize / 2f, cy - dotSize / 2f, dotSize, dotSize);

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    // ==================== Primitives ====================

    private void drawSegment(SpriteBatch batch, float x1, float y1, float x2, float y2, float thickness) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.01f) return;
        float ang = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;

        batch.draw(
                pixelTex,
                x1, y1 - thickness / 2f,
                0f, thickness / 2f,
                len, thickness,
                1f, 1f,
                ang,
                0, 0, 1, 1,
                false, false
        );
    }

    private void drawRing(SpriteBatch batch, float cx, float cy, float radius, float thickness, int segments) {
        float step = 360f / segments;
        for (int i = 0; i < segments; i++) {
            float a1 = i * step;
            float a2 = (i + 1) * step;
            float x1 = cx + MathUtils.cosDeg(a1) * radius;
            float y1 = cy + MathUtils.sinDeg(a1) * radius;
            float x2 = cx + MathUtils.cosDeg(a2) * radius;
            float y2 = cy + MathUtils.sinDeg(a2) * radius;
            drawSegment(batch, x1, y1, x2, y2, thickness);
        }
    }

    // ==================== Utilities ====================

    private static float normalizeDeg(float d) {
        d %= 360f;
        if (d < 0f) d += 360f;
        return d;
    }

    private static float shortestAngleDelta(float from, float to) {
        return MathUtils.atan2(
                MathUtils.sinDeg(to - from),
                MathUtils.cosDeg(to - from)
        ) * MathUtils.radiansToDegrees;
    }

    // ==================== Resource Management ====================

    private static synchronized void initPixel() {
        if (pixelTex == null) {
            Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pm.setColor(Color.WHITE);
            pm.fill();
            pixelTex = new Texture(pm);
            pixelTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            pm.dispose();
        }
        pixelRefCount++;
    }

    public void dispose() {
        if (hudFont != null) {
            hudFont.dispose();
            hudFont = null;
        }
        disposePixel();
    }

    private static synchronized void disposePixel() {
        pixelRefCount--;
        if (pixelRefCount <= 0 && pixelTex != null) {
            pixelTex.dispose();
            pixelTex = null;
            pixelRefCount = 0;
        }
    }
}

