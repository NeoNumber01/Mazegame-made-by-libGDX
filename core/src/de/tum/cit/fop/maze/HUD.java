package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/** In-game HUD, displays health information, etc. */
public class HUD {
    private final SpriteBatch spriteBatch;
    private final Viewport viewport;

    // Futuristic HUD modules
    private final SciFiHUD sciFiHUD;
    private final SciFiCompassHUD compassHUD;

    public HUD(SpriteBatch spriteBatch) {
        this.spriteBatch = spriteBatch;

        // HUD viewport (screen space)
        viewport = new ExtendViewport(1280, 720);

        // Fonts: use default; replace with your sci-fi font if desired
        BitmapFont hudFont = new BitmapFont();

        // HUD components
        sciFiHUD = new SciFiHUD(hudFont);
        compassHUD = new SciFiCompassHUD();
    }

    public void update(
            int health, boolean hasKey, float speed, boolean hasShield, float pointerDegree) {
        sciFiHUD.setHealth(health, 100f); // assuming max health 100; adjust if different
        sciFiHUD.setKeyCollected(hasKey);
        sciFiHUD.setShieldActive(hasShield);
        sciFiHUD.setSpeed(speed);

        compassHUD.setTargetAngle(pointerDegree);
    }

    /** Call once per frame so modules can animate. */
    public void onFrame(float delta) {
        sciFiHUD.update(delta);
        compassHUD.update(delta);
    }

    public void render() {
        viewport.apply();
        spriteBatch.setProjectionMatrix(viewport.getCamera().combined);

        // Render Sci-Fi HUD (expects batch already begun)
        spriteBatch.begin();
        sciFiHUD.render(spriteBatch, viewport);
        spriteBatch.end();

        // Render compass HUD separately (it manages its own begin/end)
        compassHUD.render(spriteBatch, viewport.getWorldWidth(), viewport.getWorldHeight());
    }

    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    public void dispose() {
        sciFiHUD.dispose();
        compassHUD.dispose();
    }
}
