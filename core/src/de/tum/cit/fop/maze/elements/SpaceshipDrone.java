package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * A drone-like spaceship entity that ignores obstacles and bounces off maze border.
 * It flies around with random steering, leaving thruster particles.
 */
public class SpaceshipDrone extends Entity {

    private final SpaceshipParticleSystem particles;

    // movement
    private final Vector2 vel = new Vector2();
    private final Vector2 dir = new Vector2(1, 0);

    // tunables
    public float speed = 180f;
    public float steeringJitterDeg = 40f; // random steering strength
    public float steeringInterval = 0.35f;

    private float steerTimer = 0f;

    private final Vector2 tmpDelta = new Vector2();

    public SpaceshipDrone(Maze maze, Vector2 position) {
        super(maze, position, new Vector2(28, 28), new Vector2(0, 0));
        particles = new SpaceshipParticleSystem();
        dir.setToRandomDirection();
        vel.set(dir).scl(speed);
    }

    @Override
    public void onFrame(float deltaTime) {
        // random steering
        steerTimer -= deltaTime;
        if (steerTimer <= 0f) {
            steerTimer = steeringInterval * MathUtils.random(0.7f, 1.4f);
            float turn = MathUtils.random(-steeringJitterDeg, steeringJitterDeg);
            dir.rotateDeg(turn).nor();
            vel.set(dir).scl(speed);
        }

        // move ignoring obstacles, but bounce on border
        Vector2 pos = getPosition();
        float nx = pos.x + vel.x * deltaTime;
        float ny = pos.y + vel.y * deltaTime;

        // border (maze.getBorder includes outer walls). We'll bounce within it.
        float minX = maze.getBorder().x;
        float minY = maze.getBorder().y;
        float maxX = maze.getBorder().x + maze.getBorder().width - getSize().x;
        float maxY = maze.getBorder().y + maze.getBorder().height - getSize().y;

        boolean bounced = false;
        if (nx < minX) { nx = minX; vel.x = Math.abs(vel.x); bounced = true; }
        if (nx > maxX) { nx = maxX; vel.x = -Math.abs(vel.x); bounced = true; }
        if (ny < minY) { ny = minY; vel.y = Math.abs(vel.y); bounced = true; }
        if (ny > maxY) { ny = maxY; vel.y = -Math.abs(vel.y); bounced = true; }

        if (bounced) {
            // add a little random angle so it doesn't get stuck in axis bounces
            vel.rotateDeg(MathUtils.random(-30f, 30f));
            dir.set(vel).nor();
        }

        // apply displacement directly (ignore collision)
        tmpDelta.set(nx - pos.x, ny - pos.y);
        super.displace(tmpDelta);

        // emit thruster particles from tail
        float cx = getCenter().x;
        float cy = getCenter().y;
        float tailX = cx - dir.x * 14f;
        float tailY = cy - dir.y * 14f;
        particles.emit(tailX, tailY, dir.x, dir.y, 4);
        particles.update(deltaTime);
    }

    @Override
    public void render() {
        SpriteBatch batch = maze.getGame().getSpriteBatch();
        TextureRegion tex = maze.getGame().getResourcePack().getSpaceshipTexture();

        // particles behind ship
        particles.render(batch);

        // draw ship with slight glow
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        batch.setColor(0.15f, 0.9f, 1f, 0.18f);
        renderTextureV2(tex, 1.15f);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(Color.WHITE);

        // rotate sprite? (we keep unrotated to avoid extra draw complexity in this project)
        renderTextureV2(tex, 1.0f);
    }
}
