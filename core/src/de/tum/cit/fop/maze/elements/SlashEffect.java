package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Pool;

/**
 * A reusable slash arc visual effect for melee attacks.
 * Features:
 * - Crescent-shaped arc with gradient transparency
 * - Motion blur trailing effect
 * - Additive blending for glow
 * - Object pooling for GC efficiency
 */
public class SlashEffect implements Pool.Poolable {

    // ===== TUNABLE PARAMETERS =====
    /** Duration of the slash effect in seconds */
    public static final float DURATION = 0.15f;
    /** Number of trail copies for motion blur */
    public static final int TRAIL_COUNT = 4;
    /** Alpha at the inner edge of the arc */
    public static final float ALPHA_INNER = 0.95f;
    /** Alpha at the outer edge of the arc */
    public static final float ALPHA_OUTER = 0.2f;
    /** Base scale of the slash texture */
    public static final float BASE_SCALE = 1.0f;
    /** Scale expansion during animation (adds to BASE_SCALE) */
    public static final float SCALE_EXPANSION = 0.3f;
    /** Distance from player center to slash origin */
    public static final float OFFSET_DISTANCE = 12f;
    /** Slash arc width */
    public static final float ARC_WIDTH = 48f;
    /** Slash arc height */
    public static final float ARC_HEIGHT = 24f;
    // ==============================

    // Shared texture (procedurally generated crescent)
    private static Texture slashTexture;
    private static int textureRefCount = 0;

    // Instance state
    private float x, y;
    private float angle; // degrees
    private float elapsed;
    private boolean finished;
    private Color tintColor;

    // Trail states (staggered positions for motion blur)
    private final float[] trailAlphas = new float[TRAIL_COUNT];
    private final float[] trailAngles = new float[TRAIL_COUNT];
    private final float[] trailScales = new float[TRAIL_COUNT];

    public SlashEffect() {
        tintColor = new Color(1f, 1f, 1f, 1f);
        initSharedTexture();
    }

    /**
     * Initialize shared texture (procedurally generated white crescent arc).
     * Called once per instance creation, reference counted for disposal.
     */
    private static synchronized void initSharedTexture() {
        if (slashTexture == null) {
            // Create a crescent/arc shape procedurally
            int width = 64;
            int height = 32;
            Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);

            float centerX = width / 2f;
            float centerY = height * 0.8f; // Arc center below the texture
            float outerRadius = width * 0.9f;
            float innerRadius = width * 0.5f;

            for (int px = 0; px < width; px++) {
                for (int py = 0; py < height; py++) {
                    float dx = px - centerX;
                    float dy = py - centerY;
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);

                    // Only draw in the arc region
                    if (dist >= innerRadius && dist <= outerRadius && py < centerY) {
                        // Calculate angle from center
                        float angleRad = (float) Math.atan2(dy, dx);
                        float angleDeg = angleRad * MathUtils.radiansToDegrees;

                        // Limit to upper arc (-180 to 0 degrees, i.e., top half)
                        if (angleDeg >= -170 && angleDeg <= -10) {
                            // Gradient: inner edge brighter, outer edge dimmer
                            float radialT = (dist - innerRadius) / (outerRadius - innerRadius);
                            float alpha = MathUtils.lerp(ALPHA_INNER, ALPHA_OUTER, radialT);

                            // Horizontal fade at edges
                            float edgeFade = 1f - Math.abs(angleDeg + 90f) / 80f;
                            edgeFade = MathUtils.clamp(edgeFade, 0f, 1f);
                            edgeFade = Interpolation.smooth.apply(edgeFade);

                            alpha *= edgeFade;

                            // Anti-aliasing at edges
                            float aaOuter = 1f - MathUtils.clamp((dist - outerRadius + 2) / 2f, 0f, 1f);
                            float aaInner = MathUtils.clamp((dist - innerRadius) / 2f, 0f, 1f);
                            alpha *= aaOuter * aaInner;

                            pixmap.setColor(1f, 1f, 1f, alpha);
                            pixmap.drawPixel(px, py);
                        }
                    }
                }
            }

