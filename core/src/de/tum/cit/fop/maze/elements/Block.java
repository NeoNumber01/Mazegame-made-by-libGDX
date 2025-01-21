package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/** Base abstraction of a static block in the maze. */
public abstract class Block extends MazeObject {
    private final TextureRegion texture;
    private final boolean obstacle;

    /**
     * Creates a new block with the given texture at the given position
     *
     * @param maze the maze it belongs to
     * @param texture texture of the block, normally static
     * @param position position of the block
     * @param obstacle if a block is an obstacle, it cannot be walked ed on
     */
    public Block(Maze maze, TextureRegion texture, Vector2 position, boolean obstacle) {
        super(
                maze,
                position,
                new Vector2(maze.getBlockSize(), maze.getBlockSize()),
                new Vector2(0f, 0f));
        this.texture = texture;
        this.obstacle = obstacle;
    }

    /** Returns if it is an obstacle, i.e. player cannot walk on. */
    public boolean isObstacle() {
        return obstacle;
    }

    @Override
    public void render() {
        super.game
                .getSpriteBatch()
                .draw(
                        texture,
                        getPosition().x,
                        getPosition().y,
                        maze.getBlockSize(),
                        maze.getBlockSize());
    }

    @Override
    public boolean overlaps(Rectangle other) {
        return obstacle && super.overlaps(other);
    }

    /** Triggers event when other object arrives this block. */
    public void onArrival(MazeObject other) {}

    /** Returns the row number of the block in maze. */
    public int getRow() {
        return maze.getRow(this);
    }

    /** Returns the column number of the block in maze. */
    public int getColumn() {
        return maze.getColumn(this);
    }
}
