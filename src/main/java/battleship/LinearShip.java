package battleship;

/**
 * Base class for ships whose positions follow a straight line.
 */
public abstract class LinearShip extends Ship {

	protected LinearShip(String category, Compass bearing, IPosition pos, int size) {
		super(category, bearing, pos, size);
	}

	protected final void initializePositions() {
		IPosition initialPosition = getPosition();

		switch (getBearing()) {
			case NORTH, SOUTH:
				for (int rowOffset = 0; rowOffset < getSize(); rowOffset++) {
					getPositions().add(new Position(
							initialPosition.getRow() + rowOffset,
							initialPosition.getColumn()
					));
				}
				break;
			case EAST, WEST:
				for (int columnOffset = 0; columnOffset < getSize(); columnOffset++) {
					getPositions().add(new Position(
							initialPosition.getRow(),
							initialPosition.getColumn() + columnOffset
					));
				}
				break;
		}
	}
}
