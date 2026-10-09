package battleship.ui;

import battleship.Game;
import battleship.IFleet;
import battleship.IGame;
import battleship.IMove;
import battleship.IPosition;
import battleship.IShip;
import battleship.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An immutable view of the player's fleet and incoming shots.
 * Contains no mutable game objects or graphical toolkit dependencies.
 */
public final class BoardSnapshot {
    private final List<List<BoardCellState>> cells;
    private final int remainingShips;

    private BoardSnapshot(List<List<BoardCellState>> cells, int remainingShips) {
        this.cells = cells.stream().map(List::copyOf).toList();
        this.remainingShips = remainingShips;
    }

    /**
     * Captures the current board on the thread that owns the game.
     * Shots outside the board are ignored. Ship damage is read from both the
     * move history and the ship positions, including damage applied directly.
     *
     * @param game the game to capture, without concurrent mutations
     * @return a snapshot that can safely be sent to the graphical thread
     * @throws NullPointerException if the game is null
     */
    public static BoardSnapshot from(IGame game) {
        Objects.requireNonNull(game, "game");
        IFleet fleet = game.getMyFleet();
        boolean[][] shots = new boolean[Game.BOARD_SIZE][Game.BOARD_SIZE];

        for (IMove move : game.getAlienMoves()) {
            for (IPosition shot : move.getShots()) {
                int row = shot.getRow();
                int column = shot.getColumn();
                if (row >= 0 && row < Game.BOARD_SIZE && column >= 0 && column < Game.BOARD_SIZE) {
                    shots[row][column] = true;
                }
            }
        }

        List<List<BoardCellState>> cells = new ArrayList<>(Game.BOARD_SIZE);
        for (int row = 0; row < Game.BOARD_SIZE; row++) {
            List<BoardCellState> cellRow = new ArrayList<>(Game.BOARD_SIZE);
            for (int column = 0; column < Game.BOARD_SIZE; column++) {
                Position position = new Position(row, column);
                IShip ship = fleet.shipAt(position);
                BoardCellState state;
                if (ship == null) {
                    state = shots[row][column] ? BoardCellState.MISS : BoardCellState.EMPTY;
                } else if (!ship.stillFloating()) {
                    state = BoardCellState.SUNK;
                } else {
                    boolean hit = shots[row][column] || ship.getPositions().stream()
                            .anyMatch(part -> part.equals(position) && part.isHit());
                    state = hit ? BoardCellState.HIT : BoardCellState.SHIP;
                }
                cellRow.add(state);
            }
            cells.add(cellRow);
        }

        return new BoardSnapshot(cells, fleet.getFloatingShips().size());
    }

    /**
     * Returns the number of board rows.
     *
     * @return the row count
     */
    public int getRows() {
        return cells.size();
    }

    /**
     * Returns the number of board columns.
     *
     * @return the column count
     */
    public int getColumns() {
        return cells.get(0).size();
    }

    /**
     * Returns the visual state at zero-based board coordinates.
     *
     * @param row the row index
     * @param column the column index
     * @return the captured cell state
     * @throws IndexOutOfBoundsException if either coordinate is outside the board
     */
    public BoardCellState getCellState(int row, int column) {
        return cells.get(row).get(column);
    }

    /**
     * Returns the number of ships that were still floating when captured.
     *
     * @return the remaining ship count
     */
    public int getRemainingShips() {
        return remainingShips;
    }
}
