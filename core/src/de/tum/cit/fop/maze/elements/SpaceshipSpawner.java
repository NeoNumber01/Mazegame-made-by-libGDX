package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * Periodically spawns a SpaceshipPickup at a random walkable tile.
 *
 * Requirements:
 * - Always spawns (no probability of not spawning)
 * - Pickup exists for a while, then despawns, then respawns elsewhere
 */
public class SpaceshipSpawner {

    private final Maze maze;

    // Tunables
    public float spawnInterval = 2.0f;   // more frequent
    public float lifetime = 10.0f;       // stays longer on map

    private float timer = 0f;
    private float lifeTimer = 0f;

    private SpaceshipPickup current;

    private final Vector2 spawnPos = new Vector2();

    public SpaceshipSpawner(Maze maze) {
        this.maze = maze;
        // spawn immediately at level start
        timer = spawnInterval;
    }

    public void update(float dt) {
        // If pickup exists, count down its life
        if (current != null) {
            lifeTimer -= dt;
            if (lifeTimer <= 0f) {
                // remove it and schedule next spawn
                maze.getEntities().removeValue(current, true);
                current = null;
                timer = 0f; // allow immediate respawn
            }
            return;
        }

        // ensure it always respawns
        timer += dt;
        if (timer >= spawnInterval) {
            timer = 0f;
            spawnNow();
        }
    }

    /** Call when pickup was collected, so next cycle can start. */
    public void onCollected() {
        current = null;
        timer = 0f;
        lifeTimer = 0f;
    }

    private void spawnNow() {
        findRandomWalkableWorldPos(spawnPos);
        current = new SpaceshipPickup(maze, spawnPos);
        maze.getEntities().add(current);
        lifeTimer = lifetime;
    }

    private void findRandomWalkableWorldPos(Vector2 out) {
        int w = maze.getWidth();
        int h = maze.getHeight();
        float tile = maze.getBlockSize();
        Vector2 mazePos = maze.getPosition();

        // Try to spawn near player first
        Player player = maze.getPlayer();
        if (player != null) {
            Vector2 pCenter = player.getCenter();
            int px = (int) ((pCenter.x - mazePos.x) / tile);
            int py = (int) ((pCenter.y - mazePos.y) / tile);
            int radius = 8;

            for (int attempt = 0; attempt < 50; attempt++) {
                int dx = MathUtils.random(-radius, radius);
                int dy = MathUtils.random(-radius, radius);
                int tx = px + dx;
                int ty = py + dy;

                if (tx < 1 || tx >= w - 1 || ty < 1 || ty >= h - 1) continue;
                if (maze.isWall(tx, ty)) continue;

                out.set(mazePos.x + tx * tile + tile * 0.5f, mazePos.y + ty * tile + tile * 0.5f);
                return;
            }
        }

        for (int attempt = 0; attempt < 260; attempt++) {
            int tx = MathUtils.random(1, Math.max(1, w - 2));
            int ty = MathUtils.random(1, Math.max(1, h - 2));
            if (maze.isWall(tx, ty)) continue;

            out.set(mazePos.x + tx * tile + tile * 0.5f, mazePos.y + ty * tile + tile * 0.5f);
            return;
        }

        out.set(maze.getEntry().getCenter());
    }
}
