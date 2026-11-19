package battleship;

/**
 * Options shared by console board views.
 *
 * @param showShots whether to display shot markers
 * @param showLegend whether to display the board legend
 */
public record BoardDisplayOptions(boolean showShots, boolean showLegend) {
	public static final BoardDisplayOptions FLEET_WITH_LEGEND = new BoardDisplayOptions(false, true);
	public static final BoardDisplayOptions SHOTS_WITHOUT_LEGEND = new BoardDisplayOptions(true, false);
	public static final BoardDisplayOptions SHOTS_WITH_LEGEND = new BoardDisplayOptions(true, true);
}
