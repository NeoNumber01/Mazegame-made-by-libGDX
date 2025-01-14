package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
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

    private Label healthLabel;
    private Label keyStatusLabel;
    private final float viewPointWidth = 1024f;

    public HUD(SpriteBatch spriteBatch) {
        camera = new OrthographicCamera(viewPointWidth, getViewPointHeight());
        // 设置视口和舞台
        float viewportWidth = 800f;
        viewport =
                new FitViewport(
                        viewportWidth,
                        viewportWidth
                                * Gdx.graphics.getHeight()
                                / Gdx.graphics.getWidth()); // 替换为游戏的实际分辨率
        stage = new Stage(viewport, spriteBatch);

        // 创建字体样式
        BitmapFont font = new BitmapFont(); // 默认字体
        Label.LabelStyle labelStyle = new Label.LabelStyle(font, Color.WHITE);

        // 初始化标签
        healthLabel = new Label("Health: 100", labelStyle);
        keyStatusLabel = new Label("Key: Not Collected", labelStyle);

        // 使用 Table 布局
        Table table = new Table();
        table.top(); // 设置表格内容靠顶部显示
        table.setFillParent(true); // 填充整个屏幕

        // 将标签添加到表中
        table.add(healthLabel).expandX().padTop(10);
        table.row(); // 换行
        table.add(keyStatusLabel).expandX().padTop(10);

        // 将表格添加到舞台
        stage.addActor(table);
    }

    public void update(int health, boolean hasKey) {
        this.health = health;
        this.hasKey = hasKey;

        // 更新标签内容
        healthLabel.setText("Health: " + health);
        keyStatusLabel.setText("Key: " + (hasKey ? "Collected" : "Not Collected"));
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
        stage.dispose(); // 释放资源
    }
}
