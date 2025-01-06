package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;


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
        //音量调节
        // Volume Button
        TextButton volumeButton = new TextButton("Volume", game.getSkin());
        table.add(volumeButton).width(300).padBottom(20).row();
        volumeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // 打开音量控制面板
                showVolumeDialog();
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
    private void showVolumeDialog() {
        Dialog volumeDialog = new Dialog("Volume Control", game.getSkin()) {
            @Override
            protected void result(Object object) {
                if ((boolean) object) {
                    this.hide(); // 关闭对话框
                }
            }
        };

        Table dialogContent = new Table();
        dialogContent.add(new Label("Volume:", game.getSkin())).padBottom(20).row();

        float initialValue = game.getVolume() * 100f;
        Slider volumeSlider = new Slider(0f, 100f, 1f, false, game.getSkin(), "default-horizontal");
        volumeSlider.setValue(initialValue);
        dialogContent.add(volumeSlider).width(300).padBottom(20).row();

        volumeSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                float sliderValue = volumeSlider.getValue();
                game.setVolume(sliderValue / 100f); // 更新音量
            }
        });

        volumeDialog.getContentTable().add(dialogContent).row();
        volumeDialog.button("Close", true);
        volumeDialog.show(stage);
    }

    @Override
    public void show() {
        // 设置此Screen时会调用，设置输入处理器
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
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        // 切换到别的Screen
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
