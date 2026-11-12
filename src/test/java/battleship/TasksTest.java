package battleship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

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
		String output = captureOutput(() -> invokeRunMenu(
				"janela estado mapa rajada simula tiros ajuda desconhecido desisto"));

		assertTrue(output.contains("Crie primeiro uma frota"));
		assertTrue(output.contains("Que comando é esse"));
		assertTrue(output.contains("Bons ventos!"));
	}

	@Test
	@DisplayName("Menu commands operate on a loaded fleet and game")
	void menuHandlesCommandsAfterFleetCreation() {
		StringBuilder input = new StringBuilder("lefrota ");
		int[][] positions = {
				{0, 0}, {0, 2}, {0, 4}, {0, 6}, {0, 8},
				{2, 0}, {2, 2}, {2, 4}, {2, 6}, {2, 8}, {4, 0}
		};
		for (int[] position : positions) {
			input.append("barca ").append(position[0]).append(' ').append(position[1]).append(" n ");
		}
		input.append("estado mapa tiros rajada A1 B2 C3\ndesisto");

		String output = captureOutput(() -> invokeRunMenu(input.toString()));

		assertTrue(output.contains("Estado da Frota"));
		assertTrue(output.contains("Tempo da jogada"));
		assertTrue(output.contains("Bons ventos!"));
	}

	@Test
	@DisplayName("A winning volley saves the result and displays the leaderboard")
	void winningVolleySavesAndPrintsLeaderboard() {
		boolean[] calls = new boolean[2];
		Leaderboard leaderboard = new Leaderboard() {
			@Override
			public void saveResult(String playerName, int shots) {
				calls[0] = true;
				assertEquals("Ada", playerName);
				assertEquals(12, shots);
			}

			@Override
			public void printTop5() {
				calls[1] = true;
			}
		};

		String output = captureOutput(() -> Tasks.runMenu(new Scanner(winningGameInput()), leaderboard));

		assertTrue(calls[0]);
		assertTrue(calls[1]);
		assertTrue(output.contains("Result saved successfully."));
		assertTrue(output.contains("Total shots: 12"));
	}

	@Test
	@DisplayName("Leaderboard I/O failures are reported without skipping the other operation")
	void winningVolleyReportsLeaderboardFailures() {
		Leaderboard leaderboard = new Leaderboard() {
			@Override
			public void saveResult(String playerName, int shots) throws java.io.IOException {
				throw new java.io.IOException("save failed");
			}

			@Override
			public void printTop5() throws java.io.IOException {
				throw new java.io.IOException("display failed");
			}
		};

		String output = captureOutput(() -> Tasks.runMenu(new Scanner(winningGameInput()), leaderboard));

		assertTrue(output.contains("Could not save result: save failed"));
		assertTrue(output.contains("Could not display leaderboard: display failed"));
	}

	@Test
	@DisplayName("Simulation handles completed games and interrupted waits")
	void simulationCompletesAndPreservesInterrupts() {
		boolean[] gameOverCalled = new boolean[1];
		Game completingGame = new Game(new Fleet()) {
			private final int[] remainingShips = {1, 0, 0};
			private int index;

			@Override
			public int getRemainingShips() {
				return remainingShips[index++];
			}

			@Override
			public String randomEnemyFire() {
				return "";
			}

			@Override
			public void printMyBoard(boolean showShots, boolean showLegend) {
			}

			@Override
			public void over() {
				gameOverCalled[0] = true;
			}
		};

		try {
			Thread.currentThread().interrupt();
			assertTrue(Tasks.simulateGame(completingGame, new Fleet()));
			assertTrue(Thread.currentThread().isInterrupted());
			assertTrue(gameOverCalled[0]);
		} finally {
			assertTrue(Thread.interrupted());
		}

		Game incompleteGame = new Game(new Fleet()) {
			private final int[] remainingShips = {0, 1};
			private int index;

			@Override
			public int getRemainingShips() {
				return remainingShips[index++];
			}
		};
		assertFalse(Tasks.simulateGame(incompleteGame, new Fleet()));
	}

	@Test
	@DisplayName("The menu exits after a completed simulation")
	void menuExitsAfterSimulationCompletes() {
		CompletingGame game = new CompletingGame();
		String input = fleetSetupCommands() + "\nsimula";
		String output;

		try {
			Thread.currentThread().interrupt();
			output = captureOutput(() ->
					Tasks.runMenu(new Scanner(input), new Leaderboard(), ignored -> game));
		} finally {
			assertTrue(Thread.interrupted());
		}

		assertTrue(output.contains("Maldito sejas"));
	}

	@Test
	@DisplayName("The menu continues after a simulation that has not finished")
	void menuContinuesAfterIncompleteSimulation() {
		Game game = new Game(new Fleet()) {
			private final int[] remainingShips = {0, 1};
			private int index;

			@Override
			public int getRemainingShips() {
				return remainingShips[index++];
			}
		};
		String output = captureOutput(() -> Tasks.runMenu(
				new Scanner(fleetSetupCommands() + "\nsimula desisto"),
				new Leaderboard(),
				ignored -> game));

		assertTrue(output.contains("Bons ventos!"));
	}

	@Test
	@DisplayName("The menu reports when it cannot open the board window")
	void menuReportsBoardWindowFailure() {
		String output = captureOutput(() -> Tasks.runMenu(
				new Scanner(fleetSetupCommands() + "\njanela desisto"),
				new Leaderboard(),
				ignored -> new Game(new Fleet())));

		assertTrue(output.contains("Não foi possível abrir a janela:"));
	}

	@Test
	@DisplayName("An open board is shown and updated after game actions")
	void menuShowsAndUpdatesOpenBoard() {
		RecordingBoardDisplay boardDisplay = new RecordingBoardDisplay(false);

		Tasks.runMenu(
				new Scanner(fleetSetupCommands() + "\njanela mapa tiros rajada A1 B2 C3\ndesisto"),
				new Leaderboard(),
				fleet -> new Game(fleet),
				boardDisplay);

		assertEquals(1, boardDisplay.showCalls);
		assertEquals(2, boardDisplay.updateCalls);
		assertEquals(0, boardDisplay.shutdownCalls);
	}

	@Test
	@DisplayName("Board-window failures with causes are logged and reported")
	void menuLogsBoardWindowFailureCause() {
		RecordingBoardDisplay boardDisplay = new RecordingBoardDisplay(true);
		String output = captureOutput(() -> Tasks.runMenu(
				new Scanner(fleetSetupCommands() + "\njanela desisto"),
				new Leaderboard(),
				fleet -> new Game(fleet),
				boardDisplay));

		assertTrue(output.contains("Não foi possível abrir a janela: failure"));
	}

	@Test
	@DisplayName("The menu can generate a random fleet and then exit")
	void menuGeneratesRandomFleet() {
		String output = captureOutput(() -> invokeRunMenu("gerafrota estado mapa tiros desisto"));

		assertTrue(output.contains("Estado da Frota"));
		assertTrue(output.contains("Bons ventos!"));
	}

	@Test
	@DisplayName("The public menu always shuts down the board window")
	void menuShutsDownOnNormalExit() {
		String output = captureOutput(() -> runMenuWithInput("desisto"));

		assertTrue(output.contains("Bons ventos!"));
	}

	@Test
	@DisplayName("The public menu shuts down the board window when input fails")
	void menuShutsDownWhenInputFails() {
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
	@DisplayName("Classic positions accept compact, spaced, and lowercase forms")
	void readClassicPositionAcceptsSupportedFormats() {
		assertEquals(new Position('A', 3), Tasks.readClassicPosition(new Scanner("A3")));
		assertEquals(new Position('B', 7), Tasks.readClassicPosition(new Scanner("b 7")));
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

	private void invokeRunMenu(String input) {
		Tasks.runMenu(new Scanner(input), new Leaderboard());
	}

	private String winningGameInput() {
		return fleetSetupCommands()
				+ "\nrajada A1 A3 A5\n"
				+ "rajada A7 A9 C1\n"
				+ "rajada C3 C5 C7\n"
				+ "rajada C9 E1 B2\n"
				+ "Ada\n"
				+ "desisto";
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

	private static final class CompletingGame extends Game {
		private final int[] remainingShips = {1, 0, 0};
		private int index;

		private CompletingGame() {
			super(new Fleet());
		}

		@Override
		public int getRemainingShips() {
			return remainingShips[index++];
		}

		@Override
		public String randomEnemyFire() {
			return "";
		}

		@Override
		public void printMyBoard(boolean showShots, boolean showLegend) {
		}
	}

	private static final class RecordingBoardDisplay implements Tasks.BoardDisplay {
		private final boolean failOnShow;
		private int showCalls;
		private int updateCalls;
		private int shutdownCalls;

		private RecordingBoardDisplay(boolean failOnShow) {
			this.failOnShow = failOnShow;
		}

		@Override
		public boolean isOpen() {
			return true;
		}

		@Override
		public void update(battleship.ui.BoardSnapshot snapshot) {
			updateCalls++;
		}

		@Override
		public void show(battleship.ui.BoardSnapshot snapshot) {
			showCalls++;
			if (failOnShow) {
				throw new IllegalStateException("failure", new IllegalStateException("cause"));
			}
		}

		@Override
		public void shutdown() {
			shutdownCalls++;
		}
	}
}
