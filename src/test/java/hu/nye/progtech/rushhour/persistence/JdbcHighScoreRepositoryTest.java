package hu.nye.progtech.rushhour.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JdbcHighScoreRepositoryTest {

    private HighScoreRepository repository;

    @BeforeEach
    void setUp() {
        String uniqueDbUrl = "jdbc:h2:mem:rushhour_test_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        repository = new JdbcHighScoreRepository(uniqueDbUrl, "sa", "");
    }

    @Test
    void incrementWinCountShouldAccumulateWinsAndOrderByDescendingCount() {
        // Given
        String alice = "Alice";
        String bob = "Bob";

        // When
        repository.incrementWinCount(bob);
        repository.incrementWinCount(alice);
        repository.incrementWinCount(alice);

        // Then
        Map<String, Integer> scores = repository.getHighScores();
        assertEquals(2, scores.get(alice));
        assertEquals(1, scores.get(bob));
    }
}