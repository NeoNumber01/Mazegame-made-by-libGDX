package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public abstract class Block extends GameObject implements Visible {
    private final Maze maze;

    private final TextureRegion texture;
    private final Rectangle rect; // visual object and hitbox aligns

    private final boolean obstacle;

    /**
     * Creates a new block with the given texture at the given position
     *
     * @param maze the maze it belongs to
     * @param texture texture of the block, normally static
     * @param position position of the block
     * @param obstacle if a block is an obstacle, it cannot be walked on
     */
    public Block(Maze maze, TextureRegion texture, Vector2 position, boolean obstacle) {
        super(maze.game);
        this.maze = maze;
        this.texture = texture;
        this.rect = new Rectangle(position.x, position.y, maze.getBlocksize(), maze.getBlocksize());
        this.obstacle = obstacle;
    }

    public Vector2 getPosition() {
        return new Vector2(rect.x, rect.y);
    }

    public boolean isObstacle() {
        return obstacle;
    }

    @Override
    public void render() {
        super.game
                .getSpriteBatch()
                .draw(texture, rect.x, rect.y, maze.getBlocksize(), maze.getBlocksize());
    }

    public boolean overlaps(Rectangle other) {
        return rect.overlaps(other);
    }
}
