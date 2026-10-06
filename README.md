# Pathfinding

A small Java Swing app that generates a random maze and finds a path through it. You pick a start cell (the robot) and an end cell (home). The app computes a path with a recursive depth-first search and animates the robot walking it.

## Table of contents

- [Features](#features)
- [Requirements](#requirements)
- [Getting started](#getting-started)
- [How to use](#how-to-use)
- [Project structure](#project-structure)
- [How it works](#how-it-works)
  - [Grid model](#grid-model)
  - [Maze generation](#maze-generation)
  - [Pathfinding](#pathfinding-1)
  - [Rendering and animation](#rendering-and-animation)
- [Running the tests](#running-the-tests)
- [Configuration](#configuration)
- [Known issues](#known-issues)
- [Ideas for extension](#ideas-for-extension)

## Features

- **Random maze generation** using an adapted version of Eller's algorithm, so every passage cell can reach every other one.
- **Adjustable openness**: a sparsity setting removes extra walls, so there is more than one route.
- **Click to choose** where the robot starts and where home is.
- **Pathfinding** with recursive depth-first search (DFS).
- **Animation**: the robot moves along the path one cell every 300 ms.
- **JUnit tests** that check that a path is valid, that adjacent start and end cells work, and that an unreachable end returns an empty path.

## Requirements

| Tool | Version | Notes |
|------|---------|-------|
| JDK  | **21 or newer** | The code uses `List.getFirst()` / `List.getLast()`, added in Java 21. The IntelliJ project is set to language level 24. |
| JUnit | 4.13 + JUnit Jupiter API | Needed only for the tests. The test class uses JUnit 4's `@Test` and runner together with JUnit 5's `@DisplayName` / `@Tag` annotations. |
| Hamcrest | 2.x | A runtime dependency of JUnit 4. |

Check your Java version with:

```bash
java -version
```

## Getting started

### Option 1: IntelliJ IDEA (recommended)

The repository already has an IntelliJ project (`.idea/`, `Pathfinding.iml`).

1. **File → Open** and select the repository folder.
2. Set the Project SDK to JDK 21+ (**File → Project Structure → Project**).
3. Open `src/Maze.java` and run `main`.
4. Make sure the **working directory** of the run configuration is the project root. Images are loaded from the relative path `resources/` (see [Known issues](#known-issues)).

### Option 2: Command line

Run these from the repository root:

```bash
# Compile
javac -d out src/*.java

# Run (from the repo root, so resources/ resolves correctly)
java -cp out Maze
```

A window titled **"Maze"** opens with a 10 × 10 grid.

## How to use

1. **Click a white (passage) cell** to place the robot 🤖 (the start).
2. **Click another passage cell** to place home 🏠 (the end).
3. Press **Solve** to compute a path. It is drawn in blue.
4. Press **Run** to watch the robot follow the path.
5. Press **Reset** to generate a new maze and clear your selections.

Clicking behaviour:

| Click | Effect |
|-------|--------|
| 1st | Sets the start cell |
| 2nd | Sets the end cell |
| 3rd | Clears the end and the path, and makes the clicked cell the new start |
| On a wall / outside the grid | Ignored |

> **Tip:** Pressing **Solve** before choosing both cells does nothing. Pressing **Run** before a path exists does nothing.

## Project structure

```
pathfinding/
├── src/
│   ├── Maze.java          # Entry point: builds the JFrame and the Reset/Solve/Run buttons
│   ├── MazePanel.java     # Maze generation, rendering, mouse input and animation
│   └── PathFinder.java    # Recursive DFS pathfinding algorithm
├── test/
│   └── TestPathFinder.java  # JUnit tests for PathFinder
├── resources/
│   ├── robot.png          # Sprite for the start cell / moving robot
│   └── home.png           # Sprite for the end cell
├── Pathfinding.iml        # IntelliJ module file
└── .idea/                 # IntelliJ project settings
```

### Class overview

| Class | What it does |
|-------|--------------|
| [`Maze`](src/Maze.java) | Creates the window on the Swing event thread, adds a `MazePanel` in the centre and a button bar at the bottom. |
| [`MazePanel`](src/MazePanel.java) | Extends `JPanel`. Holds the wall grid, generates mazes, handles clicks, paints the grid, path and sprites, and runs the animation `Timer`. |
| [`PathFinder`](src/PathFinder.java) | Stateless helper class. `findPath(start, end, rows, cols, walls)` returns the path as a `List<Point>`. |

## How it works

### Grid model

The maze is a `boolean[rows][cols]` array called `walls`:

- `true` → wall (drawn black)
- `false` → passage (drawn white)

Cells are `java.awt.Point` objects, used as **`(row, col)`**:

```java
Point p = new Point(row, col);
p.x  // row
p.y  // column
```

> ⚠️ This is the opposite of the usual screen convention, where `x` is horizontal. When painting, the code converts with `x = p.y * CELL_SIZE` and `y = p.x * CELL_SIZE`.

### Maze generation

`MazePanel.generateMaze()` works in three phases.

**1. Eller's algorithm on a half-resolution grid.**
The algorithm runs on a `(rows/2) × (cols/2)` grid of *passage cells*. It goes through the grid one row at a time and tracks which connected "set" each cell belongs to:

- Neighbouring cells in a row are merged at random, which removes the wall between them, but only when they are in different sets. This keeps loops from forming.
- Each set must send **at least one** vertical connection down to the next row. Other cells in the set may connect down at random.
- In the last row, all neighbouring cells in different sets are merged, so the whole maze is connected.

The result is a *perfect maze*: every passage cell can reach every other, and there is exactly one route between any two of them.

**2. Projection onto the full grid.**
Passage cell `(pRow, pCol)` maps to grid cell `(2·pRow + 1, 2·pCol + 1)`. A removed wall between two passage cells opens the grid cell between them.

**3. Sparsity pass.**
Some walls are then removed at random to make the maze more open and create loops:

- Border walls on row 0 and column 0 next to a passage are removed with probability `SPARSITY` (0.7).
- Interior walls are removed with probability `SPARSITY × 0.5`.
- `canRemoveWall(row, col)` only allows a wall to be removed if at least one of its four neighbours is already a passage. This way no disconnected open area is created.

Because walls are only ever removed, the maze stays fully connected after this pass.

### Pathfinding

`PathFinder.findPath` runs a **recursive depth-first search**:

```
findPathRecursive(current):
    if current == end:
        path.add(current); return true
    mark current visited
    for each direction in [Up, Down, Left, Right]:
        if neighbour is in bounds, not a wall, not visited:
            if findPathRecursive(neighbour):
                path.add(0, current)   # prepend while the recursion unwinds
                return true
    return false
```

Key properties:

| Property | Value |
|----------|-------|
| Movement | 4 directions (no diagonals) |
| Neighbour order | Up → Down → Left → Right |
| Returns | Path from start to end, **both included**, or an empty list if there is none |
| Shortest path? | **No.** DFS returns the *first* path it finds. In an open maze this can be a long detour. |
| Time complexity | O(rows × cols): each cell is visited at most once |
| Space complexity | O(rows × cols) for the visited set and the recursion stack |

If `startCell` or `endCell` is `null`, an empty list is returned.

### Rendering and animation

`paintComponent` draws, in this order:

1. Each cell as a 40 × 40 px square (black or white) with a light-gray grid line.
2. The path as blue squares. They are skipped on the end cell and on the robot's current cell, where the sprites are drawn.
3. The robot sprite at its animated position, or at the start cell when no animation is running.
4. The home sprite at the end cell.

`run()` starts a `javax.swing.Timer` that fires every **300 ms**. Each tick moves the robot one step along the path. When the robot reaches the end, the timer stops.

## Running the tests

The tests are in [`test/TestPathFinder.java`](test/TestPathFinder.java):

| Test | Scenario | Checks |
|------|----------|--------|
| `testHappyPath` | 4 × 4 maze with walls, from the top-left to the bottom-right | Path is not empty, starts and ends correctly, each step is to an adjacent cell (Manhattan distance 1), and no step is on a wall |
| `testAdjacentStartAndEnd` | 3 × 3 grid with no walls, start and end next to each other | Path exists and starts and ends correctly |
| `testNoPathFound` | 4 × 4 maze with a full row of walls between start and end | An empty path is returned |

Each test prints an ASCII picture of the solution. `S` = start, `E` = end, `#` = wall, and numbers are positions along the path:

```
Your solution:
S 1 2 3
. # # 4
. . . 5
# # . E
```

### In IntelliJ

1. Add **JUnit 4** and **JUnit Jupiter API** to the module, for example by hovering over the red `org.junit` import and choosing *Add 'JUnit4' to classpath*.
2. Mark `test/` as a **Test Sources Root** (right-click → *Mark Directory as*).
3. Right-click `TestPathFinder` → **Run**.

### From the command line

> The test file must be adjusted first. See [Known issues](#known-issues).

With JUnit jars available locally (for example from `~/.m2/repository`):

```bash
JUNIT4=path/to/junit-4.13.2.jar
HAMCREST=path/to/hamcrest-2.2.jar
JUPITER_API=path/to/junit-jupiter-api.jar
APIGUARDIAN=path/to/apiguardian-api.jar
CP="out:$JUNIT4:$HAMCREST:$JUPITER_API:$APIGUARDIAN"

javac -d out src/*.java
javac -cp "$CP" -d out test/TestPathFinder.java
java  -cp "$CP" org.junit.runner.JUnitCore TestPathFinder
```

Expected output ends with:

```
OK (3 tests)
```

## Configuration

The settings are constants in the source:

| Setting | Location | Default | Effect |
|---------|----------|---------|--------|
| Grid size | `new MazePanel(10, 10)` in `Maze.java` | 10 × 10 | Rows × columns. Use **even** numbers so the half-resolution generator fills the grid. |
| `CELL_SIZE` | `MazePanel.java` | `40` | Size of each cell in pixels. Increase it if the window is too small. |
| `SPARSITY` | `MazePanel.java` | `0.7` | `0.0`–`1.0`. Higher values give more open mazes with more alternative routes. |
| Animation speed | `new Timer(300, …)` in `MazePanel.run()` | 300 ms | Delay between robot steps. |

## Known issues

- **The test file does not compile as committed.** `TestPathFinder.java` declares `package test;`, but `PathFinder` is in the default (unnamed) package, and Java does not allow code in a named package to use default-package classes. Fix: remove the `package test;` line, or move all classes into a common named package.
- **Images are loaded from a path relative to the working directory.** `new ImageIcon("resources/robot.png")` only works when the app is started from the repository root. If you start it from somewhere else, the sprites are missing (nothing is drawn, and there is no error). Loading them with `getClass().getResource(...)` from the classpath would make this more robust.
- **No dependency management.** There is no Maven/Gradle build, so JUnit has to be added by hand.
- **DFS does not find the shortest path.** This is expected for this algorithm, but you may notice detours in open mazes.

## Ideas for extension

- Use **breadth-first search** (BFS) to get the shortest path on this unweighted grid, or **A\*** with a Manhattan-distance heuristic.
- Show the cells the search visits, not just the final path.
- Add a dropdown to compare algorithms side by side.
- Add Maven or Gradle so tests run with a single command (`mvn test` / `gradle test`).
- Make grid size, sparsity and animation speed adjustable from the UI.
