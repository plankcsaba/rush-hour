package hu.nye.progtech.rushhour.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hu.nye.progtech.rushhour.exception.InvalidMoveException;
import hu.nye.progtech.rushhour.model.BoardState;
import hu.nye.progtech.rushhour.model.Direction;
import hu.nye.progtech.rushhour.model.Orientation;
import hu.nye.progtech.rushhour.model.Position;
import hu.nye.progtech.rushhour.model.Vehicle;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GameServiceTest {

    private GameService gameService;

    @BeforeEach
    void setUp() {
        gameService = new GameService();
    }

    @Test
    void validMoveUpdatesVehiclePosition() throws InvalidMoveException {
        BoardState initial = gameService.createDefaultBoard();

        BoardState updated = gameService.applyMove(initial, 'C', Direction.DOWN, 1);

        Vehicle movedC = gameService.findVehicleById(updated, 'C').orElseThrow();
        assertEquals(new Position(2, 4), movedC.getHeadPosition());
    }

    @Test
    void moveBlockedByAnotherVehicleThrowsException() {
        BoardState state = BoardState.builder()
                .withDimensions(4, 6)
                .withExitA(new Position(1, 5))
                .withExitB(new Position(2, 0))
                .withVehicles(List.of(
                        new Vehicle('A', new Position(1, 1), 2, Orientation.HORIZONTAL),
                        new Vehicle('C', new Position(1, 3), 2, Orientation.VERTICAL)
                ))
                .build();

        assertThrows(InvalidMoveException.class,
                () -> gameService.applyMove(state, 'A', Direction.RIGHT, 2));
    }

    @Test
    void wrongOrientationThrowsException() {
        BoardState state = gameService.createDefaultBoard();

        assertThrows(InvalidMoveException.class,
                () -> gameService.applyMove(state, 'A', Direction.UP, 1));
    }

    @Test
    void nonPositiveStepsThrowsException() {
        BoardState state = gameService.createDefaultBoard();

        assertThrows(InvalidMoveException.class,
                () -> gameService.applyMove(state, 'A', Direction.RIGHT, 0));
    }

    @Test
    void unknownVehicleThrowsException() {
        BoardState state = gameService.createDefaultBoard();

        assertThrows(InvalidMoveException.class,
                () -> gameService.applyMove(state, 'Z', Direction.RIGHT, 1));
    }

    @Test
    void heroASlidesOutOnRightEdgeAndWins() throws InvalidMoveException {
        BoardState state = BoardState.builder()
                .withDimensions(4, 6)
                .withExitA(new Position(1, 5))
                .withExitB(new Position(2, 0))
                .withVehicles(List.of(
                        new Vehicle('A', new Position(1, 3), 2, Orientation.HORIZONTAL),
                        new Vehicle('B', new Position(2, 2), 2, Orientation.HORIZONTAL)
                ))
                .build();

        assertFalse(gameService.isHumanWinner(state));

        BoardState touchingExit = gameService.applyMove(state, 'A', Direction.RIGHT, 1);
        assertFalse(gameService.isHumanWinner(touchingExit));

        BoardState slidOffBoard = gameService.applyMove(state, 'A', Direction.RIGHT, 2);
        assertTrue(gameService.isHumanWinner(slidOffBoard));
    }

    @Test
    void heroBSidesOutOnLeftEdgeAndAiWins() throws InvalidMoveException {
        BoardState state = BoardState.builder()
                .withDimensions(4, 6)
                .withExitA(new Position(1, 5))
                .withExitB(new Position(2, 0))
                .withVehicles(List.of(
                        new Vehicle('A', new Position(1, 2), 2, Orientation.HORIZONTAL),
                        new Vehicle('B', new Position(2, 1), 2, Orientation.HORIZONTAL)
                ))
                .build();

        assertFalse(gameService.isAiWinner(state));

        BoardState slidOffBoard = gameService.applyMove(state, 'B', Direction.LEFT, 2);
        assertTrue(gameService.isAiWinner(slidOffBoard));
    }

    @Test
    void heroACannotLeaveThroughExitB() {
        BoardState state = BoardState.builder()
                .withDimensions(4, 6)
                .withExitA(new Position(1, 5))
                .withExitB(new Position(2, 0))
                .withVehicles(List.of(
                        new Vehicle('A', new Position(2, 1), 2, Orientation.HORIZONTAL),
                        new Vehicle('B', new Position(1, 2), 2, Orientation.HORIZONTAL)
                ))
                .build();

        assertThrows(InvalidMoveException.class,
                () -> gameService.applyMove(state, 'A', Direction.LEFT, 2));
        assertFalse(gameService.isHumanWinner(state));
    }

    @Test
    void nonHeroVehicleCannotSlideOffBoard() {
        BoardState state = BoardState.builder()
                .withDimensions(4, 6)
                .withExitA(new Position(1, 5))
                .withExitB(new Position(2, 0))
                .withVehicles(List.of(
                        new Vehicle('A', new Position(1, 1), 2, Orientation.HORIZONTAL),
                        new Vehicle('C', new Position(0, 4), 2, Orientation.HORIZONTAL)
                ))
                .build();

        assertThrows(InvalidMoveException.class,
                () -> gameService.applyMove(state, 'C', Direction.RIGHT, 1));
    }

    @Test
    void verticalExitsSupportedWhenConfigured() throws InvalidMoveException {
        BoardState downExitState = BoardState.builder()
                .withDimensions(4, 5)
                .withExitA(new Position(3, 2))
                .withExitB(new Position(0, 1))
                .withVehicles(List.of(
                        new Vehicle('A', new Position(2, 2), 2, Orientation.VERTICAL),
                        new Vehicle('B', new Position(0, 1), 2, Orientation.VERTICAL)
                ))
                .build();

        BoardState afterDown = gameService.applyMove(downExitState, 'A', Direction.DOWN, 1);
        assertTrue(gameService.isHumanWinner(afterDown));

        BoardState upExitState = BoardState.builder()
                .withDimensions(4, 5)
                .withExitA(new Position(1, 4))
                .withExitB(new Position(0, 1))
                .withVehicles(List.of(
                        new Vehicle('A', new Position(1, 2), 2, Orientation.HORIZONTAL),
                        new Vehicle('B', new Position(0, 1), 2, Orientation.VERTICAL)
                ))
                .build();

        BoardState afterUp = gameService.applyMove(upExitState, 'B', Direction.UP, 1);
        assertTrue(gameService.isAiWinner(afterUp));
    }
}