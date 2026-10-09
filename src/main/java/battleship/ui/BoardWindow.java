package battleship.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * JavaFX board view that receives immutable snapshots from the command thread.
 * All scene changes run on the JavaFX Application Thread. Closing the window
 * hides it; the console owns the lifetime of the JavaFX runtime.
 */
public final class BoardWindow {
    private static CompletableFuture<Void> toolkitReady;
    private static boolean stopped;
    private static volatile boolean open;
    private static BoardSnapshot pendingSnapshot;
    private static boolean updateQueued;

    /** Window and controls accessed only on the JavaFX Application Thread. */
    private static Stage stage;
    private static Label[][] cells;
    private static Label fleetStatus;

    private BoardWindow() {
    }

    /**
     * Opens or shows the window with the supplied snapshot.
     *
     * @param snapshot board state copied on the command thread
     * @throws NullPointerException if the snapshot is null
     * @throws IllegalStateException if JavaFX cannot start, the launch configuration
     *                               is unsupported or the runtime was shut down
     */
    public static void show(BoardSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "O estado do tabuleiro não pode ser nulo.");
        startToolkit();
        runAndWait(() -> {
            synchronized (BoardWindow.class) {
                if (stopped) {
                    throw new IllegalStateException("A janela do tabuleiro já foi encerrada.");
                }
            }
            if (stage == null) {
                createWindow(snapshot);
            }
            render(snapshot);
            stage.show();
            stage.setIconified(false);
            stage.toFront();
            open = true;
        });
    }

    /** @return true if the board window is open */
    public static boolean isOpen() {
        return open;
    }

    /**
     * Updates an open window, retaining only the latest pending snapshot during
     * rapid updates so the JavaFX event queue does not grow without bounds.
     *
     * @param snapshot board state copied on the command thread
     * @throws NullPointerException if the snapshot is null
     */
    public static synchronized void update(BoardSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "O estado do tabuleiro não pode ser nulo.");
        if (!open || stopped) {
            return;
        }
        pendingSnapshot = snapshot;
        if (!updateQueued) {
            updateQueued = true;
            Platform.runLater(BoardWindow::applyPendingUpdate);
        }
    }

    /**
     * Permanently closes the window and exits JavaFX when the console ends.
     * Does not initialize JavaFX if the window has never been opened.
     */
    public static void shutdown() {
        CompletableFuture<Void> ready;
        synchronized (BoardWindow.class) {
            if (stopped) {
                return;
            }
            stopped = true;
            open = false;
            pendingSnapshot = null;
            ready = toolkitReady;
        }
        if (ready != null && !ready.isCompletedExceptionally()) {
            ready.thenRun(() -> Platform.runLater(() -> {
                if (stage != null) {
                    stage.close();
                    stage = null;
                    cells = null;
                    fleetStatus = null;
                }
                Platform.exit();
            }));
        }
    }

    /** Starts the toolkit once and keeps it alive when the last window is hidden. */
    private static synchronized void startToolkit() {
        if (stopped) {
            throw new IllegalStateException("A janela do tabuleiro já foi encerrada.");
        }
        if (toolkitReady == null) {
            if (!Platform.class.getModule().isNamed()) {
                throw new IllegalStateException("Inicie o jogo através de battleship.Main "
                        + "para carregar os módulos JavaFX.");
            }
            CompletableFuture<Void> ready = new CompletableFuture<>();
            toolkitReady = ready;
            try {
                Platform.startup(() -> {
                    Platform.setImplicitExit(false);
                    ready.complete(null);
                });
            } catch (RuntimeException | LinkageError failure) {
                ready.completeExceptionally(failure);
            }
        }
        await(toolkitReady);
    }

    /** Executes a scene operation on the JavaFX thread and propagates failures. */
    private static void runAndWait(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }
        CompletableFuture<Void> completion = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                action.run();
                completion.complete(null);
            } catch (RuntimeException | Error failure) {
                completion.completeExceptionally(failure);
            }
        });
        await(completion);
    }

    /** Waits for a JavaFX operation while preserving interruption and its cause. */
    private static void await(CompletableFuture<Void> completion) {
        try {
            completion.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operação da janela interrompida.", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Error error && !(cause instanceof LinkageError)) {
                throw error;
            }
            throw new IllegalStateException("Não foi possível iniciar ou atualizar a janela: "
                    + cause.getMessage(), cause);
        }
    }

    /** Applies the latest snapshot without reading the mutable game model. */
    private static void applyPendingUpdate() {
        BoardSnapshot snapshot;
        synchronized (BoardWindow.class) {
            updateQueued = false;
            snapshot = pendingSnapshot;
            pendingSnapshot = null;
            if (stopped || !open) {
                return;
            }
        }
        if (snapshot != null) {
            render(snapshot);
        }
    }

    /** Builds the scene programmatically on the JavaFX Application Thread. */
    private static void createWindow(BoardSnapshot snapshot) {
        Label title = new Label("O meu tabuleiro");
        title.getStyleClass().add("board-title");
        fleetStatus = new Label();
        fleetStatus.getStyleClass().add("fleet-status");
        VBox header = new VBox(6, title, fleetStatus);

        GridPane board = createBoard(snapshot);
        VBox.setVgrow(board, Priority.ALWAYS);
        Label legendTitle = new Label("Legenda");
        legendTitle.getStyleClass().add("legend-title");
        FlowPane legend = new FlowPane(16, 10);
        for (BoardCellState state : BoardCellState.values()) {
            Label marker = new Label(symbol(state));
            marker.getStyleClass().addAll("board-cell", "legend-marker", styleClass(state));
            marker.setMinSize(30, 30);
            marker.setPrefSize(30, 30);
            marker.setAlignment(Pos.CENTER);
            HBox entry = new HBox(7, marker, new Label(description(state)));
            entry.setAlignment(Pos.CENTER_LEFT);
            legend.getChildren().add(entry);
        }
        Label hint = new Label("Jogue na consola. Use janela para voltar a abrir este tabuleiro.");
        hint.getStyleClass().add("board-hint");
        hint.setWrapText(true);
        VBox footer = new VBox(10, legendTitle, legend, hint);

        VBox root = new VBox(20, header, board, footer);
        root.setPadding(new Insets(24));
        root.getStyleClass().add("board-root");
        Scene scene = new Scene(root, 620, 740);
        scene.getStylesheets().add(Objects.requireNonNull(
                BoardWindow.class.getResource("board.css"), "Falta o estilo do tabuleiro.").toExternalForm());

        Stage window = new Stage();
        window.setTitle("Battleship — O meu tabuleiro");
        window.setScene(scene);
        window.setMinWidth(520);
        window.setMinHeight(660);
        window.setOnHidden(event -> open = false);
        stage = window;
    }

    /** Builds a grid containing coordinate labels and one label per board cell. */
    private static GridPane createBoard(BoardSnapshot snapshot) {
        GridPane board = new GridPane();
        board.getStyleClass().add("board-grid");
        board.setHgap(5);
        board.setVgap(5);
        board.setPadding(new Insets(14));
        board.setAlignment(Pos.CENTER);
        board.getColumnConstraints().add(new ColumnConstraints(24));
        board.getRowConstraints().add(new RowConstraints(24));
        for (int column = 0; column < snapshot.getColumns(); column++) {
            ColumnConstraints width = new ColumnConstraints(24, 42, Double.MAX_VALUE);
            width.setHgrow(Priority.ALWAYS);
            board.getColumnConstraints().add(width);
            board.add(coordinate(Integer.toString(column + 1)), column + 1, 0);
        }
        cells = new Label[snapshot.getRows()][snapshot.getColumns()];
        for (int row = 0; row < snapshot.getRows(); row++) {
            RowConstraints height = new RowConstraints(24, 42, Double.MAX_VALUE);
            height.setVgrow(Priority.ALWAYS);
            board.getRowConstraints().add(height);
            board.add(coordinate(Character.toString((char) ('A' + row))), 0, row + 1);
            for (int column = 0; column < snapshot.getColumns(); column++) {
                Label cell = new Label();
                cell.setAlignment(Pos.CENTER);
                cell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                cell.setTooltip(new Tooltip());
                cells[row][column] = cell;
                board.add(cell, column + 1, row + 1);
            }
        }
        return board;
    }

    /** Creates a coordinate label that stays centered as the window is resized. */
    private static Label coordinate(String value) {
        Label label = new Label(value);
        label.getStyleClass().add("coordinate");
        label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER);
        return label;
    }

    /** Refreshes the existing controls from a snapshot on the JavaFX thread. */
    private static void render(BoardSnapshot snapshot) {
        fleetStatus.setText("Navios a flutuar: " + snapshot.getRemainingShips());
        for (int row = 0; row < snapshot.getRows(); row++) {
            for (int column = 0; column < snapshot.getColumns(); column++) {
                BoardCellState state = snapshot.getCellState(row, column);
                Label cell = cells[row][column];
                cell.setText(symbol(state));
                cell.getStyleClass().setAll("board-cell", styleClass(state));
                String detail = (char) ('A' + row) + Integer.toString(column + 1)
                        + ": " + description(state);
                cell.getTooltip().setText(detail);
                cell.setAccessibleText(detail);
            }
        }
    }

    /** Returns the CSS class used for a cell state. */
    private static String styleClass(BoardCellState state) {
        return state.name().toLowerCase(Locale.ROOT);
    }

    /** Returns a symbol that distinguishes a state without relying on color. */
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
}
