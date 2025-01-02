package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import de.tum.cit.fop.maze.MazeRunnerGame;

/** A game object, which can move, has a hitbox, and animation. */
public abstract class Entity extends GameObject implements Move, Visible {
    // position of the hitbox indicates the position of the hitbox,
    // position + visualOffset is the position of the visual object
    protected Vector2 visualOffset;
    protected Rectangle
            hitbox; // to avoid complex math handling, we assume everything is a rectangle

    public Entity(MazeRunnerGame game, Vector2 position, Vector2 size, Vector2 visualOffset) {
        super(game);
        hitbox = new Rectangle(position.x, position.y, size.x, size.y);
        this.visualOffset = visualOffset;
    }

    public Vector2 getPosition() {
        return new Vector2(hitbox.x, hitbox.y);
    }

    public Vector2 getSize() {
        return new Vector2(hitbox.width, hitbox.height);
    }

    public Vector2 getVisualPosition() {
        return new Vector2(hitbox.x + visualOffset.x, hitbox.y + visualOffset.y);
    }

    @Override
    public void performMovement(Vector2 delta) {
        hitbox.setPosition(hitbox.x + delta.x, hitbox.y + delta.y);
    }
}
