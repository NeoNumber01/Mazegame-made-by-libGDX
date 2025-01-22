package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

/** A special type of obstacle that explodes after collides with player. */
public class Mine extends InteractiveElements {
    private final Animation<TextureRegion> explosionAnimation;
    private final float explosionRadius = 40f;
    private final int damage = 40; //
    private final float delayBeforeExplosion = 1.0f;
    private final float explosionDuration = 2.0f;
    private final Sound explosion;
    private boolean triggered = false;
    private boolean exploded = false;
    private float explosionStartTime = -1f;

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
        super.onCollision(other);
        if (!triggered && other instanceof Player) {
            triggerExplosion();
        }
    }

    private void triggerExplosion() {
        triggered = true;
        explosionStartTime = maze.getGame().getStateTime();
        System.out.println("Mine triggered! Explosion in " + delayBeforeExplosion + " seconds.");
    }

    /** Trigger sound effect, texture change, and deal damage to player. */
    private void explode() {
        exploded = true;
        explosion.play();
        System.out.println("Mine exploded!");

        for (MazeObject obj : maze) {
            float distance = getPosition().dst(obj.getPosition());
            if (distance <= explosionRadius) {
                if (obj instanceof Player player) {
                    player.modifyHealth(-damage);
                    System.out.println("Player took damage: " + damage);
                } else if (obj instanceof Mob mob) {
                    mob.modifyHealth(-damage);
                    System.out.println("Mob took damage: " + damage);
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
