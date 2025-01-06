package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

import de.tum.cit.fop.maze.Helper.Direction;

public class MoveAnimation {

    private Animation<TextureRegion> animationUp, animationDown, animationLeft, animationRight;

    public MoveAnimation(
            Array<TextureRegion> texturesUp,
            Array<TextureRegion> texturesDown,
            Array<TextureRegion> texturesLeft,
            Array<TextureRegion> texturesRight,
            float frameDuration) {
        this.animationUp = new Animation<>(frameDuration, texturesUp);
        this.animationDown = new Animation<>(frameDuration, texturesDown);
        this.animationLeft = new Animation<>(frameDuration, texturesLeft);
        this.animationRight = new Animation<>(frameDuration, texturesRight);
    }

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
        Animation<TextureRegion> animation =
                switch (direction) {
                    case UP -> animationUp;
                    case DOWN -> animationDown;
                    case LEFT -> animationLeft;
                    case RIGHT -> animationRight;
                };
        return animation.getKeyFrame(stateTime, true);
    }
}
