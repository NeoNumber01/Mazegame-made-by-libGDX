package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/** Interactive collectable required to win the game. */
public class Key extends InteractiveElements {
    private final Sound keys;
    private float scale = 0.75f;

    public Key(Maze maze, TextureRegion texture, Vector2 position) {
        super(maze, position, new Vector2(32, 32), new Vector2(0, 0));
        this.keys = Gdx.audio.newSound(Gdx.files.internal("Keys.mp3"));
    }

    @Override
    public void render() {
        SpriteBatch batch = maze.getGame().getSpriteBatch();
        TextureRegion texture = maze.getGame().getResourcePack().getKeyTexture();
        float time = maze.getGame().getStateTime();

        // --- Render Glow Effect ---
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE); // Additive blending
        
        float glowAlpha = 0.5f + 0.3f * MathUtils.sin(time * 3f);
        float glowScale = scale * (1.2f + 0.2f * MathUtils.sin(time * 3f + MathUtils.PI / 4f));
        
        batch.setColor(1f, 0.9f, 0.2f, glowAlpha); // Golden glow
        renderTextureV2(texture, glowScale);
        
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA); // Restore normal blending
        batch.setColor(Color.WHITE);
        // --------------------------

        renderFlashing(texture, scale);
    }

    @Override
    public void onCollision(MazeObject other) {
        super.onCollision(other);
        if (other instanceof Player player) {

            player.setHasKey(true);

            keys.play();

            maze.getEntities().removeValue(this, true);
        }
    }
}
