package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Timer;

/** Interactive collectable that provides speed upgrades for the player. */
public class Lightning extends InteractiveElements {
    private final Sound lightning;
    private float scale = 1.0f;

    public Lightning(Maze maze, TextureRegion texture, Vector2 position) {
        super(maze, position, new Vector2(16f, 16f), new Vector2(0, 0));
        this.lightning = Gdx.audio.newSound(Gdx.files.internal("Keys.mp3"));
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    @Override
    public void render() {
        renderTextureV2(maze.getGame().getResourcePack().getLightingTexture(), scale);
    }

    @Override
    public void onCollision(MazeObject other) {
        if (other instanceof Player player) {

            player.setSpeedFactor(player.getSpeedFactor() + 100f);
            lightning.play();

            // 启动一个延迟任务，10 秒后恢复速度
            Timer.schedule(
                    new Timer.Task() {
                        @Override
                        public void run() {
                            player.setSpeedFactor(player.getSpeedFactor() - 100f);
                        }
                    },
                    10); // 延迟 10 秒

            maze.getEntities().removeValue(this, true);
        }
    }
}
