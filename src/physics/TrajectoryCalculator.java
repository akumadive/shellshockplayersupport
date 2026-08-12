package physics;

import model.PlayerState;
import model.Shot;
import model.TerrainProfile;
import model.TrajectoryPoint;

import java.util.ArrayList;
import java.util.List;

public class TrajectoryCalculator {

    private final PhysicsModel physicsModel;

    /*
     * Radius um SELF, in dem Terrain-Kollisionen
     * ignoriert werden.
     *
     * Grund:
     * Der erkannte Spielerpunkt liegt sehr nah
     * an der Terrain-Oberfläche.
     *
     * Wir ignorieren deshalb NICHT mehr pauschal
     * die ersten 20 Simulationsschritte, sondern
     * nur einen kleinen Bereich direkt um SELF.
     */
    private static final double SHOOTER_IGNORE_RADIUS = 14.0;

    /*
     * Maximale Distanz zwischen zwei Prüf-Punkten
     * bei der Terrain-Kollision.
     *
     * Dadurch können dünne Spitzen und steile
     * Terrain-Kanten nicht einfach zwischen
     * zwei Physics-Steps übersprungen werden.
     */
    private static final double COLLISION_SAMPLE_DISTANCE = 0.5;

    /*
     * Unterhalb dieses Bildschirmbereichs
     * brauchen wir nicht weiter zu simulieren.
     */
    private static final double MAX_SCREEN_Y = 1200.0;

    /*
     * Schutz gegen Endlossimulationen.
     */
    private static final int MAX_STEPS = 10000;

    /*
     * Bestehende Physics-Auflösung.
     *
     * Die funktionierende Flugphysik wird
     * hier NICHT verändert.
     */
    private static final double TIME_STEP = 0.05;


    public TrajectoryCalculator(
            PhysicsModel physicsModel
    ) {

        this.physicsModel =
                physicsModel;
    }


    /*
     * Alte Methode bleibt erhalten.
     *
     * Dadurch funktioniert sämtlicher bisheriger
     * Code weiterhin exakt wie vorher:
     *
     * calculate(shooter, shot, terrain)
     *
     * bedeutet automatisch Wind = 0.
     */
    public List<TrajectoryPoint> calculate(
            PlayerState shooter,
            Shot shot,
            TerrainProfile terrain
    ) {

        return calculate(
                shooter,
                shot,
                terrain,
                0.0
        );
    }


    // =========================================================
    // CALCULATE WITH WIND
    // =========================================================

    public List<TrajectoryPoint> calculate(
            PlayerState shooter,
            Shot shot,
            TerrainProfile terrain,
            double wind
    ) {

        List<TrajectoryPoint> points =
                new ArrayList<>();


        // =====================================================
        // INITIAL VELOCITY
        // =====================================================

        double angleRadians =
                Math.toRadians(
                        shot.getAngle()
                );

        double velocity =
                shot.getPower()
                *
                physicsModel.getPowerScale();

        double vx =
                Math.cos(angleRadians)
                *
                velocity;

        double vy =
                -Math.sin(angleRadians)
                *
                velocity;


        // =====================================================
        // START POSITION
        // =====================================================

        double x =
                shooter.getX();

        double y =
                shooter.getY();


        // =====================================================
        // WIND
        // =====================================================

        double windAcceleration =
                physicsModel.getWindAcceleration(
                        wind
                );


        // =====================================================
        // SIMULATION
        // =====================================================

        double time = 0.0;

        for (int i = 0;
             i < MAX_STEPS;
             i++) {


            /*
             * Aktuelle Position speichern.
             */
            points.add(
                    new TrajectoryPoint(
                            x,
                            y,
                            time
                    )
            );


            // =================================================
            // NEXT POSITION
            // =================================================

            double nextX =
                    x
                    +
                    vx * TIME_STEP;

            double nextY =
                    y
                    +
                    vy * TIME_STEP;

            double nextTime =
                    time
                    +
                    TIME_STEP;


            // =================================================
            // TERRAIN COLLISION
            // =================================================

            /*
             * Wichtig:
             *
             * Wir prüfen NICHT nur nextX/nextY.
             *
             * Stattdessen prüfen wir die komplette Strecke
             * vom aktuellen Punkt zum nächsten Punkt.
             *
             * Dadurch werden auch:
             *
             * - dünne Spitzen
             * - kleine Hügel
             * - steile Wände
             * - schnelle Projektile
             *
             * zuverlässig erkannt.
             */
            TerrainCollision collision =
                    findTerrainCollision(
                            x,
                            y,
                            time,

                            nextX,
                            nextY,
                            nextTime,

                            shooter,
                            terrain
                    );


            if (collision != null) {

                /*
                 * Exakten Kollisionspunkt noch
                 * in die Trajectory aufnehmen.
                 */
                points.add(
                        new TrajectoryPoint(
                                collision.x,
                                collision.y,
                                collision.time
                        )
                );

                /*
                 * Projektil ist im Terrain:
                 * Simulation beendet.
                 */
                break;
            }


            // =================================================
            // APPLY POSITION
            // =================================================

            x = nextX;
            y = nextY;
            time = nextTime;


            // =================================================
            // SCREEN BOUNDS
            // =================================================

            if (x < 0 ||
                x >= terrain.getWidth()) {

                break;
            }


            if (y > MAX_SCREEN_Y) {

                break;
            }


            // =================================================
            // ACCELERATION
            // =================================================

            /*
             * Positive Windwerte:
             * Wind nach rechts.
             *
             * Negative Windwerte:
             * Wind nach links.
             */
            vx +=
                    windAcceleration
                    *
                    TIME_STEP;


            /*
             * Bildschirmkoordinaten:
             *
             * +Y = nach unten.
             */
            vy +=
                    physicsModel.getGravity()
                    *
                    TIME_STEP;
        }


        return points;
    }


