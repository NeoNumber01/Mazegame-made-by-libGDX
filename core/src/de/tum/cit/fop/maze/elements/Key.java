package de.tum.cit.fop.maze.elements;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
public class Key extends Block {
    public Key(Maze maze, TextureRegion texture, Vector2 position){
        super(maze, texture, position, false);
    }
    @Override
    public void onArrival(MazeObject other) {
        if (other instanceof Player player) {
            // 给玩家标记 hasKey
            player.setHasKey(true);
            // 播放捡到钥匙的音效
            // game.playSound("pickup_key.wav");
            // 在控制台输出或 HUD 提示
            System.out.println("You've picked up the key!");
            Vector2 pos = getPosition();
            int i = (int) ((pos.x - maze.getPosition().x) / maze.getBlockSize());
            int j = (int) ((pos.y - maze.getPosition().y) / maze.getBlockSize());
            // 从 Maze 中移除这把钥匙，或者让它不再渲染/碰撞
            // 如果您要完全移除，需要考虑 Maze 迭代时的安全性，可以标记一下让 MazeScreen 下次循环时移除
            maze.setBlock(i, j, new Path(maze, game.getResourcePack().getBlockTexture(), pos));
            // 若 Key 并不在 entities 里，也可以将所属的 maze[][] 里对应格子设为 null 或 Path。
        }
    }

}
