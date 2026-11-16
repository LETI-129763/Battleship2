package battleship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the original Tasks class (no production code changes).
 *
 * Paths that end in System.exit(0) (winning volley, completed simulation)
 * are NOT exercised here, because they would terminate the test JVM.
 */
class TasksTest {

	@Test
	@DisplayName("The help menu lists the supported commands")
	void menuHelpListsCommands() {
		String output = captureOutput(Tasks::menuHelp);

		assertTrue(output.contains("gerafrota"));
		assertTrue(output.contains("lefrota"));
		assertTrue(output.contains("rajada"));
		assertTrue(output.contains("desisto"));
	}

	@Test
	@DisplayName("Menu commands without a game handle missing game state and unknown commands")
	void menuHandlesCommandsBeforeGameCreation() {
		String output = captureOutput(() -> runMenuWithInput(
				"janela estado mapa rajada simula tiros ajuda desconhecido desisto"));

		assertTrue(output.contains("Crie primeiro uma frota"));
		assertTrue(output.contains("Que comando é esse"));
		assertTrue(output.contains("Bons ventos!"));
	}

	@Test
	@DisplayName("Menu commands operate on a loaded fleet and game")
	void menuHandlesCommandsAfterFleetCreation() {
		String input = fleetSetupCommands() + "\nestado mapa tiros rajada A1 B2 C3\ndesisto";

		String output = captureOutput(() -> runMenuWithInput(input));

		assertTrue(output.contains("Estado da Frota"));
		assertTrue(output.contains("Tempo da jogada"));
		assertTrue(output.contains("Bons ventos!"));
	}

	@Test
	@DisplayName("The menu can generate a random fleet and then exit")
	void menuGeneratesRandomFleet() {
		String output = captureOutput(() -> runMenuWithInput("gerafrota estado mapa tiros desisto"));

		assertTrue(output.contains("Estado da Frota"));
		assertTrue(output.contains("Bons ventos!"));
	}

	@Test
	@DisplayName("The menu reports when it cannot open the board window")
	void menuReportsBoardWindowFailure() {
		// Depends on BoardWindow.show failing in this environment (e.g. headless/CI).
		String output = captureOutput(() -> runMenuWithInput(
				fleetSetupCommands() + "\njanela desisto"));

		assertTrue(output.contains("Não foi possível abrir a janela:"));
	}

	@Test
	@DisplayName("The public menu exits normally on the quit command")
	void menuExitsOnNormalExit() {
		String output = captureOutput(() -> runMenuWithInput("desisto"));

		assertTrue(output.contains("Bons ventos!"));
	}

	@Test
	@DisplayName("The public menu propagates the failure when the input ends")
	void menuFailsWhenInputEnds() {
		assertThrows(NoSuchElementException.class, () -> runMenuWithInput(""));
	}

	@Test
	@DisplayName("The player name reader trims input and retries empty names")
	void readPlayerNameTrimsAndRetries() {
		String[] name = new String[1];
		String output = captureOutput(() -> name[0] = invokeReadPlayerName("\n   \n Ada Lovelace \n"));

		assertEquals("Ada Lovelace", name[0]);
		assertTrue(output.contains("Player name cannot be empty."));
	}

	@Test
	@DisplayName("A fleet can be built while invalid and colliding ships are skipped")
	void buildFleetSkipsInvalidAndCollidingShips() {
		StringBuilder input = new StringBuilder("unknown 0 0 n barca 0 0 n barca 0 0 n ");
		int added = 0;
		for (int row = 0; row <= 8 && added < Fleet.FLEET_SIZE; row += 2) {
			for (int column = 0; column <= 8 && added < Fleet.FLEET_SIZE; column += 2) {
				if (row == 0 && column == 0) {
					added++;
					continue;
				}
				input.append("barca ").append(row).append(' ').append(column).append(" n ");
				added++;
			}
		}

		Fleet fleet = Tasks.buildFleet(new Scanner(input.toString()));

		assertEquals(Fleet.FLEET_SIZE, fleet.getShips().size());
		assertEquals(Fleet.FLEET_SIZE, fleet.getShipsLike("Barca").size());
	}

