package hu.nye.progtech.rushhour.model;

import java.util.Objects;

/**
 * Immutábilis Value Object egy táblabeli koordináta (sor, oszlop) ábrázolására.
 */
public final class Position {

    private final int row;
    private final int col;

    public Position(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public Position move(Direction direction, int steps) {
        return new Position(
                row + direction.getRowDelta() * steps,
                col + direction.getColDelta() * steps
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Position position)) {
            return false;
        }
        return row == position.row && col == position.col;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }

    @Override
    public String toString() {
        char colChar = (char) ('a' + col);
        return colChar + String.valueOf(row + 1);
    }
}