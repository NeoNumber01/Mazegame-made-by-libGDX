package de.tum.cit.fop.maze.elements;

public interface Health {
    void modifyHealth(float delta);

    void onEmptyHealth();
}
