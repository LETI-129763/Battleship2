package battleship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LongMethodDemoTest {

	@Test
	@DisplayName("A high-accuracy victory receives the top rating and rank")
	void reportForExcellentVictory() {
		String report = LongMethodDemo.buildGameReport("Ada", 10, 8, 11);

		assertAll(
				() -> assertTrue(report.contains("Player: Ada\n")),
				() -> assertTrue(report.contains("Misses: 2\n")),
				() -> assertTrue(report.contains("Accuracy: 80.0%\n")),
				() -> assertTrue(report.contains("Rating: Excellent\n")),
				() -> assertTrue(report.contains("Score: 626\n")),
				() -> assertTrue(report.contains("Rank: Admiral\n")),
				() -> assertTrue(report.contains("Remaining enemy ships: 0\n")),
				() -> assertTrue(report.contains("Result: Victory\n"))
		);
	}

	@Test
	@DisplayName("Accuracy and score thresholds select good, average, captain, and sailor outcomes")
	void reportCoversIntermediateThresholds() {
		String goodCaptainReport = LongMethodDemo.buildGameReport("Grace", 10, 6, 6);
		String averageSailorReport = LongMethodDemo.buildGameReport("Turing", 5, 5, 2);
		String averageReport = LongMethodDemo.buildGameReport("Noor", 10, 4, 1);

		assertTrue(goodCaptainReport.contains("Rating: Good\n"));
		assertTrue(goodCaptainReport.contains("Score: 352\n"));
		assertTrue(goodCaptainReport.contains("Rank: Captain\n"));
		assertTrue(goodCaptainReport.contains("Result: In progress\n"));
		assertTrue(averageSailorReport.contains("Rank: Sailor\n"));
		assertTrue(averageReport.contains("Rating: Average\n"));
	}

	@Test
	@DisplayName("Zero shots and too many shots produce poor ratings and the appropriate result")
	void reportCoversZeroAccuracyAndTooManyShots() {
		String noShotsReport = LongMethodDemo.buildGameReport("Lin", 0, 0, 0);
		String tooManyShotsReport = LongMethodDemo.buildGameReport("Sam", 101, 0, 1);

		assertTrue(noShotsReport.contains("Accuracy: 0.0%\n"));
		assertTrue(noShotsReport.contains("Rating: Poor\n"));
		assertTrue(noShotsReport.contains("Score: 0\n"));
		assertTrue(noShotsReport.contains("Rank: Cabin boy\n"));
		assertTrue(noShotsReport.contains("Result: In progress\n"));
		assertTrue(tooManyShotsReport.contains("Result: Too many shots\n"));
	}
}
