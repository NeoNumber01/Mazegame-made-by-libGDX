package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.math.Vector2;

import de.tum.cit.fop.maze.Helper;

public abstract class Mob extends Entity {
    private final MoveAnimation moveAnimation;

    public Mob(
            Maze maze,
            Vector2 position,
            Vector2 size,
            Vector2 visualOffset,
            MoveAnimation moveAnimation) {
        super(maze, position, size, visualOffset);
        this.moveAnimation = moveAnimation;
        changeDirection();
    }

    @Override
    public void render() {
        renderTexture(moveAnimation.getTexture(super.direction, super.game.getStateTime()));
    }

    @Override
    public void onCollision(MazeObject other) {
        if (other instanceof Player) {
            ((Player) other).modifyHealth(-10f);
        }
    }

    @Override
    public void onFrame(float deltaTime) {
        // minimal path-finding implementation, only consider the situation where player is in
        // current row/column.
        int playerCol = maze.getPlayer().getColumn(),
                playerRow = maze.getPlayer().getRow(),
                mobCol = getColumn(),
                mobRow = getRow();
        int maxDist = 4;
        if (playerCol == mobCol
                && maze.isColumnClear(mobCol, playerRow, mobRow)
                && Math.abs(playerRow - mobRow) < maxDist) {
            this.direction =
                    maze.getPlayer().getPosition().x - getPosition().x > 0
                            ? Helper.Direction.RIGHT
                            : Helper.Direction.LEFT;
        }
        if (playerRow == mobRow
                && maze.isRowClear(mobRow, playerCol, mobCol)
                && Math.abs(playerCol - mobCol) < maxDist) {
            this.direction =
                    maze.getPlayer().getPosition().y - getPosition().y > 0
                            ? Helper.Direction.UP
                            : Helper.Direction.DOWN;
        }
        performDisplacement(deltaTime, direction);
    }

    public void changeDirection() {
        direction = Helper.getRandomDirection();
    }
}
