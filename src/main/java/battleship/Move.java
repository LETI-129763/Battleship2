package battleship;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.*;

/**
 * Shot
 *
 * @author Your Name
 * Date: 20/02/2026
 * Time: 19:39
 */
public class Move implements IMove {

	//-------------------------------------------------------------------
	private final int number;
	private final List<IPosition> shots;
	private final List<IGame.ShotResult> shotResults;

	//-------------------------------------------------------------------
	public Move(int moveNumber, List<IPosition> moveShots, List<IGame.ShotResult> moveResults) {
		this.number = moveNumber;
		this.shots = moveShots;
		this.shotResults = moveResults;
	}

	@Override
	public String toString() {
		return "Move{" +
				"number=" + number +
				", shots=" + shots.size() +
				", results=" + shotResults.size() +
				'}';
	}

	@Override
	public int getNumber() {
		return this.number;
	}

	@Override
	public List<IPosition> getShots() {
		return this.shots;
	}

	@Override
	public List<IGame.ShotResult> getShotResults() {
		return this.shotResults;
	}

	/**
	 * Processes the results of enemy fire on the game board, analyzing the outcomes of shots,
	 * such as valid shots, repeated shots, missed shots, hits on ships, and sunk ships. It can
	 * also display a detailed summary of the shot results if verbose mode is activated.
	 *
	 * @param verbose a boolean indicating whether a detailed summary should be printed to the console
	 *                for the processed enemy fire data.
	 * @return a JSON-formatted string that encapsulates the results, including counts of valid shots,
	 *         repeated shots, missed shots, shots outside the game board, and details of hits and
	 *         sunk ships.
	 */
	@Override
	public String processEnemyFire(boolean verbose) {
		ShotStatistics statistics = collectShotStatistics();
		int outsideShots = Game.NUMBER_SHOTS - statistics.validShots() - statistics.repeatedShots();

		if (verbose) {
			printSummary(statistics, outsideShots);
		}

		String jsonString = serializeResponse(createResponse(statistics, outsideShots));
		System.out.println(jsonString);
		System.out.println();

		return jsonString;
	}

	private ShotStatistics collectShotStatistics() {
		int validShots = 0;
		int repeatedShots = 0;
		int missedShots = 0;
		Map<String, Integer> sunkBoatsCount = new HashMap<>();
		Map<String, Integer> hitsPerBoat = new HashMap<>();

		for (IGame.ShotResult result : shotResults) {
			if (!result.valid()) {
				continue;
			}

			if (result.repeated()) {
				repeatedShots++;
				continue;
			}

			validShots++;
			if (result.ship() == null) {
				missedShots++;
				continue;
			}

			String boatName = result.ship().getCategory();
			hitsPerBoat.put(boatName, hitsPerBoat.getOrDefault(boatName, 0) + 1);
			if (result.sunk()) {
				sunkBoatsCount.put(boatName, sunkBoatsCount.getOrDefault(boatName, 0) + 1);
			}
		}

		return new ShotStatistics(validShots, repeatedShots, missedShots, sunkBoatsCount, hitsPerBoat);
	}

	private Map<String, Object> createResponse(ShotStatistics statistics, int outsideShots) {
		Map<String, Object> response = new HashMap<>();
		response.put("validShots", statistics.validShots());
		response.put("outsideShots", outsideShots);
		response.put("repeatedShots", statistics.repeatedShots());
		response.put("missedShots", statistics.missedShots());
		response.put("sunkBoats", createSunkBoats(statistics.sunkBoatsCount()));
		response.put("hitsOnBoats", createBoatHits(statistics));
		return response;
	}

	private List<Map<String, Object>> createSunkBoats(Map<String, Integer> sunkBoatsCount) {
		List<Map<String, Object>> sunkBoats = new ArrayList<>();
		for (Map.Entry<String, Integer> entry : sunkBoatsCount.entrySet()) {
			Map<String, Object> boat = new HashMap<>();
			boat.put("type", entry.getKey());
			boat.put("count", entry.getValue());
			sunkBoats.add(boat);
		}
		return sunkBoats;
	}

	private List<Map<String, Object>> createBoatHits(ShotStatistics statistics) {
		List<Map<String, Object>> boatHits = new ArrayList<>();
		for (Map.Entry<String, Integer> entry : statistics.hitsPerBoat().entrySet()) {
			if (!statistics.sunkBoatsCount().containsKey(entry.getKey())) {
				Map<String, Object> boat = new HashMap<>();
				boat.put("type", entry.getKey());
				boat.put("hits", entry.getValue());
				boatHits.add(boat);
			}
		}
		return boatHits;
	}

