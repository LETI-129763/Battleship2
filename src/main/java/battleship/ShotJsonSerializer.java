package battleship;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Converts shot positions to their classic coordinates and serializes them as JSON. */
final class ShotJsonSerializer {
	private ShotJsonSerializer() {
	}

	/**
	 * Serializes shots in order, preserving row/column field order and indentation.
	 *
	 * @param shots the non-null list of shot positions
	 * @return the formatted JSON representation
	 * @throws RuntimeException if JSON serialization fails, retaining the original cause
	 */
	static String serialize(List<IPosition> shots) {
		assert shots != null;

		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

		List<Map<String, Object>> simplifiedShots = new ArrayList<>();
		for (IPosition shot : shots) {
			Map<String, Object> simplePos = new LinkedHashMap<>();
			simplePos.put("row", String.valueOf(shot.getClassicRow()));
			simplePos.put("column", shot.getClassicColumn());
			simplifiedShots.add(simplePos);
		}

		try {
			return objectMapper.writeValueAsString(simplifiedShots);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Erro ao serializar o JSON", e);
		}
	}
}
