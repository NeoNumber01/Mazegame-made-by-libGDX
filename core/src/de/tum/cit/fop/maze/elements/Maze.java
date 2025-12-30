package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import de.tum.cit.fop.maze.InvalidMaze;
import de.tum.cit.fop.maze.MazeRunnerCamera;
import de.tum.cit.fop.maze.MazeRunnerGame;

import java.util.Iterator;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.function.Function;

/** The maze that contains all blocks */
public class Maze extends GameObject implements Iterable<MazeObject>, Visible {

    private final float blockSize = 32f;
    // base position of the maze
    private final Vector2 position;
    // size of the maze, in number of blocks
    private final int width;
    private final int height;
    private final Rectangle border;
    // this should never be exposed directly,
    // so that we can switch to other implementations, like Array<> provided by libGDX
    private final Block[][] maze;
    private final Array<Entity> entities;
    private final Array<Exit> exits;
    private Entry entry;
    private Player player;
    private MazeRunnerCamera camera;
    private final SpaceshipSpawner spaceshipSpawner;
    // Skull Boss (always spawns after a delay)
    private final SkullBoss skullBoss;

    /**
     * Constructor for Maze. Initializes all important elements.
     *
     * @param position The base position of the maze. Should be normally (0, 0).
     */
    public Maze(MazeRunnerGame game, Vector2 position, Properties mapProperties)
            throws InvalidMaze {
        super(game);
        this.position = position;

        // If width/height specified in properties file, use it as-is. Otherwise, use max position
        // x/y as width/height
        // properties are 0-indexed, thus +1
        // invisible border blocks are created, thus further +2
        this.width =
                mapProperties.get("Width") != null
                        ? Integer.parseInt((String) mapProperties.get("Width")) + 2
                        : mapProperties.stringPropertyNames().stream()
                                        .map(pair -> Integer.parseInt(pair.split(",")[0]))
                                        .max(Integer::compareTo)
                                        .orElse(0)
                                + 3;
        this.height =
                mapProperties.get("Height") != null
                        ? Integer.parseInt((String) mapProperties.get("Height")) + 2
                        : mapProperties.stringPropertyNames().stream()
                                        .map(pair -> Integer.parseInt(pair.split(",")[1]))
                                        .max(Integer::compareTo)
                                        .orElse(0)
                                + 3;

        border = new Rectangle(position.x, position.y, width * blockSize, height * blockSize);

        maze = new Block[width][height];
        entities = new Array<>();
        exits = new Array<>();
        boolean hasKey = false;
        BiFunction<Integer, Integer, Vector2> calcPosition =
                (x, y) -> new Vector2(position.x + x * blockSize, position.y + y * blockSize);

        for (int i = 1; i < width - 1; ++i) {
            for (int j = 1; j < height - 1; ++j) {
                // added border makes index +1, here map to original index
                String blockTypeStr = (String) mapProperties.get((i - 1) + "," + (j - 1));
                int blockTypeCode = blockTypeStr == null ? -1 : Integer.parseInt(blockTypeStr);
                Vector2 pos = calcPosition.apply(i, j);
                switch (blockTypeCode) {
                    case 0: // Wall
                        maze[i][j] = new Wall(this, game.getResourcePack().getWallTexture(), pos);
                        break;
                    case 1: // Entry Point
                        maze[i][j] = new Entry(this, game.getResourcePack().getEntryTexture(), pos);
                        entry = (Entry) maze[i][j];
                        break;
                    case 2: // TODO: Exit
                        maze[i][j] = new Exit(this, game.getResourcePack().getExitTexture(), pos);
                        exits.add((Exit) maze[i][j]);
                        break;
                    case 3: // Trap
                        TextureRegion floorTexture = game.getResourcePack().getPathTexture();

                        // 不再获取单一静态纹理，而是获取动画对象
                        Animation<TextureRegion> trapAnimation =
                                game.getResourcePack().getTrapAnimation();

                        // 创建陷阱对象，并传入动画
                        maze[i][j] = new Trap(this, floorTexture, trapAnimation, pos);
                        break;

                    case 4: // Enemy
                        entities.add(new Skeleton(this, pos));
                        break;
                    case 5: // Key
                        entities.add(new Key(this, game.getResourcePack().getKeyTexture(), pos));
                        hasKey = true;
                        break;
                    case 6: // Lives
                        Lives life =
                                new Lives(this, game.getResourcePack().getFullHeartTexture(), pos);
                        life.setScale(0.5f);
                        entities.add(life);
                        break;
                    case 7: // Lightning
                        Lightning lightning =
                                new Lightning(
                                        this, game.getResourcePack().getLightingTexture(), pos);
                        lightning.setScale(1.0f);
                        entities.add(lightning);
                        break;
                    case 8: // Shield
                        entities.add(
                                new Shield(this, game.getResourcePack().getShieldTexture(), pos));
                        break;
                    case 9: // MovableWall
                        entities.add(
                                new MovableWall(
                                        this, game.getResourcePack().getWallTexture(), pos));
                        break;
                    case 10: // Mine
                        Animation<TextureRegion> explosionAnimation =
                                game.getResourcePack().getExplosionAnimation();
                        entities.add(
                                new Mine(
                                        this,
                                        game.getResourcePack().getMineTexture(),
                                        pos,
                                        explosionAnimation));
                        break;
                }
                // fallback: empty block rendered as path
                if (maze[i][j] == null) {
                    maze[i][j] = new Path(this, game.getResourcePack().getPathTexture(), pos);
                }
            }
        }

        for (int i = 0; i < width; ++i) {
            for (int j = 0; j < height; ++j) {
                if (maze[i][j] == null) {
                    maze[i][j] =
                            new Wall(
                                    this,
                                    game.getResourcePack().getBlackBlockTexture(),
                                    calcPosition.apply(i, j));
                }
            }
        }

        if (exits.isEmpty()) throw new InvalidMaze("Maze must have at least one exit!");
        if (entry == null) throw new InvalidMaze("Maze must have an entry!");
        if (!hasKey) throw new InvalidMaze("Maze must have a key!");

        // Spaceship pickup spawner (always spawns periodically)
        spaceshipSpawner = new SpaceshipSpawner(this);
        // Optional: tweak timing
        // spaceshipSpawner.spawnInterval = 8f;
        // spaceshipSpawner.lifetime = 6f;

        // Initialize Skull Boss (will enter from outside the map after a delay)
        skullBoss = new SkullBoss(this);
    }

