package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
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

/** The game screen that appears when player dies. */
public class GameOverScreen implements Screen {

    private final MazeRunnerGame game;
    private final Stage stage;
    private Music afterDeath;

    public GameOverScreen(MazeRunnerGame game) {
        this.game = game;
        game.stopMusic();
        afterDeath = Gdx.audio.newMusic(Gdx.files.internal("after death.mp3"));
        afterDeath.setLooping(true); // Optional: Set to loop
        afterDeath.setVolume(0.5f); // Set volume (adjust as needed)
        afterDeath.play(); // Start playing music
        var camera = new OrthographicCamera();
        camera.zoom = 1.5f; // Set camera zoom for a closer view

        Viewport viewport = new ScreenViewport(camera);
        stage = new Stage(viewport, game.getSpriteBatch());
        Gdx.input.setInputProcessor(stage);

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        // Title: "Game Over"
        table.add(new Label("You Are Dead!", game.getSkin(), "title")).padBottom(80).row();

        // Restart Button
        TextButton restartButton = new TextButton("Restart Game", game.getSkin());
        table.add(restartButton).width(300).padBottom(20).row();
        restartButton.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        afterDeath.stop();
                        game.startNewGame(); // Restart the game
                    }
                });

        // Exit Button
        TextButton exitButton = new TextButton("Exit to Menu", game.getSkin());
        table.add(exitButton).width(300).row();
        exitButton.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        afterDeath.stop();
                        game.goToMenu(); // Return to the main menu
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
