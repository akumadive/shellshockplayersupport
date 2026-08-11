package model;

public class Shot {

    private final double power;
    private final double angle;

    public Shot(double power, double angle) {
        this.power = power;
        this.angle = angle;
    }

    public double getPower() {
        return power;
    }

    public double getAngle() {
        return angle;
    }
}