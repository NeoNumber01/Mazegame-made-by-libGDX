package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

/**
 * Handles the fog of war effect that limits player visibility.
 * Creates a circular visible area around the player with smooth edge transitions.
 */
public class FogOfWar {
    private final ShapeRenderer shapeRenderer;
    private final Texture gradientTexture;

    // Configuration
    private float visibleRadius = 125f;

    /**
     * Creates a new FogOfWar instance.
     */
    public FogOfWar() {
        shapeRenderer = new ShapeRenderer();
        gradientTexture = createGradientTexture(512);
    }

    /**
     * Creates a large circular gradient texture for smooth lighting effect.
     * Center is brightest (fully transparent), naturally fades to pure black at edge.
     * Designed for atmospheric effect with a large dim/grey transition area.
     */
    private Texture createGradientTexture(int size) {
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);

        float center = size / 2f;
        // Visible edge covers most of the texture
        float visibleEdge = center * 0.95f;
        // Small bright core (15% of visible radius) to ensure center is clear
        float brightCore = visibleEdge * 0.05f;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = x - center;
                float dy = y - center;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                float alpha;
                if (dist >= visibleEdge) {
                    // Outside visible area - fully dark (pure black)
                    alpha = 1f;
                } else if (dist <= brightCore) {
                    // Bright center core - fully transparent
                    alpha = 0f;
                } else {
                    // Transition zone from bright core to dark edge
                    float t = (dist - brightCore) / (visibleEdge - brightCore);

                    // "Grey/dim area largest" -> Use a power curve < 1
                    // This makes alpha rise quickly from 0 to grey levels,
                    // creating a large area of partial darkness (atmosphere)
                    alpha = (float) Math.pow(t, 0.75f);
                }

                pixmap.setColor(0, 0, 0, alpha);
                pixmap.drawPixel(x, y);
            }
        }

        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();

        return texture;
    }

    /**
     * Renders the fog of war effect around the given position.
     * Uses stencil buffer for fog coverage and gradient for smooth lighting.
     *
     * @param batch The sprite batch to use for rendering
     * @param camera The camera to use for projection
     * @param centerX X position of the visibility center (usually player position)
     * @param centerY Y position of the visibility center
     */
    public void render(SpriteBatch batch, OrthographicCamera camera, float centerX, float centerY) {
        // Enable stencil testing
        Gdx.gl.glEnable(GL20.GL_STENCIL_TEST);
        Gdx.gl.glClearStencil(0);
        Gdx.gl.glClear(GL20.GL_STENCIL_BUFFER_BIT);

        // Step 1: Draw circle to stencil buffer (mark visible area)
        Gdx.gl.glColorMask(false, false, false, false);
        Gdx.gl.glStencilFunc(GL20.GL_ALWAYS, 1, 0xFF);
        Gdx.gl.glStencilOp(GL20.GL_KEEP, GL20.GL_KEEP, GL20.GL_REPLACE);

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.circle(centerX, centerY, visibleRadius);
        shapeRenderer.end();

        // Step 2: Draw black fog where stencil is 0 (outside visible circle)
        Gdx.gl.glColorMask(true, true, true, true);
        Gdx.gl.glStencilFunc(GL20.GL_EQUAL, 0, 0xFF);
        Gdx.gl.glStencilOp(GL20.GL_KEEP, GL20.GL_KEEP, GL20.GL_KEEP);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Draw large black rectangle covering entire viewport
        float coverSize = Math.max(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()) * camera.zoom * 2f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 1f);
        shapeRenderer.rect(
            centerX - coverSize,
            centerY - coverSize,
            coverSize * 2f,
            coverSize * 2f
        );
        shapeRenderer.end();

        // Step 3: Draw gradient texture over visible area for lighting effect
        Gdx.gl.glStencilFunc(GL20.GL_EQUAL, 1, 0xFF);

        // Gradient size: texture's 95% = visibleRadius, so full size = visibleRadius / 0.95 * 2
        float gradientSize = visibleRadius * 2.1f;

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(
            gradientTexture,
            centerX - gradientSize / 2f,
            centerY - gradientSize / 2f,
            gradientSize,
            gradientSize
        );
        batch.end();

        // Disable stencil testing
        Gdx.gl.glDisable(GL20.GL_STENCIL_TEST);
    }

    /**
     * Convenience method using Vector2 for position.
     */
    public void render(SpriteBatch batch, OrthographicCamera camera, Vector2 center) {
        render(batch, camera, center.x, center.y);
    }

    /**
     * Sets the visible radius around the player.
     */
    public void setVisibleRadius(float radius) {
        this.visibleRadius = radius;
    }

    /**
     * Gets the current visible radius.
     */
    public float getVisibleRadius() {
        return visibleRadius;
    }

    /**
     * Disposes of resources used by the fog of war.
     */
    public void dispose() {
        shapeRenderer.dispose();
        gradientTexture.dispose();
    }
}
