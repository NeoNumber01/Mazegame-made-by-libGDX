package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public class Trap extends Block {
    public Trap(Maze maze, TextureRegion texture, Vector2 position) {
        super(maze, texture, position, false);
    }
    @Override
    public void onArrival(MazeObject other) {
        if (other instanceof Player player) {
            player.setHasKey(true);
            // TODO：播放音效


            ((Player) other).modifyHealth(-10f);

            Vector2 pos = getPosition();
            int i = (int) ((pos.x - maze.getPosition().x) / maze.getBlockSize());
            int j = (int) ((pos.y - maze.getPosition().y) / maze.getBlockSize());
            // 从 Maze 中移fi钥匙
            maze.setBlock(i, j, new Path(maze, game.getResourcePack().getTrapTexture(), pos));
        }
    }
}
