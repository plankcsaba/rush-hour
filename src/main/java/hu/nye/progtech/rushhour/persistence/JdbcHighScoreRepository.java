package hu.nye.progtech.rushhour.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * H2 adatbázist és JDBC-t használó HighScoreRepository implementáció.
 */
public class JdbcHighScoreRepository implements HighScoreRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcHighScoreRepository.class);
    private final String jdbcUrl;
    private final String user;
    private final String password;

    public JdbcHighScoreRepository(String jdbcUrl, String user, String password) {
        this.jdbcUrl = jdbcUrl;
        this.user = user;
        this.password = password;
        initDatabase();
    }

    private void initDatabase() {
        String createTableSql = """
                CREATE TABLE IF NOT EXISTS high_scores (
                    player_name VARCHAR(100) PRIMARY KEY,
                    wins INT NOT NULL
                )
                """;
        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password);
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSql);
            LOGGER.info("H2 adatbázis high_scores tábla inicializálva.");
        } catch (SQLException e) {
            LOGGER.error("Hiba az adatbázis inicializálásakor", e);
        }
    }

    @Override
    public void incrementWinCount(String playerName) {
        String selectSql = "SELECT wins FROM high_scores WHERE player_name = ?";
        String insertSql = "INSERT INTO high_scores (player_name, wins) VALUES (?, 1)";
        String updateSql = "UPDATE high_scores SET wins = wins + 1 WHERE player_name = ?";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password);
             PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
            selectStmt.setString(1, playerName);
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setString(1, playerName);
                        updateStmt.executeUpdate();
                    }
                } else {
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                        insertStmt.setString(1, playerName);
                        insertStmt.executeUpdate();
                    }
                }
            }
            LOGGER.info("Győzelem elmentve a következő játékosnak: {}", playerName);
        } catch (SQLException e) {
            LOGGER.error("Hiba a győzelem mentésekor: {}", playerName, e);
        }
    }

    @Override
    public Map<String, Integer> getHighScores() {
        Map<String, Integer> scores = new LinkedHashMap<>();
        String sql = "SELECT player_name, wins FROM high_scores ORDER BY wins DESC";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                scores.put(rs.getString("player_name"), rs.getInt("wins"));
            }
        } catch (SQLException e) {
            LOGGER.error("Hiba a high score táblázat lekérdezésekor", e);
        }
        return scores;
    }
}