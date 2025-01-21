package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

import de.tum.cit.fop.maze.elements.MoveAnimation;

import java.util.function.Function;

/* load and serves art assets */
public class ResourcePack { // Character animation
    private MoveAnimation playerWalkAnimation;
    private MoveAnimation playerSprintAnimation;
    private MoveAnimation playerAttackAnimation;
    private MoveAnimation SkeletonMoveAnimation;
    private Animation<TextureRegion> trapAnimation;
    private Animation<TextureRegion> explosionAnimation;
    private TextureRegion blockTexture,
            blackBlockTexture,
            keyTexture,
            fullHeartTexture,
            halfHeartTexture,
            lightningTexture,
            shieldTexture,
            wallTexture,
            pathTexture,
            exitTexture,
            entryTexture,
            frame1,
            frame2,
            mineTexture;

    public ResourcePack() {
        loadPlayerAnimation();
        loadMobTexture();
        loadBlockTexture();
        loadKeyTexture();
        loadHeartTextures();
        loadLightningTexture();
        loadShieldTexture();
        loadTrapTexture();
        loadWallTexture();
        loadPathTexture();
        loadExitTexture();
        loadEntryTexture();
        loadTrapTexture();
    }

    public MoveAnimation getPlayerAttackAnimation() {
        return playerAttackAnimation;
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

        Function<Helper.Direction, Integer> getRowNumber_atk =
                dir ->
                        switch (dir) {
                            case UP -> 1;
                            case DOWN -> 0;
                            case LEFT -> 3;
                            case RIGHT -> 2;
                        };

        playerAttackAnimation = new MoveAnimation();

        for (Helper.Direction direction : Helper.Direction.values()) {
            int row = getRowNumber_atk.apply(direction);
            playerAttackAnimation.loadDirectionAnimation(
                    0.1f,
                    direction,
                    loadTextureArray(
                            walkSheet,
                            new PixelVector(0, 128 + row * 32),
                            new PixelVector(32, 32),
                            new PixelVector(32, 0),
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
        // 该资源是10x20
        keyTexture = new TextureRegion(keySheet, 0, 0, 10, 20);
    }

    private void loadHeartTextures() {
        Texture fullHeartSheet = new Texture(Gdx.files.internal("Lives.png"));
        fullHeartTexture =
                new TextureRegion(
                        fullHeartSheet,
                        0,
                        0,
                        fullHeartSheet.getWidth(),
                        fullHeartSheet.getHeight());

        Texture halfHeartSheet = new Texture(Gdx.files.internal("halfLives.png"));
        halfHeartTexture =
                new TextureRegion(
                        halfHeartSheet,
                        0,
                        0,
                        halfHeartSheet.getWidth(),
                        halfHeartSheet.getHeight());
    }

    public TextureRegion getKeyTexture() {
        return keyTexture;
    }

    public TextureRegion getFullHeartTexture() {
        return fullHeartTexture;
    }

    private void loadLightningTexture() {
        Texture LightningSheet = new Texture(Gdx.files.internal("Lightning.png"));
        lightningTexture = new TextureRegion(LightningSheet, 0, 0, 16, 16);
    }

    public TextureRegion getLightingTexture() {
        return lightningTexture;
    }

    public TextureRegion getHalfHeartTexture() {
        return halfHeartTexture;
    }

    private void loadShieldTexture() {
        Texture shieldSheet = new Texture(Gdx.files.internal("Armor.png"));
        shieldTexture = new TextureRegion(shieldSheet, 0, 0, 16, 16);
    }

    public TextureRegion getShieldTexture() {
        return shieldTexture;
    }

    private void loadTrapTexture() {
        Texture trapSheet01 = new Texture(Gdx.files.internal("electric03.png"));
        Texture trapSheet02 = new Texture(Gdx.files.internal("electric04.png"));
        frame1 = new TextureRegion(trapSheet01, 0, 0, 16, 16);
        frame2 = new TextureRegion(trapSheet02, 0, 0, 16, 16);
        trapAnimation = new Animation<>(0.3f, frame1, frame2);
        trapAnimation.setPlayMode(Animation.PlayMode.LOOP);
    }

    private void loadWallTexture() {
        Texture wallSheet = new Texture(Gdx.files.internal("wall.png"));
        wallTexture = new TextureRegion(wallSheet, 0, 0, 16, 16);
    }

    private void loadPathTexture() {
        Texture pathSheet = new Texture(Gdx.files.internal("floor01.png"));
        pathTexture = new TextureRegion(pathSheet, 0, 0, 16, 16);
    }

    private void loadExitTexture() {
        Texture tilesSheet = new Texture(Gdx.files.internal("door.png"));

        int tileSize = 16;
        exitTexture = new TextureRegion(tilesSheet, 0, 0, tileSize, tileSize);
    }

    private void loadEntryTexture() {
        Texture tilesSheet = new Texture(Gdx.files.internal("basictiles.png"));

        int tileSize = 16;
        entryTexture = new TextureRegion(tilesSheet, 16, 112, tileSize, tileSize);
    }

    public TextureRegion getMineTexture() {
        return new TextureRegion(new Texture(Gdx.files.internal("Skull.png")));
    }

    public Animation<TextureRegion> getExplosionAnimation() {
        TextureRegion explosion1 =
                new TextureRegion(new Texture(Gdx.files.internal("explosion1.png")));
        TextureRegion explosion2 =
                new TextureRegion(new Texture(Gdx.files.internal("explosion2.png")));
        TextureRegion explosion3 =
                new TextureRegion(new Texture(Gdx.files.internal("explosion3.png")));
        TextureRegion explosion4 =
                new TextureRegion(new Texture(Gdx.files.internal("explosion4.png")));
        return new Animation<>(0.3f, explosion1, explosion2, explosion3, explosion4);
    }

    public TextureRegion getExitTexture() {
        return exitTexture;
    }

    public TextureRegion getEntryTexture() {
        return entryTexture;
    }

    public Animation<TextureRegion> getTrapAnimation() {
        return trapAnimation;
    }

    public TextureRegion getWallTexture() {
        return wallTexture;
    }

    public TextureRegion getPathTexture() {
        return pathTexture;
    }

    /**
     * Loads a consecutive set of textures of the same size
     *
     * @param origin original texture that needs to be cut
     * @param position base position to start
     * @param size size of individual texture
     * @param offset movement to the position of next frame
     * @param count number of frames to be loaded in total * @return desired textures
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
