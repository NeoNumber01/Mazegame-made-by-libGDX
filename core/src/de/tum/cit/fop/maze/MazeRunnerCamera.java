package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

/** A simple wrapper of OrthographicCamera */
public class MazeRunnerCamera {
    private final MazeRunnerGame game;
    private final OrthographicCamera camera;
    private final float viewPointWidth = 1024f;
    // 定义缩放的最小值和最大值
    private final float minZoom = 0.9f; // 最小缩放比例
    private final float maxZoom = 1.1f; // 最大缩放比例

    // Screen Shake
    private float shakeDuration = 0f;
    private float shakeIntensity = 0f;

    public MazeRunnerCamera(MazeRunnerGame game, Vector2 initialPosition) {
        this.game = game;
        camera = new OrthographicCamera(viewPointWidth, getViewPointHeight());
        moveTowards(initialPosition);
    }

    private float getViewPointHeight() {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        if (w <= 0 || h <= 0) return viewPointWidth * 9f / 16f; // Fallback to 16:9
        return viewPointWidth * h / w;
    }

    public void moveTowards(Vector2 targetPosition) {
        Vector2 currentPos = new Vector2(camera.position.x, camera.position.y);
        Vector2 delta = targetPosition.sub(currentPos);
        camera.translate(delta);
    }

    public void shake(float duration, float intensity) {
        this.shakeDuration = duration;
        this.shakeIntensity = intensity;
    }

    public void update(float deltaTime) {
        if (shakeDuration > 0) {
            shakeDuration -= deltaTime;
            if (shakeDuration < 0) shakeDuration = 0;
        }
    }

    public void refresh() {
        camera.update(); // Update the camera base state

        float shakeX = 0;
        float shakeY = 0;

        // Apply shake
        if (shakeDuration > 0) {
            float currentIntensity = shakeIntensity; 
            shakeX = (float) (Math.random() * 2 - 1) * currentIntensity;
            shakeY = (float) (Math.random() * 2 - 1) * currentIntensity;
            camera.translate(shakeX, shakeY);
            camera.update();
        }

        // Set up and begin drawing with the sprite batch
        game.getSpriteBatch().setProjectionMatrix(camera.combined);
        
        // Restore position so the shake doesn't accumulate/drift the camera permanently
        if (shakeDuration > 0) {
            camera.translate(-shakeX, -shakeY);
            // We don't call update() here, so 'camera' object technically remains at base position
            // for next logic operations (like unproject), but the Matrix passed to SpriteBatch
            // was the shaken one.
            // Wait, if we translate back but don't update, the internal matrices are still shaken?
            // No, translate() updates the position vector. update() recalculates the matrices from the position vector.
            // So:
            // 1. translate(shake) -> pos modified
            // 2. update() -> combined matrix updated with shake
            // 3. setProjectionMatrix(combined) -> renderer gets shaken view
            // 4. translate(-shake) -> pos restored to original
            // 5. update() -> combined matrix restored to original (ready for logic)
            camera.update();
        }
    }


    public void resize() {
        // Avoid resizing if dimensions are invalid (minimized)
        if (Gdx.graphics.getWidth() <= 0 || Gdx.graphics.getHeight() <= 0) return;

        // setToOrtho() will cause viewpoint center tp deviate from player, hence we move it back
        // manually
        Vector2 originalPos = new Vector2(camera.position.x, camera.position.y);
        camera.setToOrtho(false, viewPointWidth, getViewPointHeight());
        
        // Only move back if the original position was valid (not NaN/Inf)
        if (!Float.isNaN(originalPos.x) && !Float.isNaN(originalPos.y)) {
            moveTowards(originalPos);
        }
    }

    public void zoom(float deltaTime, float scaleMultiplier) {
        float newZoom = camera.zoom + 0.1f * deltaTime * scaleMultiplier;
        if (newZoom >= minZoom && newZoom <= maxZoom) {
            camera.zoom = newZoom;
        }
    }

    public void resetZoom() {
        camera.zoom = 1f;
    }

    public Vector3 project(Vector3 position) {
        return camera.project(position);
    }

    public OrthographicCamera getCamera() {
        return camera;
    }
}
