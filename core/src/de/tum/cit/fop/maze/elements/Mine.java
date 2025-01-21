package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public class Mine extends InteractiveElements {
    private final Animation<TextureRegion> explosionAnimation;
    private final float explosionRadius = 40f;
    private final int damage = 20; //
    private final float delayBeforeExplosion = 2.0f;
    private final float explosionDuration = 2.0f;
    private boolean triggered = false;
    private boolean exploded = false;
    private float explosionStartTime = -1f;
    private Sound explosion;

    public Mine(
            Maze maze,
            TextureRegion texture,
            Vector2 position,
            Animation<TextureRegion> explosionAnimation) {
        super(maze, position, new Vector2(16, 16), new Vector2(0, 0));
        this.explosionAnimation = explosionAnimation;
        this.explosion = Gdx.audio.newSound(Gdx.files.internal("explode.ogg"));
    }

    @Override
    public void onCollision(MazeObject other) {
        if (!triggered && other instanceof Player) {
            triggerExplosion();
        }
    }

    private void triggerExplosion() {
        triggered = true;
        explosionStartTime = maze.getGame().getStateTime();
        System.out.println("Mine triggered! Explosion in " + delayBeforeExplosion + " seconds.");
    }

    private void explode() {
        exploded = true;
        explosion.play();
        System.out.println("Mine exploded!");

        for (MazeObject obj : maze) {
            if (obj instanceof Player player) {
                float distance = getPosition().dst(player.getPosition());
                if (distance <= explosionRadius) {
                    player.modifyHealth(-damage);
                    System.out.println("Player took damage: " + damage);
                }
            }
        }
    }

    @Override
    public void render() {
        float currentTime = maze.getGame().getStateTime();

        if (triggered && !exploded) {
            if (currentTime - explosionStartTime >= delayBeforeExplosion) {
                explode();
                explosionStartTime = currentTime;
            }
        }

        if (exploded) {
            float stateTime = currentTime - explosionStartTime;
            TextureRegion explosionFrame = explosionAnimation.getKeyFrame(stateTime, false);
            maze.getGame()
                    .getSpriteBatch()
                    .draw(
                            explosionFrame,
                            getPosition().x,
                            getPosition().y,
                            getSize().x,
                            getSize().y);

            if (explosionAnimation.isAnimationFinished(stateTime)) {
                maze.getEntities().removeValue(this, true);
            }
        } else {
            renderTextureV2(
                    maze.getGame().getResourcePack().getMineTexture(), 1f, new Vector2(0, 0));
        }
    }
}
