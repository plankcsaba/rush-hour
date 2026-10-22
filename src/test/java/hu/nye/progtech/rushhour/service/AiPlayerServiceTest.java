package hu.nye.progtech.rushhour.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import hu.nye.progtech.rushhour.model.BoardState;
import hu.nye.progtech.rushhour.model.Orientation;
import hu.nye.progtech.rushhour.model.Position;
import hu.nye.progtech.rushhour.model.Vehicle;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiPlayerServiceTest {

    @Mock
    private GameService gameService;

    private AiPlayerService underTest;

    @BeforeEach
    void setUp() {
        underTest = new AiPlayerService(gameService, new Random(42));
    }

    @Test
    void testMakeRandomMoveShouldSelectValidMoveFromAvailableVehicle() {
        Vehicle v1 = new Vehicle('A', new Position(2, 1), 2, Orientation.HORIZONTAL);
        BoardState initialState = BoardState.builder()
                .withVehicles(List.of(v1))
                .build();

        Vehicle movedV1 = v1.withMovedPosition(hu.nye.progtech.rushhour.model.Direction.RIGHT, 1);
        BoardState expectedNextState = BoardState.builder()
                .withVehicles(List.of(movedV1))
                .build();

        when(gameService.getValidMovesForVehicle(initialState, 'A'))
                .thenReturn(List.of(expectedNextState));

        BoardState result = underTest.makeRandomMove(initialState);

        assertEquals(expectedNextState, result);
        verify(gameService).getValidMovesForVehicle(initialState, 'A');
    }

    @Test
    void testMakeRandomMoveShouldReturnCurrentStateWhenNoVehicleCanMove() {
        Vehicle v1 = new Vehicle('A', new Position(2, 1), 2, Orientation.HORIZONTAL);
        BoardState initialState = BoardState.builder()
                .withVehicles(List.of(v1))
                .build();

        when(gameService.getValidMovesForVehicle(initialState, 'A'))
                .thenReturn(Collections.emptyList());

        BoardState result = underTest.makeRandomMove(initialState);

        assertEquals(initialState, result);
    }
}