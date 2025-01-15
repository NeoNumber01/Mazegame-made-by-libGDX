package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import de.tum.cit.fop.maze.MazeRunnerGame;

import java.util.Iterator;
import java.util.Properties;

/*
 * The maze that contains all blocks
 */
public class Maze extends GameObject implements Iterable<MazeObject>, Visible {

    private final float blockSize = 32f;
    // base position of the maze
    private final Vector2 position;
    // size of the maze, in number of blocks
    private final int width;
    private final int height;
    // this should never be exposed directly,
    // so that we can switch to other implementations, like Array<> provided by libGDX
    private final Block[][] maze;
    private final Array<Entity> entities;
    private Entry entry;

    /**
     * Constructor for Maze. Initializes all important elements.
     *
     * @param position The base position of the maze. Should be normally (0, 0).
     */
    public Maze(MazeRunnerGame game, Vector2 position, Properties mapProperties) {
        super(game);
        this.position = position;

        // properties are 0-indexed

        this.width =
                mapProperties.stringPropertyNames().stream()
                                .map(pair -> Integer.parseInt(pair.split(",")[0]))
                                .max(Integer::compareTo)
                                .orElse(0)
                        + 1;
        this.height =
                mapProperties.stringPropertyNames().stream()
                                .map(pair -> Integer.parseInt(pair.split(",")[1]))
                                .max(Integer::compareTo)
                                .orElse(0)
                        + 1;

        maze = new Block[width][height];
        entities = new Array<>();

        for (int i = 0; i < width; ++i) {
            for (int j = 0; j < height; ++j) {
                String blockTypeStr = (String) mapProperties.get(i + "," + j);
                int blockTypeCode = blockTypeStr == null ? -1 : Integer.parseInt(blockTypeStr);
                Vector2 pos = new Vector2(position.x + i * blockSize, position.y + j * blockSize);
                switch (blockTypeCode) {
                    case 0: // Wall
                        maze[i][j] =
                                new Wall(this, game.getResourcePack().getBlackBlockTexture(), pos);
                        break;
                    case 1: // Entry Point
                        maze[i][j] = new Entry(this, game.getResourcePack().getBlockTexture(), pos);
                        entry = (Entry) maze[i][j];
                        break;
                    case 2: // TODO: Exit
                        maze[i][j] = new Exit(this, game.getResourcePack().getBlockTexture(), pos);
                        break;
                    case 3: // TODO: Trap
                        break;
                    case 4: // TODO: Enemy
                        entities.add(new Skeleton(this, pos));
                        break;
                    case 5: // TODO: Key
                        maze[i][j] = new Key(this, game.getResourcePack().getKeyTexture(), pos);
                        break;
                    case 6: // TODO: Lives
                        entities.add(
                                new Lives(this, game.getResourcePack().getFullHeartTexture(), pos));
                        break;
                    case 7: // lIGHTING
                        entities.add(
                                new Lightning(
                                        this, game.getResourcePack().getLightingTexture(), pos));
                        break;
                }
                // fallback: empty block rendered as path
                if (maze[i][j] == null) {
                    maze[i][j] = new Path(this, game.getResourcePack().getBlockTexture(), pos);
                }
            }
        }
    }

    public Array<Entity> getEntities() {
        return entities;
    }

    @Override
    public void render() {
        for (MazeObject obj : this) {
            obj.render();
        }
    }

    public float getBlockSize() {
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
        if (position.x >= this.position.x + width * blockSize
                || position.y >= this.position.y + height * blockSize) {
            return null;
        }
        int i = (int) ((position.x - this.position.x) / blockSize);
        int j = (int) ((position.y - this.position.y) / blockSize);
        return maze[i][j];
    }

    public Entry getEntry() {
        return this.entry;
    }

    @Override
    public Iterator<MazeObject> iterator() {
        return new Iterator<>() {
            boolean isBlock = true;
            private int i = 0;
            private int j = 0;

            @Override
            public boolean hasNext() {
                return isBlock ? (i < width && j < height) : (i < entities.size);
            }

            @Override
            public MazeObject next() {
                if (isBlock) {
                    Block block = maze[i][j];
                    if (++j == height) {
                        j = 0;
                        ++i;
                    }
                    if (i >= width) { // reuse index var to enum entities
                        i = 0;
                        isBlock = false;
                    }
                    return block;
                } else {
                    return entities.get(i++);
                }
            }
        };
    }

    @Override
    public void onFrame(float deltaTime) {
        for (MazeObject obj : this) {
            obj.onFrame(deltaTime);
        }
    }

    public Vector2 getPosition() {
        return position;
    }

    public void setBlock(int i, int j, Block block) {
        maze[i][j] = block;
    }
}
