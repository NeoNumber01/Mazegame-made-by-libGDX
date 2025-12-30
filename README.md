# Mazerunner

## Build & Run

This project utilizes gradle as build system.

### Using Jetbrains IDEA (recommended)

Run configuration is provided in `.idea` folder.

In Jetbrains IDEA, simply click the green triangle beside "Run Game" on top-right and the game should start.

#### Note for MacOS users

You may need to add `-XstartOnFirstThread` to VM options.

Click the 3-dots on the right of the play button. Find and click "Configuration - Edit". In the pop-up menu, find "Build
and run", then click "Modify options" on the right. Make sure you select "Add VM options". Then you'll find "VM options"
input box appear under "Build and run". paste the mentioned argument and click `OK` on the very bottom to save.

## Gameplay

### Keybindings

- Arrow keys: movement
    - press `Shift` to sprint
- `Space`: attack ,also for spaceship
- `z`/`x`: camera zoom in/out
- `Esc`: pause menu
- `R`: saber surrounding to attack
- `F`: energy cannon attack
- `Q`: lightning attack

## Technical details

The game consists of two subprojects, `desktop` and `core`. The former one spawns the game window, and the latter one
manages all in-game elements.

In `core` class, the following class encapsulates core features and provides basic abstraction:

- `*Screen`: Each of them corresponds to a different stage of the program. E.g. `MenuScreen` is the main screen of the
  game. `GameScreen` is where actual gameplay happens.
- `MazeRunnerGame`: handles gameplay logic together with `GameScreen`.
- `ResourcesPack`: imports and provides most art resources
- `HUD`: in-game HUD
- in sub-package `de.tum.cit.fop.maze` (bullet list hierarchy represents inheritance)
    - `MazeObject`: generic super class
        - `Maze`: handles maze-level logic i.e. the whole map
        - `MazeObject`
            - `Block`: static component of the maze, where path, wall, trap, etc. are derived.
            - `Entity`: dynamic components of the map
                - `Player`
                - `Mob`: dynamic element that deals damage to the player.
                - `InteractiveElements`: collectable elements, e.g. keys, health, power-ups

For complete structure, see UML graph:

![UML](/docs/UML.png)

## Team members

In lexicographic order:

- Shujun Liao (NeoNumber01)
- Tonglin Liu (Merrkry)
- Yiheng Zou (LastiaraZyh)