package hu.nye.progtech.rushhour.service;

import hu.nye.progtech.rushhour.model.BoardState;
import hu.nye.progtech.rushhour.model.Vehicle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A gépi ellenfél (AI) lépungenerálásáért felelős szolgáltatás.
 */
public class AiPlayerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiPlayerService.class);
    private final GameService gameService;
    private final Random random;

    public AiPlayerService(GameService gameService, Random random) {
        this.gameService = gameService;
        this.random = random;
    }

    /**
     * Véletlenszerűen kiválaszt egy járművet, amelynek van érvényes lépése,
     * majd véletlenszerűen végrehajtja az egyik lehetséges lépését.
     */
    public BoardState makeRandomMove(BoardState currentState) {
        List<Vehicle> shuffledVehicles = new ArrayList<>(currentState.getVehicles());
        Collections.shuffle(shuffledVehicles, random);

        for (Vehicle candidate : shuffledVehicles) {
            List<BoardState> possibleMoves = gameService.getValidMovesForVehicle(currentState, candidate.getId());
            if (!possibleMoves.isEmpty()) {
                BoardState chosenState = possibleMoves.get(random.nextInt(possibleMoves.size()));
                LOGGER.info("AI játékos a(z) '{}' járművel lépett.", candidate.getId());
                return chosenState;
            }
        }

        LOGGER.warn("AI nem talált egyetlen érvényes lépést sem!");
        return currentState;
    }
}