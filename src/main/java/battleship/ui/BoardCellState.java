package battleship.ui;

/** Visual states of a board position, independent of the graphics library. */
public enum BoardCellState {
    /** Water that has not been fired upon. */
    EMPTY,
    /** An undamaged position of a floating ship. */
    SHIP,
    /** A shot that hit water. */
    MISS,
    /** A damaged position of a ship that is still floating. */
    HIT,
    /** A position occupied by a sunken ship. */
    SUNK
}
