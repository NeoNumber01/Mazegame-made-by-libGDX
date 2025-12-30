package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Pool;

/**
 * Object pool for SlashEffect instances.
 * Manages creation, reuse, and disposal of slash effects to minimize GC.
 *
 * Usage:
 * 1. Call obtain() to get a SlashEffect, then start() it
 * 2. Call updateAndRender() each frame to update all active effects
 * 3. Finished effects are automatically returned to the pool
 * 4. Call dispose() when done to clean up resources
 */
public class SlashEffectPool implements Disposable {

    private static final int INITIAL_CAPACITY = 8;
    private static final int MAX_CAPACITY = 32;

    private final Pool<SlashEffect> pool;
    private final Array<SlashEffect> activeEffects;
    private Color defaultTint;

    public SlashEffectPool() {
        this.activeEffects = new Array<>(false, INITIAL_CAPACITY);
        this.defaultTint = new Color(0.7f, 0.9f, 1f, 1f); // Light blue default

        this.pool = new Pool<SlashEffect>(INITIAL_CAPACITY, MAX_CAPACITY) {
            @Override
            protected SlashEffect newObject() {
                return new SlashEffect();
            }
        };
    }

    /**
     * Set default tint color for new effects.
     * @param color Tint color (default is light blue)
     */
    public void setDefaultTint(Color color) {
        this.defaultTint.set(color);
    }

    /**
     * Spawn a new slash effect at the given position and angle.
     * @param x Center X position
     * @param y Center Y position
     * @param angleDegrees Direction angle in degrees (0=right, 90=up, 180=left, 270=down)
     * @return The spawned SlashEffect instance
     */
    public SlashEffect spawn(float x, float y, float angleDegrees) {
        SlashEffect effect = pool.obtain();
        effect.setTintColor(defaultTint);
        effect.start(x, y, angleDegrees);
        activeEffects.add(effect);
        return effect;
    }

    /**
     * Spawn a new slash effect with custom tint color.
     * @param x Center X position
     * @param y Center Y position
     * @param angleDegrees Direction angle
     * @param tint Custom tint color
     * @return The spawned SlashEffect instance
     */
    public SlashEffect spawn(float x, float y, float angleDegrees, Color tint) {
        SlashEffect effect = pool.obtain();
        effect.setTintColor(tint);
        effect.start(x, y, angleDegrees);
        activeEffects.add(effect);
        return effect;
    }

    /**
     * Update all active effects and return finished ones to the pool.
     * @param deltaTime Frame delta time in seconds
     */
    public void update(float deltaTime) {
        for (int i = activeEffects.size - 1; i >= 0; i--) {
            SlashEffect effect = activeEffects.get(i);
            effect.update(deltaTime);

            if (effect.isFinished()) {
                activeEffects.removeIndex(i);
                pool.free(effect);
            }
        }
    }

    /**
     * Render all active effects.
     * @param batch SpriteBatch (must already be begun)
     */
    public void render(SpriteBatch batch) {
        for (SlashEffect effect : activeEffects) {
            effect.render(batch);
        }
    }

    /**
     * Update and render all active effects in one call.
     * @param batch SpriteBatch (must already be begun)
     * @param deltaTime Frame delta time in seconds
     */
    public void updateAndRender(SpriteBatch batch, float deltaTime) {
        update(deltaTime);
        render(batch);
    }

    /**
     * Get number of currently active effects.
     */
    public int getActiveCount() {
        return activeEffects.size;
    }

    /**
     * Get number of free (pooled) effects.
     */
    public int getFreeCount() {
        return pool.getFree();
    }

    /**
     * Clear all active effects immediately.
     */
    public void clear() {
        for (SlashEffect effect : activeEffects) {
            pool.free(effect);
        }
        activeEffects.clear();
    }

    @Override
    public void dispose() {
        clear();
        pool.clear();
        SlashEffect.disposeShared();
    }
}

