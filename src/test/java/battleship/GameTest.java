package battleship;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamWriteConstraints;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Isolated;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Game.
 * Author: britoeabreu
 * Date: 2024-03-19
 * Time: 15:30
 * Cyclomatic Complexity for each method:
 * - Game (constructor): 1
 * - fire: 7
 * - getShots: 1
 * - getRepeatedShots: 1
 * - getInvalidShots: 1
 * - getHits: 1
 * - getSunkShips: 1
 * - getRemainingShips: 1
 * - validShot: 3
 * - repeatedShot: 2
 * - printBoard: 1
 * - printValidShots: 1
 * - printFleet: 1
 */
@Isolated("Temporarily changes Jackson's JVM-wide serialization constraints")
public class GameTest {

	private Game game;

	@BeforeEach
	void setUp() {
		game = new Game(new Fleet()); // Assuming Fleet is a concrete implementation of IFleet
	}

	@AfterEach
	void tearDown() {
		game = null;
	}

	@Test
	@DisplayName("A new game starts with empty history and zero counters")
	void constructor() {
		assertNotNull(game, "Game instance should not be null after construction.");
		assertNotNull(game.getAlienMoves(), "Shots list should not be null after initialization.");
		assertTrue(game.getAlienMoves().isEmpty(), "Shots list should be empty upon initialization.");
		assertEquals(0, game.getInvalidShots(), "Invalid shots count should be zero upon initialization.");
		assertEquals(0, game.getRepeatedShots(), "Repeated shots count should be zero upon initialization.");
		assertEquals(0, game.getHits(), "Hits count should be zero upon initialization.");
		assertEquals(0, game.getSunkShips(), "Sunk ships count should be zero upon initialization.");
	}

	@Test
	@DisplayName("An out-of-board shot increments the invalid counter")
	void fire2() {
		Position invalidPosition = new Position(-1, 5);
		game.fireSingleShot(invalidPosition, false);
		assertEquals(1, game.getInvalidShots(), "Invalid shots counter should increase for an invalid shot.");
	}

	@Test
	@DisplayName("An explicitly repeated shot increments the repeated counter")
	void fire3() {
		Position position = new Position(2, 3);
		game.fireSingleShot(position, false);
		game.fireSingleShot(position, true);
		assertEquals(1, game.getRepeatedShots(), "Repeated shots counter should increase for a repeated shot.");
	}

	@Test
	@DisplayName("Recorded shots are recognized as repeated")
	void repeatedShot1() {
		List<IPosition> positions = List.of(new Position(2, 3), new Position(2, 4), new Position(2, 5));
		game.fireShots(positions);
		Position position = new Position(2, 3);
		assertTrue(game.repeatedShot(position), "Position (2,3) should be marked as repeated after firing.");
	}

	@Test
	@DisplayName("An unrecorded shot is not repeated")
	void repeatedShot2() {
		Position position = new Position(2, 3);
		assertFalse(game.repeatedShot(position), "Position (2,3) should not be marked as repeated before firing.");
	}

	@Test
	@DisplayName("Firing a volley records one enemy move")
	void getAlienMoves() {
		List<IPosition> positions = List.of(new Position(2, 3), new Position(2, 4), new Position(2, 5));
		game.fireShots(positions);
		assertEquals(1, game.getAlienMoves().size(), "Shots list should contain one shot after firing once.");
	}

	@Test
	@DisplayName("Remaining ships count decreases when a ship sinks")
	void getRemainingShips() {
		IFleet fleet = game.getMyFleet();
		Ship ship1 = new Barge(Compass.NORTH, new Position(1, 1));
		Ship ship2 = new Frigate(Compass.EAST, new Position(5, 5));

		fleet.addShip(ship1);
		assertEquals(1, game.getRemainingShips(), "Just one ship was created!");
		fleet.addShip(ship2);
		assertEquals(2, game.getRemainingShips(), "Two ships were created!");
		ship2.sink();
		assertEquals(1, game.getRemainingShips(), "Remaining ships count should be 1 after sinking one of two ships.");
	}


    @Test
    @DisplayName("A new game keeps the supplied fleet and initializes separate opponent state")
    void initializesBothSides() {
        Fleet fleet = new Fleet();
        Game initialized = new Game(fleet);
        assertSame(fleet, initialized.getMyFleet());
        assertNotSame(fleet, initialized.getAlienFleet());
        assertTrue(initialized.getAlienFleet().getShips().isEmpty());
        assertTrue(initialized.getMyMoves().isEmpty());
        assertNotSame(initialized.getMyMoves(), initialized.getAlienMoves());
    }

