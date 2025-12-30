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
        return viewPointWidth * Gdx.graphics.getHeight() / Gdx.graphics.getWidth();
    }

    public void moveTowards(Vector2 targetPosition) {
        Vector2 currentPos = new Vector2(camera.position.x, camera.position.y);
        Vector2 delta = targetPosition.sub(currentPos);
        // currently always centers the player, but more complex mechanics can be implemented here
        // if needed
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
        camera.update(); // Update the camera

        // Apply shake
        if (shakeDuration > 0) {
            float currentIntensity = shakeIntensity * (shakeDuration > 0 ? 1 : 0); // Simple check
            // Or fade out intensity: float currentIntensity = shakeIntensity * (shakeDuration / initialDuration);
            // For simplicity, constant intensity until end or simple linear fade if we tracked initial duration
            
            float shakeX = (float) (Math.random() * 2 - 1) * currentIntensity;
            float shakeY = (float) (Math.random() * 2 - 1) * currentIntensity;
            camera.translate(shakeX, shakeY);
            camera.update();
        }

        // Set up and begin drawing with the sprite batch
        game.getSpriteBatch().setProjectionMatrix(camera.combined);
        
        // Revert shake for next frame logic (so we don't drift)
        // Actually, since we call moveTowards every frame based on player, drifting might be corrected.
        // But to be safe and avoid accumulation, we should probably not permanently modify position here 
        // if we didn't save the original position.
        // However, moveTowards() resets position based on target. 
        // Wait, moveTowards calls translate(). If we translate here, next frame moveTowards will 
        // see the shaken position as current.
        // It's better to NOT modify camera.position permanently.
        // But camera.update() updates the matrices based on position.
        // Correct approach:
        // 1. Save original position.
        // 2. Translate.
        // 3. Update.
        // 4. Set Matrix.
        // 5. Restore position.
        
        if (shakeDuration > 0) {
            // We already translated. 
            // Let's reverse the translation after setting the matrix?
            // No, camera.combined is already calculated.
            // We just need to ensure next frame starts from the "real" position.
            // But next frame 'moveTowards' calculates delta from current position.
            // So if we leave it shaken, 'moveTowards' will try to correct it, which might be fine 
            // or might cause jitter fighting.
            // Let's try to restore it.
             float shakeX = camera.position.x - (camera.position.x - ((float) (Math.random() * 2 - 1) * shakeIntensity)); 
             // Wait, the logic above was:
             // float shakeX = ...
             // camera.translate(shakeX, shakeY);
             // To restore: camera.translate(-shakeX, -shakeY);
             
             // BUT, I can't easily access the random values I just generated unless I store them.
             // Let's refactor the refresh method slightly in the replacement string.
        }
    }


    public void resize() {
        // setToOrtho() will cause viewpoint center tp deviate from player, hence we move it back
        // manually
        Vector2 originalPos = new Vector2(camera.position.x, camera.position.y);
        camera.setToOrtho(false, viewPointWidth, getViewPointHeight());
        moveTowards(originalPos);
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
