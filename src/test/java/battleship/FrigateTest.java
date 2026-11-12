package battleship;

import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the Frigate class.
 * Author: ${user.name}
 * Date: ${current_date}
 * Time: ${current_time}
 * Cyclomatic Complexity for each method:
 * - Constructor: 5
 * - getSize: 1
 * - stillFloating: 2
 * - getPositions: 2
 * - getTopMostPos: 2
 * - getBottomMostPos: 2
 * - getLeftMostPos: 2
 * - getRightMostPos: 2
 */
public class FrigateTest {

	private Frigate frigate;

	@BeforeEach
	void setUp() {
		frigate = new Frigate(Compass.NORTH, new Position(5, 5));
	}

	@AfterEach
	void tearDown() {
		frigate = null;
	}

	/**
	 * Test for the constructor with NORTH bearing.
	 * Cyclomatic Complexity: 5
	 */
	@Test
	@DisplayName("Criar uma fragata orientada a NORTH")
	void testConstructorNorth() {
		assertNotNull(frigate, "Error: Frigate instance should not be null.");
		assertEquals("Fragata", frigate.getCategory(), "Error: Frigate category should be 'Fragata'.");
		assertEquals(Compass.NORTH, frigate.getBearing(), "Error: Frigate bearing is incorrect.");
		assertEquals(4, frigate.getSize(), "Error: Frigate size should be 4.");

		List<IPosition> positions = frigate.getPositions();
		assertEquals(4, positions.size(), "Error: Frigate should have exactly 4 positions.");
		assertEquals(new Position(5, 5), positions.get(0), "Error: First position is incorrect for NORTH.");
		assertEquals(new Position(6, 5), positions.get(1), "Error: Second position is incorrect for NORTH.");
		assertEquals(new Position(7, 5), positions.get(2), "Error: Third position is incorrect for NORTH.");
		assertEquals(new Position(8, 5), positions.get(3), "Error: Fourth position is incorrect for NORTH.");
	}

	/**
	 * Test for the constructor with EAST bearing.
	 */
	@Test
	@DisplayName("Criar uma fragata orientada a EAST")
	void testConstructorEast() {
		frigate = new Frigate(Compass.EAST, new Position(5, 5));
		List<IPosition> positions = frigate.getPositions();

		assertNotNull(frigate, "Error: Frigate instance should not be null.");
		assertEquals(4, positions.size(), "Error: Frigate should have exactly 4 positions.");
		assertEquals(new Position(5, 5), positions.get(0), "Error: First position is incorrect for EAST.");
		assertEquals(new Position(5, 6), positions.get(1), "Error: Second position is incorrect for EAST.");
		assertEquals(new Position(5, 7), positions.get(2), "Error: Third position is incorrect for EAST.");
		assertEquals(new Position(5, 8), positions.get(3), "Error: Fourth position is incorrect for EAST.");
	}

	/**
	 * Test for the constructor with WEST bearing.
	 */
	@Test
	@DisplayName("Criar uma fragata orientada a WEST")
	void testConstructorWest() {
		frigate = new Frigate(Compass.WEST, new Position(5, 5));
		List<IPosition> positions = frigate.getPositions();

		assertNotNull(frigate, "Error: Frigate instance should not be null.");
		assertEquals(4, positions.size(), "Error: Frigate should have exactly 4 positions.");
		assertEquals(new Position(5, 5), positions.get(0), "Error: First position is incorrect for WEST.");
		assertEquals(new Position(5, 6), positions.get(1), "Error: Second position is incorrect for WEST.");
		assertEquals(new Position(5, 7), positions.get(2), "Error: Third position is incorrect for WEST.");
		assertEquals(new Position(5, 8), positions.get(3), "Error: Fourth position is incorrect for WEST.");
	}

	/**
	 * Test for the getSize method.
	 * Cyclomatic Complexity: 1
	 */
	@Test
	@DisplayName("A fragata tem tamanho quatro")
	void testGetSize() {
		assertEquals(4, frigate.getSize(), "Error: Frigate size should be 4.");
	}

	/**
	 * Test for the stillFloating method (all positions intact).
	 * Cyclomatic Complexity: 2
	 */
	@Test
	@DisplayName("Uma fragata intacta continua a flutuar")
	void testStillFloating1() {
		assertTrue(frigate.stillFloating(), "Error: Frigate should still be floating.");
	}

	/**
	 * Test for the stillFloating method (one position hit).
	 */
	@Test
	@DisplayName("Uma fragata com uma posição atingida continua a flutuar")
	void testStillFloating2() {
		frigate.getPositions().get(0).shoot();
		assertTrue(frigate.stillFloating(), "Error: Frigate should still be floating with one hit position.");
	}

	/**
	 * Test for the stillFloating method (all positions hit).
	 */
	@Test
	@DisplayName("Uma fragata com todas as posições atingidas afunda")
	void testStillFloating3() {
		frigate.getPositions().forEach(IPosition::shoot);
		assertFalse(frigate.stillFloating(), "Error: Frigate should not be floating if all positions are hit.");
	}

	/**
	 * Test for the getTopMostPos method.
	 * Cyclomatic Complexity: 2
	 */
	@Test
	@DisplayName("Obter a linha superior da fragata")
	void testGetTopMostPos() {
		assertEquals(5, frigate.getTopMostPos(), "Error: The topmost position should be 5.");
	}

	/**
	 * Test for the getBottomMostPos method.
	 * Cyclomatic Complexity: 2
	 */
	@Test
	@DisplayName("Obter a linha inferior da fragata")
	void testGetBottomMostPos() {
		assertEquals(8, frigate.getBottomMostPos(), "Error: The bottommost position should be 8.");
	}

	/**
	 * Test for the getLeftMostPos method.
	 * Cyclomatic Complexity: 2
	 */
	@Test
	@DisplayName("Obter a coluna mais à esquerda da fragata")
	void testGetLeftMostPos() {
		assertEquals(5, frigate.getLeftMostPos(), "Error: The leftmost position should be 5.");
	}

	/**
	 * Test for the getRightMostPos method.
	 * Cyclomatic Complexity: 2
	 */
	@Test
	@DisplayName("Obter a coluna mais à direita da fragata")
	void testGetRightMostPos() {
		assertEquals(5, frigate.getRightMostPos(), "Error: The rightmost position should be 5.");
	}

	/**
	 * Test for the constructor with invalid input (null).
	 */
	@Test
	@DisplayName("Rejeitar parâmetros nulos na construção da fragata")
	void testConstructorInvalidInput() {
		assertThrows(NullPointerException.class, () -> new Frigate(null, null),
				"Error: NullPointerException should be thrown for null input.");
		assertThrows(NullPointerException.class, () -> new Frigate(Compass.NORTH, null),
				"Error: NullPointerException should be thrown for null position.");
	}

	@Test
	@DisplayName("Criar uma fragata orientada a SOUTH com quatro posições")
	void testConstructorSouth() {
		Frigate southFrigate =
				new Frigate(Compass.SOUTH, new Position(5, 5));

		assertEquals("Fragata", southFrigate.getCategory());
		assertEquals(Compass.SOUTH, southFrigate.getBearing());
		assertEquals(4, southFrigate.getSize());

		List<IPosition> positions = southFrigate.getPositions();

		assertEquals(4, positions.size());
		assertEquals(new Position(5, 5), positions.get(0));
		assertEquals(new Position(6, 5), positions.get(1));
		assertEquals(new Position(7, 5), positions.get(2));
		assertEquals(new Position(8, 5), positions.get(3));
	}
}