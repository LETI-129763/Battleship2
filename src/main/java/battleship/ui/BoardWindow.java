package battleship.ui;

import org.piccolo2d.PCanvas;
import org.piccolo2d.PNode;
import org.piccolo2d.nodes.PPath;
import org.piccolo2d.nodes.PText;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

/**
 * Board rendered with Piccolo2D nodes on a canvas hosted in a Swing window.
 * Accepts immutable snapshots without accessing the game. Closing hides the
 * window so it can be reopened from the console.
 * Public methods may be called from the command thread.
 */
public final class BoardWindow {
    private static final int CELL_SIZE = 42;
    private static final int CELL_GAP = 4;
    private static final int CELL_STEP = CELL_SIZE + CELL_GAP;
    private static final int GRID_X = 96;
    private static final int GRID_Y = 130;
    private static final Color INK = Color.decode("#16324f");
    private static final Color MUTED = Color.decode("#536b80");

    private static boolean created;
    private static boolean shutdown;
    private static volatile boolean open;
    private static BoardSnapshot pendingSnapshot;
    private static boolean updateQueued;

    /** Window created and accessed only on the Swing Event Dispatch Thread. */
    private static JFrame window;
    /** Cell shapes created and accessed only on the Swing Event Dispatch Thread. */
    private static PPath[][] cells;
    /** Cell symbols created and accessed only on the Swing Event Dispatch Thread. */
    private static PText[][] markers;
    /** Fleet counter created and accessed only on the Swing Event Dispatch Thread. */
    private static PText fleetStatus;

    private BoardWindow() {
    }

