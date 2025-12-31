package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;

import de.tum.cit.fop.maze.GameOverScreen;
import de.tum.cit.fop.maze.Helper;
import de.tum.cit.fop.maze.MazeRunnerGame;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/** The main characters. */
public class Player extends Entity implements Health, Disposable {

    private final MoveAnimation walkAnimation, sprintAnimation, attackAnimation;
    private final float maxHealth;
    private final MazeRunnerGame game;
    private final float shieldDuration = 10f;
    private final float attackAnimationDuration = 0.4f;
    private final Sound playerOnHit;
    private final Sound swing;
    private final Sound monsterHit;
    private final Sound spaceshipSound;
    private long spaceshipSoundId = -1;
    private float health;
    private float lastHitTimestamp;
    private boolean hasKey;
    private float speedFactor = 64f;
    private boolean hasShield = false;
    private float shieldStartTime = 0f;
    private float attackAnimationTimer = 0f;
    private boolean isRed = false;
    private float redEffectTimer = 0f;
    private static final float MAX_SPEED = 96f;
    private boolean isMoving = false;

    // Spaceship Overheat
    private float shipHeat = 0f;
    private boolean shipOverheated = false;
    private static final float SHIP_MAX_HEAT = 1.0f;
    private static final float SHIP_HEAT_RATE = 1.0f / 3.0f;
    private static final float SHIP_COOL_RATE = 1.0f;
    private static final float SHIP_OVERHEAT_PENALTY = 0.5f;
    private float shipOverheatTimer = 0f;
    private float shipFireTimer = 0f;
    private static final float SHIP_FIRE_RATE = 0.1f;

    // Energy Cannon (projectile weapon)
    private EnergyCannon energyCannon;
    private final Vector2 tmpShootDir = new Vector2();
    private final Vector2 tmpMuzzlePos = new Vector2();

    // Reusable movement vector
    private final Vector2 tmpMove = new Vector2();

    // Visual Effects Fields
    private Texture particleTexture;
    private List<AttackParticle> particles;
    private float effectAngle = 0f;

    // Slash Effect System
    private SlashEffectPool slashEffectPool;

    // Spaceship pickup effect
    private boolean spaceshipMode = false;
    private float spaceshipModeTimer = 0f;

    // spaceship movement/visuals
    private final Vector2 shipVel = new Vector2();
    private final Vector2 shipDir = new Vector2(1f, 0f);
    private final Vector2 tmpPos = new Vector2();
    private final Vector2 tmpDeltaPos = new Vector2();
    private final Vector2 tmpNormal = new Vector2();
    private final SpaceshipParticleSystem shipParticles = new SpaceshipParticleSystem();

    private float shipSteerTimer = 0f;

    // tunables
    private static final float SHIP_DURATION = 5f;
    private static final float SHIP_SPEED = 220f;
    private static final float SHIP_STEER_INTERVAL = 0.25f;
    private static final float SHIP_STEER_JITTER_DEG = 55f;
    private static final float SHIP_BOUNCE_SCATTER_DEG = 18f;
    private static final float SHIP_EXIT_MARGIN = 2.0f;
    private static final float SHIP_TEXTURE_FORWARD_OFFSET_DEG = 270f;
    private static final float SHIP_EXIT_PUSHOUT_MAX_RADIUS = 48f;
    private static final float SHIP_EXIT_PUSHOUT_STEP = 4f;

    // Rotating Light Saber Skill (R key)
    private LightSaberOrbit lightSaberOrbit;

    private class AttackParticle {
        float x, y;
        float vx, vy;
        float life, maxLife;
        float size;
        Color color;

        AttackParticle(float x, float y, float directionAngle, boolean isSpark) {
            this.x = x;
            this.y = y;
            float angleSpread = isSpark ? 120f : 45f;
            float angle = directionAngle + MathUtils.random(-angleSpread/2f, angleSpread/2f);
            float speed = isSpark ? MathUtils.random(200f, 400f) : MathUtils.random(50f, 150f);
            this.vx = MathUtils.cosDeg(angle) * speed;
            this.vy = MathUtils.sinDeg(angle) * speed;
            this.maxLife = MathUtils.random(0.2f, 0.4f);
            this.life = maxLife;
            this.size = isSpark ? MathUtils.random(2f, 4f) : MathUtils.random(3f, 6f);
            if (isSpark) {
                this.color = new Color(1f, 1f, 0.8f, 1f);
            } else {
                this.color = new Color(0f, 0.8f, 1f, 1f);
            }
        }
    }

