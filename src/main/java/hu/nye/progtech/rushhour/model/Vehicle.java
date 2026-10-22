package hu.nye.progtech.rushhour.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutábilis Value Object, amely egy 1 dimenziós, L hosszúságú járművet reprezentál.
 */
public final class Vehicle {

    private final char id;
    private final Position headPosition;
    private final int length;
    private final Orientation orientation;

    public Vehicle(char id, Position headPosition, int length, Orientation orientation) {
        this.id = id;
        this.headPosition = Objects.requireNonNull(headPosition);
        this.length = length;
        this.orientation = Objects.requireNonNull(orientation);
    }

    public char getId() {
        return id;
    }

    public Position getHeadPosition() {
        return headPosition;
    }

    public int getLength() {
        return length;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    /**
     * Visszaadja a jármű által elfoglalt összes mező koordinátáját.
     */
    public List<Position> getOccupiedPositions() {
        List<Position> positions = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            int r = headPosition.getRow() + (orientation == Orientation.VERTICAL ? i : 0);
            int c = headPosition.getCol() + (orientation == Orientation.HORIZONTAL ? i : 0);
            positions.add(new Position(r, c));
        }
        return positions;
    }

    /**
     * Létrehoz egy új, elmozdított jármű példányt (Immutability megőrzése).
     */
    public Vehicle withMovedPosition(Direction direction, int steps) {
        return new Vehicle(id, headPosition.move(direction, steps), length, orientation);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Vehicle vehicle)) {
            return false;
        }
        return id == vehicle.id
                && length == vehicle.length
                && Objects.equals(headPosition, vehicle.headPosition)
                && orientation == vehicle.orientation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, headPosition, length, orientation);
    }

    @Override
    public String toString() {
        return "Vehicle{"
                + "id=" + id
                + ", head=" + headPosition
                + ", length=" + length
                + ", orientation=" + orientation
                + '}';
    }
}