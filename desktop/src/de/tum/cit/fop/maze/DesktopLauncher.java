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

        // 获取显示器的分辨率
        Graphics.DisplayMode displayMode = Lwjgl3ApplicationConfiguration.getDisplayMode();
        // 设置窗口大小 (这里示例：80% 的屏幕大小)
        config.setWindowedMode(
            Math.round(0.8f * displayMode.width), Math.round(0.8f * displayMode.height)
        );

        // 开启垂直同步 (Vsync)
        config.useVsync(true);

        // 配置后缓冲区格式: RGBA 各 8 位，深度 16 位，Stencil 8 位，采样 0
        // 参数含义: (r, g, b, a, depth, stencil, msaaSamples)
        config.setBackBufferConfig(
            8, 8, 8, 8,  // RGBA 各 8 位
            16,          // 深度缓冲 16 位
            8,           // 模板缓冲 8 位 (Stencil)
            0            // MSAA 采样次数 (可以根据需要改成 4、8 等)
        );

        new Lwjgl3Application(new MazeRunnerGame(new DesktopFileChooser()), config);
    }
}
