package hu.nye.progtech.rushhour.ui;

import hu.nye.progtech.rushhour.exception.InvalidMoveException;
import hu.nye.progtech.rushhour.model.BoardState;
import hu.nye.progtech.rushhour.model.Direction;
import hu.nye.progtech.rushhour.model.Position;
import hu.nye.progtech.rushhour.model.Vehicle;
import hu.nye.progtech.rushhour.persistence.FileBoardRepository;
import hu.nye.progtech.rushhour.persistence.HighScoreRepository;
import hu.nye.progtech.rushhour.service.AiPlayerService;
import hu.nye.progtech.rushhour.service.GameService;
import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A parancssoros felhasználói interakciót és a játékmenetet vezérlő osztály.
 */
public class Game {

    private static final Logger LOGGER = LoggerFactory.getLogger(Game.class);
    private static final String DEFAULT_INPUT_FILE = "board.txt";

    private final GameService gameService;
    private final AiPlayerService aiPlayerService;
    private final FileBoardRepository fileBoardRepository;
    private final HighScoreRepository highScoreRepository;
    private final Scanner scanner;
    private BoardState boardState;
    private String playerName;

    public Game(GameService gameService,
                AiPlayerService aiPlayerService,
                FileBoardRepository fileBoardRepository,
                HighScoreRepository highScoreRepository,
                Scanner scanner) {
        this.gameService = gameService;
        this.aiPlayerService = aiPlayerService;
        this.fileBoardRepository = fileBoardRepository;
        this.highScoreRepository = highScoreRepository;
        this.scanner = scanner;
    }

    /**
     * Elindítja a játékot: betölti a kezdőpályát, bekéri a játékos nevét és futtatja a főciklust.
     */
    public void start() {
        LOGGER.info("Rush Hour NxM játék indítása...");
        initBoard();

        System.out.println("====================================================");
        System.out.println("       ÜDVÖZÖLLEK A RUSH HOUR NxM JÁTÉKBAN!         ");
        System.out.println("====================================================");
        System.out.print("Kérlek, add meg a neved: ");
        playerName = scanner.nextLine().trim();
        if (playerName.isEmpty()) {
            playerName = "Jatekos1";
        }

        printHelp();

        boolean running = true;
        while (running) {
            printBoard();

            if (checkWinConditions()) {
                break;
            }

            System.out.print("\n[" + playerName + "] Parancs > ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                continue;
            }

            String[] parts = input.split("\\s+");
            String command = parts[0].toLowerCase();

            switch (command) {
                case "move" -> handleMoveCommand(parts);
                case "save" -> handleSaveTxt(parts);
                case "load" -> handleLoadTxt(parts);
                case "savexml" -> handleSaveXml(parts);
                case "loadxml" -> handleLoadXml(parts);
                case "highscore" -> printHighScores();
                case "help" -> printHelp();
                case "exit" -> {
                    System.out.println("Kilépés a játékból. Viszlát!");
                    running = false;
                }
                default -> System.out.println("Ismeretlen parancs! Írd be a 'help' parancsot a segítséghez.");
            }
        }
    }

    private void initBoard() {
        File defaultFile = new File(DEFAULT_INPUT_FILE);
        if (defaultFile.exists() && defaultFile.isFile()) {
            try {
                boardState = fileBoardRepository.loadFromTxt(defaultFile.toPath());
                System.out.println("Pálya sikeresen betöltve a(z) " + DEFAULT_INPUT_FILE + " fájlból.");
                return;
            } catch (Exception e) {
                LOGGER.warn("Nem sikerült betölteni a(z) {} fájlt, alapértelmezett pálya indul.", DEFAULT_INPUT_FILE, e);
            }
        }
        boardState = gameService.createDefaultBoard();
    }

    private void handleMoveCommand(String[] parts) {
        if (parts.length < 4) {
            System.out.println("Használat: move <jármű_id> <up|down|left|right> <lépésszám> (pl.: move C down 2)");
            return;
        }
        try {
            char vehicleId = parts[1].charAt(0);
            Direction dir = Direction.valueOf(parts[2].toUpperCase());
            int steps = Integer.parseInt(parts[3]);

            boardState = gameService.applyMove(boardState, vehicleId, dir, steps);
            System.out.println("Sikeres lépés!");

            if (checkWinConditions()) {
                printBoard();
                System.exit(0);
            }

            System.out.println("A gépi ellenfél (AI) lép...");
            boardState = aiPlayerService.makeRandomMove(boardState);
        } catch (IllegalArgumentException e) {
            System.out.println("Hibás irány vagy lépésszám! Irányok: UP, DOWN, LEFT, RIGHT.");
        } catch (InvalidMoveException e) {
            System.out.println("Érvénytelen lépés: " + e.getMessage());
        }
    }

