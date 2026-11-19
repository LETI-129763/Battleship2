package battleship;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/** Reads moves from console input. */
public final class MoveReader {
	private MoveReader() {
	}

	/**
	 * Reads a shot count followed by that many zero-based row and column pairs.
	 * Leaves any trailing tokens in the scanner for subsequent reads.
	 *
	 * @param moveNumber the number assigned to the move
	 * @param sc the scanner supplying the shot count and coordinates
	 * @return the move with ordered shots and no shot results
	 * @throws java.util.InputMismatchException if a required token is not an integer
	 * @throws java.util.NoSuchElementException if a required token is missing
	 */
	public static Move readMove(int moveNumber, Scanner sc) {
		int numShots = sc.nextInt();
		List<IPosition> moveShots = new ArrayList<>();
		for (int i = 0; i < numShots; i++) {
			int row = sc.nextInt();
			int col = sc.nextInt();
			moveShots.add(new Position(row, col));
		}
		return new Move(moveNumber, moveShots, new ArrayList<>());
	}
}
