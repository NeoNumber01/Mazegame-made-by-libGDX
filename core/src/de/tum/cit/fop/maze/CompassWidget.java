package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * Pixel-dungeon style compass UI widget (SpriteBatch-based).
 *
 * Layers (recommended textures):
 * - base: inner plate
 * - ring: bezel / frame
 * - needle: pointer
 *
 * Also supports a built-in placeholder renderer if textures are missing.
 */
public class CompassWidget {

    public static class Style {
        /** UI scale applied to the texture's original pixel size (keeps pixel-art look). */
        public float scale = 1.0f;

        /** Margin from the top-left corner in UI world units. */
        public float margin = 18f;

        /** Damped smoothing speed (1/s). Typical: 10..18. Higher = snappier. */
        public float rotationSmoothing = 14f;

        /** Needle base alpha (overall opacity). */
        public float needleAlpha = 0.95f;

        /** Extra highlight intensity for the needle (0..1). */
        public float needleHighlight = 0.35f;

        /** Base tint for ring/base to simulate brass/metal.
         *  Brass-ish: (0.95,0.82,0.45)
         *  Dark iron: (0.55,0.60,0.65)
         */
        public final Color metalTint = new Color(0.90f, 0.78f, 0.45f, 1f);

        /** A cool glow tint for ticks/highlights. */
        public final Color glowTint = new Color(0.55f, 0.85f, 1f, 1f);

        /** How strong the bezel highlight overlay is (0..1). */
        public float bezelHighlight = 0.35f;

        /** How strong the inner shadow overlay is (0..1). */
        public float innerShadow = 0.35f;
    }

    private final Style style;

    private TextureRegion baseTex;
    private TextureRegion ringTex;
    private TextureRegion needleTex;

    // Fallback 1x1 pixel texture for overlay shading and placeholder mode
    private static Texture pixelTex;
    private static int pixelRefCount = 0;

    private float targetAngleDeg = 0f;
    private float currentAngleDeg = 0f;

    public CompassWidget(Style style, TextureRegion base, TextureRegion ring, TextureRegion needle) {
        this.style = style == null ? new Style() : style;
        this.baseTex = base;
        this.ringTex = ring;
        this.needleTex = needle;
        initPixel();

        // Initialize to target to avoid a big first-frame snap
        this.currentAngleDeg = this.targetAngleDeg;
    }

    /** Convenience ctor using existing assets in this repo: Compass.png + Pointer.png.
     *  We reuse Compass.png for both base and ring (with tint overlays), and Pointer.png as needle.
     */
    public static CompassWidget fromDefaultAssets(Style style) {
        TextureRegion compass = null;
        TextureRegion pointer = null;
        try {
            compass = new TextureRegion(new Texture(Gdx.files.internal("Compass.png")));
            pointer = new TextureRegion(new Texture(Gdx.files.internal("Pointer.png")));
        } catch (Exception ignored) {
            // Placeholder mode will kick in
        }
        return new CompassWidget(style, compass, compass, pointer);
    }

    /** Set target angle in degrees. 0=right, 90=up (libGDX convention). */
    public void setTargetAngle(float degrees) {
        targetAngleDeg = normalizeDeg(degrees);
    }

    /** Set target direction vector. Vector does not have to be normalized. */
    public void setTargetDirection(Vector2 dir) {
        if (dir == null || dir.isZero(0.0001f)) return;
        setTargetAngle(MathUtils.atan2(dir.y, dir.x) * MathUtils.radiansToDegrees);
    }

    /**
     * Smoothly approaches target angle. Call once per frame.
     */
    public void update(float dt) {
        // Shortest-path angular smoothing
        float delta = MathUtils.atan2(
                        MathUtils.sinDeg(targetAngleDeg - currentAngleDeg),
                        MathUtils.cosDeg(targetAngleDeg - currentAngleDeg))
                * MathUtils.radiansToDegrees;

        // Exponential damped smoothing: current += delta * (1 - e^(-k*dt))
        float k = Math.max(0f, style.rotationSmoothing);
        float a = 1f - (float) Math.exp(-k * dt);
        currentAngleDeg = normalizeDeg(currentAngleDeg + delta * a);
    }

