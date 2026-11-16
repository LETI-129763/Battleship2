package battleship;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the original Fleet class (no production code changes).
 */
class FleetTest {

	private Fleet fleet;

	@BeforeEach
	void setUp() {
		fleet = new Fleet();
	}

	@Test
	@DisplayName("A new fleet has no ships")
	void constructorCreatesEmptyFleet() {
		assertNotNull(fleet.getShips());
		assertTrue(fleet.getShips().isEmpty());
	}

	@Test
	@DisplayName("A randomly generated fleet has the configured number of valid ships")
	void createRandomCreatesFullFleet() {
		IFleet randomFleet = Fleet.createRandom();

		assertNotNull(randomFleet);
		assertEquals(Fleet.FLEET_SIZE, randomFleet.getShips().size());
		assertTrue(randomFleet.getShips().stream().allMatch(IShip::isInsideBoard));
	}

	@Test
	@DisplayName("Repeated random fleet creation always yields valid fleets, even when ships must be retried")
	void createRandomRepeatedlyCreatesValidFleets() {
		// Random placement sometimes collides and forces a retry; many runs exercise that path.
		for (int i = 0; i < 200; i++) {
			IFleet randomFleet = Fleet.createRandom();

			assertEquals(Fleet.FLEET_SIZE, randomFleet.getShips().size());
			assertTrue(randomFleet.getShips().stream().allMatch(IShip::isInsideBoard));
		}
	}

	@Test
	@DisplayName("Valid ships are accepted until the fleet size limit rejects the next one")
	void addShipEnforcesTheFleetSizeLimit() {
		// Grid positions are 2 cells apart, so none is outside the board or at collision risk.
		boolean rejected = false;
		for (int row = 0; row <= 8 && !rejected; row += 2) {
			for (int column = 0; column <= 8 && !rejected; column += 2) {
				rejected = !fleet.addShip(new Barge(Compass.NORTH, new Position(row, column)));
			}
		}

		assertTrue(rejected, "the fleet should stop accepting ships at some point");
		assertTrue(fleet.getShips().size() >= Fleet.FLEET_SIZE);
	}

	@Test
	@DisplayName("A ship outside the board is rejected")
	void addShipRejectsShipOutsideBoard() {
		assertFalse(fleet.addShip(new Barge(Compass.NORTH, new Position(99, 99))));
		assertTrue(fleet.getShips().isEmpty());
	}

	@Test
	@DisplayName("Fleet operations reject null arguments")
	void operationsRejectNullArguments() {
		assertThrows(AssertionError.class, () -> fleet.addShip(null));
		assertThrows(AssertionError.class, () -> fleet.getShipsLike(null));
		assertThrows(AssertionError.class, () -> fleet.shipAt(null));
		assertThrows(AssertionError.class, () -> fleet.printShips(null));
		assertThrows(AssertionError.class, () -> fleet.printShipsByCategory(null));
	}

	@Test
	@DisplayName("A ship too close to an existing ship is rejected")
	void addShipRejectsCollisionRisk() {
		assertTrue(fleet.addShip(new Barge(Compass.NORTH, new Position(1, 1))));
		assertTrue(fleet.addShip(new Barge(Compass.NORTH, new Position(5, 5))));
		assertFalse(fleet.addShip(new Barge(Compass.NORTH, new Position(5, 5))));
		assertEquals(2, fleet.getShips().size());
	}

	@Test
	@DisplayName("The collision-risk check rejects null and detects adjacent ships")
	void collisionRiskChecksNullAndNearbyShips() {
		assertFalse(invokeCollisionRisk(fleet, new Barge(Compass.NORTH, new Position(5, 5))));
		assertTrue(fleet.addShip(new Barge(Compass.NORTH, new Position(1, 1))));
		assertTrue(invokeCollisionRisk(fleet, new Barge(Compass.NORTH, new Position(1, 2))));
		assertFalse(invokeCollisionRisk(fleet, new Barge(Compass.NORTH, new Position(5, 5))));
		assertThrows(AssertionError.class, () -> invokeCollisionRisk(fleet, null));
	}

	@Test
	@DisplayName("The ships accessor returns the fleet's ships")
	void getShipsReturnsAddedShips() {
		IShip ship = new Barge(Compass.NORTH, new Position(1, 1));
		fleet.addShip(ship);

		assertEquals(List.of(ship), fleet.getShips());
	}

