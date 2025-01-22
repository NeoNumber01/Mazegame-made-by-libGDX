package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/** In-game HUD, displays health information, etc. */
public class HUD {
    private final SpriteBatch spriteBatch;
    private final float viewPointWidth = 1024f;
    private final Label keyStatusLabel;
    private final Label shieldStatusLabel;
    private final Table livesTable;
    private final Label speedLabel;
    private final TextureRegion fullHeartTexture;
    private final TextureRegion halfHeartTexture;
    private final TextureRegion compassTexture, pointerTexture;
    public Stage stage;
    private float pointerDegree = 0f;
    private int health;
    private boolean hasKey;
    private boolean hasShield;

    public HUD(SpriteBatch spriteBatch) {
        this.spriteBatch = spriteBatch;

        // Although libGDX doc recommends binding the camera to viewport, this breaks currently
        // implementation. So let it be.
        Viewport viewport = new ExtendViewport(1280, 720);
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
        speedLabel = new Label("Speed: 0.0", labelStyle);
        shieldStatusLabel = new Label("Shield: Inactive", labelStyle);
        // Main layout table
        Table mainTable = new Table();
        mainTable.top();
        mainTable.setFillParent(true);

        // Add lives table and key status label to main table
        mainTable.add(livesTable).expandX().padTop(10); // Heart icons
        mainTable.row();
        mainTable.add(keyStatusLabel).expandX().padTop(10); // Key status
        mainTable.row();
        mainTable.add(shieldStatusLabel).expandX().padTop(10);
        mainTable.row();
        mainTable.add(speedLabel).expandX().padTop(10);
        // Add main table to the stage
        stage.addActor(mainTable);

        compassTexture = new TextureRegion(new Texture(Gdx.files.internal("Compass.png")));
        pointerTexture = new TextureRegion(new Texture(Gdx.files.internal("Pointer.png")));

        // Initialize health display
        updateLivesDisplay(100); // Default to full health
    }

    public void update(
            int health, boolean hasKey, float speed, boolean hasShield, float pointerDegree) {
        this.health = health;
        this.hasKey = hasKey;
        this.hasShield = hasShield;
        this.pointerDegree = pointerDegree;
        // Update lives display
        updateLivesDisplay(health);

        // Update key status
        keyStatusLabel.setText("Key: " + (hasKey ? "Collected" : "Not Collected"));
        shieldStatusLabel.setText("Shield: " + (hasShield ? "Active" : "Inactive"));
        speedLabel.setText(String.format("Speed: %.1f", speed));
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
        // 确保罗盘位置固定。需要重新绘制spriteBatch的投影矩阵，
        // 因为战争迷雾使用过
        spriteBatch.setProjectionMatrix(stage.getViewport().getCamera().combined);
        spriteBatch.begin();

        float margin = 20f; // 罗盘离屏幕边缘的距离
        float compassSize = compassTexture.getRegionWidth();
        float pointerSizeX = pointerTexture.getRegionWidth();
        float pointerSizeY = pointerTexture.getRegionHeight();

        // 计算罗盘的位置，始终固定在左上角
        float offsetX = margin; // 靠近左侧
        float offsetY = stage.getViewport().getWorldHeight() - compassSize - margin; // 靠近上侧

        spriteBatch.draw(compassTexture, offsetX, offsetY, compassSize, compassSize);
        spriteBatch.draw(
                pointerTexture,
                offsetX + compassSize / 2f - pointerSizeX / 2f,
                offsetY + compassSize / 2f - pointerSizeY / 2f,
                pointerSizeX / 2f,
                pointerSizeY / 2f,
                pointerSizeX,
                pointerSizeY,
                1f,
                1f,
                pointerDegree + 90);

        spriteBatch.end();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true); // Update stage viewport
    }

    public void dispose() {
        stage.dispose();
        fullHeartTexture.getTexture().dispose(); // Release full heart texture resource
        halfHeartTexture.getTexture().dispose(); // Release half heart texture resource
    }
}
