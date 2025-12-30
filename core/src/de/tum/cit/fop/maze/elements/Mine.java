package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** A special type of obstacle that explodes after collides with player. */
public class Mine extends InteractiveElements {
    private final Animation<TextureRegion> explosionAnimation;
    private final float explosionRadius = 40f; // Reduced range
    private final int damage = 40; //
    private final float delayBeforeExplosion = 1.0f;
    private final float explosionDuration = 2.0f;
    private final Sound explosion;
    private boolean triggered = false;
    private boolean exploded = false;
    private float explosionStartTime = -1f;
    private final List<ExplosionDebris> debrisList = new ArrayList<>();

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
        
        // Trigger Screen Shake
        if (maze.getCamera() != null) {
            maze.getCamera().shake(1.0f, 15f); // 1 sec, 15 intensity
        }

        // Spawn Debris
        for (int i = 0; i < 30; i++) {
            debrisList.add(new ExplosionDebris(getCenter().x, getCenter().y));
        }

        for (MazeObject obj : maze) {
            float distance = getPosition().dst(obj.getPosition());
            if (distance <= explosionRadius) {
                // Linear damage falloff: 100% at center, 0% at edge
                float scale = 1.0f - (distance / explosionRadius);
                int actualDamage = Math.max(0, (int) (damage * scale));
                
                if (obj instanceof Player player) {
                    player.modifyHealth(-actualDamage);
                    System.out.println("Player took damage: " + actualDamage);
                } else if (obj instanceof Mob mob) {
                    mob.modifyHealth(-actualDamage);
                    System.out.println("Mob took damage: " + actualDamage);
                }
            }
        }
    }

    @Override
    public void render() {
        float currentTime = maze.getGame().getStateTime();
        float deltaTime = Gdx.graphics.getDeltaTime();

        if (triggered && !exploded) {
            if (currentTime - explosionStartTime >= delayBeforeExplosion) {
                explode();
                explosionStartTime = currentTime;
            }
        }

        if (exploded) {
            float stateTime = currentTime - explosionStartTime;
            
            // Draw Explosion Animation (Larger)
            if (!explosionAnimation.isAnimationFinished(stateTime)) {
                TextureRegion explosionFrame = explosionAnimation.getKeyFrame(stateTime, false);
                float animSize = 64f; // Reduced size
                maze.getGame()
                        .getSpriteBatch()
                        .draw(
                                explosionFrame,
                                getCenter().x - animSize / 2f,
                                getCenter().y - animSize / 2f,
                                animSize,
                                animSize);
            }

            // Update and Render Debris
            Iterator<ExplosionDebris> iter = debrisList.iterator();
            while (iter.hasNext()) {
                ExplosionDebris debris = iter.next();
                debris.update(deltaTime);
                debris.render(maze.getGame().getSpriteBatch());
                if (debris.isFinished()) {
                    iter.remove();
                }
            }

            if (explosionAnimation.isAnimationFinished(stateTime) && debrisList.isEmpty()) {
                maze.getEntities().removeValue(this, true);
            }
        } else {
            renderTextureV2(
                    maze.getGame().getResourcePack().getMineTexture(), 1f, new Vector2(0, 0));
        }
    }
}
