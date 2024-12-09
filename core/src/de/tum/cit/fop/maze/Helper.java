package de.tum.cit.fop.maze;

import com.badlogic.gdx.math.Vector2;

public class Helper {

    public static enum Direction {
        UP,
        DOWN,
        LEFT,
        RIGHT;

        /**
         * Converts the direction to a vector. Useful when moving objects in a specific direction.
         *
         * @return vector representing the direction
         */
        public Vector2 toVector2(float factor) {
            return switch (this) {
                case UP -> new Vector2(0, factor);
                case DOWN -> new Vector2(0, -factor);
                case LEFT -> new Vector2(-factor, 0);
                case RIGHT -> new Vector2(factor, 0);
            };
        }

        /**
         * Converts the direction to array index. Useful when storing direction-specific data in an array.
         *
         * @return index value in [0, 3], delegated to Up, Down, Left, Right respectively
         */
        public int toIndex() {
            return switch (this) {
                case UP -> 0;
                case DOWN -> 1;
                case LEFT -> 2;
                case RIGHT -> 3;
            };
        }
    }
}
