package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import de.tum.cit.fop.maze.Helper;

/** A game object, which can move, has a hitbox, and animation. */
public abstract class Entity extends MazeObject implements Move, Visible {
    // position of the hitbox indicates the position of the hitbox,
    // position + visualOffset is the position of the visual object
    protected Vector2 visualOffset;
    protected Rectangle
            hitbox; // to avoid complex math handling, we assume everything is a rectangle
    protected Helper.Direction direction;

    public Entity(Maze maze, Vector2 position, Vector2 size, Vector2 visualOffset) {
        super(maze);
        hitbox = new Rectangle(position.x, position.y, size.x, size.y);
        this.visualOffset = visualOffset;
        direction = Helper.Direction.DOWN;
    }

    public Vector2 getPosition() {
        return new Vector2(hitbox.x, hitbox.y);
    }

    public Vector2 getSize() {
        return new Vector2(hitbox.width, hitbox.height);
    }

    public Vector2 getVisualPosition() {
        return new Vector2(hitbox.x + visualOffset.x, hitbox.y + visualOffset.y);
    }

    @Override
    public void performMovement(Vector2 delta) {
        direction = Helper.Vector2Direction(delta);
        hitbox.setPosition(hitbox.x + delta.x, hitbox.y + delta.y);
    }

    public void renderTexture(TextureRegion texture) {
        renderTexture(texture, 1f);
    }

    public void renderTexture(TextureRegion texture, float scale) {
        Vector2 visualPosition = getVisualPosition();
        super.game
                .getSpriteBatch()
                .draw(
                        texture,
                        visualPosition.x,
                        visualPosition.y,
                        texture.getRegionWidth() * scale,
                        texture.getRegionHeight() * scale);
    }
}
