package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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
    private final Texture livesTexture;
    private final float viewPointWidth = 1024f;

    public HUD(SpriteBatch spriteBatch) {
        camera = new OrthographicCamera(viewPointWidth, getViewPointHeight());
        float viewportWidth = 800f;
        viewport = new FitViewport(
            viewportWidth,
            viewportWidth * Gdx.graphics.getHeight() / Gdx.graphics.getWidth()
        );
        stage = new Stage(viewport, spriteBatch);

        // 加载生命图标纹理
        livesTexture = new Texture(Gdx.files.internal("Lives.png"));

        // 创建显示生命图标的表格
        livesTable = new Table();
        livesTable.top();
        livesTable.setFillParent(false); // 防止填充整个屏幕

        // 初始化钥匙状态标签
        BitmapFont font = new BitmapFont(); // 默认字体
        Label.LabelStyle labelStyle = new Label.LabelStyle(font, Color.WHITE);
        keyStatusLabel = new Label("Key: Not Collected", labelStyle);

        // 主布局表格
        Table mainTable = new Table();
        mainTable.top();
        mainTable.setFillParent(true);

        // 添加 Lives 表格和钥匙状态标签到主表格
        mainTable.add(livesTable).expandX().padTop(10); // 生命图标
        mainTable.row();
        mainTable.add(keyStatusLabel).expandX().padTop(10); // 钥匙状态

        // 将主表格添加到舞台
        stage.addActor(mainTable);

        // 初始化健康值显示
        updateLivesDisplay(100); // 默认满血
    }

    public void update(int health, boolean hasKey) {
        this.health = health;
        this.hasKey = hasKey;

        // 更新生命显示
        updateLivesDisplay(health);

        // 更新钥匙状态
        keyStatusLabel.setText("Key: " + (hasKey ? "Collected" : "Not Collected"));
    }

    private void updateLivesDisplay(int health) {
        livesTable.clear(); // 清空表格内容

        int numberOfLives = health / 20; // 每 20 点健康值显示一个图标
        for (int i = 0; i < numberOfLives; i++) {
            Image lifeImage = new Image(livesTexture);
            livesTable.add(lifeImage).pad(2); // 添加图标并设置间距
        }
    }

    private float getViewPointHeight() {
        return viewPointWidth * Gdx.graphics.getHeight() / Gdx.graphics.getWidth();
    }

    public void render() {
        stage.draw(); // 绘制 HUD
    }

    public void resize() {
        camera.setToOrtho(false, viewPointWidth, getViewPointHeight());
    }

    public void dispose() {
        stage.dispose();
        livesTexture.dispose(); // 释放纹理资源
    }
}
