package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import de.tum.cit.fop.maze.elements.Exit;
import de.tum.cit.fop.maze.elements.Player;

public class NoKeyScreen implements Screen {

    private final MazeRunnerGame game;
    private final Stage stage;
    private final Player player;
    private final Exit exit;

    public NoKeyScreen(MazeRunnerGame game, Player player, Exit exit) {
        this.game = game;
        this.player = player;
        this.exit = exit;

        // 设置舞台和视口
        stage = new Stage(new ScreenViewport(), game.getSpriteBatch());
        Gdx.input.setInputProcessor(stage);

        // 创建 UI 表格
        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        // 添加提示信息
        Label messageLabel = new Label("You don't have enough keys to exit!", game.getSkin(), "title");
        table.add(messageLabel).padBottom(50).center().row();

        // 添加 "Return to Game" 按钮
        TextButton returnButton = new TextButton("Return to Game", game.getSkin());
        table.add(returnButton).width(300).padBottom(20).center().row();

        // 设置按钮监听器
        returnButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // 恢复游戏状态并返回游戏界面
                game.resumeFromExit(player, exit);
            }
        });
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
    }
}
