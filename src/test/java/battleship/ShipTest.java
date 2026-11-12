package battleship;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Test class for Ship.
 * Author: ${user.name}
 * Date: ${current_date}
 * Time: ${current_time}
 * Cyclomatic Complexity for each method:
 * - Constructor: 1
 * - getCategory: 1
 * - getSize: 1
 * - getBearing: 1
 * - getPositions: 1
 * - stillFloating: 2
 * - shoot: 2
 * - occupies: 2
 * - tooCloseTo (IShip): 2
 * - tooCloseTo (IPosition): 2
 * - getTopMostPos: 2
 * - getBottomMostPos: 2
 * - getLeftMostPos: 2
 * - getRightMostPos: 2
 */
public class ShipTest {

    private Ship ship, shipBorder;

    @BeforeEach
    void setUp() {
        // Since Ship is abstract, instantiate it with a concrete subclass (e.g., Barge)
        ship = new Barge(Compass.NORTH, new Position(5, 5));
        shipBorder = new Barge(Compass.NORTH, new Position(0, 0));
    }

    @AfterEach
    void tearDown() {
        ship = null;
    }

    /**
     * Test for the constructor.
     * Cyclomatic Complexity: 1
     */
    @Test
    @DisplayName("A barge initializes category, bearing, size and occupied positions")
    void testConstructor() {
        assertNotNull(ship, "Error: Ship instance should not be null.");
        assertEquals("Barca", ship.getCategory(), "Error: Ship category is incorrect.");
        assertEquals(Compass.NORTH, ship.getBearing(), "Error: Ship bearing is incorrect.");
        assertEquals(1, ship.getSize(), "Error: Ship size is incorrect.");
        assertFalse(ship.getPositions().isEmpty(), "Error: Ship positions should not be empty.");
    }

    /**
     * Test for the getCategory method.
     * Cyclomatic Complexity: 1
     */
    @Test
    @DisplayName("Get category returns the ship category")
    void testGetCategory() {
        assertEquals("Barca", ship.getCategory(), "Error: Ship category should be 'Barca'.");
    }

    /**
     * Test for the getSize method.
     * Cyclomatic Complexity: 1
     */
    @Test
    @DisplayName("Get size returns the number of occupied cells")
    void testGetSize() {
        assertEquals(1, ship.getSize(), "Error: Ship size should be 1.");
    }

    /**
     * Test for the getBearing method.
     * Cyclomatic Complexity: 1
     */
    @Test
    @DisplayName("Get bearing returns the ship orientation")
    void testGetBearing() {
        assertEquals(Compass.NORTH, ship.getBearing(), "Error: Ship bearing should be NORTH.");
    }

    /**
     * Test for the getPositions method.
     * Cyclomatic Complexity: 1
     */
    @Test
    @DisplayName("Get positions returns the occupied board coordinates")
    void testGetPositions() {
        List<IPosition> positions = ship.getPositions();
        assertNotNull(positions, "Error: Ship positions should not be null.");
        assertEquals(1, positions.size(), "Error: Ship should have exactly one position.");
        assertEquals(5, positions.get(0).getRow(), "Error: Position's row should be 5.");
        assertEquals(5, positions.get(0).getColumn(), "Error: Position's column should be 5.");
    }

    /**
     * Test for the stillFloating method (all positions intact).
     * Cyclomatic Complexity: 2
     */
    @Test
    @DisplayName("An intact ship floats")
    void testStillFloating1() {
        assertTrue(ship.stillFloating(), "Error: Ship should still be floating.");
    }

    /**
     * Test for the stillFloating method (all positions hit).
     */
    @Test
    @DisplayName("A fully hit ship sinks")
    void testStillFloating2() {
        ship.getPositions().get(0).shoot();
        assertFalse(ship.stillFloating(), "Error: Ship should no longer be floating after being hit.");
    }

    /**
     * Test for the shoot method (valid position).
     * Cyclomatic Complexity: 2
     */
    @Test
    @DisplayName("Shooting an occupied cell marks it as hit")
    void testShoot1() {
        Position target = new Position(5, 5);
        ship.shoot(target);
        assertTrue(ship.getPositions().get(0).isHit(), "Error: Position should be marked as hit.");
    }

    /**
     * Test for the shoot method (invalid position).
     */
    @Test
    @DisplayName("Shooting another cell leaves the ship intact")
    void testShoot2() {
        Position target = new Position(0, 0);
        ship.shoot(target); // No exception expected
        assertFalse(ship.getPositions().get(0).isHit(), "Error: Position should not be marked as hit for an invalid target.");
    }

    /**
     * Test for the occupies method (position occupied).
     * Cyclomatic Complexity: 2
     */
    @Test
    @DisplayName("A ship occupies its own position")
    void testOccupies1() {
        Position pos = new Position(5, 5);
        assertTrue(ship.occupies(pos), "Error: Ship should occupy position (5, 5).");
    }

