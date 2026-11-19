package battleship;

/**
 * Creates fleets with the game's predefined ship composition.
 */
public final class RandomFleetFactory {

	private static final String[] SHIP_TYPES = {
			"galeao", "fragata",
			"nau", "nau",
			"caravela", "caravela", "caravela",
			"barca", "barca", "barca", "barca"
	};

	private RandomFleetFactory() {
	}

	public static IFleet create() {
		IFleet randomFleet = new Fleet();
		int fleetSize = 0;

		while (fleetSize < SHIP_TYPES.length) {
			Ship ship = Ship.buildShip(
					SHIP_TYPES[fleetSize],
					Compass.randomBearing(),
					Position.randomPosition()
			);

			if (ship != null && randomFleet.addShip(ship)) {
				fleetSize++;
			}
		}
		return randomFleet;
	}
}
