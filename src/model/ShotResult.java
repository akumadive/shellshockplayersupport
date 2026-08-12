package model;


public class ShotResult {

    private final Shot shot;
    private final PlayerState target;

    private final double closestDistance;

    private final double closestX;
    private final double closestY;

    /*
     * 1 = normaler Shot
     * 2 = X2 getroffen
     * 3 = X3 getroffen
     */
    private final int damageMultiplier;


    // =========================================================
    // OLD CONSTRUCTOR
    // =========================================================

    /*
     * Alte Aufrufe bleiben kompatibel.
     */
    public ShotResult(
            Shot shot,
            PlayerState target,
            double closestDistance,
            double closestX,
            double closestY
    ) {

        this(
                shot,
                target,
                closestDistance,
                closestX,
                closestY,
                1
        );
    }


    // =========================================================
    // CONSTRUCTOR WITH MULTIPLIER
    // =========================================================

    public ShotResult(
            Shot shot,
            PlayerState target,
            double closestDistance,
            double closestX,
            double closestY,
            int damageMultiplier
    ) {

        this.shot =
                shot;

        this.target =
                target;

        this.closestDistance =
                closestDistance;

        this.closestX =
                closestX;

        this.closestY =
                closestY;

        this.damageMultiplier =
                Math.max(
                        1,
                        damageMultiplier
                );
    }


    // =========================================================
    // GETTERS
    // =========================================================

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


    public int getDamageMultiplier() {

        return damageMultiplier;
    }


    // =========================================================
    // HIT
    // =========================================================

    public boolean isDirectHit() {

        return closestDistance
                <=
                15.0;
    }


    public boolean usesDamageMultiplier() {

        return damageMultiplier
                >
                1;
    }


    // =========================================================
    // DEBUG
    // =========================================================

    @Override
    public String toString() {

        return
                "ShotResult{"
                +
                "power="
                +
                shot.getPower()
                +
                ", angle="
                +
                shot.getAngle()
                +
                ", distance="
                +
                String.format(
                        "%.2f",
                        closestDistance
                )
                +
                ", multiplier=X"
                +
                damageMultiplier
                +
                '}';
    }
}