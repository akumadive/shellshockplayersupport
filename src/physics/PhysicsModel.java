package physics;

public class PhysicsModel {

    /*
     * No-Wind-Baseline.
     *
     * Die behalten wir unverändert, weil der
     * Solver damit bereits einen echten Treffer
     * produziert hat.
     */
    private final double powerScale = 0.50;
    private final double gravity = 1.0;

    /*
     * Wind-Kalibrierung.
     *
     * ShellShock-Wind wird als horizontale
     * Beschleunigung behandelt.
     *
     * wind > 0  -> rechts
     * wind < 0  -> links
     *
     * Erster Fit aus unseren Referenzshots:
     * -66
     * -32
     * +51
     * +30
     *
     * Das ist ab jetzt unser Baseline-Wert.
     */
    private final double windScale = 0.0035;


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