package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public class Shield extends InteractiveElements {
    private Sound shiled;

    public Shield(Maze maze, TextureRegion texture, Vector2 position) {
        super(maze, position, new Vector2(16f, 16f), new Vector2(0, 0));
        this.shiled = Gdx.audio.newSound(Gdx.files.internal("Keys.mp3"));
    }

    @Override
    public void render() {
        renderTextureV2(maze.getGame().getResourcePack().getShieldTexture(), 2f);
    }

    @Override
    public void onCollision(MazeObject other) {
        if (other instanceof Player player) {
            player.activateShield(); // Activate shield for the player
            shiled.play();
            maze.getEntities().removeValue(this, true); // Remove shield from the maze
        }
    }
}