    public Player(MazeRunnerGame game, Maze maze, Vector2 position) {
        super(maze, position, new Vector2(16f, 22f), new Vector2(0f, -5f));
        maze.setPlayer(this);
        walkAnimation = game.getResourcePack().getPlayerWalkAnimation();
        sprintAnimation = game.getResourcePack().getPlayerSprintAnimation();
        attackAnimation = game.getResourcePack().getPlayerAttackAnimation();
        health = maxHealth = 100f;
        this.game = game;
        this.hasKey = false;
        this.playerOnHit = Gdx.audio.newSound(Gdx.files.internal("playeronHit.wav"));
        this.swing = Gdx.audio.newSound(Gdx.files.internal("swing.wav"));
        this.monsterHit = Gdx.audio.newSound(Gdx.files.internal("monster.wav"));
        this.spaceshipSound = Gdx.audio.newSound(Gdx.files.internal("The_sound_of_the_spaceshippickup.wav"));

        // Initialize Energy Cannon
        this.energyCannon = new EnergyCannon(maze);

        // Initialize Effects
        particles = new ArrayList<>();
        slashEffectPool = new SlashEffectPool();
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        particleTexture = new Texture(pixmap);
        pixmap.dispose();

        // Light-saber orbit
        lightSaberOrbit = new LightSaberOrbit(maze, particleTexture);
    }

    @Override
    public void render() {
        if (spaceshipMode) {
            TextureRegion ship = maze.getGame().getResourcePack().getSpaceshipTexture();
            SpriteBatch batch = maze.getGame().getSpriteBatch();

            shipParticles.render(batch);

            float angleDeg = MathUtils.atan2(shipDir.y, shipDir.x) * MathUtils.radiansToDegrees + SHIP_TEXTURE_FORWARD_OFFSET_DEG;

            float scaleGlow = 1.15f;
            float scale = 1.0f;

            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            batch.setColor(0.15f, 0.9f, 1f, 0.18f);
            drawCenteredRot(batch, ship, scaleGlow, angleDeg);

            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            batch.setColor(Color.WHITE);
            drawCenteredRot(batch, ship, scale, angleDeg);
            return;
        }

        if (isRed) {
            maze.getGame().getSpriteBatch().setColor(1f, 0f, 0f, 1f);
        }
        if (attackAnimationTimer > 0f) {
            super.renderTextureV2(
                    attackAnimation.getTextureNoLoop(
                            direction, attackAnimationDuration - attackAnimationTimer),
                    1f,
                    new Vector2(0f, 0f));
        } else {
            MoveAnimation currentMoveAnimation = isSprinting() ? sprintAnimation : walkAnimation;
            TextureRegion texture;
            if (isMoving) {
                texture = currentMoveAnimation.getTexture(direction, super.game.getStateTime());
            } else {
                texture = currentMoveAnimation.getTexture(direction, 0);
            }
            super.renderTexture(texture);
        }
        maze.getGame().getSpriteBatch().setColor(0f, 0f, 0f, 1f);

        renderEffects();
    }

    private void renderEffects() {
        if (!particles.isEmpty()) {
            game.getSpriteBatch().setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            for (AttackParticle p : particles) {
                game.getSpriteBatch().setColor(p.color);
                game.getSpriteBatch().draw(particleTexture, p.x, p.y, p.size, p.size);
            }
            game.getSpriteBatch().setColor(Color.WHITE);
            game.getSpriteBatch().setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }

        slashEffectPool.render(game.getSpriteBatch());
        lightSaberOrbit.render(game.getSpriteBatch(), getCenter());
        energyCannon.render(game.getSpriteBatch());
    }

    private boolean isSprinting() {
        return getMotion() == Motion.SPRINT;
    }

    @Override
    public float getMoveDistance(float deltaTime) {
        return speedFactor * deltaTime * (isSprinting() ? 1.3f : 1f);
    }

