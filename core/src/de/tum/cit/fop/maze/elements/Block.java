package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

public class Block extends GameObject implements Visible {
    private final Maze maze;

    private final TextureRegion texture;
    private final Rectangle box;

    private final boolean walkable;

    /**
     * Creates a new block with the given texture at the given position.
     *
     * @param texture the texture of the block
     * @param x the position of the block on the x-axis
     * @param y the position of the block on the y-axis
     */
    public Block(Maze maze, TextureRegion texture, float x, float y, boolean walkable) {
        this.maze = maze;
        this.texture = texture;
        this.box = new Rectangle(x, y, maze.getBlocksize(), maze.getBlocksize());
        this.walkable = walkable;
    }

    public Rectangle getBox() {
        return box;
    }

    public boolean isWalkable() {
        return walkable;
    }

    @Override
    public TextureRegion getTexture(float stateTime) {
        return texture;
    }
}
