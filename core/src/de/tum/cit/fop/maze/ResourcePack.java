package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

import de.tum.cit.fop.maze.elements.MoveAnimation;

import java.util.function.Function;

/* load and serves art assets */
public class ResourcePack { // Character animation
    private MoveAnimation playerWalkAnimation, playerSprintAnimation;
    private MoveAnimation SkeletonMoveAnimation;
    private TextureRegion blockTexture, blackBlockTexture, keyTexture, fullHeartTexture, halfHeartTexture;

    public ResourcePack() {
        loadPlayerAnimation();
        loadMobTexture();
        loadBlockTexture();
        loadKeyTexture();
        loadHeartTextures();
    }

    public MoveAnimation getSkeletonMoveAnimation() {
        return SkeletonMoveAnimation;
    }

    public MoveAnimation getPlayerSprintAnimation() {
        return playerSprintAnimation;
    }

    public TextureRegion getBlockTexture() {
        return blockTexture;
    }

    public TextureRegion getBlackBlockTexture() {
        return blackBlockTexture;
    }

    public MoveAnimation getPlayerWalkAnimation() {
        return playerWalkAnimation;
    }

    private void loadBlockTexture() {
        Texture tilesSheet = new Texture(Gdx.files.internal("basictiles.png"));

        int tileSize = 16;
        blockTexture = new TextureRegion(tilesSheet, tileSize, 0, tileSize, tileSize);
        blackBlockTexture =
            new TextureRegion(tilesSheet, tileSize * 6, tileSize * 2, tileSize, tileSize);
    }

    private void loadPlayerAnimation() {
        Texture walkSheet = new Texture(Gdx.files.internal("character.png"));

        PixelVector size = new PixelVector(16, 32);
        int frameCount = 4;

        Function<Helper.Direction, Integer> getRowNumber =
            dir ->
                switch (dir) {
                    case UP -> 2;
                    case DOWN -> 0;
                    case LEFT -> 3;
                    case RIGHT -> 1;
                };

        playerWalkAnimation = new MoveAnimation();

        for (Helper.Direction direction : Helper.Direction.values()) {
            // locate the row corresponds to the direction
            int row = getRowNumber.apply(direction);
            playerWalkAnimation.loadDirectionAnimation(
                0.1f,
                direction,
                loadTextureArray(
                    walkSheet,
                    new PixelVector(0, row * size.y),
                    size,
                    new PixelVector(size.x, 0),
                    frameCount));
        }

        playerSprintAnimation = new MoveAnimation();

        for (Helper.Direction direction : Helper.Direction.values()) {
            // locate the row corresponds to the direction
            int row = getRowNumber.apply(direction);
            playerSprintAnimation.loadDirectionAnimation(
                0.1f,
                direction,
                loadTextureArray(
                    walkSheet,
                    new PixelVector(144, row * size.y),
                    size,
                    new PixelVector(size.x, 0),
                    frameCount));
        }
    }

    private void loadMobTexture() {
        Texture origin = new Texture(Gdx.files.internal("mobs.png"));

        PixelVector size = new PixelVector(16, 16);
        int frameCount = 3;

        Function<Helper.Direction, Integer> getRowNumber =
            dir ->
                switch (dir) {
                    case UP -> 3;
                    case DOWN -> 0;
                    case LEFT -> 1;
                    case RIGHT -> 2;
                };

        SkeletonMoveAnimation = new MoveAnimation();

        for (Helper.Direction direction : Helper.Direction.values()) {
            int row = getRowNumber.apply(direction);
            SkeletonMoveAnimation.loadDirectionAnimation(
                0.1f,
                direction,
                loadTextureArray(
                    origin,
                    new PixelVector(144, row * size.y),
                    size,
                    new PixelVector(size.x, 0),
                    frameCount));
        }
    }

    private void loadKeyTexture() {
        Texture keySheet = new Texture(Gdx.files.internal("Key.png"));
        // 该资源是16x16
        keyTexture = new TextureRegion(keySheet, 0, 0, 256, 256);
    }

    private void loadHeartTextures() {
        Texture fullHeartSheet = new Texture(Gdx.files.internal("Lives.png"));
        fullHeartTexture = new TextureRegion(fullHeartSheet, 0, 0, fullHeartSheet.getWidth(), fullHeartSheet.getHeight());

        Texture halfHeartSheet = new Texture(Gdx.files.internal("halfLives.png"));
        halfHeartTexture = new TextureRegion(halfHeartSheet, 0, 0, halfHeartSheet.getWidth(), halfHeartSheet.getHeight());
    }

    public TextureRegion getKeyTexture() {
        return keyTexture;
    }

    public TextureRegion getFullHeartTexture() {
        return fullHeartTexture;
    }

    public TextureRegion getHalfHeartTexture() {
        return halfHeartTexture;
    }

    /**
     * Loads a consecutive set of textures of the same size
     *
     * @param origin original texture that needs to be cut
     * @param position base position to start
     * @param size size of individual texture
     * @param offset movement to the position of next frame
     * @param count number of frames to be loaded in total    * @return desired textures
     */
    private Array<TextureRegion> loadTextureArray(
        Texture origin, PixelVector position, PixelVector size, PixelVector offset, int count) {
        Array<TextureRegion> result = new Array<>();
        for (int i = 0; i < count; ++i) {
            // start position of current texture
            int posX = position.x + offset.x * i, posY = position.y + offset.y * i;
            result.add(new TextureRegion(origin, posX, posY, size.x, size.y));
        }
        return result;
    }

    public record PixelVector(int x, int y) {}
}
