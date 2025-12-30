package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

/** Displays background messages during gameplay. */
public class StoryScreen {
    private static StoryScreen instance;
    private final BitmapFont font;
    private final float FADE_IN_TIME = 1.0f;
    private final float DISPLAY_TIME = 3.0f;
    private final float FADE_OUT_TIME = 1.0f;
    private final Stage stage;
    private final GlyphLayout glyphLayout;
    private String message;
    private float alpha;
    private boolean showing;
    private float timer;

    // Base screen dimensions for scaling reference
    private static final float BASE_SCREEN_WIDTH = 1280f;
    private static final float BASE_FONT_SCALE = 2.0f;

    /** To tell background story and no key information */
    public StoryScreen() {
        font = new BitmapFont();
        font.setColor(new Color(1, 1, 1, 0)); // Initial fully transparent
        // Font scale will be set dynamically in render() based on screen size
        showing = false;
        alpha = 0;
        stage = new Stage(new ScreenViewport());
        glyphLayout = new GlyphLayout();
    }

    public static StoryScreen getInstance() {
        if (instance == null) {
            instance = new StoryScreen();
        }
        return instance;
    }

    public void showMessage(String text) {
        if (showing && message.equals(text)) {
            return; // Avoid repetition of messages
        }
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

        // Update viewport to match current screen size
        stage.getViewport().update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        batch.setProjectionMatrix(stage.getViewport().getCamera().combined);
        batch.begin();

        float screenWidth = stage.getViewport().getWorldWidth();
        float screenHeight = stage.getViewport().getWorldHeight();

        // Calculate dynamic font scale based on screen width
        float scaleFactor = Math.max(0.8f, Math.min(3.0f, screenWidth / BASE_SCREEN_WIDTH * BASE_FONT_SCALE));
        font.getData().setScale(scaleFactor);
        font.setColor(1, 1, 1, alpha);

        // Position text on the right side of the screen
        float marginRight = screenWidth * 0.05f; // 5% margin from right edge
        float marginTop = screenHeight * 0.15f;

        // Use larger text width on smaller screens to ensure text fits
        float textWidthPercent = screenWidth < 800 ? 0.6f : (screenWidth < 1200 ? 0.45f : 0.35f);
        float textWidth = screenWidth * textWidthPercent;

        // Use GlyphLayout for proper text measurement and wrapping
        glyphLayout.setText(font, message, font.getColor(), textWidth, Align.right, true);

        // Position text on right side of screen, upper portion
        float offsetX = screenWidth - textWidth - marginRight;
        float offsetY = screenHeight - marginTop;

        font.draw(batch, glyphLayout, offsetX, offsetY);
        batch.end();
    }

    public boolean isShowing() {
        return showing;
    }
}
