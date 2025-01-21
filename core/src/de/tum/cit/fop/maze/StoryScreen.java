package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class StoryScreen {
    private static StoryScreen instance;
    private final BitmapFont font;
    private final float FADE_IN_TIME = 1.0f;
    private final float DISPLAY_TIME = 3.0f;
    private final float FADE_OUT_TIME = 1.0f;
    private final Stage stage;
    private String message;
    private float alpha;
    private boolean showing;
    private float timer;

    public StoryScreen() {
        font = new BitmapFont();
        font.setColor(new Color(1, 1, 1, 0)); // Initial fully transparent
        font.getData().setScale(1.5f);
        showing = false;
        alpha = 0;
        stage = new Stage(new ScreenViewport());
    }

    public static StoryScreen getInstance() {
        if (instance == null) {
            instance = new StoryScreen();
        }
        return instance;
    }

    public void showMessage(String text) {
        message = text;
        timer = 0;
        showing = true;
        alpha = 0; // Start fade-in effect
        Gdx.app.log("StoryScreen", "Displaying message: " + message);
    }

    public void update(float delta) {
        if (showing) {
            timer += delta;

            if (timer < FADE_IN_TIME) {
                alpha = timer / FADE_IN_TIME;
            } else if (timer < FADE_IN_TIME + DISPLAY_TIME) {
                alpha = 1;
            } else if (timer < FADE_IN_TIME + DISPLAY_TIME + FADE_OUT_TIME) {
                alpha = 1 - (timer - FADE_IN_TIME - DISPLAY_TIME) / FADE_OUT_TIME;
            } else {
                showing = false;
                alpha = 0;
            }

            font.setColor(1, 1, 1, alpha);
        }
    }

    public void render(SpriteBatch batch) {
        if (!showing) return;

        batch.setProjectionMatrix(stage.getViewport().getCamera().combined);
        batch.begin();

        float margin = 20f; // Margin from screen edge
        float textWidth = font.getRegion().getRegionWidth();
        float offsetX = stage.getViewport().getWorldWidth() - textWidth - margin; // Right side
        float offsetY = stage.getViewport().getWorldHeight() - margin; // Top side

        font.draw(batch, message, offsetX, offsetY);
        batch.end();
    }

    public boolean isShowing() {
        return showing;
    }
}