    @Test
    @DisplayName("Shots distinguish water, damage and sinking and update counters")
    void resolvesHitsAndSinks() {
        Ship ship = new Caravel(Compass.EAST, new Position(2, 2));
        assertTrue(game.getMyFleet().addShip(ship));
        assertEquals(new IGame.ShotResult(true, false, null, false),
                game.fireSingleShot(new Position(0, 0), false));
        assertEquals(new IGame.ShotResult(true, false, ship, false),
                game.fireSingleShot(new Position(2, 2), false));
        assertEquals(1, game.getHits());
        assertEquals(0, game.getSunkShips());
        assertEquals(1, game.getRemainingShips());
        assertEquals(new IGame.ShotResult(true, false, ship, true),
                game.fireSingleShot(new Position(2, 3), false));
        assertEquals(2, game.getHits());
        assertEquals(1, game.getSunkShips());
        assertEquals(0, game.getRemainingShips());
    }

    @Test
    @DisplayName("Invalid shots take precedence over the repeated flag")
    void invalidShotResult() {
        assertEquals(new IGame.ShotResult(false, false, null, false),
                game.fireSingleShot(new Position(10, 0), true));
        assertEquals(1, game.getInvalidShots());
        assertEquals(0, game.getRepeatedShots());
        assertEquals(0, game.getHits());
    }

    @Test
    @DisplayName("Moves number sequentially and detect repetition within and across volleys")
    void recordsMovesAndRepetitions() {
        Position first = new Position(0, 0);
        Position second = new Position(1, 1);
        game.fireShots(List.of(first, first, second));
        game.fireShots(List.of(first, new Position(3, 3), new Position(10, 10)));
        assertEquals(2, game.getAlienMoves().size());
        assertEquals(1, game.getAlienMoves().get(0).getNumber());
        assertEquals(2, game.getAlienMoves().get(1).getNumber());
        assertEquals(new IGame.ShotResult(true, true, null, false),
                game.getAlienMoves().get(0).getShotResults().get(1));
        assertEquals(new IGame.ShotResult(true, true, null, false),
                game.getAlienMoves().get(1).getShotResults().get(0));
        assertEquals(2, game.getRepeatedShots());
        assertEquals(1, game.getInvalidShots());
        assertTrue(game.repeatedShot(new Position(3, 3)));
        assertFalse(game.repeatedShot(new Position(9, 9)));
    }

    @Test
    @DisplayName("An incorrect volley size fails without recording a move or changing counters")
    void rejectsWrongVolleySize() {
        assertThrows(IllegalArgumentException.class, () -> game.fireShots(List.of()));
        assertThrows(IllegalArgumentException.class, () -> game.fireShots(List.of(new Position(0, 0))));
        assertThrows(IllegalArgumentException.class, () -> game.fireShots(List.of(
                new Position(0, 0), new Position(0, 1), new Position(0, 2), new Position(0, 3))));
        assertTrue(game.getAlienMoves().isEmpty());
        assertEquals(0, game.getHits());
        assertEquals(0, game.getInvalidShots());
        assertEquals(0, game.getRepeatedShots());
    }

    @Test
    @DisplayName("Shot JSON preserves order and classic board coordinates")
    void serializesShots() throws Exception {
        var mapper = new ObjectMapper();
        assertEquals(mapper.readTree("[]"), mapper.readTree(Game.jsonShots(List.of())));
        var json = mapper.readTree(Game.jsonShots(List.of(new Position(0, 0), new Position(9, 9))));
        assertEquals(2, json.size());
        assertEquals("A", json.get(0).get("row").asText());
        assertEquals(1, json.get(0).get("column").asInt());
        assertEquals("J", json.get(1).get("row").asText());
        assertEquals(10, json.get(1).get("column").asInt());
    }

