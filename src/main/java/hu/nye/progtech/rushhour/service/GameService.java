package hu.nye.progtech.rushhour.service;

import hu.nye.progtech.rushhour.exception.InvalidMoveException;
import hu.nye.progtech.rushhour.model.BoardState;
import hu.nye.progtech.rushhour.model.Direction;
import hu.nye.progtech.rushhour.model.Orientation;
import hu.nye.progtech.rushhour.model.Position;
import hu.nye.progtech.rushhour.model.Vehicle;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Rush Hour NxM játék üzleti logikáját, lépésvalidációját és győzelmi feltételeit kezelő osztály.
 */
public class GameService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GameService.class);

    public static final char HUMAN_HERO_ID = 'A';
    public static final char AI_HERO_ID = 'B';

    /**
     * Létrehozza az alapértelmezett 6x12-es kezdő táblaállapotot.
     *
     * @return az alapértelmezett {@link BoardState}
     */
    public BoardState createDefaultBoard() {
        LOGGER.info("Alapértelmezett (default) 6x12-es pálya létrehozása.");
        List<Vehicle> defaultVehicles = List.of(
                new Vehicle(HUMAN_HERO_ID, new Position(2, 1), 2, Orientation.HORIZONTAL),
                new Vehicle(AI_HERO_ID, new Position(3, 9), 2, Orientation.HORIZONTAL),
                new Vehicle('C', new Position(1, 4), 3, Orientation.VERTICAL),
                new Vehicle('D', new Position(2, 7), 3, Orientation.VERTICAL),
                new Vehicle('E', new Position(0, 2), 2, Orientation.HORIZONTAL),
                new Vehicle('F', new Position(4, 3), 2, Orientation.VERTICAL),
                new Vehicle('G', new Position(5, 5), 3, Orientation.HORIZONTAL),
                new Vehicle('H', new Position(0, 8), 2, Orientation.VERTICAL)
        );

        return BoardState.builder()
                .withDimensions(6, 12)
                .withExitA(new Position(2, 11))
                .withExitB(new Position(3, 0))
                .withVehicles(defaultVehicles)
                .build();
    }

    /**
     * Megkeres egy járművet a táblán az azonosítója alapján.
     *
     * @param state     az aktuális táblaállapot
     * @param vehicleId a keresett jármű azonosítója
     * @return a megtalált jármű {@link Optional}-ban
     */
    public Optional<Vehicle> findVehicleById(BoardState state, char vehicleId) {
        char upperId = Character.toUpperCase(vehicleId);
        return state.getVehicles().stream()
                .filter(v -> Character.toUpperCase(v.getId()) == upperId)
                .findFirst();
    }

    /**
     * Ellenőrzi, hogy egy adott pozíció a tábla belső határain belül van-e.
     *
     * @param state az aktuális táblaállapot
     * @param pos   a vizsgált pozíció
     * @return {@code true}, ha a pozíció a táblán belül van
     */
    public boolean isWithinBounds(BoardState state, Position pos) {
        return pos.getRow() >= 0 && pos.getRow() < state.getRows()
                && pos.getCol() >= 0 && pos.getCol() < state.getCols();
    }

    /**
     * Végrehajt és validál egy lépést a táblán, majd visszaadja az új {@link BoardState}-et.
     *
     * @param state     az aktuális táblaállapot
     * @param vehicleId a mozgatandó jármű azonosítója
     * @param direction a lépés iránya
     * @param steps     a lépések száma
     * @return az új táblaállapot
     * @throws InvalidMoveException ha a lépés szabálytalan
     */
    public BoardState applyMove(BoardState state, char vehicleId, Direction direction, int steps)
            throws InvalidMoveException {
        BoardState nextState = simulateMove(state, vehicleId, direction, steps);
        LOGGER.info("Lépés végrehajtva: jármű={}, irány={}, lépésszám={}",
                Character.toUpperCase(vehicleId), direction, steps);
        return nextState;
    }

    private BoardState simulateMove(BoardState state, char vehicleId, Direction direction, int steps)
            throws InvalidMoveException {
        if (steps <= 0) {
            throw new InvalidMoveException("A lépésszámnak pozitív egész számnak kell lennie!");
        }

        Vehicle vehicle = findVehicleById(state, vehicleId)
                .orElseThrow(() -> new InvalidMoveException("Nem található jármű ezzel az azonosítóval: " + vehicleId));

        validateOrientation(vehicle, direction);

        Set<Position> occupiedByOthers = state.getVehicles().stream()
                .filter(v -> v.getId() != vehicle.getId())
                .flatMap(v -> v.getOccupiedPositions().stream())
                .collect(Collectors.toSet());

        for (int step = 1; step <= steps; step++) {
            Vehicle intermediate = vehicle.withMovedPosition(direction, step);
            for (Position pos : intermediate.getOccupiedPositions()) {
                if (occupiedByOthers.contains(pos)) {
                    throw new InvalidMoveException("Az út nem szabad! Egy másik jármű útban van a(z) "
                            + pos + " mezőn.");
                }
                if (!isWithinBounds(state, pos) && !canExitThroughOwnExit(state, vehicle, direction, pos)) {
                    throw new InvalidMoveException("A jármű nem léphet le a játéktábláról ezen a ponton: " + pos);
                }
            }
        }

        Vehicle movedVehicle = vehicle.withMovedPosition(direction, steps);

        List<Vehicle> updatedVehicles = state.getVehicles().stream()
                .map(v -> v.getId() == vehicle.getId() ? movedVehicle : v)
                .toList();

        return BoardState.builder()
                .withDimensions(state.getRows(), state.getCols())
                .withExitA(state.getExitA())
                .withExitB(state.getExitB())
                .withVehicles(updatedVehicles)
                .build();
    }

    private boolean canExitThroughOwnExit(BoardState state, Vehicle vehicle, Direction direction, Position offBoardPos) {
        char upperId = Character.toUpperCase(vehicle.getId());
        Position ownExit;
        if (upperId == HUMAN_HERO_ID) {
            ownExit = state.getExitA();
        } else if (upperId == AI_HERO_ID) {
            ownExit = state.getExitB();
        } else {
            return false;
        }

        if (ownExit == null || !isWithinBounds(state, ownExit)) {
            return false;
        }

        return switch (direction) {
            case RIGHT -> ownExit.getCol() == state.getCols() - 1
                    && offBoardPos.getRow() == ownExit.getRow()
                    && offBoardPos.getCol() >= state.getCols();
            case LEFT -> ownExit.getCol() == 0
                    && offBoardPos.getRow() == ownExit.getRow()
                    && offBoardPos.getCol() < 0;
            case DOWN -> ownExit.getRow() == state.getRows() - 1
                    && offBoardPos.getCol() == ownExit.getCol()
                    && offBoardPos.getRow() >= state.getRows();
            case UP -> ownExit.getRow() == 0
                    && offBoardPos.getCol() == ownExit.getCol()
                    && offBoardPos.getRow() < 0;
        };
    }

    private void validateOrientation(Vehicle vehicle, Direction direction) throws InvalidMoveException {
        if (vehicle.getOrientation() == Orientation.HORIZONTAL
                && (direction == Direction.UP || direction == Direction.DOWN)) {
            throw new InvalidMoveException("Horizontális jármű csak balra (LEFT) vagy jobbra (RIGHT) mozoghat!");
        }
        if (vehicle.getOrientation() == Orientation.VERTICAL
                && (direction == Direction.LEFT || direction == Direction.RIGHT)) {
            throw new InvalidMoveException("Vertikális jármű csak felfelé (UP) vagy lefelé (DOWN) mozoghat!");
        }
    }

    /**
     * Összegyűjti az adott járműhöz tartozó összes érvényes új táblaállapotot.
     *
     * @param state     az aktuális táblaállapot
     * @param vehicleId a jármű azonosítója
     * @return az érvényes lépések utáni táblaállapotok listája
     */
    public List<BoardState> getValidMovesForVehicle(BoardState state, char vehicleId) {
        List<BoardState> validStates = new ArrayList<>();
        Optional<Vehicle> optVehicle = findVehicleById(state, vehicleId);
        if (optVehicle.isEmpty()) {
            return validStates;
        }

        Vehicle vehicle = optVehicle.get();
        List<Direction> allowedDirections = vehicle.getOrientation() == Orientation.HORIZONTAL
                ? List.of(Direction.LEFT, Direction.RIGHT)
                : List.of(Direction.UP, Direction.DOWN);

        int maxSteps = state.getRows() + state.getCols();

        for (Direction dir : allowedDirections) {
            for (int step = 1; step <= maxSteps; step++) {
                try {
                    validStates.add(simulateMove(state, vehicleId, dir, step));
                } catch (InvalidMoveException e) {
                    break;
                }
            }
        }

        return validStates;
    }

    /**
     * Ellenőrzi, hogy az 'A' céljármű kicsúszott-e a tábláról a saját 'exitA' kijáratán keresztül.
     * A győzelem mindig a céljárműhöz és a saját kijáratához kötődik.
     *
     * @param state az aktuális táblaállapot
     * @return {@code true}, ha a humán játékos céljárműve kijutott a saját kijáratán
     */
    public boolean isHumanWinner(BoardState state) {
        return hasExitedThroughOwnExit(state, HUMAN_HERO_ID, state.getExitA());
    }

    /**
     * Ellenőrzi, hogy a 'B' céljármű kicsúszott-e a tábláról a saját 'exitB' kijáratán keresztül.
     * A győzelem mindig a céljárműhöz és a saját kijáratához kötődik.
     *
     * @param state az aktuális táblaállapot
     * @return {@code true}, ha a gépi ellenfél céljárműve kijutott a saját kijáratán
     */
    public boolean isAiWinner(BoardState state) {
        return hasExitedThroughOwnExit(state, AI_HERO_ID, state.getExitB());
    }

    private boolean hasExitedThroughOwnExit(BoardState state, char heroId, Position ownExit) {
        if (ownExit == null || !isWithinBounds(state, ownExit)) {
            return false;
        }
        return findVehicleById(state, heroId)
                .map(v -> v.getOccupiedPositions().stream().anyMatch(pos -> !isWithinBounds(state, pos))
                        && isAlignedWithExit(state, v, ownExit))
                .orElse(false);
    }

    private boolean isAlignedWithExit(BoardState state, Vehicle vehicle, Position exit) {
        if (exit.getCol() == state.getCols() - 1 && vehicle.getOrientation() == Orientation.HORIZONTAL) {
            return vehicle.getHeadPosition().getRow() == exit.getRow()
                    && vehicle.getOccupiedPositions().stream().anyMatch(p -> p.getCol() >= state.getCols());
        }
        if (exit.getCol() == 0 && vehicle.getOrientation() == Orientation.HORIZONTAL) {
            return vehicle.getHeadPosition().getRow() == exit.getRow()
                    && vehicle.getOccupiedPositions().stream().anyMatch(p -> p.getCol() < 0);
        }
        if (exit.getRow() == state.getRows() - 1 && vehicle.getOrientation() == Orientation.VERTICAL) {
            return vehicle.getHeadPosition().getCol() == exit.getCol()
                    && vehicle.getOccupiedPositions().stream().anyMatch(p -> p.getRow() >= state.getRows());
        }
        if (exit.getRow() == 0 && vehicle.getOrientation() == Orientation.VERTICAL) {
            return vehicle.getHeadPosition().getCol() == exit.getCol()
                    && vehicle.getOccupiedPositions().stream().anyMatch(p -> p.getRow() < 0);
        }
        return false;
    }
}