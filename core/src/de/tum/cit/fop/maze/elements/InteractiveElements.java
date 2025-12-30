package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.Helper;

/** Interactive collectables, e.g. keys, power-ups. */
public abstract class InteractiveElements extends Entity {

    public InteractiveElements(Maze maze, Vector2 position, Vector2 size, Vector2 visualOffset) {
        super(maze, position, size, visualOffset);
    }

    protected void renderFlashing(TextureRegion texture, float baseScale) {
        float time = maze.getGame().getStateTime();
        
        // Alpha pulse: smooth oscillation between 0.4 and 1.0 (more visible blinking)
        float alpha = 0.7f + 0.3f * (float)Math.sin(time * 5f);
        
        // Scale pulse: breathe effect between 90% and 110% of base scale (more visible breathing)
        float currentScale = baseScale * (1.0f + 0.1f * (float)Math.sin(time * 3f));

        maze.getGame().getSpriteBatch().setColor(1f, 1f, 1f, alpha);
        renderTextureV2(texture, currentScale);
        maze.getGame().getSpriteBatch().setColor(1f, 1f, 1f, 1f);
    }

    @Override
    public void onCollision(MazeObject other) {
        if (other instanceof Mob mob) {
            mob.changeDirection();
        }
    }

    @Override
    public void performDisplacement(float deltaTime, Helper.Direction direction) {
        super.performDisplacement(deltaTime, direction);
    }

    @Override
    public float getMoveDistance(float deltaTime) {
        return super.getMoveDistance(deltaTime);
    }
}
