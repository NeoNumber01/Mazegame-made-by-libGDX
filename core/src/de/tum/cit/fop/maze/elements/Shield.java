package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

/** Interactive collectable that provides defense upgrades for the player. */
public class Shield extends InteractiveElements {
    private final Sound shield;

    public Shield(Maze maze, TextureRegion texture, Vector2 position) {
        super(maze, position, new Vector2(16f, 16f), new Vector2(0, 0));
        this.shield = Gdx.audio.newSound(Gdx.files.internal("Keys.mp3"));
    }

    @Override
    public void render() {
        renderTextureV2(maze.getGame().getResourcePack().getShieldTexture(), 2f);
    }

    @Override
    public void onCollision(MazeObject other) {
        super.onCollision(other);
        if (other instanceof Player player) {
            player.activateShield(); // Activate shield for the player
            shield.play();
            maze.getEntities().removeValue(this, true); // Remove shield from the maze
        }
    }
}
