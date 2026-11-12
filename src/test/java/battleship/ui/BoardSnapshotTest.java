package battleship.ui;

import battleship.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BoardSnapshotTest {
    @Test
    @DisplayName("An empty fleet produces a ten-by-ten board of empty cells")
    void capturesEmptyBoard() {
        BoardSnapshot snapshot = BoardSnapshot.from(new Game(new Fleet()));
        assertEquals(10, snapshot.getRows());
        assertEquals(10, snapshot.getColumns());
        assertEquals(0, snapshot.getRemainingShips());
        for (int row = 0; row < 10; row++) {
            for (int column = 0; column < 10; column++) {
                assertEquals(BoardCellState.EMPTY, snapshot.getCellState(row, column));
            }
        }
    }

    @Test
    @DisplayName("Ships, history hits, direct damage, misses and sunk ships have distinct states")
    void capturesAllCellStates() {
        Fleet fleet = new Fleet();
        Ship floating = new Frigate(Compass.EAST, new Position(2, 2));
        Ship sunk = new Barge(Compass.NORTH, new Position(7, 7));
        assertTrue(fleet.addShip(floating));
        assertTrue(fleet.addShip(sunk));
        floating.shoot(new Position(2, 3));
        sunk.sink();
        Game game = new Game(fleet);
        game.getAlienMoves().add(new Move(1,
                List.of(new Position(2, 2), new Position(0, 0), new Position(9, 9)), List.of()));

        BoardSnapshot snapshot = BoardSnapshot.from(game);
        assertEquals(BoardCellState.HIT, snapshot.getCellState(2, 2));
        assertEquals(BoardCellState.HIT, snapshot.getCellState(2, 3));
        assertEquals(BoardCellState.SHIP, snapshot.getCellState(2, 4));
        assertEquals(BoardCellState.SHIP, snapshot.getCellState(2, 5));
        assertEquals(BoardCellState.SUNK, snapshot.getCellState(7, 7));
        assertEquals(BoardCellState.MISS, snapshot.getCellState(0, 0));
        assertEquals(BoardCellState.MISS, snapshot.getCellState(9, 9));
        assertEquals(BoardCellState.EMPTY, snapshot.getCellState(5, 5));
        assertEquals(1, snapshot.getRemainingShips());
    }

    @Test
    @DisplayName("Each out-of-board boundary is ignored while edge shots are retained")
    void ignoresOutOfBoundsShots() {
        Game game = new Game(new Fleet());
        game.getAlienMoves().add(new Move(1, List.of(
                new Position(-1, 0), new Position(10, 0),
                new Position(0, -1), new Position(0, 10),
                new Position(0, 9), new Position(9, 0)), List.of()));
        game.getAlienMoves().add(new Move(2, List.of(), List.of()));
        BoardSnapshot snapshot = BoardSnapshot.from(game);
        for (int row = 0; row < 10; row++) {
            for (int column = 0; column < 10; column++) {
                BoardCellState expected = (row == 0 && column == 9) || (row == 9 && column == 0)
                        ? BoardCellState.MISS : BoardCellState.EMPTY;
                assertEquals(expected, snapshot.getCellState(row, column));
            }
        }
    }

    @Test
    @DisplayName("A snapshot retains its cells and ship count after the source game changes")
    void isolatesCapturedState() {
        Fleet fleet = new Fleet();
        Ship ship = new Barge(Compass.NORTH, new Position(4, 4));
        assertTrue(fleet.addShip(ship));
        Game game = new Game(fleet);
        List<IPosition> shots = new ArrayList<>(List.of(new Position(0, 0)));
        game.getAlienMoves().add(new Move(1, shots, List.of()));
        BoardSnapshot before = BoardSnapshot.from(game);
        ship.sink();
        shots.add(new Position(0, 1));
        BoardSnapshot after = BoardSnapshot.from(game);
        assertEquals(BoardCellState.SHIP, before.getCellState(4, 4));
        assertEquals(1, before.getRemainingShips());
        assertEquals(BoardCellState.EMPTY, before.getCellState(0, 1));
        assertEquals(BoardCellState.SUNK, after.getCellState(4, 4));
        assertEquals(0, after.getRemainingShips());
        assertEquals(BoardCellState.MISS, after.getCellState(0, 1));
    }

    @Test
    @DisplayName("Null games and invalid cell indices are rejected")
    void rejectsInvalidAccess() {
        assertThrows(NullPointerException.class, () -> BoardSnapshot.from(null));
        BoardSnapshot snapshot = BoardSnapshot.from(new Game(new Fleet()));
        assertThrows(IndexOutOfBoundsException.class, () -> snapshot.getCellState(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> snapshot.getCellState(10, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> snapshot.getCellState(0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> snapshot.getCellState(0, 10));
    }
}
