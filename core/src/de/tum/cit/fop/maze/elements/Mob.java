package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.math.Vector2;

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

    public void handleAI() {}
}
