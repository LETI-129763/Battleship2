package battleship;

import battleship.ui.BoardSnapshot;
import battleship.ui.BoardWindow;

import java.util.Scanner;
import java.io.IOException;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * The type Tasks.
 */
public class Tasks {
	interface BoardDisplay {
		boolean isOpen();
		void update(BoardSnapshot snapshot);
		void show(BoardSnapshot snapshot);
		void shutdown();
	}

	/**
	 * The constant LOGGER.
	 */
	private static final Logger LOGGER = LogManager.getLogger();
	private static final BoardDisplay BOARD_DISPLAY = new BoardDisplay() {
		@Override
		public boolean isOpen() {
			return BoardWindow.isOpen();
		}

		@Override
		public void update(BoardSnapshot snapshot) {
			BoardWindow.update(snapshot);
		}

		@Override
		public void show(BoardSnapshot snapshot) {
			BoardWindow.show(snapshot);
		}

		@Override
		public void shutdown() {
			BoardWindow.shutdown();
		}
	};

	/**
	 * The constant GOODBYE_MESSAGE.
	 */
	private static final String GOODBYE_MESSAGE = "Bons ventos!";

	/**
	 * Strings to be used by the user
	 */
	private static final String AJUDA = "ajuda";
	private static final String GERAFROTA = "gerafrota";
	private static final String LEFROTA = "lefrota";
	private static final String DESISTIR = "desisto";
	private static final String RAJADA = "rajada";
	private static final String TIROS = "tiros";
	private static final String MAPA = "mapa";
	private static final String STATUS = "estado";
	private static final String SIMULA = "simula";
	private static final String JANELA = "janela";

	/**
	 * This task also tests the fighting element of a round of three shots
	 */
	public static void menu() {
		try {
			runMenu();
		} finally {
			BOARD_DISPLAY.shutdown();
		}
	}

	/** Reads console commands and applies them on the command thread. */
	private static void runMenu() {
		runMenu(new Scanner(System.in), new Leaderboard());
	}

	static void runMenu(Scanner in, Leaderboard leaderboard) {
		runMenu(in, leaderboard, Game::new);
	}

	static void runMenu(Scanner in, Leaderboard leaderboard, Function<IFleet, IGame> gameFactory) {
		runMenu(in, leaderboard, gameFactory, BOARD_DISPLAY);
	}

	static void runMenu(Scanner in, Leaderboard leaderboard, Function<IFleet, IGame> gameFactory,
						BoardDisplay boardDisplay) {
		IFleet myFleet = null;
		IGame game = null;
		GameClock clock = new GameClock(); // STOPWATCH
		String playerName = "";
		int totalShots = 0;

		menuHelp();

		System.out.print("> ");
		String command = in.next();
		while (!command.equals(DESISTIR)) {

			switch (command) {
				case GERAFROTA:
					totalShots = 0;
					myFleet = Fleet.createRandom();
					game = gameFactory.apply(myFleet);
					clock.startGame(); // STOPWATCH
					game.printMyBoard(false, true);
					updateBoardWindow(game, boardDisplay);
					break;
				case LEFROTA:
					totalShots = 0;
					myFleet = buildFleet(in);
					game = gameFactory.apply(myFleet);
					clock.startGame(); // STOPWATCH
					game.printMyBoard(false, true);
					updateBoardWindow(game, boardDisplay);
					break;
				case JANELA:
					if (game == null) {
						System.out.println("Crie primeiro uma frota com gerafrota ou lefrota.");
					} else {
						try {
							boardDisplay.show(BoardSnapshot.from(game));
						} catch (IllegalStateException e) {
							System.out.println("Não foi possível abrir a janela: " + e.getMessage());
							if (e.getCause() != null) {
								LOGGER.error("Falha ao abrir a janela do tabuleiro", e);
							}
						}
					}
					break;
				case STATUS:
					if (myFleet != null)
						myFleet.printStatus();
					break;
				case MAPA:
					if (myFleet != null)
						game.printMyBoard(false, true);
					break;
				case RAJADA:
					if (game != null) {
						long ms = clock.endMove(); // STOPWATCH
						game.readEnemyFire(in);
						totalShots += Game.NUMBER_SHOTS;

						System.out.println("Tempo da jogada: " + GameClock.format(ms)); // STOPWATCH
						myFleet.printStatus();
						game.printMyBoard(true, false);
						updateBoardWindow(game, boardDisplay);

						if (game.getRemainingShips() == 0) {
							clock.endGame(); // STOPWATCH
							System.out.println(clock.summary()); // STOPWATCH
							game.over();
							boardDisplay.shutdown();

							System.out.println("Total shots: " + totalShots);
							playerName = readPlayerName(in);

							try {
								leaderboard.saveResult(playerName, totalShots);
								System.out.println("Result saved successfully.");
							} catch (IOException e) {
								System.out.println("Could not save result: " + e.getMessage());
							}

							try {
								leaderboard.printTop5();
							} catch (IOException e) {
								System.out.println("Could not display leaderboard: " + e.getMessage());
							}

							return;
						}
						clock.startMove(); // STOPWATCH: começa a contar a rajada seguinte
					}
					break;
				case SIMULA:
					if (game != null) {
						if (simulateGame(game, myFleet, boardDisplay)) {
							return;
						}
					}
					break;
				case TIROS:
					if (game != null)
						game.printMyBoard(true, true);
					break;
                case AJUDA:
                    menuHelp();
                    break;
				default:
					System.out.println("Que comando é esse??? Repete ...");
			}
			System.out.print("> ");
			command = in.next();
		}
		System.out.println(GOODBYE_MESSAGE);
	}

