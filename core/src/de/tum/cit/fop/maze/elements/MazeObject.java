package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/** A generic abstraction of in-maze game elements. */
public abstract class MazeObject extends GameObject implements Visible, Collision {
    protected final Maze maze;

    private final Rectangle hitbox;
    // legacy design: hitbox position + visualOffset = texture position
    // replaced by center-based calculation in renderTextureV2()
    private final Vector2 visualOffset;

    public MazeObject(Maze maze, Vector2 position, Vector2 size, Vector2 visualOffset) {
        super(maze.game);
        this.maze = maze;
        hitbox = new Rectangle(position.x, position.y, size.x, size.y);
        this.visualOffset = visualOffset;
    }

    /** Returns the position of the hitbox */
    public Vector2 getPosition() {
        return new Vector2(hitbox.x, hitbox.y);
    }

    public Vector2 getCenter() {
        Vector2 result = new Vector2();
        return hitbox.getCenter(result);
    }

    public Rectangle getHitbox() {
        return hitbox;
    }

    /** Returns the position where the texture should be rendered at. */
    private Vector2 getVisualPosition() {
        return getPosition().add(visualOffset);
    }

    public Vector2 getSize() {
        return new Vector2(hitbox.width, hitbox.height);
    }

    public boolean overlaps(Rectangle other) {
        return hitbox.overlaps(other);
    }

    /**
     * Calls spritebatch to render a texture at MazeObject's location. Uses visualOffset and hitbox
     * position to determine actual texture position. This is a legacy implementation reserved for
     * compatibility, use renderTextureV2() if possible
     */
    public void renderTexture(TextureRegion texture) {
        renderTexture(texture, 1f);
    }

    /**
     * @param scale BROKEN, never use it!
     */
    private void renderTexture(TextureRegion texture, float scale) {
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

    /**
     * Calls spritebatch to render a texture at MazeObject's location. Uses visualOffset and hitbox
     * position to determine actual texture position. This is the recommended implementation. It
     * aligns the center of hitbox and scaled TextureRegion, then applies offset to the position.
     */
    public void renderTextureV2(TextureRegion texture, float scale, Vector2 offset) {
        Vector2 center = getCenter();
        super.game
                .getSpriteBatch()
                .draw(
                        texture,
                        center.x - texture.getRegionWidth() * scale / 2f + offset.x,
                        center.y - texture.getRegionHeight() * scale / 2f + offset.y,
                        texture.getRegionWidth() * scale,
                        texture.getRegionHeight() * scale);
    }

    /**
     * Calls spritebatch to render a texture at MazeObject's location. Uses visualOffset and hitbox
     * position to determine actual texture position. This is the recommended implementation.
     */
    public void renderTextureV2(TextureRegion texture, float scale) {
        renderTextureV2(texture, scale, new Vector2(0f, 0f));
    }

    /** A wrapper to re-position hitbox. */
    protected void displace(Vector2 displacement) {
        hitbox.setPosition((new Vector2(hitbox.x, hitbox.y)).add(displacement));
    }

    /** Returns the block where the object is on. If it is a block returns itself. */
    public Block getBlock() {
        Block result = maze.getBlock(getPosition());
        if (result == null) { // DEBUG log, normally this should not be triggered
            System.out.printf(
                    "Out of bound: %s %f %f\n",
                    this.getClass().getName(), getPosition().x, getPosition().y);
        }
        return result;
    }
}
