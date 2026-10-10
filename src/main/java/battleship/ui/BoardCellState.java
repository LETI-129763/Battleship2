package battleship.ui;

/**
 * Visual states of a board cell, independent of the graphical toolkit.
 */
public enum BoardCellState {
    /** Water that has not been targeted. */
    EMPTY,
    /** An undamaged part of a floating ship. */
    SHIP,
    /** A shot that landed in water. */
    MISS,
    /** A damaged part of a ship that is still floating. */
    HIT,
    /** A part of a sunk ship. */
    SUNK
}