	static boolean simulateGame(IGame game, IFleet myFleet) {
		return simulateGame(game, myFleet, BOARD_DISPLAY);
	}

	static boolean simulateGame(IGame game, IFleet myFleet, BoardDisplay boardDisplay) {
		while (game.getRemainingShips() > 0) {
			game.randomEnemyFire();
			myFleet.printStatus();
			game.printMyBoard(true, false);
			updateBoardWindow(game, boardDisplay);
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		if (game.getRemainingShips() == 0) {
			game.over();
			boardDisplay.shutdown();
			return true;
		}
		return false;
	}

	private static String readPlayerName(Scanner in) {

		String playerName;

		do {
			System.out.print("Enter player name: ");
			playerName = in.nextLine().trim();

			if (playerName.isEmpty()) {
				System.out.println("Player name cannot be empty.");
			}
		} while (playerName.isEmpty());

		return playerName;
	}

	/**
	 * Copies the game state on the command thread for an open graphical window.
	 *
	 * @param game current game to display
	 */
	private static void updateBoardWindow(IGame game, BoardDisplay boardDisplay) {
		if (boardDisplay.isOpen()) {
			boardDisplay.update(BoardSnapshot.from(game));
		}
	}

	/**
	 * This function provides help information about the menu commands.
	 */
	public static void menuHelp() {
		System.out.println("======================= AJUDA DO MENU =========================");
		System.out.println("Digite um dos comandos abaixo para interagir com o jogo:");
		System.out.println("- " + GERAFROTA + ": Gera uma frota aleatória de navios.");
		System.out.println("- " + LEFROTA + ": Permite criar e carregar uma frota personalizada.");
		System.out.println("- " + STATUS + ": Mostra o status atual da frota.)");
		System.out.println("- " + MAPA + ": Exibe o mapa da frota.");
		System.out.println("- " + JANELA + ": Abre a janela gráfica do tabuleiro.");
		System.out.println("- " + RAJADA + ": Realiza uma rajada de disparos.");
		System.out.println("- " + SIMULA + ": Simula um jogo completo.");
		System.out.println("- " + TIROS + ": Lista os tiros válidos realizados (* = tiro em navio, o = tiro na água)");
		System.out.println("- " + DESISTIR + ": Encerra o jogo.");
		System.out.println("===============================================================");
	}
	/**
	 * This operation allows the build up of a fleet, given user data
	 *
	 * @param in The scanner to read from
	 * @return The fleet that has been built
	 */
	public static Fleet buildFleet(Scanner in) {

		assert in != null;

		Fleet fleet = new Fleet();
		int i = 0; // i represents the total of successfully created ships
		while (i < Fleet.FLEET_SIZE) {
			IShip s = readShip(in);
			if (s != null) {
				boolean success = fleet.addShip(s);
				if (success)
					i++;
				else
					LOGGER.info("Falha na criacao de {} {} {}", s.getCategory(), s.getBearing(), s.getPosition());
			} else {
				LOGGER.info("Navio desconhecido!");
			}
		}
		LOGGER.info("{} navios adicionados com sucesso!", i);
		return fleet;
	}

	/**
	 * This operation reads data about a ship, build it and returns it
	 *
	 * @param in The scanner to read from
	 * @return The created ship based on the data that has been read
	 */
	public static Ship readShip(Scanner in) {

		assert in != null;

		String shipKind = in.next();
		Position pos = readPosition(in);
		char c = in.next().charAt(0);
		Compass bearing = Compass.charToCompass(c);
		return Ship.buildShip(shipKind, bearing, pos);
	}

	/**
	 * This operation allows reading a position in the map
	 *
	 * @param in The scanner to read from
	 * @return The position that has been read
	 */
	public static Position readPosition(Scanner in) {

		assert in != null;

		int row = in.nextInt();
		int column = in.nextInt();
		return new Position(row, column);
	}

	/**
	 * This operation allows reading a position in the map
	 *
	 * @param in The scanner to read from
	 * @return The classic position that has been read
	 */
	public static IPosition readClassicPosition(@NotNull Scanner in) {
		// Verifica se ainda há tokens disponíveis
		if (!in.hasNext()) {
			throw new IllegalArgumentException("Nenhuma posição válida encontrada!");
		}

		String part1 = in.next(); // Primeiro token
		String part2 = null;

		if (in.hasNextInt()) {
			part2 = in.next(); // Segundo token, se disponível
		}

		String input = (part2 != null) ? part1 + part2 : part1;

		// Normalizar o input para tratar letras maiúsculas e minúsculas
		input = input.toUpperCase();

		// Verificar os dois formatos possíveis: compactos e com espaço
		if (part2 == null && input.matches("[A-Z]\\d+")) {
			char column = input.charAt(0); // Extrair a coluna
			int row = Integer.parseInt(input.substring(1)); // Extrair a linha
			return new Position(column, row);
		} else if (part2 != null && part1.toUpperCase().matches("[A-Z]") && part2.matches("\\d+")) {
			char column = part1.toUpperCase().charAt(0); // Extrair a coluna
			int row = Integer.parseInt(part2); // Extrair a linha
			return new Position(column, row);
		} else {
			throw new IllegalArgumentException("Formato inválido. Use 'A3', 'A 3' ou similar.");
		}
	}

}
