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

public class EndScreen implements Screen {

    private final MazeRunnerGame game;
    private final Stage stage;
    private final int score;
    private Music victory;

    public EndScreen(MazeRunnerGame game, int score) {
        this.game = game;
        this.score = score;
        victory = Gdx.audio.newMusic(Gdx.files.internal("victory.mp3"));
        victory.setLooping(true); // Optional: Loop the music
        victory.setVolume(0.5f); // Set the volume (adjust as needed)
        victory.play(); // Start playing the music
        // Set up camera and viewport
        var camera = new OrthographicCamera();
        camera.zoom = 1.5f;
        stage = new Stage(new ScreenViewport(camera), game.getSpriteBatch());
        Gdx.input.setInputProcessor(stage);

        // Set up UI table
        Table table = new Table();
        table.setFillParent(true); // Make the table fill the entire screen
        stage.addActor(table);

        // Add "Game Completed" message label
        Label messageLabel =
                new Label("Congratulations! You completed the game!", game.getSkin(), "title");
        table.add(messageLabel).padBottom(50).center().row();

        // Display the score
        Label scoreLabel = new Label("Your Score: " + score, game.getSkin());
        table.add(scoreLabel).padBottom(50).center().row();

        // Add "Return to Menu" button
        TextButton returnButton = new TextButton("Return to Menu", game.getSkin());
        table.add(returnButton).width(300).padBottom(20).center().row();
        returnButton.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        victory.stop();
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
