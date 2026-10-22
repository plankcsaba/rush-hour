package hu.nye.progtech.rushhour.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    private GameService underTest;

    @BeforeEach
    void setUp() {
        underTest = new GameService();
    }

    @Test
    void testCreateDefaultBoardShouldReturnValid6x12Board() {
        BoardState board = underTest.createDefaultBoard();

        assertNotNull(board);
        assertEquals(6, board.getRows());
        assertEquals(12, board.getCols());
        assertFalse(board.getVehicles().isEmpty());
        assertTrue(underTest.findVehicleById(board, 'A').isPresent());
        assertTrue(underTest.findVehicleById(board, 'B').isPresent());
    }

    @Test
    void testApplyMoveShouldSlideHorizontalVehicleWhenPathIsClear() throws InvalidMoveException {
        BoardState board = underTest.createDefaultBoard();

        // 'A' jármű (2,1)-en áll, hossz=2 (tehát (2,1) és (2,2)). A (2,3) üres, így 1-et léphet jobbra.
        BoardState updated = underTest.applyMove(board, 'A', Direction.RIGHT, 1);

        Vehicle movedA = underTest.findVehicleById(updated, 'A').orElseThrow();
        assertEquals(new Position(2, 2), movedA.getHeadPosition());
    }

    @Test
    void testApplyMoveShouldThrowExceptionWhenStepsNotPositive() {
        BoardState board = underTest.createDefaultBoard();

        assertThrows(InvalidMoveException.class,
                () -> underTest.applyMove(board, 'A', Direction.RIGHT, 0));
    }

    @Test
    void testApplyMoveShouldThrowExceptionWhenVehicleNotFound() {
        BoardState board = underTest.createDefaultBoard();

        assertThrows(InvalidMoveException.class,
                () -> underTest.applyMove(board, 'Z', Direction.RIGHT, 1));
    }

    @Test
    void testApplyMoveShouldThrowExceptionWhenHorizontalMovesVertically() {
        BoardState board = underTest.createDefaultBoard();

        assertThrows(InvalidMoveException.class,
                () -> underTest.applyMove(board, 'A', Direction.UP, 1));
    }

    @Test
    void testApplyMoveShouldThrowExceptionWhenVerticalMovesHorizontally() {
        BoardState board = underTest.createDefaultBoard();

        assertThrows(InvalidMoveException.class,
                () -> underTest.applyMove(board, 'C', Direction.LEFT, 1));
    }

    @Test
    void testApplyMoveShouldThrowExceptionWhenCollisionOccurs() {
        BoardState board = underTest.createDefaultBoard();

        // 'A' előtt a (2,4) mezőn a 'C' vertikális jármű áll, így 3 lépés jobbra ütközést okoz
        assertThrows(InvalidMoveException.class,
                () -> underTest.applyMove(board, 'A', Direction.RIGHT, 3));
    }

    @Test
    void testApplyMoveShouldThrowExceptionWhenLeavingBoard() {
        BoardState board = underTest.createDefaultBoard();

        // 'A' a (2,1)-en áll, 2 lépés balra lelépne a (2,-1) mezőre
        assertThrows(InvalidMoveException.class,
                () -> underTest.applyMove(board, 'A', Direction.LEFT, 2));
    }

    @Test
    void testWinConditionsShouldDetectHumanAndAiWinners() {
        BoardState winningHumanBoard = BoardState.builder()
                .withDimensions(6, 12)
                .withExitA(new Position(2, 11))
                .withExitB(new Position(3, 0))
                .withVehicles(List.of(
                        new Vehicle('A', new Position(2, 10), 2, Orientation.HORIZONTAL),
                        new Vehicle('B', new Position(3, 5), 2, Orientation.HORIZONTAL)
                ))
                .build();

        assertTrue(underTest.isHumanWinner(winningHumanBoard));
        assertFalse(underTest.isAiWinner(winningHumanBoard));

        BoardState winningAiBoard = BoardState.builder()
                .withDimensions(6, 12)
                .withExitA(new Position(2, 11))
                .withExitB(new Position(3, 0))
                .withVehicles(List.of(
                        new Vehicle('A', new Position(2, 1), 2, Orientation.HORIZONTAL),
                        new Vehicle('B', new Position(3, 0), 2, Orientation.HORIZONTAL)
                ))
                .build();

        assertFalse(underTest.isHumanWinner(winningAiBoard));
        assertTrue(underTest.isAiWinner(winningAiBoard));
    }

    @Test
    void testGetValidMovesForVehicleShouldReturnEmptyListForUnknownVehicle() {
        BoardState board = underTest.createDefaultBoard();
        List<BoardState> moves = underTest.getValidMovesForVehicle(board, 'X');
        assertTrue(moves.isEmpty());
    }

    @Test
    void testGetValidMovesForVehicleShouldReturnAllValidLongMoves() {
        BoardState board = underTest.createDefaultBoard();
        List<BoardState> moves = underTest.getValidMovesForVehicle(board, 'A');
        // 'A' balra 1-et (2,0), jobbra 1-et (2,2) tud lépni
        assertEquals(2, moves.size());
    }
}