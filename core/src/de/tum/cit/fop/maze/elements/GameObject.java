package de.tum.cit.fop.maze.elements;

import de.tum.cit.fop.maze.MazeRunnerGame;

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