	@Test
	@DisplayName("Ships can be filtered by category")
	void getShipsLikeFiltersByCategory() {
		IShip barge = new Barge(Compass.NORTH, new Position(1, 1));
		IShip caravel = new Caravel(Compass.NORTH, new Position(4, 4));
		fleet.addShip(barge);
		fleet.addShip(caravel);

		assertEquals(List.of(barge), fleet.getShipsLike("Barca"));
		assertTrue(fleet.getShipsLike("unknown").isEmpty());
		assertTrue(new Fleet().getShipsLike("Barca").isEmpty());
	}

	@Test
	@DisplayName("Ships are separated into floating and sunk groups")
	void floatingAndSunkShipsReflectHits() {
		IShip floatingShip = new Caravel(Compass.NORTH, new Position(4, 4));
		IShip sunkShip = new Barge(Compass.NORTH, new Position(1, 1));
		fleet.addShip(floatingShip);
		fleet.addShip(sunkShip);

		sunkShip.sink();

		assertEquals(List.of(floatingShip), fleet.getFloatingShips());
		assertEquals(List.of(sunkShip), fleet.getSunkShips());
		assertTrue(new Fleet().getFloatingShips().isEmpty());
		assertTrue(new Fleet().getSunkShips().isEmpty());
	}

	@Test
	@DisplayName("A ship can be found at an occupied position")
	void shipAtReturnsMatchingShipOrNull() {
		IShip ship = new Barge(Compass.NORTH, new Position(1, 1));
		IShip otherShip = new Barge(Compass.NORTH, new Position(5, 5));
		fleet.addShip(ship);
		fleet.addShip(otherShip);

		assertSame(ship, fleet.shipAt(new Position(1, 1)));
		assertSame(otherShip, fleet.shipAt(new Position(5, 5)));
		assertNull(fleet.shipAt(new Position(8, 8)));
		assertNull(new Fleet().shipAt(new Position(1, 1)));
	}

	@Test
	@DisplayName("Fleet printing methods display the requested ships and status")
	void printingMethodsWriteExpectedOutput() {
		IShip floatingShip = new Barge(Compass.NORTH, new Position(1, 1));
		IShip sunkShip = new Caravel(Compass.NORTH, new Position(4, 4));
		fleet.addShip(floatingShip);
		fleet.addShip(sunkShip);
		sunkShip.sink();

		assertEquals(floatingShip + System.lineSeparator() + sunkShip + System.lineSeparator(),
				captureOutput(() -> fleet.printShips(List.of(floatingShip, sunkShip))));
		assertEquals(floatingShip + System.lineSeparator(),
				captureOutput(() -> fleet.printShipsByCategory("Barca")));
		assertEquals(floatingShip + System.lineSeparator(),
				captureOutput(fleet::printFloatingShips));
		assertEquals(floatingShip + System.lineSeparator() + sunkShip + System.lineSeparator(),
				captureOutput(fleet::printAllShips));
		assertEquals("Estado da Frota: 1 a flutuar, 1 afundados!" + System.lineSeparator(),
				captureOutput(fleet::printStatus));
		Fleet emptyFleet = new Fleet();
		assertEquals("", captureOutput(() -> emptyFleet.printShips(List.of())));
		assertEquals("", captureOutput(emptyFleet::printFloatingShips));
		assertEquals("", captureOutput(emptyFleet::printAllShips));
		assertEquals("", captureOutput(() -> emptyFleet.printShipsByCategory("Barca")));
	}

	// ---------- helpers ----------

	private String captureOutput(Runnable action) {
		PrintStream originalOut = System.out;
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (PrintStream capturedOut = new PrintStream(output, true, StandardCharsets.UTF_8)) {
			System.setOut(capturedOut);
			action.run();
		} finally {
			System.setOut(originalOut);
		}
		return output.toString(StandardCharsets.UTF_8);
	}

	private boolean invokeCollisionRisk(Fleet target, IShip ship) {
		try {
			Method collisionRisk = Fleet.class.getDeclaredMethod("colisionRisk", IShip.class);
			collisionRisk.setAccessible(true);
			return (boolean) collisionRisk.invoke(target, ship);
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof AssertionError assertionError) {
				throw assertionError;
			}
			throw new AssertionError(e.getCause());
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}
}
