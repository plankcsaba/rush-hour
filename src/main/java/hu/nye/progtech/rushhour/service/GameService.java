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
     * Létrehozza a beégetett (default) 6x12-es kezdőpályát, ha nem áll rendelkezésre bemeneti fájl.
     */
    public BoardState createDefaultBoard() {
        LOGGER.info("Alapértelmezett (default) 6x12-es pálya létrehozása.");
        List<Vehicle> defaultVehicles = List.of(
                // Humán játékos céljárműve (A) - 3-as sor (index 2), bal oldalon indul, célja a jobb szél (2, 11)
                new Vehicle(HUMAN_HERO_ID, new Position(2, 1), 2, Orientation.HORIZONTAL),
                // Gépi játékos céljárműve (B) - 4-es sor (index 3), jobb oldalon indul, célja a bal szél (3, 0)
                new Vehicle(AI_HERO_ID, new Position(3, 9), 2, Orientation.HORIZONTAL),
                // Semleges akadályok (C, D, E, F, G, H)
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
     * Megkeres egy járművet az azonosítója alapján Stream API segítségével.
     */
    public Optional<Vehicle> findVehicleById(BoardState state, char vehicleId) {
        char upperId = Character.toUpperCase(vehicleId);
        return state.getVehicles().stream()
                .filter(v -> Character.toUpperCase(v.getId()) == upperId)
                .findFirst();
    }

    /**
     * Végrehajt egy csúsztatási lépést a megadott járművel, irányban és lépésszámmal.
     */
    public BoardState applyMove(BoardState state, char vehicleId, Direction direction, int steps)
            throws InvalidMoveException {
        if (steps <= 0) {
            throw new InvalidMoveException("A lépésszámnak pozitív egész számnak kell lennie!");
        }

        Vehicle vehicle = findVehicleById(state, vehicleId)
                .orElseThrow(() -> new InvalidMoveException("Nem található jármű ezzel az azonosítóval: " + vehicleId));

        validateOrientation(vehicle, direction);

        // Az összes többi jármű által elfoglalt mező kigyűjtése Stream API-val
        Set<Position> occupiedByOthers = state.getVehicles().stream()
                .filter(v -> v.getId() != vehicle.getId())
                .flatMap(v -> v.getOccupiedPositions().stream())
                .collect(Collectors.toSet());

        // Útvonal ellenőrzése lépésről lépésre (nem ugorhat át más járművet és nem léphet le a tábláról)
        for (int step = 1; step <= steps; step++) {
            Vehicle intermediate = vehicle.withMovedPosition(direction, step);
            for (Position pos : intermediate.getOccupiedPositions()) {
                if (!isWithinBounds(state, pos)) {
                    throw new InvalidMoveException("A jármű nem léphet le a játéktábláról!");
                }
                if (occupiedByOthers.contains(pos)) {
                    throw new InvalidMoveException("Az út nem szabad! Egy másik jármű útban van a(z) "
                            + pos + " mezőn.");
                }
            }
        }

        Vehicle movedVehicle = vehicle.withMovedPosition(direction, steps);
        List<Vehicle> updatedVehicles = state.getVehicles().stream()
                .map(v -> v.getId() == vehicle.getId() ? movedVehicle : v)
                .toList();

        LOGGER.info("Lépés végrehajtva: jármű={}, irány={}, lépésszám={}", vehicle.getId(), direction, steps);

        return BoardState.builder()
                .withDimensions(state.getRows(), state.getCols())
                .withExitA(state.getExitA())
                .withExitB(state.getExitB())
                .withVehicles(updatedVehicles)
                .build();
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

    private boolean isWithinBounds(BoardState state, Position pos) {
        return pos.getRow() >= 0 && pos.getRow() < state.getRows()
                && pos.getCol() >= 0 && pos.getCol() < state.getCols();
    }

    /**
     * Kigyűjti egy adott jármű összes érvényes elmozdulási állapotát az aktuális táblán.
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

        int maxDimension = Math.max(state.getRows(), state.getCols());
        for (Direction dir : allowedDirections) {
            for (int step = 1; step < maxDimension; step++) {
                try {
                    validStates.add(applyMove(state, vehicleId, dir, step));
                } catch (InvalidMoveException e) {
                    break; // Ha 1 lépés nem szabad adott irányban, távolabbi sem lehet az
                }
            }
        }
        return validStates;
    }

    /**
     * Ellenőrzi, hogy a humán játékos ('A' céljármű) elérte-e a kijáratát.
     */
    public boolean isHumanWinner(BoardState state) {
        return findVehicleById(state, HUMAN_HERO_ID)
                .map(v -> v.getOccupiedPositions().contains(state.getExitA()))
                .orElse(false);
    }

    /**
     * Ellenőrzi, hogy a gépi játékos ('B' céljármű) elérte-e a kijáratát.
     */
    public boolean isAiWinner(BoardState state) {
        return findVehicleById(state, AI_HERO_ID)
                .map(v -> v.getOccupiedPositions().contains(state.getExitB()))
                .orElse(false);
    }
}