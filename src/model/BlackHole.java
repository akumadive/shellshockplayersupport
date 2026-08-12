package model;


public class BlackHole {

    private final double centerX;
    private final double centerY;

    /*
     * Schwarzer Kern.
     *
     * Projektil betritt diesen Bereich:
     * -> zerstört
     * -> Shot ungültig
     */
    private final double coreRadius;


    /*
     * Wirkungsradius der Gravitation.
     *
     * Wird aus der tatsächlichen Größe
     * des Black Holes abgeleitet.
     */
    private final double influenceRadius;


    public BlackHole(
            double centerX,
            double centerY,
            double coreRadius,
            double influenceRadius
    ) {

        this.centerX =
                centerX;

        this.centerY =
                centerY;

        this.coreRadius =
                coreRadius;

        this.influenceRadius =
                influenceRadius;
    }


    // =========================================================
    // GETTERS
    // =========================================================

    public double getCenterX() {

        return centerX;
    }


    public double getCenterY() {

        return centerY;
    }


    public double getCoreRadius() {

        return coreRadius;
    }


    public double getInfluenceRadius() {

        return influenceRadius;
    }


    // =========================================================
    // GEOMETRY
    // =========================================================

    public double distanceTo(
            double x,
            double y
    ) {

        double dx =
                x
                -
                centerX;


        double dy =
                y
                -
                centerY;


        return Math.sqrt(
                dx * dx
                +
                dy * dy
        );
    }


    public boolean containsCore(
            double x,
            double y
    ) {

        double dx =
                x
                -
                centerX;


        double dy =
                y
                -
                centerY;


        return dx * dx
                +
                dy * dy
                <=
                coreRadius
                *
                coreRadius;
    }


    public boolean isInsideInfluence(
            double x,
            double y
    ) {

        double dx =
                x
                -
                centerX;


        double dy =
                y
                -
                centerY;


        return dx * dx
                +
                dy * dy
                <=
                influenceRadius
                *
                influenceRadius;
    }


    @Override
    public String toString() {

        return
                "BlackHole{"
                +
                "center=("
                +
                String.format(
                        "%.1f",
                        centerX
                )
                +
                ", "
                +
                String.format(
                        "%.1f",
                        centerY
                )
                +
                ")"
                +
                ", coreRadius="
                +
                String.format(
                        "%.1f",
                        coreRadius
                )
                +
                ", influenceRadius="
                +
                String.format(
                        "%.1f",
                        influenceRadius
                )
                +
                '}';
    }
}