package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

import de.tum.cit.fop.maze.Helper.Direction;

/** A set of animations that has their variants on all 4 directions. */
public class MoveAnimation {
    private Animation<TextureRegion> animationUp, animationDown, animationLeft, animationRight;

    public MoveAnimation() {}

    /**
     * An alternative way to load the animation for a specific direction
     *
     * @param frameDuration the duration of each frame
     * @param direction the direction of the animation
     * @param textureArray the array of textures
     */
    public void loadDirectionAnimation(
            Float frameDuration, Direction direction, Array<TextureRegion> textureArray) {
        switch (direction) {
            case UP -> animationUp = new Animation<>(frameDuration, textureArray);
            case DOWN -> animationDown = new Animation<>(frameDuration, textureArray);
            case LEFT -> animationLeft = new Animation<>(frameDuration, textureArray);
            case RIGHT -> animationRight = new Animation<>(frameDuration, textureArray);
        }
    }

    public TextureRegion getTexture(Direction direction, float stateTime) {
        return getAnimation(direction).getKeyFrame(stateTime, true);
    }

    /**
     * @param stateTime How long the animation has already played.
     */
    public TextureRegion getTextureNoLoop(Direction direction, float stateTime) {
        return getAnimation(direction).getKeyFrame(stateTime, false);
    }

    private Animation<TextureRegion> getAnimation(Direction direction) {
        return switch (direction) {
            case UP -> animationUp;
            case DOWN -> animationDown;
            case LEFT -> animationLeft;
            case RIGHT -> animationRight;
        };
    }
}
