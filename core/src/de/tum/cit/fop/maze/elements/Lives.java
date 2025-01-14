package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.Helper;

public class Lives extends InteractiveElements {
    private int lives;
    private Vector2 position;



    public Lives(Maze maze, TextureRegion texture, Vector2 position) {
        super(maze, position, new Vector2(32, 32), new Vector2(0, 0));
        this.position = position;
    }


    @Override
    public void render() {
        // 确保纹理和位置正确渲染
        maze.getGame().getSpriteBatch().draw(
            maze.getGame().getResourcePack().getLivesTexture(),
            position.x, position.y,
            maze.getBlockSize(), maze.getBlockSize()
        );
    }

    @Override
    public void onCollision(MazeObject other) {
        // 判断碰撞的是否是 Player
        if (other instanceof Player player) {
            // 增加玩家的生命值
            player.modifyHealth(20);


            // 从实体列表中移除 Lives
            maze.getEntities().removeValue(this, true);
        }

    }

}
