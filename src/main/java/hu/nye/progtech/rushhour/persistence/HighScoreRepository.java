package hu.nye.progtech.rushhour.persistence;

import java.util.Map;

/**
 * Interfész a játékosok győzelmeinek adatbázisban történő tárolásához.
 */
public interface HighScoreRepository {

    void incrementWinCount(String playerName);

    Map<String, Integer> getHighScores();
}