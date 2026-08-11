package model;

public class TrajectoryPoint {

    private final double x;
    private final double y;
    private final double time;

    public TrajectoryPoint(
            double x,
            double y,
            double time
    ) {
        this.x = x;
        this.y = y;
        this.time = time;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getTime() {
        return time;
    }
}