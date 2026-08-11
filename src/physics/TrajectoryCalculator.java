package physics;

import model.PlayerState;
import model.Shot;
import model.TerrainProfile;
import model.TrajectoryPoint;

import java.util.ArrayList;
import java.util.List;

public class TrajectoryCalculator {

    private final PhysicsModel physicsModel;

    public TrajectoryCalculator(
            PhysicsModel physicsModel
    ) {
        this.physicsModel = physicsModel;
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

        double timeStep = 0.05;

        int maxSteps = 10000;


        for (int i = 0;
             i < maxSteps;
             i++) {


            points.add(
                    new TrajectoryPoint(
                            x,
                            y,
                            time
                    )
            );


            // ---------------------------------------------
            // POSITION
            // ---------------------------------------------

            x += vx * timeStep;

            y += vy * timeStep;


            // ---------------------------------------------
            // ACCELERATION
            // ---------------------------------------------

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
                    timeStep;


            /*
             * Bildschirmkoordinaten:
             * +Y = nach unten.
             */
            vy +=
                    physicsModel.getGravity()
                    *
                    timeStep;


            time += timeStep;


            // =================================================
            // SCREEN BOUNDS
            // =================================================

            if (x < 0 ||
                x >= terrain.getWidth()) {

                break;
            }


            if (y > 1200) {

                break;
            }


            // =================================================
            // TERRAIN COLLISION
            // =================================================

            /*
             * Direkt nach dem Start ignorieren wir Terrain,
             * da SELF selbst auf der Terrainkante sitzt.
             */
            if (i > 20 &&
                hitsTerrain(
                        x,
                        y,
                        terrain
                )) {

                points.add(
                        new TrajectoryPoint(
                                x,
                                y,
                                time
                        )
                );

                break;
            }
        }


        return points;
    }


    // =========================================================
    // TERRAIN COLLISION
    // =========================================================

    private boolean hitsTerrain(
            double x,
            double y,
            TerrainProfile terrain
    ) {

        int terrainX =
                (int) Math.round(x);


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


        return y >= terrainY;
    }
}