    /**
     * Render anchored at top-left of the given viewport/stage camera world.
     * Assumes batch is NOT begun; this method will begin/end.
     */
    public void render(SpriteBatch batch, float viewportWorldWidth, float viewportWorldHeight) {
        float x = style.margin;
        float y;

        float size = getVisualSize();
        y = viewportWorldHeight - size - style.margin;

        batch.begin();
        renderAt(batch, x, y, size);
        batch.end();
    }

    /** Render at a given bottom-left position in UI/world units. Assumes batch is begun. */
    public void renderAt(SpriteBatch batch, float x, float y, float size) {
        if (baseTex == null || ringTex == null || needleTex == null) {
            renderPlaceholder(batch, x, y, size);
            return;
        }

        // Keep pixel-art crisp
        baseTex.getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        ringTex.getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        needleTex.getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        // 1) Base plate
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(style.metalTint.r * 0.95f, style.metalTint.g * 0.95f, style.metalTint.b * 0.95f, 1f);
        batch.draw(baseTex, x, y, size, size);

        // 2) Fake inner shadow (no shader): draw a dark overlay slightly inset
        // This creates depth, like a recessed plate.
        if (style.innerShadow > 0.001f) {
            batch.setColor(0f, 0f, 0f, 0.35f * style.innerShadow);
            float inset = Math.max(1f, size * 0.06f);
            batch.draw(pixelTex, x + inset, y + inset, size - inset * 2f, size - inset * 2f);
        }

        // 3) Bezel / ring
        batch.setColor(style.metalTint);
        batch.draw(ringTex, x, y, size, size);

        // 4) Bezel highlight: a soft-ish top-left highlight using a translucent overlay
        // Pixel-art friendly: simple rectangle overlay, not a gradient blur.
        if (style.bezelHighlight > 0.001f) {
            batch.setColor(style.glowTint.r, style.glowTint.g, style.glowTint.b, 0.18f * style.bezelHighlight);
            float h = size * 0.30f;
            batch.draw(pixelTex, x, y + size - h, size, h);
        }

        // 5) Ticks: subtle tick marks (cool glow), using tiny rectangles
        drawTicks(batch, x, y, size);

        // 6) Needle: draw twice (colored + bright highlight with additive blending)
        float needleW = needleTex.getRegionWidth() * style.scale;
        float needleH = needleTex.getRegionHeight() * style.scale;

        // position needle centered inside the compass
        float cx = x + size / 2f;
        float cy = y + size / 2f;

        // Base needle (normal blending)
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(1f, 1f, 1f, style.needleAlpha);
        batch.draw(
                needleTex,
                cx - needleW / 2f,
                cy - needleH / 2f,
                needleW / 2f,
                needleH / 2f,
                needleW,
                needleH,
                1f,
                1f,
                currentAngleDeg + 90f // asset orientation correction (keep old behavior)
        );

        // Highlight pass (additive) – subtle glow
        if (style.needleHighlight > 0.001f) {
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            batch.setColor(style.glowTint.r, style.glowTint.g, style.glowTint.b, style.needleHighlight * 0.35f);
            batch.draw(
                    needleTex,
                    cx - needleW / 2f,
                    cy - needleH / 2f,
                    needleW / 2f,
                    needleH / 2f,
                    needleW,
                    needleH,
                    1f,
                    1f,
                    currentAngleDeg + 90f
            );
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }

        batch.setColor(Color.WHITE);
    }