	@Test
	@DisplayName("A recognized ship command builds the requested ship")
	void readShipBuildsShip() {
		Ship ship = Tasks.readShip(new Scanner("caravela 2 3 e"));

		assertInstanceOf(Caravel.class, ship);
		assertEquals(Compass.EAST, ship.getBearing());
		assertEquals(new Position(2, 3), ship.getPosition());
	}

	@Test
	@DisplayName("An unknown ship command returns no ship")
	void readShipRejectsUnknownType() {
		assertNull(Tasks.readShip(new Scanner("submarine 2 3 n")));
	}

	@Test
	@DisplayName("A ship command with an unknown bearing is rejected")
	void readShipRejectsUnknownBearing() {
		assertThrows(AssertionError.class, () -> Tasks.readShip(new Scanner("barca 2 3 x")));
	}

	@Test
	@DisplayName("Numeric row and column coordinates are read")
	void readPositionReadsCoordinates() {
		assertEquals(new Position(4, 7), Tasks.readPosition(new Scanner("4 7")));
	}

	@Test
	@DisplayName("Fleet and ship readers require a scanner")
	void readersRejectNullScanners() {
		assertThrows(AssertionError.class, () -> Tasks.buildFleet(null));
		assertThrows(AssertionError.class, () -> Tasks.readShip(null));
		assertThrows(AssertionError.class, () -> Tasks.readPosition(null));
	}

	@Test
	@DisplayName("Classic positions accept compact and spaced forms")
	void readClassicPositionAcceptsSupportedFormats() {
		assertEquals(new Position('A', 3), Tasks.readClassicPosition(new Scanner("A3")));
		assertEquals(new Position('B', 3), Tasks.readClassicPosition(new Scanner("b3")));
		assertEquals(new Position('B', 7), Tasks.readClassicPosition(new Scanner("B 7")));
	}

	@Test
	@DisplayName("Classic positions reject missing and malformed input")
	void readClassicPositionRejectsInvalidFormats() {
		assertThrows(IllegalArgumentException.class,
				() -> Tasks.readClassicPosition(new Scanner("")));
		assertThrows(IllegalArgumentException.class,
				() -> Tasks.readClassicPosition(new Scanner("12")));
		assertThrows(IllegalArgumentException.class,
				() -> Tasks.readClassicPosition(new Scanner("12 3")));
		assertThrows(IllegalArgumentException.class,
				() -> Tasks.readClassicPosition(new Scanner("A x")));
		assertThrows(IllegalArgumentException.class,
				() -> Tasks.readClassicPosition(new Scanner("A -1")));
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

	private String fleetSetupCommands() {
		StringBuilder input = new StringBuilder("lefrota ");
		int[][] positions = {
				{0, 0}, {0, 2}, {0, 4}, {0, 6}, {0, 8},
				{2, 0}, {2, 2}, {2, 4}, {2, 6}, {2, 8}, {4, 0}
		};
		for (int[] position : positions) {
			input.append("barca ").append(position[0]).append(' ').append(position[1]).append(" n ");
		}
		return input.toString();
	}

	private String invokeReadPlayerName(String input) {
		java.io.InputStream originalIn = System.in;
		try {
			System.setIn(new java.io.ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
			Method readPlayerName = Tasks.class.getDeclaredMethod("readPlayerName", Scanner.class);
			readPlayerName.setAccessible(true);
			return (String) readPlayerName.invoke(null, new Scanner(System.in));
		} catch (NoSuchMethodException | IllegalAccessException e) {
			throw new AssertionError(e);
		} catch (InvocationTargetException e) {
			throw new AssertionError(e.getCause());
		} finally {
			System.setIn(originalIn);
		}
	}

	private void runMenuWithInput(String input) {
		java.io.InputStream originalIn = System.in;
		try {
			System.setIn(new java.io.ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
			Tasks.menu();
		} finally {
			System.setIn(originalIn);
		}
	}
}