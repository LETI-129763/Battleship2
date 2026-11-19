package battleship;

/** Formats durations for presentation to the player. */
public final class DurationFormatter {

    private DurationFormatter() {
    }

    public static String format(long milliseconds) {
        return String.format("%.1f s", milliseconds / 1000.0);
    }
}
