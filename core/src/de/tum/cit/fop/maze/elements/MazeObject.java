package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public abstract class MazeObject extends GameObject implements Visible, Collision {
    protected final Maze maze;

    private final Rectangle hitbox;
    private final Vector2 visualOffset;

    public MazeObject(Maze maze, Vector2 position, Vector2 size, Vector2 visualOffset) {
        super(maze.game);
        this.maze = maze;
        hitbox = new Rectangle(position.x, position.y, size.x, size.y);
        this.visualOffset = visualOffset;
    }

    public Vector2 getPosition() {
        return new Vector2(hitbox.x, hitbox.y);
    }

    /** Returns the position where the texture should be rendered at. */
    public Vector2 getVisualPosition() {
        return getPosition().add(visualOffset);
    }

    public Vector2 getSize() {
        return new Vector2(hitbox.width, hitbox.height);
    }

    public boolean overlaps(Rectangle other) {
        return hitbox.overlaps(other);
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

    public void displace(Vector2 displacement) {
        hitbox.setPosition((new Vector2(hitbox.x, hitbox.y)).add(displacement));
    }
}
