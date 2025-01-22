package de.tum.cit.fop.maze;

/** A valid maze must have entrance, key and exit. */
public class InvalidMaze extends RuntimeException {
    public InvalidMaze(String message) {
        super(message);
    }
}
