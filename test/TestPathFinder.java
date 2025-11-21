package test;

import java.util.List;

import java.awt.Point;

import org.junit.Test;
import org.junit.jupiter.api.*;

import static org.junit.Assert.*;

public class TestPathFinder {
    @Test
    @DisplayName("Normal maze with 4 x 4 grid and walls")
    @Tag("score:1")
    public void testHappyPath() {
        // Layout (false = passage, true = wall):
        // S . . .
        // . # # .
        // . . . .
        // # # . E
        boolean[][] walls = {
                { false, false, false, false },
                { false, true, true, false },
                { false, false, false, false },
                { true, true, false, false }
        };

        Point start = new Point(0, 0); // Top-left
        Point end = new Point(3, 3); // Bottom-right

        List<Point> path = PathFinder.findPath(start, end, 4, 4, walls);

        // Verify path is not empty
        assertFalse("Path should not be empty.\nYour solution:\n" + illustrateMaze(walls, path, start, end), path.isEmpty());

        // Verify start and end points
        assertEquals("Path should start at the start cell.\nYour solution:\n" + illustrateMaze(walls, path, start, end), start, path.getFirst());
        assertEquals("Path should end at the end cell.\nYour solution:\n" + illustrateMaze(walls, path, start, end), end, path.getLast());

        // Verify each point in the path is adjacent to the next
        for (int i = 0; i < path.size() - 1; i++) {
            Point current = path.get(i);
            Point next = path.get(i + 1);

            // Calculate Manhattan distance (should be exactly 1 for adjacent cells)
            int distance = Math.abs(current.x - next.x) + Math.abs(current.y - next.y);
            assertEquals("Cells should be adjacent to each other in the path.\nYour solution:\n" + illustrateMaze(walls, path, start, end), 1, distance);
        }

        // Verify none of the path points are walls
        for (Point p : path) {
            assertFalse("Path should not go through walls at " + p + ".\nYour solution:\n" + illustrateMaze(walls, path, start, end), walls[p.x][p.y]);
        }

        System.out.println("Your solution:\n" + illustrateMaze(walls, path, start, end));
    }

    @Test
    @DisplayName("Small maze with 3 x 3 grid and no walls, where start and end are adjacent")
    @Tag("score:1")
    public void testAdjacentStartAndEnd() {
        boolean[][] walls = {
                { false, false, false },
                { false, false, false },
                { false, false, false }
        };

        Point start = new Point(1, 1); // Center
        Point end = new Point(1, 2); // Right of center (adjacent)

        List<Point> path = PathFinder.findPath(start, end, 3, 3, walls);

        // Verify path exists
        assertFalse("Path should exist for adjacent cells\nYour solution:\n" + illustrateMaze(walls, path, start, end), path.isEmpty());

        // Verify start and end
        assertEquals("Path should start at start cell\nYour solution:\n" + illustrateMaze(walls, path, start, end), start, path.getFirst());
        assertEquals("Path should end at end cell\nYour solution:\n" + illustrateMaze(walls, path, start, end), end, path.getLast());

        System.out.println("Your solution:\n" + illustrateMaze(walls, path, start, end));
    }

    @Test
    @DisplayName("Maze with no possible path due to walls blocking the way")
    @Tag("score:1")
    public void testNoPathFound() {
        // Layout:
        // S . . .
        // . . . .
        // # # # #
        // . . . E
        boolean[][] walls = {
                { false, false, false, false },
                { false, false, false, false },
                { true, true, true, true }, // Wall blocking the path
                { false, false, false, false }
        };

        Point start = new Point(0, 0); // Top-left
        Point end = new Point(3, 3); // Bottom-right (unreachable)

        List<Point> path = PathFinder.findPath(start, end, 4, 4, walls);

        // Verify path is empty when no path exists
        assertTrue("Path should be empty when no path exists", path.isEmpty());

        System.out.println("Your solution:\n" + illustrateMaze(walls, path, start, end));
    }

    private static String illustrateMaze(boolean[][] walls, List<Point> path, Point start, Point end) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < walls.length; r++) {
            for (int c = 0; c < walls[0].length; c++) {
                Point p = new Point(r, c);
                if (p.equals(start)) {
                    sb.append("S ");
                } else if (p.equals(end)) {
                    sb.append("E ");
                } else if (walls[r][c]) {
                    sb.append("# ");
                } else if (path.contains(p)) {
                    sb.append(path.indexOf(p)).append(" ");
                } else {
                    sb.append(". ");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
