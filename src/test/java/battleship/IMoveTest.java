package battleship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.InputMismatchException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class IMoveTest {
    @Test
    @DisplayName("Read a numbered move with ordered zero-based shot coordinates")
    void readsShotsInOrder() {
        try (Scanner input = new Scanner("3 0 0 4 7 9 9 42")) {
            IMove move = IMove.readMove(12, input);
            assertEquals(12, move.getNumber());
            assertEquals(List.of(new Position(0, 0), new Position(4, 7), new Position(9, 9)), move.getShots());
            assertTrue(move.getShotResults().isEmpty());
            assertEquals(42, input.nextInt(), "Only the declared coordinates should be consumed");
        }
    }

    @Test
    @DisplayName("A move declaring no shots has empty shots and results")
    void readsEmptyMove() {
        try (Scanner input = new Scanner("0")) {
            IMove move = IMove.readMove(1, input);
            assertEquals(1, move.getNumber());
            assertTrue(move.getShots().isEmpty());
            assertTrue(move.getShotResults().isEmpty());
        }
    }

    @Test
    @DisplayName("Malformed and incomplete coordinates propagate scanner errors")
    void rejectsMalformedInput() {
        try (Scanner malformed = new Scanner("1 x 2"); Scanner incomplete = new Scanner("1 2")) {
            assertThrows(InputMismatchException.class, () -> IMove.readMove(1, malformed));
            assertThrows(NoSuchElementException.class, () -> IMove.readMove(1, incomplete));
        }
    }
}
