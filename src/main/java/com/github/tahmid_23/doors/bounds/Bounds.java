package com.github.tahmid_23.doors.bounds;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;

public class Bounds {

    private static final double EPSILON = 1e-5;

    private final Point min;

    private final Point max;

    private Bounds(Point min, Point max) {
        this.min = min;
        this.max = max;
    }
    
    public static Bounds fromPoints(Point a, Point b) {
        double minX, maxX;
        if (a.x() < b.x()) {
            minX = a.x();
            maxX = b.x();
        } else {
            minX = b.x();
            maxX = a.x();
        }

        double minY, maxY;
        if (a.y() < b.y()) {
            minY = a.y();
            maxY = b.y();
        } else {
            minY = b.y();
            maxY = a.y();
        }

        double minZ, maxZ;
        if (a.z() < b.z()) {
            minZ = a.z();
            maxZ = b.z();
        } else {
            minZ = b.z();
            maxZ = a.z();
        }

        return new Bounds(new Vec(minX, minY, minZ), new Vec(maxX, maxY, maxZ));
    }

    public static Bounds fromLengths(Point origin, double width, double height, double length) {
        return Bounds.fromPoints(origin, origin.add(width, height, length));
    }

    public boolean intersects(Bounds other) {
        if (max.x() < other.getMin().x() - EPSILON || other.getMax().x() + EPSILON < min.x()) {
            return false;
        }
        if (max.y() < other.getMin().y() - EPSILON || other.getMax().y() + EPSILON < min.y()) {
            return false;
        }
        if (max.z() < other.getMin().z() - EPSILON || other.getMax().z() + EPSILON < min.z()) {
            return false;
        }
        return true;
    }

    public boolean contains(Point point) {
        if (!(min.x() - EPSILON < point.x() && point.x() < max.x() + EPSILON)) {
            return false;
        }
        if (!(min.y() - EPSILON < point.y() && point.y() < max.y() + EPSILON)) {
            return false;
        }
        if (!(min.z() - EPSILON < point.z() && point.z() < max.z() + EPSILON)) {
            return false;
        }

        return true;
    }

    public Point getMin() {
        return min;
    }

    public Point getMax() {
        return max;
    }

}