    @Override
    public void modifyHealth(float delta) {
        if (delta < 0) {
            if (game.getStateTime() - lastHitTimestamp < 1) {
                return;
            } else {
                lastHitTimestamp = game.getStateTime();
            }
            if (playerOnHit != null) {
                playerOnHit.play();
            }
            isRed = true;
            redEffectTimer = 1f;
        }
        if (delta < 0 && hasShield) {
            delta += 10;
            if (delta > 0) {
                delta = 0;
            }
        }
        if (hasShield && game.getStateTime() - shieldStartTime >= shieldDuration) {
            deactivateShield();
        }
        health += delta;
        if (health > maxHealth) health = maxHealth;
        if (health <= 0) onEmptyHealth();
        System.out.printf("Time=%f, Health=%f, Delta=%f\n", game.getStateTime(), health, delta);
    }

    @Override
    public void onEmptyHealth() {
        System.out.println("Player has died!");
        game.setScreen(new GameOverScreen(game));
    }

    public void activateShield() {
        this.hasShield = true;
        this.shieldStartTime = game.getStateTime();
    }

    private void deactivateShield() {
        this.hasShield = false;
    }

    public MazeRunnerGame getGame() { return game; }
    public boolean hasKey() { return hasKey; }
    public void setHasKey(boolean hasKey) { this.hasKey = hasKey; }
    public float getHealth() { return health; }
    public float getSpeedFactor() { return speedFactor; }
    public void setSpeedFactor(float speedFactor) { this.speedFactor = Math.min(speedFactor, MAX_SPEED); }
    public boolean hasShield() { return this.hasShield; }
    public boolean isSpaceshipMode() { return spaceshipMode; }

    public void activateSpaceshipMode() {
        spaceshipMode = true;
        spaceshipModeTimer = SHIP_DURATION;
        shipDir.setToRandomDirection();
        shipVel.set(shipDir).scl(SHIP_SPEED);
        shipSteerTimer = 0f;

        if (spaceshipSoundId == -1) {
            spaceshipSoundId = spaceshipSound.loop(0.6f);
        }
    }

    @Override
    public void onFrame(float deltaTime) {
        if (spaceshipMode) {
            updateSpaceship(deltaTime);
            slashEffectPool.update(deltaTime);
            lightSaberOrbit.update(deltaTime, getCenter());
            return;
        }

        // Trigger rotating saber skill (R key)
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            lightSaberOrbit.tryActivate();
        }

        lightSaberOrbit.update(deltaTime, getCenter());
        energyCannon.update(deltaTime);

