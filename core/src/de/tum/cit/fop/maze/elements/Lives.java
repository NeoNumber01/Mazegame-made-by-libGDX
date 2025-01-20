package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public class Lives extends InteractiveElements {
    private final Vector2 position;
    private Sound lives;
    private float scale = 1.0f;
    private Vector2 offset = new Vector2(0, 0);

    public Lives(Maze maze, TextureRegion texture, Vector2 position) {
        super(maze, position, new Vector2(32, 32), new Vector2(0, 0));
        this.position = position;
        this.lives = Gdx.audio.newSound(Gdx.files.internal("Keys.mp3"));
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public void setOffset(Vector2 offset) {
        this.offset = offset;
    }

    @Override
    public void render() {
        renderTextureV2(maze.getGame().getResourcePack().getFullHeartTexture(), scale, offset);
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
