package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class MazeRunnerCamera {
    private final MazeRunnerGame game;
    private final OrthographicCamera camera;
    private final float viewPointWidth = 1024f;

    public MazeRunnerCamera(MazeRunnerGame game, Vector2 initialPosition) {
        this.game = game;
        camera = new OrthographicCamera(viewPointWidth, getViewPointHeight());
        //        camera.position.set(
        //                camera.viewportWidth / 2 + initialPosition.x,
        //                camera.viewportHeight / 2 + initialPosition.y,
        //                0f);
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
        camera.zoom += 0.1f * deltaTime * scaleMultiplier;
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
