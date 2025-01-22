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

    public void refresh() {
        camera.update(); // Update the camera

        // Set up and begin drawing with the sprite batch
        game.getSpriteBatch().setProjectionMatrix(camera.combined);
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
