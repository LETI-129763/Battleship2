package battleship;

import java.util.List;
import java.util.Scanner;

/**
 * IMove
 *
 * @author Your Name
 * Date: 26/02/2026
 * Time: 18:32
 */
public interface IMove {
	static Move readMove(int moveNumber, Scanner sc) {
		return MoveReader.readMove(moveNumber, sc);
	}

	@Override
	String toString();

	int getNumber();

	List<IPosition> getShots();

	List<IGame.ShotResult> getShotResults();

	String processEnemyFire(boolean verbose);
}
