package hu.nye.progtech.rushhour.exception;

/**
 * Kivétel, amely akkor dobódik, ha egy játékos szabálytalan csúsztatási lépést kísérel meg.
 */
public class InvalidMoveException extends Exception {

    public InvalidMoveException(String message) {
        super(message);
    }
}