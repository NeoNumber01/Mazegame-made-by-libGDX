package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/** The MenuScreen class is responsible for displaying the main menu of the game. */
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
        table.add(new Label("Maze Runner Menu", game.getSkin(), "title")).padBottom(80).row();

        // Resume Game
        if (pauseMode) {
            TextButton resumeButton = new TextButton("Resume Game", game.getSkin());
            table.add(resumeButton).width(300).padBottom(20).row();
            resumeButton.addListener(
                    new ChangeListener() {
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
        newGameButton.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        game.startNewGame();
                    }
                });
        // 'load new Map"按钮
        TextButton loadMapButton = new TextButton("Load New Map", game.getSkin());
        table.add(loadMapButton).width(300).padBottom(20).row();
        loadMapButton.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        showLoadMapDialog();
                    }
                });
        // 音量调节
        // Volume Button
        TextButton volumeButton = new TextButton("Volume", game.getSkin());
        table.add(volumeButton).width(300).padBottom(20).row();
        volumeButton.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        // 打开音量控制面板
                        showVolumeDialog();
                    }
                });

        // Exit
        TextButton exitButton = new TextButton("Exit", game.getSkin());
        table.add(exitButton).width(300).row();
        exitButton.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        Gdx.app.exit();
                    }
                });
    }

    private void showVolumeDialog() {
        Dialog volumeDialog =
                new Dialog("Volume Control", game.getSkin()) {
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

        volumeSlider.addListener(
                new ChangeListener() {
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

    /** 选择地图 */
    private void showLoadMapDialog() {
        // 创建对话框
        Dialog loadMapDialog =
                new Dialog("Select a Map", game.getSkin()) {
                    @Override
                    protected void result(Object object) {
                        // 当点击对话框的按钮后会调用
                        // object 就是 button(...) 方法里传进去的返回值
                        if (object instanceof String) {
                            // 假设我们在 button 里传的就是地图路径
                            String mapFilePath = (String) object;
                            game.startNewGame(mapFilePath);
                        }
                        // 关闭对话框
                        this.hide();
                    }
                };

        // 文字说明
        loadMapDialog.text("Choose one of the following maps:").padBottom(20);

        Table buttonTable = new Table();
        buttonTable.defaults().width(300).padBottom(20);
        String[] mapNames = {"Map 1", "Map 2", "Map 3", "Map 4", "Map 5", "Map 6"};
        String[] mapPaths = {
            "maps/level-1.properties",
            "maps/level-2.properties",
            "maps/level-3.properties",
            "maps/level-4.properties",
            "maps/level-5.properties",
            "maps/level-6.properties",
        };
        for (int i = 0; i < mapNames.length; i++) {
            String mapName = mapNames[i];
            String mapPath = mapPaths[i];
            TextButton mapButton = new TextButton(mapName, game.getSkin());
            buttonTable.add(mapButton).width(300).padBottom(20).row();
            final String selectedMapPath = mapPath;
            mapButton.addListener(
                    new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            game.startNewGame(selectedMapPath);
                            loadMapDialog.hide(); // 关闭对话框
                        }
                    });
        }
        loadMapDialog.getContentTable().add(buttonTable).row();

        // 在对话框的 button(...) 里传递第三个参数作为“返回值”，在 result() 方法里拿
        //        loadMapDialog.button("Map 1", "maps/level-1.properties");
        //        loadMapDialog.button("Map 2", "maps/level-2.properties");
        //        loadMapDialog.button("Map 3", "maps/level-3.properties");
        //        loadMapDialog.button("Map 4", "maps/level-4.properties");
        //        loadMapDialog.button("Map 5", "maps/level-5.properties");
        // 取消
        loadMapDialog.button("Cancel", game.getSkin());
        //
        // 显示对话框
        loadMapDialog.show(stage);
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
