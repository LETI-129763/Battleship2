package battleship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class GameClockTest {

	@Test
	@DisplayName("A clock without recorded moves reports an empty summary")
	void summaryWithoutMoves() {
		GameClock clock = new GameClock();

		assertEquals("Sem rajadas registadas.", clock.summary());
		clock.endGame();
	}

	@Test
	@DisplayName("Ending a move before it starts returns zero")
	void endMoveBeforeStartReturnsZero() {
		assertEquals(0, new GameClock().endMove());
	}

	@Test
	@DisplayName("Starting a move records its duration and can be repeated")
	void recordsMoveDurations() {
		GameClock clock = new GameClock();

		clock.startMove();
		assertTrue(clock.endMove() >= 0);
		clock.startMove();
		assertTrue(clock.endMove() >= 0);
		assertTrue(clock.summary().contains("Média por rajada: "));
	}

	@Test
	@DisplayName("Starting a game resets move history and records a final summary")
	void startAndEndGameProduceSummary() {
		GameClock clock = new GameClock();
		clock.startMove();
		clock.endMove();
		clock.startGame();
		clock.endMove();
		clock.endGame();

		String summary = clock.summary();
		assertTrue(summary.startsWith("Tempo total: "));
		assertTrue(summary.contains(" | Média por rajada: "));
		assertTrue(summary.contains(" | Mais rápida: "));
		assertTrue(summary.contains(" | Mais lenta: "));
	}

	@Test
	@DisplayName("Milliseconds are formatted as seconds with one decimal place")
	void formatsMilliseconds() {
		assertEquals(String.format(Locale.getDefault(), "%.1f s", 8.2), GameClock.format(8200));
		assertEquals(String.format(Locale.getDefault(), "%.1f s", 0.0), GameClock.format(0));
	}
}
