package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * The MenuScreen class is responsible for displaying the main menu of the game.
 */
public class MenuScreen implements Screen {

    private final MazeRunnerGame game;
    private final Stage stage;
    private boolean pauseMode;


    public MenuScreen(MazeRunnerGame game, boolean pauseMode) {
        this.game = game;
        this.pauseMode = pauseMode;

        var camera = new OrthographicCamera();
        camera.zoom = 1.5f; // Set camera zoom for a closer view

        Viewport viewport = new ScreenViewport(camera);
        stage = new Stage(viewport, game.getSpriteBatch());
        Gdx.input.setInputProcessor(stage);


        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        // Title
        table.add(new Label("Maze Runner Menu", game.getSkin(), "title"))
            .padBottom(80)
            .row();

        // Resume Game
        if (pauseMode) {
            TextButton resumeButton = new TextButton("Resume Game", game.getSkin());
            table.add(resumeButton).width(300).padBottom(20).row();
            resumeButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    // Continue
                    game.goToGame();
                }
            });
        }

        // New Game
        TextButton newGameButton = new TextButton("Start New Game", game.getSkin());
        table.add(newGameButton).width(300).padBottom(20).row();
        newGameButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.startNewGame();
            }
        });

        // Exit
        TextButton exitButton = new TextButton("Exit", game.getSkin());
        table.add(exitButton).width(300).row();
        exitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Gdx.app.exit();
            }
        });
    }

    @Override
    public void show() {
        // 设置此Screen时会调用，设置输入处理器
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1/30f));
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
    public void hide() {
        // 切换到别的Screen
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
