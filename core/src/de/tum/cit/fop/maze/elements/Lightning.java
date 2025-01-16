package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public class Lightning extends InteractiveElements {

    private final Vector2 position;

    public Lightning(Maze maze, TextureRegion texture, Vector2 position) {
        super(maze, position, new Vector2(32, 32), new Vector2(0, 0));
        this.position = position;
    }

    @Override
    public void render() {
        // 确保纹理和位置正确渲染
        maze.getGame()
                .getSpriteBatch()
                .draw(
                        maze.getGame().getResourcePack().getLightingTexture(),
                        position.x,
                        position.y,
                        maze.getBlockSize(),
                        maze.getBlockSize());
    }

    @Override
    public void onCollision(MazeObject other) {
        if (other instanceof Player player) {

            player.setSpeedFactor(player.getSpeedFactor() + 100f);

            maze.getEntities().removeValue(this, true);
        }
    }
}
