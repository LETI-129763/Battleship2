package battleship;

import java.util.ArrayList;
import java.util.List;

/** Stores move durations and calculates their aggregate statistics. */
final class MoveStatistics {

    private final List<Long> moveTimesMs = new ArrayList<>();

    void clear() {
        moveTimesMs.clear();
    }

    void record(long durationMs) {
        moveTimesMs.add(durationMs);
    }

    boolean isEmpty() {
        return moveTimesMs.isEmpty();
    }

    long averageTimeMs() {
        return (long) moveTimesMs.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);
    }

    long minimumTimeMs() {
        return moveTimesMs.stream()
                .mapToLong(Long::longValue)
                .min()
                .orElse(0);
    }

    long maximumTimeMs() {
        return moveTimesMs.stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0);
    }
}
