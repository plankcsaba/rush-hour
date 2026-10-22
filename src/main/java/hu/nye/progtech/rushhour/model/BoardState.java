package hu.nye.progtech.rushhour.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutábilis Value Object, amely az NxM-es játéktábla aktuális állapotát írja le.
 * Builder tervezési mintával példányosítható.
 */
public final class BoardState {

    private final int rows;
    private final int cols;
    private final Position exitA;
    private final Position exitB;
    private final List<Vehicle> vehicles;

    private BoardState(Builder builder) {
        this.rows = builder.rows;
        this.cols = builder.cols;
        this.exitA = Objects.requireNonNull(builder.exitA);
        this.exitB = Objects.requireNonNull(builder.exitB);
        this.vehicles = Collections.unmodifiableList(builder.vehicles);
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public Position getExitA() {
        return exitA;
    }

    public Position getExitB() {
        return exitB;
    }

    public List<Vehicle> getVehicles() {
        return vehicles;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BoardState that)) {
            return false;
        }
        return rows == that.rows
                && cols == that.cols
                && Objects.equals(exitA, that.exitA)
                && Objects.equals(exitB, that.exitB)
                && Objects.equals(vehicles, that.vehicles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rows, cols, exitA, exitB, vehicles);
    }

    @Override
    public String toString() {
        return "BoardState{"
                + "rows=" + rows
                + ", cols=" + cols
                + ", exitA=" + exitA
                + ", exitB=" + exitB
                + ", vehicles=" + vehicles
                + '}';
    }

    /**
     * Builder osztály a BoardState objektumok biztonságos felépítéséhez.
     */
    public static final class Builder {
        private int rows = 6;
        private int cols = 12;
        private Position exitA = new Position(2, 11);
        private Position exitB = new Position(3, 0);
        private List<Vehicle> vehicles = Collections.emptyList();

        private Builder() {
        }

        public Builder withDimensions(int rows, int cols) {
            this.rows = rows;
            this.cols = cols;
            return this;
        }

        public Builder withExitA(Position exitA) {
            this.exitA = exitA;
            return this;
        }

        public Builder withExitB(Position exitB) {
            this.exitB = exitB;
            return this;
        }

        public Builder withVehicles(List<Vehicle> vehicles) {
            this.vehicles = List.copyOf(vehicles);
            return this;
        }

        public BoardState build() {
            return new BoardState(this);
        }
    }
}