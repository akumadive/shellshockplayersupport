package model;

public class ShotResult {

    private final Shot shot;
    private final PlayerState target;

    private final double closestDistance;

    private final double closestX;
    private final double closestY;

    public ShotResult(
            Shot shot,
            PlayerState target,
            double closestDistance,
            double closestX,
            double closestY
    ) {
        this.shot = shot;
        this.target = target;
        this.closestDistance = closestDistance;
        this.closestX = closestX;
        this.closestY = closestY;
    }

    public Shot getShot() {
        return shot;
    }

    public PlayerState getTarget() {
        return target;
    }

    public double getClosestDistance() {
        return closestDistance;
    }

    public double getClosestX() {
        return closestX;
    }

    public double getClosestY() {
        return closestY;
    }

    public boolean isDirectHit() {
        return closestDistance <= 15.0;
    }
}