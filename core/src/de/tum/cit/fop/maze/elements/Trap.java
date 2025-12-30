package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import de.tum.cit.fop.maze.MazeRunnerGame;

/** A special type of block on which player can walk on but loses health. */
public class Trap extends Block {
    private static final float DAMAGE_INTERVAL = 0.5f;
    private final Animation<TextureRegion> trapAnimation;
    private float lastDamageTime = 0f;
    private boolean playerOnTrap = false;

    // New irregular electric arc effect
    private final ElectricTrapEffect electricEffect;

    public Trap(
            Maze maze,
            TextureRegion texture,
            Animation<TextureRegion> trapAnimation,
            Vector2 position) {
        super(maze, texture, position, false);
        this.trapAnimation = trapAnimation;
        this.electricEffect = new ElectricTrapEffect(maze.getBlockSize());
        this.electricEffect.setPosition(position.x, position.y);
    }

    @Override
    public void onArrival(MazeObject other) {
        if (other instanceof Player) {
            playerOnTrap = true;
        }
    }

    /** Checks if player is in this block. */
    public boolean contains(Rectangle playerHitbox) {
        Rectangle trapRect =
                new Rectangle(
                        getPosition().x,
                        getPosition().y,
                        maze.getBlockSize(),
                        maze.getBlockSize());

        return trapRect.overlaps(playerHitbox);
    }

    @Override
    public void onFrame(float deltaTime) {
        // Update electric VFX (independent from damage)
        electricEffect.setPosition(getPosition().x, getPosition().y);
        electricEffect.update(deltaTime);

        if (playerOnTrap) {
            MazeRunnerGame game = maze.getGame();
            float currentTime = game.getStateTime();
            if (currentTime - lastDamageTime >= DAMAGE_INTERVAL) {
                Player player = maze.getPlayer();
                if (player != null && this.contains(player.getHitbox())) {
                    player.modifyHealth(-10f); // 持续造成伤害
                    lastDamageTime = currentTime;
                } else {
                    playerOnTrap = false; // 玩家已离开陷阱
                }
            }
        }
    }

    @Override
    public void render() {
        // 1. Draw the floor first (otherwise it's black underneath)
        super.render();

        // 2. Keep the existing animated sprite (optional) but remove the old grid-like glow.
        // If you later decide you don't want ANY animation sprite, you can delete these 2 lines.
        float stateTime = Gdx.graphics.getDeltaTime() + maze.getGame().getStateTime();
        TextureRegion currentFrame = trapAnimation.getKeyFrame(stateTime * 2.0f, true);
        super.game.getSpriteBatch().setColor(Color.WHITE);
        super.game.getSpriteBatch().draw(currentFrame, getPosition().x, getPosition().y, maze.getBlockSize(), maze.getBlockSize());

        // 3. Render new irregular electric arcs on top
        electricEffect.render(super.game.getSpriteBatch());
    }

    public void dispose() {
        electricEffect.dispose();
    }
}