            slashTexture = new Texture(pixmap);
            slashTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            pixmap.dispose();
        }
        textureRefCount++;
    }

    /**
     * Start/restart the slash effect at given position and angle.
     * @param x Center X position
     * @param y Center Y position
     * @param angleDegrees Direction angle in degrees (0=right, 90=up, 180=left, 270=down)
     */
    public void start(float x, float y, float angleDegrees) {
        this.x = x;
        this.y = y;
        this.angle = angleDegrees;
        this.elapsed = 0f;
        this.finished = false;

        // Initialize trail states with staggered values
        for (int i = 0; i < TRAIL_COUNT; i++) {
            float stagger = (float) i / TRAIL_COUNT;
            trailAlphas[i] = 0f;
            trailAngles[i] = angleDegrees - stagger * 15f; // Trailing angle offset
            trailScales[i] = BASE_SCALE - stagger * 0.1f;
        }
    }

    /**
     * Set tint color for the slash effect.
     * @param color Tint color (white = no tint)
     */
    public void setTintColor(Color color) {
        this.tintColor.set(color);
    }

    /**
     * Update effect state. Call every frame.
     * @param deltaTime Frame delta time in seconds
     */
    public void update(float deltaTime) {
        if (finished) return;

        elapsed += deltaTime;

        if (elapsed >= DURATION) {
            finished = true;
            return;
        }

        float progress = elapsed / DURATION;

        // Update trail states
        for (int i = 0; i < TRAIL_COUNT; i++) {
            float stagger = (float) i / TRAIL_COUNT;
            float trailProgress = MathUtils.clamp(progress - stagger * 0.2f, 0f, 1f);

            // Alpha curve: fade in quickly, fade out with pow2Out
            float fadeIn = MathUtils.clamp(trailProgress * 5f, 0f, 1f);
            float fadeOut = 1f - Interpolation.pow2Out.apply(trailProgress);
            trailAlphas[i] = fadeIn * fadeOut * (1f - stagger * 0.3f);

            // Angle sweep (slight rotation during slash)
            trailAngles[i] = angle + (1f - trailProgress) * 20f - stagger * 10f;

            // Scale expansion
            trailScales[i] = BASE_SCALE + SCALE_EXPANSION * Interpolation.pow2Out.apply(trailProgress)
                           - stagger * 0.05f;
        }
    }

    /**
     * Render the slash effect with additive blending.
     * @param batch SpriteBatch (must be in begun state, will temporarily end for blend change)
     */
    public void render(SpriteBatch batch) {
        if (finished || slashTexture == null) return;

        // Enable additive blending for glow effect
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

        float texW = slashTexture.getWidth();
        float texH = slashTexture.getHeight();

        // Render trails from back to front (oldest first, most recent last)
        for (int i = TRAIL_COUNT - 1; i >= 0; i--) {
            if (trailAlphas[i] <= 0.01f) continue;

            float scale = trailScales[i] * (ARC_WIDTH / texW);
            float scaleY = trailScales[i] * (ARC_HEIGHT / texH);
            float drawW = texW * scale;
            float drawH = texH * scaleY;

            // Calculate offset position based on angle
            float offsetX = MathUtils.cosDeg(trailAngles[i]) * OFFSET_DISTANCE;
            float offsetY = MathUtils.sinDeg(trailAngles[i]) * OFFSET_DISTANCE;

            float drawX = x + offsetX - drawW / 2f;
            float drawY = y + offsetY - drawH / 2f;

            // Apply tint with trail alpha
            batch.setColor(tintColor.r, tintColor.g, tintColor.b, trailAlphas[i] * tintColor.a);

            // Draw with rotation around center
            batch.draw(
                slashTexture,
                drawX, drawY,           // position
                drawW / 2f, drawH / 2f, // origin (center)
                drawW, drawH,           // size
                1f, 1f,                 // scale (already applied)
                trailAngles[i] - 90f,   // rotation (adjust for texture orientation)
                0, 0,                   // src xy
                (int) texW, (int) texH, // src size
                false, false            // flip
            );
        }

        // Restore normal blending
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(Color.WHITE);
    }

    /**
     * Check if the effect has finished playing.
     */
    public boolean isFinished() {
        return finished;
    }

    @Override
    public void reset() {
        elapsed = 0f;
        finished = true;
        x = y = angle = 0f;
        tintColor.set(Color.WHITE);
    }

    /**
     * Dispose shared resources. Call when no more SlashEffects will be created.
     */
    public static synchronized void disposeShared() {
        textureRefCount--;
        if (textureRefCount <= 0 && slashTexture != null) {
            slashTexture.dispose();
            slashTexture = null;
            textureRefCount = 0;
        }
    }
}

