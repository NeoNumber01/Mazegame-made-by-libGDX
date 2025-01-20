package de.tum.cit.fop.maze;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import games.spooky.gdx.nativefilechooser.desktop.DesktopFileChooser;

/**
 * The DesktopLauncher class is the entry point for the desktop version of the Maze Runner game. It
 * sets up the game window and launches the game using LibGDX framework.
 */
public class DesktopLauncher {
    /**
     * The main method sets up the configuration for the game window and starts the application.
     *
     * @param arg Command line arguments (not used in this application)
     */
    public static void main(String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Maze Runner");

        Graphics.DisplayMode displayMode = Lwjgl3ApplicationConfiguration.getDisplayMode();
        config.setWindowedMode(
                Math.round(0.8f * displayMode.width), Math.round(0.8f * displayMode.height));

        config.setWindowSizeLimits(1280, 720, 9999, 9999);
        config.useVsync(true);

        config.setBackBufferConfig(8, 8, 8, 8, 16, 8, 0);

        new Lwjgl3Application(new MazeRunnerGame(new DesktopFileChooser()), config);
    }
}
