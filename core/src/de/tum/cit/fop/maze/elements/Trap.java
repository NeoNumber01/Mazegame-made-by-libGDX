package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import de.tum.cit.fop.maze.MazeRunnerGame;

/** A special type of block on which player can walk on but loses health. */
public class Trap extends Block {
    private static final float DAMAGE_INTERVAL = 0.5f;
    private final Animation<TextureRegion> trapAnimation;
    private float lastDamageTime = 0f;
    private boolean playerOnTrap = false;
    private float stateTime = 0f;

    public Trap(
            Maze maze,
            TextureRegion texture,
            Animation<TextureRegion> trapAnimation,
            Vector2 position) {
        super(maze, texture, position, false);
        this.trapAnimation = trapAnimation;
    }

    @Override
    public void onArrival(MazeObject other) {
        if (other instanceof Player player) {
            playerOnTrap = true;
        }
    }

    @Override
    public void onFrame(float deltaTime) {
        if (playerOnTrap) {
            MazeRunnerGame game = maze.getGame();
            float currentTime = game.getStateTime();
            if (currentTime - lastDamageTime >= DAMAGE_INTERVAL) {
                Player player = maze.getPlayer();
                if (player != null && this.contains(player.getPosition(), player.getHitbox())) {
                    player.modifyHealth(-10f); // 持续造成伤害
                    lastDamageTime = currentTime;
                } else {
                    playerOnTrap = false; // 玩家已离开陷阱
                }
            }
        }
    }

    /** Checks if player is in this block. */
    public boolean contains(Vector2 playerPosition, Rectangle playerHitbox) {
        Rectangle trapRect =
                new Rectangle(
                        getPosition().x, // 陷阱左上角的 x 坐标
                        getPosition().y, // 陷阱左上角的 y 坐标
                        maze.getBlockSize(), // 陷阱宽
                        maze.getBlockSize() // 陷阱高
                        );

        // 检测玩家碰撞盒是否与陷阱碰撞盒重叠
        return trapRect.overlaps(playerHitbox);
    }

    @Override
    public void render() {
        stateTime += Gdx.graphics.getDeltaTime();
        TextureRegion currentFrame = trapAnimation.getKeyFrame(stateTime, true);

        super.game
                .getSpriteBatch()
                .draw(
                        currentFrame,
                        getPosition().x,
                        getPosition().y,
                        maze.getBlockSize(),
                        maze.getBlockSize());
    }
}
