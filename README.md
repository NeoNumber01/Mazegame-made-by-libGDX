# 🎮 Maze Runner: Into the Deep Dream (Java + libGDX)

> A **story-driven 2D action maze game** built with **Java** and **libGDX**, featuring cinematic cutscenes, real-time combat, item-based progression, a timed boss encounter, and a second gameplay mode (**Space Cruises**) after escaping the maze.

---

## 📽️ Demo



[startstory.webm](https://github.com/user-attachments/assets/3691469f-6158-4e26-8cd4-1f1cea234689)





https://github.com/user-attachments/assets/567327f0-1c36-47bf-9a9d-307336de0b72




[spaceshipboard.webm](https://github.com/user-attachments/assets/21cae514-24d9-4f96-bd8c-79240d82807d)



https://github.com/user-attachments/assets/1d03195a-b2f1-4b0d-aeac-23c4dfda34f9


---

## 📖 Story

The game opens with an **animated intro cutscene**:
A **young boy falls into a deep dream**—descending layer by layer—until he awakens inside a massive maze.

From that moment, the maze becomes both a prison and a battlefield:

- enemies hunt the player relentlessly,

- survival depends on combat and movement,

- the player must locate a key and reach the exit to escape the dream.

When the player escapes, a transition cutscene plays:
the boy boards a spaceship and **flies into space**, unlocking the next game mode: **Space Cruises**.

---

## 🗺️ World & Level Design (Tiled)

- The maze map is built with **Tiled**

- Huge handcrafted layout: **50 × 50**

- Designed to encourage exploration and navigation

- Contains random spawns for items and special mechanics (e.g., spaceship tiles)

---

## 🧭 Objective & Guidance System (Compass / Indicator)

A directional indicator is shown at the **top-left** of the screen:

- At the beginning, it points to the **nearest key**

- After the key is collected, it updates to point to the **nearest exit**

This keeps exploration meaningful while preventing the player from getting stuck.

---

## ⚔️ Maze Mode Gameplay
### 🧟 Enemies (Mobs)

- Regular enemies (called mobs) appear in the maze and will:

- actively search for the player,

- approach/chase the player,

- deal damage on contact/attack.

The player must fight and survive while exploring.

---

### 🎒 Items & Power-Ups

The maze contains collectible items:
| Item       | Effect                         |
|------------|--------------------------------|
| 🛡️ Armor   | Increases defense               |
| ❤️ Heart   | Increases health / heals        |
| ⚡ Lightning | Increases movement speed       |

---

## 💀 Boss Encounter (Timed)

⏱️ **10 seconds after the maze gameplay starts**, a boss spawns:

### Rotating Skull Boss

A spinning skull boss that attacks the player using:

- 🔴 Red laser shots

- 🌀 Spin-charge rush toward the player

---

## 🧨 Combat System (Maze Mode)

The player has multiple combat options:

- Melee slash: strong close-range attack

- Energy cannon (ranged): projectile-based ranged attack

- Lightning attack (boss-focused): special boss counter

- Orbiting light saber: continuous spinning damage around the player

---

## 🎮 Controls
### Maze Mode Controls

| Action                        | Key        |
| ----------------------------- | ---------- |
| Move                          | Arrow Keys |
| Sprint                        | Shift      |
| Melee Slash                   | Space      |
| Energy Cannon (Ranged)        | **F**      |
| Lightning Attack (vs Boss)    | Q          |
| Activate Orbiting Light Saber | R          |


### Space Cruises Controls

| Action     | Key        |
| ---------- | ---------- |
| Move       | Arrow Keys |
| Fire Laser | **Space**  |

---

## 🚀 Special Mechanic: Spaceship Tiles (Maze Mode)

Spaceships can **randomly appear on the maze floor**.

When the player picks one up:

- the player transforms into a spaceship,

- flies randomly inside the maze,

- stops at a new location.

This creates dynamic repositioning and adds replayability.

---

## 🔑 Progression & Winning

To escape the maze:

1. Explore the maze while surviving mobs and the boss

2. Find and collect the **key**

3. Follow the indicator to the **exit**

4. Reach the exit (with the key) to trigger a **transition cutscene**

5. Enter **Space Cruises**

6. Win Space Cruises to complete the game and see the victory ending

---

## 🌌 Space Cruises (Second Game Mode)

After the maze escape cutscene, the game switches to **Space Cruises**:

a new movement + shooting mode in space

victory leads to a final **WIN** screen confirming the successful escape

---

## 🛠 Tech Stack

- **Language**: Java

- **Framework**: libGDX

- **Map Editor**: Tiled

- **Build System**: Gradle (Wrapper included)

- **Platform**: Desktop (Windows build available; source is cross-platform)
> Cutscenes are integrated directly into the gameplay flow via dedicated screens.

---

## 🧱 Project Structure (Code Overview)

This repository follows the standard libGDX multi-module structure:

- `core/` — all gameplay logic (entities, screens, UI, mechanics)
- `desktop/` — desktop launcher entry point
- `assets/` — game resources (maps, textures, sounds, videos)
- `package/` — packaged builds (e.g., portable zip)
- `docs/` — documentation assets

---

### High-level Layout
```text
.
├─ core/
│  └─ src/main/java/de/tum/cit/fop/maze/
│     ├─ MazeRunnerGame.java
│     ├─ GameScreen.java
│     ├─ MenuScreen.java
│     ├─ CutsceneVideoScreen.java
│     ├─ StoryScreen.java
│     ├─ SpaceCruisesMiniGameScreen.java
│     ├─ HUD.java / SciFiHUD.java / SciFiCompassHUD.java
│     ├─ Maze.java / MazeRunnerCamera.java / FogOfWar.java
│     ├─ ... (other gameplay systems)
│     └─ elements/
│        ├─ Player.java
│        ├─ Mob.java
│        ├─ SkullBoss.java
│        ├─ Key.java / Entry.java / Exit.java
│        ├─ Shield.java / Health.java / Lightning.java
│        ├─ EnergyCannon.java / EnergyProjectile.java
│        ├─ LightSaberOrbit.java
│        ├─ Trap.java / Mine.java / MovableWall.java / Wall.java
│        └─ ...
│
├─ desktop/
│  └─ src/main/java/de/tum/cit/fop/maze/
│     └─ DesktopLauncher.java
│
├─ assets/                 # Tiled maps, textures, audio, cutscene videos, etc.
├─ package/
│  ├─ Maze Runner/
│  └─ MazeRunner-Windows-Portable-v1.0.zip
├─ docs/



├─ build.gradle
├─ settings.gradle
├─ gradlew / gradlew.bat
└─ README.md
```
---
### Screen / State Flow (Conceptual)

The game is organized around screens (state-driven flow), such as:

- Menu → Cutscene/Story → Maze Gameplay → Transition Cutscene → Space Cruises → End Screen / Win Screen
---

## 🚀 How to Run
### Option A — Run from Source (Recommended for developers)

### Windows
```bash
gradlew.bat desktop:run
```
### macOS / Linux
```bash
./gradlew desktop:run
```
### Option B — Run a Packaged Build (Windows Portable)

A portable build zip exists in:

- `package/MazeRunner-Windows-Portable-v1.0.zip`

Unzip it and run the included launcher inside the extracted folder.

---

## ✨ Key Highlights

- Cinematic intro + transition cutscenes integrated into gameplay

- Massive handcrafted Tiled maze (50×50)

- Enemy chasing behavior (mobs pursue the player)

- Timed boss spawn (10s after start) with multiple attack patterns

- Multiple player abilities (melee, ranged, orbiting weapon, boss counter skill)

- Item system (defense/health/speed)

- Second game mode (Space Cruises) after escaping the maze

- Clean libGDX multi-module project layout

---

## 📌 Roadmap (Ideas)

- Add more maze levels / themes

- Difficulty scaling and balancing

- Audio polish and additional VFX

- Save/load checkpoint system

---

## ⭐ Star & Feedback

If you find this project interesting, feel free to **star** the repository ⭐
Feedback and suggestions are welcome!

---
