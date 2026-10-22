package hu.nye.progtech.rushhour;

import hu.nye.progtech.rushhour.persistence.FileBoardRepository;
import hu.nye.progtech.rushhour.persistence.HighScoreRepository;
import hu.nye.progtech.rushhour.persistence.JdbcHighScoreRepository;
import hu.nye.progtech.rushhour.service.AiPlayerService;
import hu.nye.progtech.rushhour.service.GameService;
import hu.nye.progtech.rushhour.ui.Game;
import java.util.Random;
import java.util.Scanner;

/**
 * Az alkalmazás belépési pontja.
 */
public class Main {

    private static final String H2_JDBC_URL = "jdbc:h2:./data/rushhour_db;AUTO_SERVER=TRUE";

    public static void main(String[] args) {
        GameService gameService = new GameService();
        AiPlayerService aiPlayerService = new AiPlayerService(gameService, new Random());
        FileBoardRepository fileBoardRepository = new FileBoardRepository();
        HighScoreRepository highScoreRepository = new JdbcHighScoreRepository(H2_JDBC_URL, "sa", "");
        Scanner scanner = new Scanner(System.in);

        Game game = new Game(gameService, aiPlayerService, fileBoardRepository, highScoreRepository, scanner);
        game.start();
    }
}