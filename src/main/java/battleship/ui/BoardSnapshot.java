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
 * Immutable board copy that retains no references to game collections or positions.
 * Create snapshots on the command thread, between changes to the game state.
 */
public final class BoardSnapshot {
    private final List<List<BoardCellState>> cells;
    private final int remainingShips;

    private BoardSnapshot(List<List<BoardCellState>> cells, int remainingShips) {
        this.cells = List.copyOf(cells);
        this.remainingShips = remainingShips;
    }

    /**
     * Copies the state of all 100 positions using the fleet and enemy shots.
     * Whether a ship is sunk is determined by the existing ship rules.
     *
     * @param game game to read on the command thread
     * @return an immutable copy of the current board state
     * @throws NullPointerException if the game is null
     */
    public static BoardSnapshot from(IGame game) {
        Objects.requireNonNull(game, "O jogo não pode ser nulo.");
        IFleet fleet = game.getMyFleet();
        boolean[][] shots = new boolean[Game.BOARD_SIZE][Game.BOARD_SIZE];
        for (IMove move : game.getAlienMoves()) {
            for (IPosition shot : move.getShots()) {
                if (shot.isInside()) {
                    shots[shot.getRow()][shot.getColumn()] = true;
                }
            }
        }

        List<List<BoardCellState>> cells = new ArrayList<>();
        for (int row = 0; row < Game.BOARD_SIZE; row++) {
            List<BoardCellState> line = new ArrayList<>();
            for (int column = 0; column < Game.BOARD_SIZE; column++) {
                IPosition position = new Position(row, column);
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
                line.add(state);
            }
            cells.add(List.copyOf(line));
        }
        return new BoardSnapshot(cells, fleet.getFloatingShips().size());
    }

    /** @return the number of board rows */
    public int getRows() {
        return cells.size();
    }

    /** @return the number of board columns */
    public int getColumns() {
        return cells.get(0).size();
    }

    /**
     * Returns the visual state of a position using zero-based coordinates.
     *
     * @param row row index between 0 and 9
     * @param column column index between 0 and 9
     * @return the visual state of the position
     * @throws IndexOutOfBoundsException if the position is outside the board
     */
    public BoardCellState getCellState(int row, int column) {
        return cells.get(row).get(column);
    }

    /** @return the number of floating ships when the snapshot was created */
    public int getRemainingShips() {
        return remainingShips;
    }
}