    @Test
    @DisplayName("JSON serialization errors retain their cause and have a descriptive wrapper message")
    void wrapsJsonSerializationFailure() throws Exception {
        StreamWriteConstraints original = StreamWriteConstraints.defaults();
        List<IPosition> shots = List.of(new Position(0, 0));
        try {
            // Even the outer shot list requires one nesting level.
            // Disallow nesting for new mappers to trigger a real Jackson failure.
            StreamWriteConstraints.overrideDefaultStreamWriteConstraints(
                    original.rebuild().maxNestingDepth(0).build());
            RuntimeException failure = assertThrowsExactly(RuntimeException.class,
                    () -> Game.jsonShots(shots));
            assertEquals("Erro ao serializar o JSON", failure.getMessage());
            JsonProcessingException cause = assertInstanceOf(JsonProcessingException.class, failure.getCause());
            assertTrue(cause.getMessage().contains("nesting depth"));
        } finally {
            StreamWriteConstraints.overrideDefaultStreamWriteConstraints(original);
        }
        // The constraint change must not affect subsequent serialization.
        assertSame(original, StreamWriteConstraints.defaults());
        var json = new ObjectMapper().readTree(Game.jsonShots(shots));
        assertEquals("A", json.get(0).get("row").asText());
        assertEquals(1, json.get(0).get("column").asInt());
    }

    @Test
    @DisplayName("Enemy input accepts compact and separated coordinates, whitespace and lowercase")
    void readsEnemyCoordinates() throws Exception {
        try (Scanner input = new Scanner("  a1  B 2 j10  ")) {
            String result = game.readEnemyFire(input);
            assertEquals(List.of(new Position(0, 0), new Position(1, 1), new Position(9, 9)),
                    game.getAlienMoves().get(0).getShots());
            assertEquals(3, new ObjectMapper().readTree(result).size());
        }
    }

    @Test
    @DisplayName("Incomplete or missing enemy coordinates fail before a move is recorded")
    void rejectsIncompleteEnemyCoordinates() {
        for (String text : List.of("", "A1 B2", "A", "A nope")) {
            try (Scanner input = new Scanner(text + "\n")) {
                assertThrows(IllegalArgumentException.class, () -> game.readEnemyFire(input), text);
            }
        }
        assertTrue(game.getAlienMoves().isEmpty());
    }

    @Test
    @DisplayName("Only the first three complete enemy coordinates form the current volley")
    void stopsAtThreeShots() {
        try (Scanner input = new Scanner("A1 B2 C3 D4")) {
            game.readEnemyFire(input);
            assertEquals(List.of(new Position(0, 0), new Position(1, 1), new Position(2, 2)),
                    game.getAlienMoves().get(0).getShots());
        }
    }

    @Test
    @DisplayName("Random volleys contain three distinct in-board shots excluding history and sunk halos")
    void randomFireExcludesHistoryAndSunkAdjacencies() throws Exception {
        Ship sunk = new Barge(Compass.NORTH, new Position(5, 5));
        assertTrue(game.getMyFleet().addShip(sunk));
        sunk.sink();
        Position previous = new Position(0, 0);
        game.getAlienMoves().add(new Move(0, List.of(previous), List.of()));
        String json = game.randomEnemyFire();
        List<IPosition> shots = game.getAlienMoves().get(1).getShots();
        assertEquals(3, shots.size());
        assertEquals(3, new HashSet<>(shots).size());
        assertEquals(3, new ObjectMapper().readTree(json).size());
        for (IPosition shot : shots) {
            assertTrue(shot.isInside());
            assertNotEquals(previous, shot);
            assertFalse(sunk.getAdjacentPositions().contains(shot));
        }
    }

    // The generator owns its Random instance. Repetitions exercise duplicate draws
    // without asserting a particular sequence or changing the production API.
    @RepeatedTest(32)
    @DisplayName("With exactly three candidates a random volley fires each remaining position once")
    void randomFireWithExactlyThreeCandidates() {
        assertRandomVolleyFromRemaining(List.of(new Position(0, 0), new Position(0, 1), new Position(0, 2)));
    }

    @RepeatedTest(32)
    @DisplayName("With one or two candidates random fire repeats a selected position to complete a volley")
    void randomFireWithFewerThanThreeCandidates() {
        assertRandomVolleyFromRemaining(List.of(new Position(9, 9)));
        assertRandomVolleyFromRemaining(List.of(new Position(9, 8), new Position(9, 9)));
    }

