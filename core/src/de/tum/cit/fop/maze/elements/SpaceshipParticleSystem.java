package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

/**
 * Lightweight thruster particle system (no ParticleEditor).
 * Uses a 1x1 pixel texture and additive blending.
 */
public class SpaceshipParticleSystem {

    private static final int MAX = 120;

    private static Texture pixel;

    // ring buffer
    private final float[] x = new float[MAX];
    private final float[] y = new float[MAX];
    private final float[] vx = new float[MAX];
    private final float[] vy = new float[MAX];
    private final float[] life = new float[MAX];
    private final float[] maxLife = new float[MAX];
    private final float[] size = new float[MAX];

    private int head = 0;

    private final Color c = new Color();

    public SpaceshipParticleSystem() {
        ensurePixel();
    }

    private static void ensurePixel() {
        if (pixel != null) return;
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        pixel = new Texture(pm);
        pixel.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pm.dispose();
    }

    public void emit(float px, float py, float dirX, float dirY, int count) {
        // dir = ship forward; thruster goes backward
        float tx = -dirX;
        float ty = -dirY;

        for (int i = 0; i < count; i++) {
            int idx = head;
            head = (head + 1) % MAX;

            float spread = 35f;
            float ang = MathUtils.atan2(ty, tx) * MathUtils.radiansToDegrees + MathUtils.random(-spread, spread);
            float spd = MathUtils.random(60f, 160f);

            x[idx] = px + MathUtils.random(-2f, 2f);
            y[idx] = py + MathUtils.random(-2f, 2f);
            vx[idx] = MathUtils.cosDeg(ang) * spd;
            vy[idx] = MathUtils.sinDeg(ang) * spd;
            maxLife[idx] = life[idx] = MathUtils.random(0.15f, 0.35f);
            size[idx] = MathUtils.random(2f, 5f);
        }
    }

    public void update(float dt) {
        for (int i = 0; i < MAX; i++) {
            if (life[i] <= 0f) continue;
            life[i] -= dt;
            x[i] += vx[i] * dt;
            y[i] += vy[i] * dt;
            vx[i] *= 0.90f;
            vy[i] *= 0.90f;
        }
    }

    public void render(SpriteBatch batch) {
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

        for (int i = 0; i < MAX; i++) {
            if (life[i] <= 0f) continue;
            float a = life[i] / maxLife[i];

            // cyan -> white core
            c.set(0.15f, 0.85f, 1f, 0.55f * a);
            batch.setColor(c);
            float s = size[i] * (0.8f + 0.6f * (1f - a));
            batch.draw(pixel, x[i] - s / 2f, y[i] - s / 2f, s, s);

            c.set(1f, 1f, 1f, 0.20f * a);
            batch.setColor(c);
            batch.draw(pixel, x[i] - s / 4f, y[i] - s / 4f, s / 2f, s / 2f);
        }

        batch.setColor(Color.WHITE);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }
}