    private void drawTicks(SpriteBatch batch, float x, float y, float size) {
        // 8 ticks (N, NE, E, ...), pixel-ish small rectangles
        float cx = x + size / 2f;
        float cy = y + size / 2f;
        float rOuter = size * 0.48f;
        float rInner = size * 0.40f;

        batch.setColor(style.glowTint.r, style.glowTint.g, style.glowTint.b, 0.28f);
        for (int i = 0; i < 8; i++) {
            float ang = i * 45f;
            float ox = MathUtils.cosDeg(ang);
            float oy = MathUtils.sinDeg(ang);
            float x1 = cx + ox * rInner;
            float y1 = cy + oy * rInner;
            float x2 = cx + ox * rOuter;
            float y2 = cy + oy * rOuter;

            // Small thickness
            float t = Math.max(1f, size * 0.03f);
            // draw as a rotated segment using pixelTex
            drawSegment(batch, x1, y1, x2, y2, t);
        }

        // major ticks (N/E/S/W) brighter
        batch.setColor(style.glowTint.r, style.glowTint.g, style.glowTint.b, 0.42f);
        for (int i = 0; i < 4; i++) {
            float ang = i * 90f;
            float ox = MathUtils.cosDeg(ang);
            float oy = MathUtils.sinDeg(ang);
            float x1 = cx + ox * (size * 0.36f);
            float y1 = cy + oy * (size * 0.36f);
            float x2 = cx + ox * (size * 0.49f);
            float y2 = cy + oy * (size * 0.49f);
            float t = Math.max(1f, size * 0.04f);
            drawSegment(batch, x1, y1, x2, y2, t);
        }

        batch.setColor(Color.WHITE);
    }

    private void drawSegment(SpriteBatch batch, float x1, float y1, float x2, float y2, float thickness) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.01f) return;
        float ang = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;

        // Pixel snap
        x1 = MathUtils.floor(x1);
        y1 = MathUtils.floor(y1);

        batch.draw(
                pixelTex,
                x1, y1,
                0f, thickness / 2f,
                len, thickness,
                1f, 1f,
                ang,
                0, 0,
                1, 1,
                false, false
        );
    }

    private void renderPlaceholder(SpriteBatch batch, float x, float y, float size) {
        // A pixel-art-ish placeholder: dark circle plate + ring + needle segment
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Base plate
        batch.setColor(0.10f, 0.10f, 0.12f, 0.85f);
        batch.draw(pixelTex, x, y, size, size);

        // Ring (border)
        batch.setColor(0.55f, 0.55f, 0.60f, 0.9f);
        float b = Math.max(2f, size * 0.08f);
        // top
        batch.draw(pixelTex, x, y + size - b, size, b);
        // bottom
        batch.draw(pixelTex, x, y, size, b);
        // left
        batch.draw(pixelTex, x, y, b, size);
        // right
        batch.draw(pixelTex, x + size - b, y, b, size);

        // Needle
        float cx = x + size / 2f;
        float cy = y + size / 2f;
        float len = size * 0.42f;
        float nx = cx + MathUtils.cosDeg(currentAngleDeg) * len;
        float ny = cy + MathUtils.sinDeg(currentAngleDeg) * len;

        batch.setColor(1f, 1f, 1f, 0.9f);
        drawSegment(batch, cx, cy, nx, ny, Math.max(2f, size * 0.06f));

        // Glow
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        batch.setColor(0.55f, 0.85f, 1f, 0.25f);
        drawSegment(batch, cx, cy, nx, ny, Math.max(3f, size * 0.08f));

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(Color.WHITE);
    }

    public float getVisualSize() {
        if (baseTex != null) {
            return baseTex.getRegionWidth() * style.scale;
        }
        return 64f * style.scale;
    }

    public float getCurrentAngleDeg() {
        return currentAngleDeg;
    }

    public float getTargetAngleDeg() {
        return targetAngleDeg;
    }

    public void dispose() {
        // Only dispose our fallback pixel. Textures themselves are owned by HUD and may be reused.
        disposePixel();
    }

    private static float normalizeDeg(float d) {
        d %= 360f;
        if (d < 0f) d += 360f;
        return d;
    }

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

    private static synchronized void disposePixel() {
        pixelRefCount--;
        if (pixelRefCount <= 0 && pixelTex != null) {
            pixelTex.dispose();
            pixelTex = null;
            pixelRefCount = 0;
        }
    }
}

