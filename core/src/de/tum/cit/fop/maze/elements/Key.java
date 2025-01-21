package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

/** Interactive collectable required to win the game. */
public class Key extends InteractiveElements {
    private final Vector2 position;
    private float scale = 0.75f;
    private Vector2 offset = new Vector2(0, 0);
    private Sound keys;

    public Key(Maze maze, TextureRegion texture, Vector2 position) {
        super(maze, position, new Vector2(32, 32), new Vector2(0, 0));
        this.position = position;
        this.keys = Gdx.audio.newSound(Gdx.files.internal("Keys.mp3"));
    }

    @Override
    public void render() {
        renderTextureV2(maze.getGame().getResourcePack().getKeyTexture(), scale, offset);
    }

    @Override
    public void onCollision(MazeObject other) {
        if (other instanceof Player player) {

            player.setHasKey(true);

            keys.play();

            maze.getEntities().removeValue(this, true);
        }
    }
}
