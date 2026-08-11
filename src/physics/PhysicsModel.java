package physics;

public class PhysicsModel {

    private final double powerScale;
    private final double gravity;

    /*
     * Wie stark 1 Wind-Punkt die horizontale
     * Beschleunigung beeinflusst.
     *
     * Den Wert kalibrieren wir gleich.
     */
    private final double windScale;

    public PhysicsModel() {

        // FUNKTIONIERENDE BASELINE NICHT VERÄNDERN
        this.powerScale = 0.50;
        this.gravity = 1.0;

        // vorläufiger Startwert
        this.windScale = 0.01;
    }

    public double getPowerScale() {
        return powerScale;
    }

    public double getGravity() {
        return gravity;
    }

    public double getWindScale() {
        return windScale;
    }

    public double getWindAcceleration(
            double wind
    ) {
        return wind * windScale;
    }
}