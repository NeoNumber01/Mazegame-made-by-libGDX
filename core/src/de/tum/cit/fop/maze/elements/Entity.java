package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import de.tum.cit.fop.maze.Helper;

/** A game object, which can move, has a hitbox, and animation. */
public abstract class Entity extends MazeObject implements Move {
    protected Helper.Direction direction;
    protected Block currentBlock;

    public Entity(Maze maze, Vector2 position, Vector2 size, Vector2 visualOffset) {
        super(maze, position, size, visualOffset);
        direction = Helper.Direction.DOWN;
    }

    private void performForceDisplacement(Vector2 delta) {
        direction = Helper.Vector2Direction(delta);
        super.displace(delta);
    }

    /** Checks if it is possible to move an entity to some position */
    public boolean checkCollision(Vector2 position) {
        Rectangle rect = new Rectangle(position.x, position.y, getSize().x, getSize().y);
        return checkCollision(rect);
    }

    public boolean checkCollision(Rectangle rect) {
        return !getCollision(rect).isEmpty();
    }

    public Array<MazeObject> getCollision(Rectangle rect) {
        Array<MazeObject> result = new Array<>();
        for (MazeObject other : maze) {
            if (other != this && other.overlaps(rect)) {
                result.add(other);
            }
        }
        return result;
    }

    /**
     * Returns an array of all other objects that conjuncts with given rectangle.
     *
     * @param rect rectangle used to calculate adjacency
     * @return an array of collided objects
     */
    public Array<MazeObject> getAdjacent(Rectangle rect) {
        // it seems libGDX doesn't provide a way to do this, we therefore enlarge the hitbox by an
        // offset on each side to convert this into a collision problem
        float offset = 1f; // setting this too low will fail to detect
        Rectangle extendedRect =
                new Rectangle(
                        rect.x - offset,
                        rect.y - offset,
                        rect.width + 2 * offset,
                        rect.height + 2 * offset);
        return getCollision(extendedRect);
    }

    @Override
    public void performDisplacement(Vector2 displacement) {
        // check the feasibility on x- and y-axis separately, this avoids the extremely complex
        // handling when moving with collision happening on the other axis
        Vector2 projectionX = new Vector2(displacement.x, 0f),
                projectionY = new Vector2(0f, displacement.y);
        if (projectionX.len() > 0 && !checkCollision(getPosition().add(projectionX))) {
            performForceDisplacement(projectionX);
        }
        if (projectionY.len() > 0 && !checkCollision(getPosition().add(projectionY))) {
            performForceDisplacement(projectionY);
        }

        // post displacement hook
        getAdjacent(getHitbox()).forEach(x -> x.onCollision(this));

        // arrival hook
        Block newBlock = maze.getBlock(getCenter());
        if (newBlock != null && currentBlock != newBlock) {
            currentBlock = newBlock;
            newBlock.onArrival(this);
        }
    }

    public void moveTowards(Block targetBlock, float deltaTime) {
        float dist = getMoveDistance(deltaTime);
        Vector2 requiredDisplacement = targetBlock.getCenter().sub(getCenter()),
                possibleDisplacement =
                        new Vector2(
                                MathUtils.clamp(requiredDisplacement.x, -dist, dist),
                                MathUtils.clamp(requiredDisplacement.y, -dist, dist));
        performDisplacement(possibleDisplacement);
    }

    public boolean inBlockCenter() {
        // not sure round-off error should be considered
        return maze.getBlock(getCenter()).getCenter().sub(getCenter()).len2() < 1e-12f;
    }

    public int getRow() {
        return getBlock().getRow();
    }

    public int getColumn() {
        return getBlock().getColumn();
    }
}
