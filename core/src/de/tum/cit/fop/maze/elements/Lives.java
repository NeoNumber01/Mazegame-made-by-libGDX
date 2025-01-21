package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

/** Interactive collectable that heals the player. */
public class Lives extends InteractiveElements {
    private final Sound lives;
    private float scale = 1.0f;

    public Lives(Maze maze, TextureRegion texture, Vector2 position) {
        super(maze, position, new Vector2(16f, 16f), new Vector2(0, 0));
        this.lives = Gdx.audio.newSound(Gdx.files.internal("Keys.mp3"));
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    @Override
    public void render() {
        renderTextureV2(maze.getGame().getResourcePack().getFullHeartTexture(), scale);
    }

    @Override
    public void onCollision(MazeObject other) {
        if (other instanceof Player player) {
            player.modifyHealth(20);
            lives.play();
            maze.getEntities().removeValue(this, true);
        }
    }
}
