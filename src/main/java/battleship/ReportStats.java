package battleship;

/**
 * Statistics displayed in a game report header.
 */
public record ReportStats(int shots, int hits, int sunkShips, int misses) {
}
