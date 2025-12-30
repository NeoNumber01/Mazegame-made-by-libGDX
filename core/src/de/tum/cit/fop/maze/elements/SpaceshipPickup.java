package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * A collectible spaceship pickup that spawns/despawns managed by SpaceshipSpawner.
 */
public class SpaceshipPickup extends InteractiveElements {

    private float scale = 0.9f;

    public SpaceshipPickup(Maze maze, Vector2 position) {
        super(maze, position, new Vector2(28, 28), new Vector2(0, 0));
    }

    @Override
    public void render() {
        SpriteBatch batch = maze.getGame().getSpriteBatch();
        TextureRegion tex = maze.getGame().getResourcePack().getSpaceshipTexture();

        float t = maze.getGame().getStateTime();

        // subtle holographic glow
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        float a = 0.25f + 0.15f * MathUtils.sin(t * 3.5f);
        batch.setColor(0.2f, 0.9f, 1f, a);
        renderTextureV2(tex, scale * 1.15f);

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(Color.WHITE);

        // base sprite with slight pulse
        renderFlashing(tex, scale);
    }

    @Override
    public void onCollision(MazeObject other) {
        super.onCollision(other);
        if (other instanceof Player player) {
            // trigger spaceship flight mode
            player.activateSpaceshipMode();
            maze.onSpaceshipPickupCollected();
            maze.getEntities().removeValue(this, true);
        }
    }
}
