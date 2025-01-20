package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.MazeRunnerGame;

public class Trap extends Block {
    private static final float DAMAGE_INTERVAL = 0.5f;
    private float lastDamageTime = 0f;
    private boolean playerOnTrap = false;

    public Trap(Maze maze, TextureRegion texture, Vector2 position) {
        super(maze, texture, position, false);
    }

    @Override
    public void onArrival(MazeObject other) {
        if (other instanceof Player player) {
            // TODO：播放音效
            playerOnTrap = true;

            Vector2 pos = getPosition();
            int i = (int) ((pos.x - maze.getPosition().x) / maze.getBlockSize());
            int j = (int) ((pos.y - maze.getPosition().y) / maze.getBlockSize());
        }
    }

    @Override
    public void onFrame(float deltaTime) {
        if (playerOnTrap) {
            MazeRunnerGame game = maze.getGame();
            float currentTime = game.getStateTime();
            if (currentTime - lastDamageTime >= DAMAGE_INTERVAL) {
                Player player = maze.getPlayer();
                if (player != null && this.contains(player.getPosition(),player.getHitbox())) {
                    player.modifyHealth(-10f); //持续造成伤害
                    lastDamageTime = currentTime;
                } else {
                    playerOnTrap = false; //玩家已离开陷阱
                }
            }
        }
    }
    public boolean contains(Vector2 playerPosition, Rectangle playerHitbox) {
        Rectangle trapRect = new Rectangle(
            getPosition().x, //陷阱左上角的 x 坐标
            getPosition().y, //陷阱左上角的 y 坐标
            maze.getBlockSize(), //陷阱宽
            maze.getBlockSize()  //陷阱高
        );

        //检测玩家碰撞盒是否与陷阱碰撞盒重叠
        return trapRect.overlaps(playerHitbox);
    }

}