	private String serializeResponse(Map<String, Object> response) {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		try {
			return objectMapper.writeValueAsString(response);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Erro ao serializar o JSON dos resultados da jogada", e);
		}
	}

	private void printSummary(ShotStatistics statistics, int outsideShots) {
		StringBuilder output = buildSummary(statistics, outsideShots);
		System.out.println("Jogada nº" + number + " -> " + output);
	}

	private StringBuilder buildSummary(ShotStatistics statistics, int outsideShots) {
		StringBuilder output = new StringBuilder();
		if (statistics.validShots() == 0 && statistics.repeatedShots() > 0) {
			appendShotCounts(output, 0, statistics.repeatedShots());
			appendOutsideShots(output, outsideShots);
			return output;
		}

		appendShotCounts(output, statistics.validShots(), statistics.repeatedShots());
		appendBoatResults(output, statistics);
		appendMissedAndRepeatedShots(output, statistics);
		appendOutsideShots(output, outsideShots);
		return output;
	}

	private void appendShotCounts(StringBuilder output, int validShots, int repeatedShots) {
		if (validShots > 0) {
			output.append(validShots).append(" tiro").append(validShots > 1 ? "s" : "").append(" válido").append(validShots > 1 ? "s" : "").append(": ");
		}
		if (validShots == 0 && repeatedShots > 0) {
			output.append(repeatedShots).append(" tiro").append(repeatedShots > 1 ? "s" : "").append(" repetido").append(repeatedShots > 1 ? "s" : "");
		}
	}

	private void appendBoatResults(StringBuilder output, ShotStatistics statistics) {
		appendSunkBoatResults(output, statistics.sunkBoatsCount());
		appendUnsunkBoatResults(output, statistics);
	}

	private void appendSunkBoatResults(StringBuilder output, Map<String, Integer> sunkBoatsCount) {
		for (Map.Entry<String, Integer> entry : sunkBoatsCount.entrySet()) {
			String boatName = entry.getKey();
			int count = entry.getValue();
			output.append(count).append(" ").append(boatName).append(count > 1 ? "s" : "").append(" ao fundo").append(" + ");
		}
	}

	private void appendUnsunkBoatResults(StringBuilder output, ShotStatistics statistics) {
		for (Map.Entry<String, Integer> entry : statistics.hitsPerBoat().entrySet()) {
			String boatName = entry.getKey();
			int hits = entry.getValue();
			if (!statistics.sunkBoatsCount().containsKey(boatName)) {
				output.append(hits).append(" tiro").append(hits > 1 ? "s" : "").append(" num(a) ").append(boatName).append(" + ");
			}
		}
	}

	private void appendMissedAndRepeatedShots(StringBuilder output, ShotStatistics statistics) {
		if (statistics.missedShots() > 0) {
			int missedShots = statistics.missedShots();
			output.append(missedShots).append(" tiro").append(missedShots > 1 ? "s" : "").append(" na água");
		} else if (!statistics.sunkBoatsCount().isEmpty() || !statistics.hitsPerBoat().isEmpty()) {
			output.setLength(output.length() - 2);
		}

		appendRepeatedShots(output, statistics.validShots(), statistics.repeatedShots());
	}

	private void appendRepeatedShots(StringBuilder output, int validShots, int repeatedShots) {
		if (repeatedShots == 0) {
			return;
		}
		if (validShots > 0) {
			output.append(", ");
		}
		output.append(repeatedShots).append(" tiro").append(repeatedShots > 1 ? "s" : "").append(" repetido").append(repeatedShots > 1 ? "s" : "");
	}

	private void appendOutsideShots(StringBuilder output, int outsideShots) {
		if (outsideShots == 0) {
			return;
		}
		if (!output.isEmpty()) {
			output.append(", ");
		}
		output.append(outsideShots).append(" tiro").append(outsideShots > 1 ? "s" : "").append(" exterior").append(outsideShots > 1 ? "es" : "");
	}

	private record ShotStatistics(
			int validShots,
			int repeatedShots,
			int missedShots,
			Map<String, Integer> sunkBoatsCount,
			Map<String, Integer> hitsPerBoat
	) {
	}
}