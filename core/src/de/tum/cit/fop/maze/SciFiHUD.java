package de.tum.cit.fop.maze;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Futuristic HUD (spaceship/helmet style). Uses only a 1x1 white pixel + font (no per-frame allocations).
 * Layout: top-left segmented energy bar, and modular status panels for Key / Shield / Speed.
 */
public class SciFiHUD {

    // -------- Tunable colors --------
    public final Color primary = new Color(0.12f, 0.92f, 1.0f, 1f); // main cyan
    public final Color accent  = new Color(0.20f, 0.55f, 0.95f, 1f); // secondary blue
    public final Color warn    = new Color(1.00f, 0.40f, 0.35f, 1f); // low health

    // -------- Layout tunables --------
    public float barWidth = 260f;
    public float barHeight = 18f;
    public int barSegments = 12;
    public float barGap = 3.5f;
    public float panelWidth = 130f;
    public float panelHeight = 40f;
    public float padding = 12f;

    // -------- Visual effects --------
    public float bgAlpha = 0.22f;
    public float glowAlpha = 0.18f;
    public float outlineAlpha = 0.65f;
    public float pulseFreq = 3.2f; // Hz, low-health pulse

    // -------- State --------
    private float health01 = 1f;
    private boolean hasKey = false;
    private boolean shieldActive = false;
    private float speedValue = 0f;

    // -------- Rendering assets --------
    private final Texture pixel;
    private final BitmapFont font;
    private final GlyphLayout layout = new GlyphLayout();
    private final StringBuilder sb = new StringBuilder(32);

    private float time = 0f;

    public SciFiHUD(BitmapFont font) {
        this.font = font;
        this.pixel = makePixel();
    }

    private Texture makePixel() {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        Texture t = new Texture(pm);
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pm.dispose();
        return t;
    }

    // -------- State setters --------
    public void setHealth(float current, float max) {
        health01 = max <= 0 ? 0 : MathUtils.clamp(current / max, 0f, 1f);
    }

    public void setKeyCollected(boolean hasKey) { this.hasKey = hasKey; }
    public void setShieldActive(boolean active) { this.shieldActive = active; }
    public void setSpeed(float speed) { this.speedValue = speed; }

    public void update(float dt) {
        time += dt;
    }

    /** Render HUD using HUD camera projection. Caller must have set the batch projection matrix. */
    public void render(SpriteBatch batch, Viewport hudViewport) {
        float vpw = hudViewport.getWorldWidth();
        float vph = hudViewport.getWorldHeight();

        float gapPanels = 10f;
        float totalPanelsW = panelWidth * 3f + gapPanels * 2f;

        float barX = (vpw - barWidth) * 0.5f;
        float barY = vph - padding - barHeight - 10f;

        float panelY = barY - panelHeight - 12f;
        float panelX0 = (vpw - totalPanelsW) * 0.5f;
        float panelX1 = panelX0 + panelWidth + gapPanels;
        float panelX2 = panelX1 + panelWidth + gapPanels;

        // Draw health bar
        drawHealthBar(batch, barX, barY);

        // Draw panels
        drawPanel(batch, panelX0, panelY, panelWidth, panelHeight,
                "KEY", hasKey ? "UNLOCK" : "LOCKED",
                hasKey ? primary : accent, hasKey ? primary : accent);

        drawPanel(batch, panelX1, panelY, panelWidth, panelHeight,
                "SHIELD", shieldActive ? "ONLINE" : "OFFLINE",
                shieldActive ? primary : accent, shieldActive ? primary : accent);

        sb.setLength(0);
        sb.append("SPD ");
        sb.append(MathUtils.round(speedValue));
        sb.append(" u/s");
        drawPanel(batch, panelX2, panelY, panelWidth, panelHeight,
                "SPEED", sb.toString(), primary, primary);
    }

    // -------- Drawing primitives --------
    private void drawHealthBar(SpriteBatch batch, float x, float y) {
        float h = barHeight;
        float segW = (barWidth - barGap * (barSegments - 1)) / barSegments;
        float filledSegments = health01 * barSegments;

        // Low-health pulse
        float pulse = health01 < 0.32f ? (0.55f + 0.45f * MathUtils.sin(time * pulseFreq * MathUtils.PI2)) : 1f;
        Color segColor = (health01 < 0.32f)
                ? new Color(warn.r, warn.g, warn.b, 0.9f * pulse)
                : new Color(primary.r, primary.g, primary.b, 0.9f);

        // Background block
        batch.setColor(primary.r, primary.g, primary.b, bgAlpha);
        batch.draw(pixel, x - 6f, y - 6f, barWidth + 12f, h + 12f);

        // Segments + inner glow
        for (int i = 0; i < barSegments; i++) {
            float sx = x + i * (segW + barGap);
            boolean filled = i + 1 <= filledSegments;
            float alpha = filled ? segColor.a : 0.2f;

            batch.setColor(segColor.r, segColor.g, segColor.b, alpha);
            batch.draw(pixel, sx, y, segW, h);

            // Inner glow strip
            batch.setColor(segColor.r, segColor.g, segColor.b, alpha * glowAlpha * 2.2f);
            batch.draw(pixel, sx, y + h * 0.58f, segW, h * 0.18f);
        }

        // Outline frame
        batch.setColor(primary.r, primary.g, primary.b, outlineAlpha);
        batch.draw(pixel, x - 2f, y - 2f, barWidth + 4f, 2f);      // top
        batch.draw(pixel, x - 2f, y + h, barWidth + 4f, 2f);       // bottom
        batch.draw(pixel, x - 2f, y - 2f, 2f, h + 4f);             // left
        batch.draw(pixel, x + barWidth, y - 2f, 2f, h + 4f);       // right
    }

    private void drawPanel(SpriteBatch batch, float x, float y, float w, float h,
                           String title, String value, Color edgeColor, Color textColor) {
        float pad = 7f;

        // Background
        batch.setColor(edgeColor.r, edgeColor.g, edgeColor.b, bgAlpha);
        batch.draw(pixel, x, y, w, h);

        // Holo stripe
        batch.setColor(edgeColor.r, edgeColor.g, edgeColor.b, glowAlpha);
        batch.draw(pixel, x, y + h * 0.64f, w, h * 0.14f);

        // Border
        batch.setColor(edgeColor.r, edgeColor.g, edgeColor.b, outlineAlpha);
        batch.draw(pixel, x - 1f, y - 1f, w + 2f, 1f);
        batch.draw(pixel, x - 1f, y + h, w + 2f, 1f);
        batch.draw(pixel, x - 1f, y - 1f, 1f, h + 2f);
        batch.draw(pixel, x + w, y - 1f, 1f, h + 2f);

        // Text
        font.setColor(textColor);
        layout.setText(font, title);
        font.draw(batch, title, x + pad, y + h - pad);

        layout.setText(font, value);
        font.draw(batch, value, x + pad, y + pad + layout.height);
    }

    public void dispose() {
        pixel.dispose();
    }
}

