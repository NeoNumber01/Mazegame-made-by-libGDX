package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class HUD {
    public Stage stage;
    private Viewport viewport;
    private final OrthographicCamera camera;
    private int health;
    private boolean hasKey;

    private Label keyStatusLabel;
    private Table livesTable;
    private TextureRegion fullHeartTexture;
    private TextureRegion halfHeartTexture;
    private final float viewPointWidth = 1024f;

    public HUD(SpriteBatch spriteBatch) {
        camera = new OrthographicCamera(viewPointWidth, getViewPointHeight());
        float viewportWidth = 800f;
        viewport = new FitViewport(
            viewportWidth,
            viewportWidth * Gdx.graphics.getHeight() / Gdx.graphics.getWidth()
        );
        stage = new Stage(viewport, spriteBatch);

        // Load heart textures
        fullHeartTexture = new TextureRegion(new Texture(Gdx.files.internal("Lives.png")));
        halfHeartTexture = new TextureRegion(new Texture(Gdx.files.internal("halfLives.png")));

        // Create table for displaying hearts
        livesTable = new Table();
        livesTable.top();
        livesTable.setFillParent(false); // Prevent table from filling the entire screen

        // Initialize key status label
        BitmapFont font = new BitmapFont(); // Default font
        Label.LabelStyle labelStyle = new Label.LabelStyle(font, Color.WHITE);
        keyStatusLabel = new Label("Key: Not Collected", labelStyle);

        // Main layout table
        Table mainTable = new Table();
        mainTable.top();
        mainTable.setFillParent(true);

        // Add lives table and key status label to main table
        mainTable.add(livesTable).expandX().padTop(10); // Heart icons
        mainTable.row();
        mainTable.add(keyStatusLabel).expandX().padTop(10); // Key status

        // Add main table to the stage
        stage.addActor(mainTable);

        // Initialize health display
        updateLivesDisplay(100); // Default to full health
    }

    public void update(int health, boolean hasKey) {
        this.health = health;
        this.hasKey = hasKey;

        // Update lives display
        updateLivesDisplay(health);

        // Update key status
        keyStatusLabel.setText("Key: " + (hasKey ? "Collected" : "Not Collected"));
    }

    private void updateLivesDisplay(int health) {
        livesTable.clear(); // Clear table contents
        int fullHearts = health / 20; // Number of full hearts
        boolean hasHalfHeart = (health % 20) >= 10; // Whether to display a half-heart

        // Display full hearts
        for (int i = 0; i < fullHearts; i++) {
            Image fullHeart = new Image(fullHeartTexture);
            livesTable.add(fullHeart).pad(2); // Add full heart with padding
        }

        // Display half-heart
        if (hasHalfHeart) {
            Image halfHeart = new Image(halfHeartTexture);
            livesTable.add(halfHeart).pad(2); // Add half heart with padding
        }
    }

    private float getViewPointHeight() {
        return viewPointWidth * Gdx.graphics.getHeight() / Gdx.graphics.getWidth();
    }

    public void render() {
        stage.draw(); // Render the HUD
    }

    public void resize(int width, int height) {
        viewport.update(width, height, true); // Update viewport
        camera.setToOrtho(false, viewPointWidth, getViewPointHeight()); // Update camera
        stage.getViewport().update(width, height, true); // Update stage viewport
    }

    public void dispose() {
        stage.dispose();
        fullHeartTexture.getTexture().dispose(); // Release full heart texture resource
        halfHeartTexture.getTexture().dispose(); // Release half heart texture resource
    }
}
