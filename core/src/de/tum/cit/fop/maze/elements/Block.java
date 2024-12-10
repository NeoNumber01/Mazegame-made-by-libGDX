package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

import de.tum.cit.fop.maze.Constants;

public class Block extends GameObject implements Visible {
    private TextureRegion texture;
    private Rectangle box;

    public Block(TextureRegion texture, float x, float y) {
        this.texture = texture;
        this.box = new Rectangle(x, y, Constants.BLOCK_SIZE, Constants.BLOCK_SIZE);
    }

    public Rectangle getBox() {
        return box;
    }

    @Override
    public TextureRegion getTexture(float stateTime) {
        return texture;
    }
}
