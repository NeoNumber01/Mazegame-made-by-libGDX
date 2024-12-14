package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

import de.tum.cit.fop.maze.elements.MoveAnimation;

import java.util.function.BiFunction;
import java.util.stream.IntStream;

/* load and serves art assets */
public class ResourcePack { // Character animation
    private MoveAnimation playerWalkAnimation;
    private TextureRegion blockTexture, blackBlockTexture;

    public ResourcePack() {
        loadCharacterAnimation();
        loadBlockTexture();
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

    private void loadCharacterAnimation() {
        Texture walkSheet = new Texture(Gdx.files.internal("character.png"));

        int frameWidth = 16;
        int frameHeight = 32;
        int animationFrames = 4;

        // TODO: wrap this to a helper class so that mob textures can be loaded the same
        // way

        BiFunction<Integer, Integer, TextureRegion> cutWalkSheet =
                (row, col) -> {
                    return new TextureRegion(
                            walkSheet,
                            col * frameWidth,
                            row * frameHeight,
                            frameWidth,
                            frameHeight);
                };

        playerWalkAnimation = new MoveAnimation();

        for (Helper.Direction direction : Helper.Direction.values()) {
            // locate the row corresponds to the direction
            int row =
                    switch (direction) {
                        case UP -> 2;
                        case DOWN -> 0;
                        case LEFT -> 3;
                        case RIGHT -> 1;
                    };

            Array<TextureRegion> textureArray = new Array<>();

            IntStream.range(0, animationFrames)
                    .forEach(col -> textureArray.add(cutWalkSheet.apply(row, col)));
            playerWalkAnimation.loadDirectionAnimation(0.1f, direction, textureArray);
        }
    }
}
