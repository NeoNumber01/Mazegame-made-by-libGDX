package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;

public class ExplosionDebris implements Disposable {
    private float x, y;
    private float vx, vy;
    private float rotation;
    private float rotationSpeed;
    private float life;
    private float maxLife;
    private float size;
    private Color color;
    private static Texture texture;

    public ExplosionDebris(float x, float y) {
        this.x = x;
        this.y = y;
        float angle = MathUtils.random(0, 360);
        float speed = MathUtils.random(100f, 300f); // Faster debris
        this.vx = MathUtils.cosDeg(angle) * speed;
        this.vy = MathUtils.sinDeg(angle) * speed;
        this.rotation = MathUtils.random(0, 360);
        this.rotationSpeed = MathUtils.random(-720, 720); // Fast rotation
        this.maxLife = MathUtils.random(1.0f, 2.5f); // Longer life
        this.life = maxLife;
        this.size = MathUtils.random(4f, 12f);
        // Bone-like / Rock-like colors (White, Grey, yellowish)
        float r = MathUtils.random(0.8f, 1.0f);
        float g = MathUtils.random(0.8f, 1.0f);
        float b = MathUtils.random(0.7f, 0.9f);
        this.color = new Color(r, g, b, 1f);

        if (texture == null) {
            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(Color.WHITE);
            pixmap.fill();
            texture = new Texture(pixmap);
            pixmap.dispose();
        }
    }

    public void update(float delta) {
        x += vx * delta;
        y += vy * delta;
        rotation += rotationSpeed * delta;
        life -= delta;
        
        // Drag/Friction
        vx *= 0.92f;
        vy *= 0.92f;
    }

    public void render(SpriteBatch batch) {
        if (texture == null) return;
        
        float alpha = life / maxLife;
        batch.setColor(color.r, color.g, color.b, alpha);
        batch.draw(texture, x, y, size/2, size/2, size, size, 1, 1, rotation, 0, 0, 1, 1, false, false);
        batch.setColor(Color.WHITE);
    }
    
    public boolean isFinished() {
        return life <= 0;
    }

    @Override
    public void dispose() {
        // Texture managed globally or lazily
    }
}
