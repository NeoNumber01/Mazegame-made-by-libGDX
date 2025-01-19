package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Timer;

public class Lightning extends InteractiveElements {

    private final Vector2 position;
    private float scale = 1.0f;
    private Vector2 offset = new Vector2(0, 0);

    public Lightning(Maze maze, TextureRegion texture, Vector2 position) {
        super(maze, position, new Vector2(32, 32), new Vector2(0, 0));
        this.position = position;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public void setOffset(Vector2 offset) {
        this.offset = offset;
    }

    @Override
    public void render() {
        renderTextureV2(maze.getGame().getResourcePack().getLightingTexture(), scale, offset);
    }

    @Override
    public void onCollision(MazeObject other) {
        if (other instanceof Player player) {

            player.setSpeedFactor(player.getSpeedFactor() + 100f);

            // 启动一个延迟任务，10 秒后恢复速度
            Timer.schedule(new Timer.Task() {
                @Override
                public void run() {
                    player.setSpeedFactor(player.getSpeedFactor() - 100f);
                }
            }, 10);  // 延迟 10 秒

            maze.getEntities().removeValue(this, true);
        }
    }
}
