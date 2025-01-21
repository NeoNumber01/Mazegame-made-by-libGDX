package de.tum.cit.fop.maze.elements;

import de.tum.cit.fop.maze.MazeRunnerGame;

/**
 * Generic superclass, provides a pointer to MazeRunnerGame instance to handle game logic easily.
 */
public abstract class GameObject {
    protected final MazeRunnerGame game;

    public GameObject(MazeRunnerGame game) {
        this.game = game;
    }

    /** Handles per-frame logic of the object. */
    public void onFrame(float deltaTime) {}

    public MazeRunnerGame getGame() {
        return game;
    }
}