        // Fire Energy projectile (F key)
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            tmpShootDir.setZero();
            switch (direction) {
                case UP -> tmpShootDir.set(0f, 1f);
                case DOWN -> tmpShootDir.set(0f, -1f);
                case LEFT -> tmpShootDir.set(-1f, 0f);
                case RIGHT -> tmpShootDir.set(1f, 0f);
            }
            tmpMuzzlePos.set(getCenter()).mulAdd(tmpShootDir, 12f);
            energyCannon.fire(tmpMuzzlePos, tmpShootDir);
        }

        slashEffectPool.update(deltaTime);

        Iterator<AttackParticle> iter = particles.iterator();
        while (iter.hasNext()) {
            AttackParticle p = iter.next();
            p.life -= deltaTime;
            p.x += p.vx * deltaTime;
            p.y += p.vy * deltaTime;
            p.vx *= 0.92f;
            p.vy *= 0.92f;
            p.color.a = p.life / p.maxLife;
            if (p.life < p.maxLife * 0.5f) {
                p.color.r = MathUtils.lerp(p.color.r, 0f, deltaTime * 5f);
                p.color.g = MathUtils.lerp(p.color.g, 0.8f, deltaTime * 5f);
                p.color.b = MathUtils.lerp(p.color.b, 1f, deltaTime * 5f);
            }
            if (p.life <= 0) {
                iter.remove();
            }
        }

        if (isRed) {
            redEffectTimer -= deltaTime;
            if (redEffectTimer <= 0f) isRed = false;
        }
        if (Objects.requireNonNull(getMotion()) == Motion.ATTACK) {
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            attack();
        } else {
            tmpMove.setZero();
            float deltaDist = getMoveDistance(deltaTime);
            if (Gdx.input.isKeyPressed(Input.Keys.UP)) tmpMove.y += deltaDist;
            if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) tmpMove.y -= deltaDist;
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) tmpMove.x -= deltaDist;
            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) tmpMove.x += deltaDist;

            if (tmpMove.len() > deltaDist) {
                tmpMove.x /= (float) Math.sqrt(2);
                tmpMove.y /= (float) Math.sqrt(2);
            }

            this.performDisplacement(tmpMove);
            if (tmpMove.len2() > 0f) {
                direction = Helper.Vector2Direction(tmpMove);
                isMoving = true;
            } else {
                isMoving = false;
            }
        }

        handleTimers(deltaTime);
    }

    private void handleTimers(float deltaTime) {
        attackAnimationTimer = Math.max(0f, attackAnimationTimer - deltaTime);
    }

    public void attack() {
        if (getMotion() != Motion.ATTACK) {
            attackAnimationTimer = attackAnimationDuration;

            switch (direction) {
                case UP: effectAngle = 90f; break;
                case DOWN: effectAngle = 270f; break;
                case LEFT: effectAngle = 180f; break;
                case RIGHT: effectAngle = 0f; break;
            }

            slashEffectPool.spawn(getCenter().x, getCenter().y, effectAngle);

            for (int i = 0; i < 15; i++) {
                particles.add(new AttackParticle(getCenter().x, getCenter().y, effectAngle, true));
            }
            for (int i = 0; i < 20; i++) {
                float t = MathUtils.random();
                float arcPointAngle = effectAngle - 40f + t * 80f;
                float dist = MathUtils.random(10f, 30f);
                float px = getCenter().x + MathUtils.cosDeg(arcPointAngle) * dist;
                float py = getCenter().y + MathUtils.sinDeg(arcPointAngle) * dist;
                particles.add(new AttackParticle(px, py, arcPointAngle, false));
            }

            float attackHitboxSizeW = 24f, attackHitboxSizeH = 16f;
            Rectangle attackHitbox = switch (direction) {
                case UP -> new Rectangle(getCenter().x - attackHitboxSizeW / 2f, getPosition().y + getSize().y, attackHitboxSizeW, attackHitboxSizeH);
                case DOWN -> new Rectangle(getCenter().x - attackHitboxSizeW / 2f, getPosition().y - attackHitboxSizeH, attackHitboxSizeW, attackHitboxSizeH);
                case LEFT -> new Rectangle(getPosition().x - attackHitboxSizeH, getCenter().y - attackHitboxSizeW / 2f, attackHitboxSizeH, attackHitboxSizeW);
                case RIGHT -> new Rectangle(getPosition().x + getSize().x, getCenter().y - attackHitboxSizeW / 2f, attackHitboxSizeH, attackHitboxSizeW);
            };
            for (MazeObject other : getCollision(attackHitbox)) {
                if (other instanceof Mob mob) {
                    System.out.println("Hit!");
                    mob.modifyHealth(-10f);
                    monsterHit.play();
                    for(int k=0; k<5; k++) particles.add(new AttackParticle(mob.getPosition().x, mob.getPosition().y, effectAngle, true));
                }
            }
            swing.play();
        }
    }

    private Motion getMotion() {
        if (attackAnimationTimer > 0) return Motion.ATTACK;
        if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) return Motion.SPRINT;
        return Motion.WALK;
    }

    private enum Motion { WALK, SPRINT, ATTACK }

    @Override
    public void dispose() {
        if (slashEffectPool != null) {
            slashEffectPool.dispose();
            slashEffectPool = null;
        }
        if (particleTexture != null) {
            particleTexture.dispose();
            particleTexture = null;
        }
        if (lightSaberOrbit != null) {
            lightSaberOrbit.dispose();
            lightSaberOrbit = null;
        }
        if (spaceshipSound != null) {
            spaceshipSound.stop();
            spaceshipSound.dispose();
        }
        if (energyCannon != null) {
            energyCannon.dispose();
            energyCannon = null;
        }
    }

    private void updateSpaceship(float dt) {
        spaceshipModeTimer -= dt;
        if (spaceshipModeTimer <= 0f) {
            spaceshipMode = false;
            shipOverheated = false;
            shipHeat = 0f;

            if (spaceshipSoundId != -1) {
                spaceshipSound.stop(spaceshipSoundId);
                spaceshipSoundId = -1;
            }

            clampInsideMazeBorder(SHIP_EXIT_MARGIN);
            snapToNearestWalkableTile();
            resolveStuckInWall();
            shipParticles.update(dt);
            return;
        }

        shipSteerTimer -= dt;
        if (shipSteerTimer <= 0f) {
            shipSteerTimer = SHIP_STEER_INTERVAL * MathUtils.random(0.7f, 1.4f);
            float turn = MathUtils.random(-SHIP_STEER_JITTER_DEG, SHIP_STEER_JITTER_DEG);
            shipDir.set(shipVel).nor().rotateDeg(turn);
            shipVel.set(shipDir).scl(SHIP_SPEED);
        }

        if (shipOverheated) {
            shipOverheatTimer -= dt;
            if (shipOverheatTimer <= 0f) {
                shipOverheated = false;
                shipHeat = 0f;
            }
        } else {
            if (!Gdx.input.isKeyPressed(Input.Keys.F)) {
                shipHeat -= SHIP_COOL_RATE * dt;
                if (shipHeat < 0f) shipHeat = 0f;
            }
        }

        if (Gdx.input.isKeyPressed(Input.Keys.F) && !shipOverheated) {
            shipHeat += SHIP_HEAT_RATE * dt;
            if (shipHeat >= SHIP_MAX_HEAT) {
                shipOverheated = true;
                shipOverheatTimer = SHIP_OVERHEAT_PENALTY;
            } else {
                shipFireTimer -= dt;
                if (shipFireTimer <= 0f) {
                    shipFireTimer = SHIP_FIRE_RATE;
                    tmpMuzzlePos.set(getCenter()).mulAdd(shipDir, 12f);
                    energyCannon.fireForced(tmpMuzzlePos, shipDir);
                }
            }
        }

        tmpPos.set(getPosition());
        float nx = tmpPos.x + shipVel.x * dt;
        float ny = tmpPos.y + shipVel.y * dt;

        float minX = maze.getBorder().x;
        float minY = maze.getBorder().y;
        float maxX = maze.getBorder().x + maze.getBorder().width - getSize().x;
        float maxY = maze.getBorder().y + maze.getBorder().height - getSize().y;

        boolean bounced = false;
        tmpNormal.set(0f, 0f);

        if (nx < minX) { nx = minX; tmpNormal.set(1f, 0f); bounced = true; }
        else if (nx > maxX) { nx = maxX; tmpNormal.set(-1f, 0f); bounced = true; }

        if (ny < minY) { ny = minY; tmpNormal.set(0f, 1f); bounced = true; }
        else if (ny > maxY) { ny = maxY; tmpNormal.set(0f, -1f); bounced = true; }

        if (bounced) {
            float dot = shipVel.dot(tmpNormal);
            shipVel.mulAdd(tmpNormal, -2f * dot);
            shipVel.rotateDeg(MathUtils.random(-SHIP_BOUNCE_SCATTER_DEG, SHIP_BOUNCE_SCATTER_DEG));
            shipDir.set(shipVel).nor();
            shipVel.set(shipDir).scl(SHIP_SPEED);
        }

        if (shipVel.len2() > 0.0001f) {
            shipDir.set(shipVel).nor();
        }

        tmpDeltaPos.set(nx - tmpPos.x, ny - tmpPos.y);
        super.displace(tmpDeltaPos);

        float cx = getCenter().x;
        float cy = getCenter().y;
        float tailX = cx - shipDir.x * 14f;
        float tailY = cy - shipDir.y * 14f;
        shipParticles.emit(tailX, tailY, shipDir.x, shipDir.y, 5);
        shipParticles.update(dt);
    }

    private void resolveStuckInWall() {
        if (!checkCollision(getHitbox())) return;

        tmpPos.set(getPosition());
        float bestDx = 0f, bestDy = 0f;
        boolean found = false;

        for (float r = SHIP_EXIT_PUSHOUT_STEP; r <= SHIP_EXIT_PUSHOUT_MAX_RADIUS; r += SHIP_EXIT_PUSHOUT_STEP) {
            for (int a = 0; a < 16; a++) {
                float ang = a * (360f / 16f);
                float dx = MathUtils.cosDeg(ang) * r;
                float dy = MathUtils.sinDeg(ang) * r;

                float testX = tmpPos.x + dx;
                float testY = tmpPos.y + dy;

                float minX = maze.getBorder().x + SHIP_EXIT_MARGIN;
                float minY = maze.getBorder().y + SHIP_EXIT_MARGIN;
                float maxX = maze.getBorder().x + maze.getBorder().width - getSize().x - SHIP_EXIT_MARGIN;
                float maxY = maze.getBorder().y + maze.getBorder().height - getSize().y - SHIP_EXIT_MARGIN;
                if (testX < minX || testX > maxX || testY < minY || testY > maxY) continue;

                tmpDeltaPos.set(dx, dy);
                super.displace(tmpDeltaPos);
                boolean ok = !checkCollision(getHitbox());
                tmpDeltaPos.set(-dx, -dy);
                super.displace(tmpDeltaPos);

                if (ok) {
                    bestDx = dx;
                    bestDy = dy;
                    found = true;
                    break;
                }
            }
            if (found) break;
        }

        if (found) {
            tmpDeltaPos.set(bestDx, bestDy);
            super.displace(tmpDeltaPos);
        } else {
            Vector2 entryCenter = maze.getEntry().getCenter();
            tmpDeltaPos.set(entryCenter.x - getCenter().x, entryCenter.y - getCenter().y);
            super.displace(tmpDeltaPos);
            clampInsideMazeBorder(SHIP_EXIT_MARGIN);
        }
    }

    private void clampInsideMazeBorder(float margin) {
        tmpPos.set(getPosition());

        float minX = maze.getBorder().x + margin;
        float minY = maze.getBorder().y + margin;
        float maxX = maze.getBorder().x + maze.getBorder().width - getSize().x - margin;
        float maxY = maze.getBorder().y + maze.getBorder().height - getSize().y - margin;

        float cx = MathUtils.clamp(tmpPos.x, minX, maxX);
        float cy = MathUtils.clamp(tmpPos.y, minY, maxY);

        tmpDeltaPos.set(cx - tmpPos.x, cy - tmpPos.y);
        super.displace(tmpDeltaPos);
    }

    private void snapToNearestWalkableTile() {
        float tile = maze.getBlockSize();

        float cx = getCenter().x;
        float cy = getCenter().y;
        int startTx = (int) ((cx - maze.getPosition().x) / tile);
        int startTy = (int) ((cy - maze.getPosition().y) / tile);

        startTx = MathUtils.clamp(startTx, 0, maze.getWidth() - 1);
        startTy = MathUtils.clamp(startTy, 0, maze.getHeight() - 1);

        final int maxRadius = 12;
        int bestTx = startTx;
        int bestTy = startTy;
        boolean found = false;

        for (int r = 0; r <= maxRadius && !found; r++) {
            for (int dx = -r; dx <= r && !found; dx++) {
                int tx1 = startTx + dx;
                int ty1 = startTy - r;
                int ty2 = startTy + r;

                if (isWalkableTile(tx1, ty1)) { bestTx = tx1; bestTy = ty1; found = true; break; }
                if (isWalkableTile(tx1, ty2)) { bestTx = tx1; bestTy = ty2; found = true; break; }
            }
            for (int dy = -r + 1; dy <= r - 1 && !found; dy++) {
                int ty = startTy + dy;
                int tx1 = startTx - r;
                int tx2 = startTx + r;

                if (isWalkableTile(tx1, ty)) { bestTx = tx1; bestTy = ty; found = true; break; }
                if (isWalkableTile(tx2, ty)) { bestTx = tx2; bestTy = ty; found = true; break; }
            }
        }

        if (!found) return;

        float targetCx = maze.getPosition().x + bestTx * tile + tile * 0.5f;
        float targetCy = maze.getPosition().y + bestTy * tile + tile * 0.5f;

        float targetX = targetCx - getSize().x * 0.5f;
        float targetY = targetCy - getSize().y * 0.5f;

        tmpPos.set(getPosition());
        tmpDeltaPos.set(targetX - tmpPos.x, targetY - tmpPos.y);
        super.displace(tmpDeltaPos);

        clampInsideMazeBorder(SHIP_EXIT_MARGIN);
    }

    private boolean isWalkableTile(int tx, int ty) {
        if (tx <= 0 || ty <= 0 || tx >= maze.getWidth() - 1 || ty >= maze.getHeight() - 1) return false;
        return !maze.isWall(tx, ty);
    }

    private void drawCenteredRot(SpriteBatch batch, TextureRegion region, float scale, float angleDeg) {
        float w = region.getRegionWidth() * scale;
        float h = region.getRegionHeight() * scale;
        float cx = getCenter().x;
        float cy = getCenter().y;

        batch.draw(
                region,
                cx - w / 2f,
                cy - h / 2f,
                w / 2f,
                h / 2f,
                w,
                h,
                1f,
                1f,
                angleDeg);
    }
}