    /**
     * Opens or shows the window again with the supplied board state.
     *
     * @param snapshot board state copied on the command thread
     * @throws NullPointerException if the snapshot is null
     * @throws IllegalStateException if no graphical environment is available,
     *                               the window was shut down or creation fails
     */
    public static void show(BoardSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "O estado do tabuleiro não pode ser nulo.");
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException("É necessário um ambiente gráfico para abrir a janela.");
        }
        runOnEventThreadAndWait(() -> {
            synchronized (BoardWindow.class) {
                if (shutdown) {
                    throw new IllegalStateException("A janela do tabuleiro já foi encerrada.");
                }
            }
            if (window == null) {
                createWindow(snapshot);
            }
            render(snapshot);
            window.setVisible(true);
            window.setState(Frame.NORMAL);
            window.toFront();
            open = true;
        });
    }

    /** @return true if the window is open */
    public static boolean isOpen() {
        return open;
    }

    /**
     * Updates an open window. Rapid updates share a single pending task that
     * displays the latest snapshot without filling the Swing event queue.
     *
     * @param snapshot board state copied on the command thread
     * @throws NullPointerException if the snapshot is null
     */
    public static synchronized void update(BoardSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "O estado do tabuleiro não pode ser nulo.");
        if (open && !shutdown) {
            pendingSnapshot = snapshot;
            if (!updateQueued) {
                updateQueued = true;
                SwingUtilities.invokeLater(BoardWindow::applyPendingUpdate);
            }
        }
    }

    /**
     * Releases the window with {@link JFrame#dispose()} so the process can exit
     * when the menu ends. Detaching the canvas releases Piccolo2D global listeners.
     * Does not start the Swing thread if the window has never been opened.
     * Shutdown is permanent for this window.
     */
    public static void shutdown() {
        synchronized (BoardWindow.class) {
            if (!created || shutdown) {
                return;
            }
            shutdown = true;
            open = false;
            pendingSnapshot = null;
        }
        runOnEventThreadAndWait(() -> {
            window.getContentPane().removeAll();
            window.dispose();
            window = null;
            cells = null;
            markers = null;
            fleetStatus = null;
        });
    }

    /** Executes an action synchronously on the Swing Event Dispatch Thread. */
    private static void runOnEventThreadAndWait(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(action);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operação da janela interrompida.", e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("Falha ao executar uma operação da janela.", cause);
        }
    }

    /** Applies the latest queued snapshot on the Swing Event Dispatch Thread. */
    private static void applyPendingUpdate() {
        BoardSnapshot snapshot;
        synchronized (BoardWindow.class) {
            updateQueued = false;
            if (shutdown) {
                return;
            }
            snapshot = pendingSnapshot;
            pendingSnapshot = null;
        }
        if (open && snapshot != null) {
            render(snapshot);
        }
    }

    /**
     * Creates the Piccolo2D scene on the Swing Event Dispatch Thread.
     * Disables default pan and zoom gestures to keep the board fixed in place.
     */
    private static void createWindow(BoardSnapshot snapshot) {
        JFrame frame = new JFrame("Battleship — O meu tabuleiro");
        try {
            frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent event) {
                    open = false;
                }
            });

            PCanvas canvas = new PCanvas();
            canvas.setPreferredSize(new Dimension(620, 750));
            canvas.setBackground(Color.decode("#eff4f8"));
            canvas.setPanEventHandler(null);
            canvas.setZoomEventHandler(null);

            PNode scene = canvas.getLayer();
            scene.addChild(text("O meu tabuleiro", 28, 18, 28, Font.BOLD, INK));
            fleetStatus = text("", 28, 64, 16, Font.PLAIN, MUTED);
            scene.addChild(fleetStatus);
            createBoard(scene, snapshot);
            createLegend(scene);
            scene.addChild(text("Usa a consola para jogar. Fecha e volta a abrir com o comando janela.",
                    28, 720, 12, Font.PLAIN, MUTED));

            JPanel root = new JPanel(new BorderLayout());
            root.add(canvas, BorderLayout.CENTER);
            frame.setContentPane(root);
            frame.setResizable(false);
            frame.pack();
            frame.setLocationRelativeTo(null);
            window = frame;
            synchronized (BoardWindow.class) {
                created = true;
            }
        } catch (RuntimeException | Error e) {
            frame.getContentPane().removeAll();
            frame.dispose();
            cells = null;
            markers = null;
            fleetStatus = null;
            throw e;
        }
    }

    /** Adds the board coordinates, cell shapes and symbols to the scene. */
    private static void createBoard(PNode scene, BoardSnapshot snapshot) {
        scene.addChild(rectangle(28, 96, 564, 506, 18, Color.WHITE));
        PNode board = new PNode();
        scene.addChild(board);
        cells = new PPath[snapshot.getRows()][snapshot.getColumns()];
        markers = new PText[snapshot.getRows()][snapshot.getColumns()];

        for (int column = 0; column < snapshot.getColumns(); column++) {
            PText label = text(Integer.toString(column + 1), 0, 0, 14, Font.BOLD, MUTED);
            centerText(label, GRID_X + column * CELL_STEP, 100, CELL_SIZE, 26);
            board.addChild(label);
        }
        for (int row = 0; row < snapshot.getRows(); row++) {
            PText label = text(Character.toString((char) ('A' + row)),
                    0, 0, 14, Font.BOLD, MUTED);
            centerText(label, 48, GRID_Y + row * CELL_STEP, 32, CELL_SIZE);
            board.addChild(label);
            for (int column = 0; column < snapshot.getColumns(); column++) {
                PPath cell = rectangle(0, 0, CELL_SIZE, CELL_SIZE, 8, background(BoardCellState.EMPTY));
                cell.setOffset(GRID_X + column * CELL_STEP, GRID_Y + row * CELL_STEP);
                PText marker = text("", 0, 0, 22, Font.BOLD, INK);
                cell.addChild(marker);
                board.addChild(cell);
                cells[row][column] = cell;
                markers[row][column] = marker;
            }
        }
    }

    /** Adds a legend entry for each visual cell state. */
    private static void createLegend(PNode scene) {
        scene.addChild(text("Legenda", 28, 614, 16, Font.BOLD, INK));
        int index = 0;
        for (BoardCellState state : BoardCellState.values()) {
            double x = 28 + (index % 3) * 194;
            double y = 642 + (index / 3) * 36;
            PPath entry = rectangle(0, 0, 178, 30, 8, Color.WHITE);
            entry.setOffset(x, y);
            entry.addChild(rectangle(3, 3, 24, 24, 6, background(state)));
            PText marker = text(symbol(state), 0, 0, 16, Font.BOLD, foreground(state));
            centerText(marker, 3, 3, 24, 24);
            entry.addChild(marker);
            entry.addChild(text(description(state), 35, 7, 13, Font.PLAIN, INK));
            scene.addChild(entry);
            index++;
        }
    }

    /** Creates a filled rounded rectangle without an outline. */
    private static PPath rectangle(double x, double y, double width, double height,
                                   double radius, Color color) {
        PPath shape = PPath.createRoundRectangle(x, y, width, height, radius, radius);
        shape.setStroke(null);
        shape.setPaint(color);
        return shape;
    }

    /** Creates and positions a text node with the supplied font and color. */
    private static PText text(String value, double x, double y, int size, int style, Color color) {
        PText node = new PText(value);
        node.setFont(new Font(Font.SANS_SERIF, style, size));
        node.setTextPaint(color);
        node.setOffset(x, y);
        return node;
    }

    /** Centers a text node inside the specified rectangle. */
    private static void centerText(PText node, double x, double y, double width, double height) {
        node.setOffset(x + (width - node.getWidth()) / 2, y + (height - node.getHeight()) / 2);
    }

    /** Updates existing scene nodes from a snapshot on the Swing Event Dispatch Thread. */
    private static void render(BoardSnapshot snapshot) {
        fleetStatus.setText("Navios a flutuar: " + snapshot.getRemainingShips());
        for (int row = 0; row < snapshot.getRows(); row++) {
            for (int column = 0; column < snapshot.getColumns(); column++) {
                BoardCellState state = snapshot.getCellState(row, column);
                cells[row][column].setPaint(background(state));
                PText marker = markers[row][column];
                marker.setText(symbol(state));
                marker.setTextPaint(foreground(state));
                centerText(marker, 0, 0, CELL_SIZE, CELL_SIZE);
            }
        }
    }

    /** Returns the symbol used to distinguish a cell state without relying on color. */
    private static String symbol(BoardCellState state) {
        return switch (state) {
            case EMPTY -> "~";
            case SHIP -> "#";
            case MISS -> "o";
            case HIT -> "*";
            case SUNK -> "X";
        };
    }

    /** Returns the Portuguese label displayed for a cell state. */
    private static String description(BoardCellState state) {
        return switch (state) {
            case EMPTY -> "Água";
            case SHIP -> "Navio";
            case MISS -> "Tiro na água";
            case HIT -> "Navio atingido";
            case SUNK -> "Navio afundado";
        };
    }

    /** Returns the fill color for a cell state. */
    private static Color background(BoardCellState state) {
        return Color.decode(switch (state) {
            case EMPTY -> "#dceefa";
            case SHIP -> "#36566f";
            case MISS -> "#a8d5e5";
            case HIT -> "#f8ba55";
            case SUNK -> "#8d283a";
        });
    }

    /** Returns the symbol color for a cell state. */
    private static Color foreground(BoardCellState state) {
        return state == BoardCellState.SHIP || state == BoardCellState.SUNK ? Color.WHITE : INK;
    }
}
