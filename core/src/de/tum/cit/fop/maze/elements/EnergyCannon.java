package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Disposable;

/**
 * Energy cannon weapon: press key to launch a projectile that bounces off walls and kills mobs.
 *
 * This replaces the old LaserGun hitscan.
 */
public class EnergyCannon implements Disposable {

    private final Maze maze;
    private final Array<EnergyProjectile> active;
    private final Pool<EnergyProjectile> pool;
    private final Sound shootSound;

    // Tunables
    private float cooldown = 4.0f;
    private float cooldownTimer = 0f;

    // projectile parameters
    private float projectileSpeed = 260f;
    private float projectileRadius = 4f;
    private int maxBounces = 6;
    private float maxLifetime = 2.0f;

    public EnergyCannon(Maze maze) {
        this.maze = maze;
        this.active = new Array<>();
        this.pool = new Pool<EnergyProjectile>() {
            @Override
            protected EnergyProjectile newObject() {
                return new EnergyProjectile();
            }
        };
        this.shootSound = Gdx.audio.newSound(Gdx.files.internal("The_sound_of_EnergyCannon.wav"));
    }

    public void update(float dt) {
        if (cooldownTimer > 0f) cooldownTimer -= dt;

        for (int i = active.size - 1; i >= 0; i--) {
            EnergyProjectile p = active.get(i);
            p.update(dt, maze);
            if (!p.isAlive()) {
                active.removeIndex(i);
                pool.free(p);
            }
        }
    }

    public void render(SpriteBatch batch) {
        for (EnergyProjectile p : active) {
            p.render(batch);
        }
    }

    /**
     * Fire one projectile from muzzle in direction (must be normalized or non-zero).
     */
    public void fire(Vector2 muzzlePos, Vector2 dir) {
        if (cooldownTimer > 0f) return;
        if (dir == null || dir.isZero(0.0001f)) return;
        cooldownTimer = cooldown;

        fireForced(muzzlePos, dir);
    }

    public void fireForced(Vector2 muzzlePos, Vector2 dir) {
        EnergyProjectile p = pool.obtain();
        p.init(muzzlePos.x, muzzlePos.y, dir.x, dir.y,
                projectileSpeed,
                projectileRadius,
                maxBounces,
                maxLifetime);
        active.add(p);
        shootSound.play(0.5f);
    }

    @Override
    public void dispose() {
        if (shootSound != null) {
            shootSound.dispose();
        }
    }

    // Optional setters if you want to tweak from Player
    public void setCooldown(float cooldown) { this.cooldown = cooldown; }
    public void setProjectileSpeed(float projectileSpeed) { this.projectileSpeed = projectileSpeed; }
    public void setProjectileRadius(float projectileRadius) { this.projectileRadius = projectileRadius; }
    public void setMaxBounces(int maxBounces) { this.maxBounces = maxBounces; }
    public void setMaxLifetime(float maxLifetime) { this.maxLifetime = maxLifetime; }
}
