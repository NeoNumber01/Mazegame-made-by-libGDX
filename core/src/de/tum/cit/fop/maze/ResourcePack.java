package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

import de.tum.cit.fop.maze.elements.MoveAnimation;

import java.util.function.Function;

/* load and serves art assets */
public class ResourcePack { // Character animation
    private MoveAnimation playerWalkAnimation;
    private MoveAnimation playerSprintAnimation;
    private TextureRegion blockTexture, blackBlockTexture;

    public ResourcePack() {
        loadPlayerAnimation();
        loadBlockTexture();
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

    /**
     * Loads a consecutive set of textures of the same size
     *
     * @param origin original texture that needs to be cut
     * @param position base position to start
     * @param size size of individual texture
     * @param offset movement to the position of next frame
     * @param count number of frames to be loaded in total
     * @return desired textures
     */
    private Array<TextureRegion> loadTextureArray(
            Texture origin, PixelVector position, PixelVector size, PixelVector offset, int count) {
        Array<TextureRegion> result = new Array<>();
        for (int i = 0; i < count; ++i) {
            // start position of current texture
            int posX = position.x + offset.x * i, posY = position.y + offset.y * i;
            // Make sure to call the constructor that takes int as input, in case another one which
            // takes float as input and has really weired behavior is called. There's literally 0
            // documentation for them!
            result.add(new TextureRegion(origin, posX, posY, size.x, size.y));
        }
        return result;
    }

    public record PixelVector(int x, int y) {}
}