    private boolean checkWinConditions() {
        if (gameService.isHumanWinner(boardState)) {
            System.out.println("\n****************************************************");
            System.out.println(" GRATULÁLOK, " + playerName + "! Kijutottál az 'A' járművel, NYERTÉL!");
            System.out.println("****************************************************");
            highScoreRepository.incrementWinCount(playerName);
            printHighScores();
            return true;
        }
        if (gameService.isAiWinner(boardState)) {
            System.out.println("\n****************************************************");
            System.out.println(" A GÉPI JÁTÉKOS (AI) kijutott a 'B' járművel! Vesztettél.");
            System.out.println("****************************************************");
            highScoreRepository.incrementWinCount("AI (Gép)");
            printHighScores();
            return true;
        }
        return false;
    }

    private void handleSaveTxt(String[] parts) {
        String fileName = parts.length > 1 ? parts[1] : "saved_board.txt";
        try {
            fileBoardRepository.saveToTxt(boardState, Path.of(fileName));
            System.out.println("Pálya sikeresen kimentve ide: " + fileName);
        } catch (Exception e) {
            System.out.println("Hiba a mentés során: " + e.getMessage());
        }
    }

    private void handleLoadTxt(String[] parts) {
        String fileName = parts.length > 1 ? parts[1] : "saved_board.txt";
        try {
            boardState = fileBoardRepository.loadFromTxt(Path.of(fileName));
            System.out.println("Pálya sikeresen betöltve innen: " + fileName);
        } catch (Exception e) {
            System.out.println("Hiba a betöltés során: " + e.getMessage());
        }
    }

    private void handleSaveXml(String[] parts) {
        String fileName = parts.length > 1 ? parts[1] : "saved_game.xml";
        try {
            fileBoardRepository.saveToXml(boardState, Path.of(fileName));
            System.out.println("Játékállás sikeresen kimentve XML-be: " + fileName);
        } catch (Exception e) {
            System.out.println("Hiba az XML mentés során: " + e.getMessage());
        }
    }

    private void handleLoadXml(String[] parts) {
        String fileName = parts.length > 1 ? parts[1] : "saved_game.xml";
        try {
            boardState = fileBoardRepository.loadFromXml(Path.of(fileName));
            System.out.println("Játékállás sikeresen visszatöltve XML-ből: " + fileName);
        } catch (Exception e) {
            System.out.println("Hiba az XML betöltés során: " + e.getMessage());
        }
    }

    private void printHighScores() {
        System.out.println("\n================ HIGH SCORE TÁBLÁZAT ================");
        Map<String, Integer> scores = highScoreRepository.getHighScores();
        if (scores.isEmpty()) {
            System.out.println("Még nincs rögzített győzelem az adatbázisban.");
        } else {
            System.out.printf("%-25s | %s%n", "Játékos neve", "Győzelmek száma");
            System.out.println("-----------------------------------------------------");
            scores.forEach((name, wins) -> System.out.printf("%-25s | %d%n", name, wins));
        }
        System.out.println("=====================================================");
    }

    private void printBoard() {
        char[][] grid = new char[boardState.getRows()][boardState.getCols()];
        for (int r = 0; r < boardState.getRows(); r++) {
            for (int c = 0; c < boardState.getCols(); c++) {
                grid[r][c] = '.';
            }
        }

        for (Vehicle v : boardState.getVehicles()) {
            for (Position pos : v.getOccupiedPositions()) {
                grid[pos.getRow()][pos.getCol()] = v.getId();
            }
        }

        System.out.println("\nJelenlegi játéktér (Cél A: " + boardState.getExitA()
                + " [Jobb szél], Cél B: " + boardState.getExitB() + " [Bal szél]):");

        System.out.print("     ");
        for (int c = 0; c < boardState.getCols(); c++) {
            System.out.print((char) ('a' + c) + " ");
        }
        System.out.println();

        for (int r = 0; r < boardState.getRows(); r++) {
            String leftMarker = (boardState.getExitB().getRow() == r && boardState.getExitB().getCol() == 0)
                    ? "B<-" : "   ";
            String rightMarker = (boardState.getExitA().getRow() == r
                    && boardState.getExitA().getCol() == boardState.getCols() - 1)
                    ? "->A" : "";

            System.out.printf("%2d %s ", (r + 1), leftMarker);
            for (int c = 0; c < boardState.getCols(); c++) {
                System.out.print(grid[r][c] + " ");
            }
            System.out.println(rightMarker);
        }
    }

    private void printHelp() {
        System.out.println("\nElérhető parancsok:");
        System.out.println("  move <id> <up|down|left|right> <lépés> - Jármű csúsztatása (pl. move A right 2)");
        System.out.println("  save [fájlnév.txt]                     - Pálya mentése szöveges fájlba");
        System.out.println("  load [fájlnév.txt]                     - Pálya betöltése szöveges fájlból");
        System.out.println("  savexml [fájlnév.xml]                  - Játékállás mentése XML fájlba");
        System.out.println("  loadxml [fájlnév.xml]                  - Játékállás visszatöltése XML fájlból");
        System.out.println("  highscore                              - High Score táblázat megjelenítése");
        System.out.println("  help                                   - Parancsok listázása");
        System.out.println("  exit                                   - Kilépés a játékból");
    }
}