    /** Calculates the row number of given block. */
    public int getRow(Block block) {
        return (int) ((block.getPosition().x - position.x) / blockSize);
    }

    /** Calculates the column number of given block. */
    public int getColumn(Block block) {
        return (int) ((block.getPosition().y - position.y) / blockSize);
    }

    public Array<Entity> getEntities() {
        return entities;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
        entities.add(player);
    }

    public void setCamera(MazeRunnerCamera camera) {
        this.camera = camera;
    }

    public MazeRunnerCamera getCamera() {
        return camera;
    }

    @Override
    public void render() {
        for (MazeObject obj : this) {
            obj.render();
        }

        // Render Skull Boss on top of other elements
        skullBoss.render(game.getSpriteBatch());
    }

    public float getBlockSize() {
        return blockSize;
    }

    /** Returns grid width in blocks (including border). */
    public int getWidth() {
        return width;
    }

    /** Returns grid height in blocks (including border). */
    public int getHeight() {
        return height;
    }

    /** True if grid cell is a wall/obstacle (out of bounds counts as wall). */
    public boolean isWall(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return true;
        Block b = maze[x][y];
        return b != null && b.isObstacle();
    }

    /**
     * Get the block at the given position
     *
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
        // Update spawner before updating entities so spawned pickup can act immediately
        spaceshipSpawner.update(deltaTime);

        // Update Skull Boss
        skullBoss.update(deltaTime);

        for (MazeObject obj : this) {
            obj.onFrame(deltaTime);
        }
    }

    public Vector2 getPosition() {
        return position;
    }

    /**
     * Checks if the line between (row, columnStart) and (row, columnEnd) has no obstacles blocks.
     */
    public boolean isRowClear(int row, int columnStart, int columnEnd) {
        if (columnStart > columnEnd) {
            int columnTmp = columnStart;
            columnStart = columnEnd;
            columnEnd = columnTmp;
        }
        for (int i = columnStart; i <= columnEnd; ++i) {
            if (maze[row][i].isObstacle()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the line between (rowStart, column) and (rowEnd, column) has no obstacles blocks.
     */
    public boolean isColumnClear(int column, int rowStart, int rowEnd) {
        if (rowStart > rowEnd) {
            int rowTmp = rowStart;
            rowStart = rowEnd;
            rowEnd = rowTmp;
        }
        for (int i = rowStart; i <= rowEnd; ++i) {
            if (maze[i][column].isObstacle()) {
                return false;
            }
        }
        return true;
    }

    public Rectangle getBorder() {
        return border;
    }

    /** Returns the exit that has the minimal Manhattan distance to position */
    public Exit findNearestExit(Vector2 position) {
        Exit result = null;
        // calculates Manhattan distance
        Function<Exit, Float> calcDist =
                x ->
                        Math.abs(x.getPosition().x - position.x)
                                + Math.abs(x.getPosition().y - position.y);
        for (Exit exit : exits) {
            if (result == null || calcDist.apply(result) > calcDist.apply(exit)) {
                result = exit;
            }
        }
        return result;
    }

    /** Returns the degree to the nearest Exit */
    public float findNearestExitDirection(Vector2 position) {
        Exit target = findNearestExit(position);
        float deg = (float) (Math.atan2(target.getCenter().y - position.y, target.getCenter().x - position.x) * 180f / Math.PI);
        return deg;
    }

    /** Returns the Key entity that has the minimal Manhattan distance to position, or null if none remain. */
    public Key findNearestKey(Vector2 position) {
        Key result = null;
        // Manhattan distance (good enough + cheap)
        Function<Key, Float> calcDist =
                k -> Math.abs(k.getCenter().x - position.x) + Math.abs(k.getCenter().y - position.y);

        for (Entity e : entities) {
            if (!(e instanceof Key k)) continue;
            if (result == null || calcDist.apply(result) > calcDist.apply(k)) {
                result = k;
            }
        }
        return result;
    }

    /** Returns the degree to the nearest remaining Key. If no key remains, falls back to nearest exit. */
    public float findNearestKeyDirection(Vector2 position) {
        Key key = findNearestKey(position);
        if (key == null) {
            return findNearestExitDirection(position);
        }
        return (float) (Math.atan2(key.getCenter().y - position.y, key.getCenter().x - position.x) * 180f / Math.PI);
    }

    /** Returns the Skull Boss instance */
    public SkullBoss getSkullBoss() {
        return skullBoss;
    }

    void onSpaceshipPickupCollected() {
        spaceshipSpawner.onCollected();
    }
}
