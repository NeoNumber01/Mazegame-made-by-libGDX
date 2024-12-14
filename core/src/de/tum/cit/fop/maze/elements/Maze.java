package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import de.tum.cit.fop.maze.MazeRunnerGame;

import java.util.Iterator;

/*
 * The maze that contains all blocks
 */
public class Maze implements Iterable<Block> {
    private final MazeRunnerGame game;

    private final float blockSize = 32f;
    // base position of the maze
    private final Vector2 position;
    // size of the maze, in number of blocks
    private final int length, width;
    // this should never be exposed directly,
    // so that we can switch to other implementations, like Array<> provided by libGDX
    private final Block[][] maze;

    /**
     * Constructor for Maze. Initializes all important elements.
     *
     * @param position The base position of the maze. Should be normally (0, 0).
     * @param length The length of the maze in number of blocks.
     * @param width The width of the maze in number of blocks.
     */
    public Maze(MazeRunnerGame game, Vector2 position, int length, int width) {
        this.game = game;
        this.position = position;
        this.length = length;
        this.width = width;
        maze = new Block[length][width];

        // TODO: read the maze from a file
        for (int i = 0; i < length; ++i) {
            for (int j = 0; j < width; ++j) {
                maze[i][j] =
                        new Path(
                                this,
                                game.getResourcePack().getBlockTexture(),
                                position.x + i * blockSize,
                                position.y + j * blockSize);
            }
        }
        maze[5][5] =
                new Wall(
                        this,
                        game.getResourcePack().getBlackBlockTexture(),
                        position.x + 5 * blockSize,
                        position.y + 5 * blockSize);
    }

    public float getBlocksize() {
        return blockSize;
    }

    /* Get the block at the given position
     * @param position The position of the block
     * @return The block at the given position, or null if the position is outside the maze
     */
    public Block getBlock(Vector2 position) {
        if (position.x < this.position.x || position.y < this.position.y) {
            return null;
        }
        if (position.x >= this.position.x + length * blockSize
                || position.y >= this.position.y + width * blockSize) {
            return null;
        }
        int i = (int) ((position.x - this.position.x) / blockSize);
        int j = (int) ((position.y - this.position.y) / blockSize);
        return maze[i][j];
    }

    /* Get the blocks surrounding the given position, 5 * 5 blocks in total
     * @param position The position of the center block
     * @return An array of blocks surrounding the given position
     */
    public Array<Block> getSurroundBlocks(Vector2 position) {
        Array<Block> blocks = new Array<Block>();
        for (int i = -2; i <= 2; ++i) {
            for (int j = -2; j <= 2; ++j) {
                Block block =
                        getBlock(
                                new Vector2(
                                        position.x + i * blockSize, position.y + j * blockSize));
                if (block != null) {
                    blocks.add(block);
                }
            }
        }
        return blocks;
    }

    @Override
    public Iterator<Block> iterator() {
        return new Iterator<Block>() {
            private int i = 0;
            private int j = 0;

            @Override
            public boolean hasNext() {
                return i < length && j < width;
            }

            @Override
            public Block next() {
                Block block = maze[i][j];
                if (++j == width) {
                    j = 0;
                    ++i;
                }
                return block;
            }
        };
    }
}
