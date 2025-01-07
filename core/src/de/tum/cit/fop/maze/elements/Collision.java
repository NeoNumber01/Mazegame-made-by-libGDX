package de.tum.cit.fop.maze.elements;

import com.badlogic.gdx.math.Rectangle;

public interface Collision {
    boolean overlaps(Rectangle other);
}