    // =========================================================
    // TERRAIN COLLISION
    // =========================================================

    private TerrainCollision findTerrainCollision(
            double startX,
            double startY,
            double startTime,

            double endX,
            double endY,
            double endTime,

            PlayerState shooter,
            TerrainProfile terrain
    ) {

        double dx =
                endX
                -
                startX;

        double dy =
                endY
                -
                startY;


        double segmentLength =
                Math.sqrt(
                        dx * dx
                        +
                        dy * dy
                );


        /*
         * Mindestens einen Sample durchführen.
         *
         * Bei längeren Bewegungen entstehen
         * entsprechend mehr Zwischenprüfungen.
         */
        int samples =
                Math.max(
                        1,
                        (int) Math.ceil(
                                segmentLength
                                /
                                COLLISION_SAMPLE_DISTANCE
                        )
                );


        for (int sample = 1;
             sample <= samples;
             sample++) {


            double progress =
                    (double) sample
                    /
                    samples;


            double sampleX =
                    startX
                    +
                    dx * progress;

            double sampleY =
                    startY
                    +
                    dy * progress;

            double sampleTime =
                    startTime
                    +
                    (
                            endTime
                            -
                            startTime
                    )
                    *
                    progress;


            /*
             * Außerhalb der horizontalen Map:
             * dort kann kein Terrain mehr geprüft werden.
             */
            if (sampleX < 0 ||
                sampleX >= terrain.getWidth()) {

                continue;
            }


            /*
             * Direkt um SELF herum ignorieren wir
             * Terrain.
             *
             * Anders als vorher sind das aber NICHT
             * pauschal 20 komplette Physics-Steps.
             *
             * Dadurch kann ein naher Berg unmittelbar
             * vor dem Panzer jetzt korrekt blockieren.
             */
            if (isInsideShooterIgnoreArea(
                    sampleX,
                    sampleY,
                    shooter
            )) {

                continue;
            }


            if (hitsTerrain(
                    sampleX,
                    sampleY,
                    terrain
            )) {

                return new TerrainCollision(
                        sampleX,
                        sampleY,
                        sampleTime
                );
            }
        }


        return null;
    }


    // =========================================================
    // SHOOTER IGNORE AREA
    // =========================================================

    private boolean isInsideShooterIgnoreArea(
            double x,
            double y,
            PlayerState shooter
    ) {

        double dx =
                x
                -
                shooter.getX();

        double dy =
                y
                -
                shooter.getY();


        double distanceSquared =
                dx * dx
                +
                dy * dy;


        return distanceSquared
                <=
                SHOOTER_IGNORE_RADIUS
                *
                SHOOTER_IGNORE_RADIUS;
    }


    // =========================================================
    // SINGLE TERRAIN TEST
    // =========================================================

    private boolean hitsTerrain(
            double x,
            double y,
            TerrainProfile terrain
    ) {

        /*
         * Nicht round(), sondern floor().
         *
         * x = 100.9 befindet sich noch in
         * Terrain-Spalte 100.
         */
        int terrainX =
                (int) Math.floor(x);


        if (terrainX < 0 ||
            terrainX >= terrain.getWidth()) {

            return false;
        }


        if (!terrain.hasTerrainAt(
                terrainX
        )) {

            return false;
        }


        int terrainY =
                terrain.getY(
                        terrainX
                );


        /*
         * Bildschirm:
         *
         * kleinere Y = weiter oben
         * größere Y = weiter unten
         *
         * Sobald das Projektil auf oder unterhalb
         * der Terrain-Oberfläche liegt, ist es
         * kollidiert.
         */
        return y >= terrainY;
    }


    // =========================================================
    // INTERNAL TERRAIN COLLISION
    // =========================================================

    private static class TerrainCollision {

        private final double x;
        private final double y;
        private final double time;


        private TerrainCollision(
                double x,
                double y,
                double time
        ) {

            this.x = x;
            this.y = y;
            this.time = time;
        }
    }
}