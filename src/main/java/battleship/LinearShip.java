package battleship;

/**
 * Base class for ships whose positions follow a straight line.
 */
public abstract class LinearShip extends Ship {

	protected LinearShip(String category, Compass bearing, IPosition pos, int size) {
		super(category, bearing, pos, size);
	}
}