    private void assertRandomVolleyFromRemaining(List<IPosition> remaining) {
        Game almostFinished = new Game(new Fleet());
        List<IPosition> history = new ArrayList<>();
        for (int row = 0; row < Game.BOARD_SIZE; row++) {
            for (int column = 0; column < Game.BOARD_SIZE; column++) {
                Position position = new Position(row, column);
                if (!remaining.contains(position)) {
                    history.add(position);
                }
            }
        }
        almostFinished.getAlienMoves().add(new Move(0, history, List.of()));
        almostFinished.randomEnemyFire();
        List<IPosition> shots = almostFinished.getAlienMoves().get(1).getShots();
        assertEquals(3, shots.size());
        assertEquals(new HashSet<>(remaining), new HashSet<>(shots));
        assertEquals(3 - remaining.size(), almostFinished.getRepeatedShots());
        assertEquals(0, almostFinished.getInvalidShots());
    }

    @Test
    @DisplayName("Board output shows ships, sinking halos, hits and water shots, ignoring invalid shots")
    void printsBoardStates() {
        Fleet fleet = new Fleet();
        Ship floating = new Caravel(Compass.EAST, new Position(2, 2));
        Ship sunk = new Barge(Compass.NORTH, new Position(6, 6));
        assertTrue(fleet.addShip(floating));
        assertTrue(fleet.addShip(sunk));
        sunk.sink();
        List<IMove> moves = List.of(new Move(1, List.of(
                new Position(2, 2), new Position(2, 2), new Position(0, 0), new Position(0, 0),
                new Position(6, 5), new Position(-1, 0)), List.of()));
        String output = captureOutput(() -> Game.printBoard(fleet, moves, true, true));
        assertEquals('*', boardCell(output, 2, 2));
        assertEquals('#', boardCell(output, 2, 3));
        assertEquals('#', boardCell(output, 6, 6));
        assertEquals('-', boardCell(output, 5, 5));
        assertEquals('o', boardCell(output, 6, 5));
        assertEquals('o', boardCell(output, 0, 0));
        assertEquals('.', boardCell(output, 9, 9));
        assertTrue(output.contains("LEGENDA"));
        String hiddenShots = captureOutput(() -> Game.printBoard(fleet, moves, false, false));
        assertEquals('#', boardCell(hiddenShots, 2, 2));
        assertEquals('.', boardCell(hiddenShots, 0, 0));
        assertFalse(hiddenShots.contains("LEGENDA"));
    }

    @Test
    @DisplayName("Board wrappers render the fleet and history belonging to their respective player")
    void printsBothPlayersBoards() {
        assertTrue(game.getMyFleet().addShip(new Barge(Compass.NORTH, new Position(1, 1))));
        assertTrue(game.getAlienFleet().addShip(new Barge(Compass.NORTH, new Position(8, 8))));
        game.getAlienMoves().add(new Move(1, List.of(new Position(1, 1)), List.of()));
        game.getMyMoves().add(new Move(1, List.of(new Position(8, 8)), List.of()));
        String mine = captureOutput(() -> game.printMyBoard(true, false));
        String alien = captureOutput(() -> game.printAlienBoard(true, false));
        assertEquals('*', boardCell(mine, 1, 1));
        assertEquals('.', boardCell(mine, 8, 8));
        assertEquals('*', boardCell(alien, 8, 8));
        assertEquals('.', boardCell(alien, 1, 1));
    }

    @Test
    @DisplayName("Ending the game prints the farewell without changing its state")
    void printsGameOver() {
        String output = captureOutput(game::over);
        assertTrue(output.contains("Java Sparrow"));
        assertTrue(game.getAlienMoves().isEmpty());
    }

    @Test
    @DisplayName("Game assertions reject null fleets, histories, shots and input")
    void rejectsNullArguments() {
        assertThrows(AssertionError.class, () -> Game.printBoard(null, List.of(), false, false));
        assertThrows(AssertionError.class, () -> Game.printBoard(new Fleet(), null, false, false));
        assertThrows(AssertionError.class, () -> Game.jsonShots(null));
        assertThrows(AssertionError.class, () -> game.readEnemyFire(null));
        assertThrows(AssertionError.class, () -> game.fireShots(null));
        assertThrows(AssertionError.class, () -> game.fireSingleShot(null, false));
        assertThrows(AssertionError.class, () -> game.repeatedShot(null));
    }

    private static char boardCell(String output, int row, int column) {
        String prefix = " " + (char) ('A' + row) + " |";
        String line = output.lines().filter(value -> value.startsWith(prefix)).findFirst().orElseThrow();
        return line.charAt(prefix.length() + 1 + column * 2);
    }

    private static String captureOutput(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(output);
            action.run();
        } finally {
            System.setOut(original);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