    /**
     * Test for the occupies method (position not occupied).
     */
    @Test
    @DisplayName("A ship does not occupy a distant position")
    void testOccupies2() {
        Position pos = new Position(1, 1);
        assertFalse(ship.occupies(pos), "Error: Ship should not occupy position (1, 1).");
    }

    /**
     * Test for the tooCloseTo method with another IShip (ships too close).
     * Cyclomatic Complexity: 2
     */
    @Test
    @DisplayName("Neighboring ships are too close")
    void testTooCloseToShip1() {
        Ship nearbyShip = new Barge(Compass.NORTH, new Position(5, 6));
        assertTrue(ship.tooCloseTo(nearbyShip), "Error: Ships should be too close.");
    }

    /**
     * Test for the tooCloseTo method with another IShip (ships not close).
     */
    @Test
    @DisplayName("Distant ships are not too close")
    void testTooCloseToShip2() {
        Ship farShip = new Barge(Compass.NORTH, new Position(10, 10));
        assertFalse(ship.tooCloseTo(farShip), "Error: Ships should not be too close.");
    }

    /**
     * Test for the tooCloseTo method with an IPosition (positions adjacent).
     * Cyclomatic Complexity: 2
     */
    @Test
    @DisplayName("A neighboring cell is too close")
    void testTooCloseToPosition1() {
        Position pos = new Position(5, 6); // Adjacent position
        assertTrue(ship.tooCloseTo(pos), "Error: Ship should be too close to the given position.");
    }

    /**
     * Test for the tooCloseTo method with an IPosition (positions not adjacent).
     */
    @Test
    @DisplayName("A distant cell is not too close")
    void testTooCloseToPosition2() {
        Position pos = new Position(7, 7); // Non-adjacent position
        assertFalse(ship.tooCloseTo(pos), "Error: Ship should not be too close to the given position.");
    }

    /**
     * Test for the getTopMostPos method.
     * Cyclomatic Complexity: 2
     */
    @Test
    @DisplayName("A single-cell ship has its own row as top boundary")
    void testGetTopMostPos() {
        assertEquals(5, ship.getTopMostPos(), "Error: The topmost position should be 5.");
    }

    /**
     * Test for the getBottomMostPos method.
     * Cyclomatic Complexity: 2
     */
    @Test
    @DisplayName("A single-cell ship has its own row as bottom boundary")
    void testGetBottomMostPos() {
        assertEquals(5, ship.getBottomMostPos(), "Error: The bottommost position should be 5.");
    }

    /**
     * Test for the getLeftMostPos method.
     * Cyclomatic Complexity: 2
     */
    @Test
    @DisplayName("A single-cell ship has its own column as left boundary")
    void testGetLeftMostPos() {
        assertEquals(5, ship.getLeftMostPos(), "Error: The leftmost position should be 5.");
    }

    /**
     * Test for the getRightMostPos method.
     * Cyclomatic Complexity: 2
     */
    @Test
    @DisplayName("A single-cell ship has its own column as right boundary")
    void testGetRightMostPos() {
        assertEquals(5, ship.getRightMostPos(), "Error: The rightmost position should be 5.");
    }

    /**
     * Test for the getRightMostPos method.
     * Cyclomatic Complexity: 2
     */
    @Test
    @DisplayName("Adjacent cells respect center and corner board boundaries")
    void testGetAdjacentPositions() {
        assertEquals(8, ship.getAdjacentPositions().size(), "Error: The number of adjacent positions of a barge in the middle of the tray should be 8.");
        assertEquals(3, shipBorder.getAdjacentPositions().size(), "Error: The number of adjacent positions of a barge in the middle of the tray should be 8.");
    }



    @Test
    @DisplayName("The factory builds every supported category and returns null for an unknown kind")
    void buildsAllShipKinds() {
        String[] kinds = {"barca", "caravela", "nau", "fragata", "galeao"};
        Class<?>[] types = {Barge.class, Caravel.class, Carrack.class, Frigate.class, Galleon.class};
        for (int index = 0; index < kinds.length; index++) {
            Ship built = Ship.buildShip(kinds[index], Compass.NORTH, new Position(2, 2));
            assertInstanceOf(types[index], built);
            assertEquals(Compass.NORTH, built.getBearing());
            assertEquals(new Position(2, 2), built.getPosition());
        }
        assertNull(Ship.buildShip("unknown", Compass.NORTH, new Position(2, 2)));
    }

    @Test
    @DisplayName("The base constructor rejects each null mandatory field")
    void rejectsNullConstructorArguments() {
        assertThrows(NullPointerException.class, () -> new TestShip(null, Compass.NORTH, new Position(0, 0)));
        assertThrows(NullPointerException.class, () -> new TestShip("Test", null, new Position(0, 0)));
        assertThrows(NullPointerException.class, () -> new TestShip("Test", Compass.NORTH, null));
    }

