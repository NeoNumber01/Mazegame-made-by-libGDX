package de.tum.cit.fop.maze.elements;

/** Basic health mechanic, may trigger event on empty health */
public interface Health {
    /**
     * A wrapper to perform add/minus on health. Max health or on-death event can be triggered here.
     */
    void modifyHealth(float delta);

    /** Defines event to be triggered on empty health i.e. death. Such as destroy the object. */
    default void onEmptyHealth() {}
}