    @Test
    @DisplayName("Extrema scan all positions independently of position order")
    void findsExtremaInUnsortedPositions() {
        Ship irregular = new TestShip(List.of(new Position(4, 4), new Position(2, 2), new Position(6, 6)));
        assertEquals(2, irregular.getTopMostPos());
        assertEquals(6, irregular.getBottomMostPos());
        assertEquals(2, irregular.getLeftMostPos());
        assertEquals(6, irregular.getRightMostPos());
        assertEquals(new Position(4, 4), irregular.getPosition());
        assertTrue(irregular.occupies(new Position(6, 6)));
        assertFalse(irregular.occupies(new Position(9, 9)));
    }

    @Test
    @DisplayName("A multi-cell ship floats until all cells are hit and sink marks every cell")
    void damagesAndSinksMultiCellShip() {
        Ship multiCell = new Caravel(Compass.EAST, new Position(3, 3));
        multiCell.shoot(new Position(3, 3));
        assertTrue(multiCell.getPositions().get(0).isHit());
        assertFalse(multiCell.getPositions().get(1).isHit());
        assertTrue(multiCell.stillFloating());
        multiCell.sink();
        assertFalse(multiCell.stillFloating());
        assertTrue(multiCell.getPositions().stream().allMatch(IPosition::isHit));
        multiCell.sink();
        assertFalse(multiCell.stillFloating(), "Sinking a sunk ship is idempotent");
    }

    @Test
    @DisplayName("Adjacent positions exclude occupied cells and contain no duplicates")
    void adjacentPositionsForMultiCellShip() {
        Ship multiCell = new Caravel(Compass.EAST, new Position(4, 4));
        List<IPosition> actual = multiCell.getAdjacentPositions();
        List<IPosition> expected = new ArrayList<>();
        for (int row = 3; row <= 5; row++) {
            for (int column = 3; column <= 6; column++) {
                Position pos = new Position(row, column);
                if (row != 4 || (column != 4 && column != 5)) {
                    expected.add(pos);
                }
            }
        }
        assertEquals(new HashSet<>(expected), new HashSet<>(actual));
        assertEquals(expected.size(), actual.size());
    }

    @Test
    @DisplayName("Proximity searches reach later ship cells and later positions of the other ship")
    void proximitySearchesAllCells() {
        Ship multiCell = new Caravel(Compass.EAST, new Position(4, 4));
        assertTrue(multiCell.tooCloseTo(new Position(4, 6)));
        assertFalse(multiCell.tooCloseTo(new Position(9, 9)));
        Ship other = new TestShip(List.of(new Position(9, 9), new Position(4, 6)));
        assertTrue(multiCell.tooCloseTo(other));
        assertFalse(multiCell.tooCloseTo(new TestShip(List.of(new Position(9, 9), new Position(8, 8)))));
    }

    @Test
    @DisplayName("Board containment checks each of the four boundaries and includes edge cells")
    void checksAllBoardBoundaries() {
        assertTrue(new Barge(Compass.NORTH, new Position(0, 0)).isInsideBoard());
        assertTrue(new Barge(Compass.NORTH, new Position(9, 9)).isInsideBoard());
        assertFalse(new Barge(Compass.NORTH, new Position(0, -1)).isInsideBoard());
        assertFalse(new Barge(Compass.NORTH, new Position(0, 10)).isInsideBoard());
        assertFalse(new Barge(Compass.NORTH, new Position(-1, 0)).isInsideBoard());
        assertFalse(new Barge(Compass.NORTH, new Position(10, 0)).isInsideBoard());
    }

    @Test
    @DisplayName("Ship text includes category, bearing and initial position")
    void describesShip() {
        assertEquals("[Barca " + Compass.NORTH + " " + new Position(5, 5) + "]", ship.toString());
    }

    @Test
    @DisplayName("Ship assertions reject null factory, proximity and shooting arguments")
    void rejectsInvalidArguments() {
        assertThrows(AssertionError.class, () -> Ship.buildShip(null, Compass.NORTH, new Position(0, 0)));
        assertThrows(AssertionError.class, () -> Ship.buildShip("barca", null, new Position(0, 0)));
        assertThrows(AssertionError.class, () -> Ship.buildShip("barca", Compass.NORTH, null));
        assertThrows(AssertionError.class, () -> ship.occupies(null));
        assertThrows(AssertionError.class, () -> ship.tooCloseTo((IShip) null));
        assertThrows(AssertionError.class, () -> ship.tooCloseTo((IPosition) null));
        assertThrows(AssertionError.class, () -> ship.shoot(null));
        assertThrows(AssertionError.class, () -> ship.shoot(new Position(-1, 0)));
    }

    /** A concrete fixture for testing base-class behavior with arbitrary position order. */
    private static final class TestShip extends Ship {
        TestShip(List<IPosition> positions) {
            super("Test", Compass.NORTH, positions.get(0), positions.size());
            getPositions().addAll(positions);
        }

        TestShip(String category, Compass bearing, IPosition position) {
            super(category, bearing, position, 1);
        }
    }
}
