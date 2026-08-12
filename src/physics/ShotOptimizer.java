package physics;

import model.Bumper;
import model.PlayerState;
import model.Shot;
import model.ShotResult;
import model.TerrainProfile;
import model.TrajectoryPoint;

import java.util.Collections;
import java.util.List;


public class ShotOptimizer {

    private final TrajectoryCalculator trajectoryCalculator;


    public ShotOptimizer(
            TrajectoryCalculator trajectoryCalculator
    ) {

        this.trajectoryCalculator =
                trajectoryCalculator;
    }


    // =========================================================
    // OLD API
    // =========================================================

    public ShotResult findBestShot(
            PlayerState shooter,
            PlayerState target,
            TerrainProfile terrain
    ) {

        return findBestShot(
                shooter,
                target,
                terrain,
                0.0,
                Collections.emptyList()
        );
    }


    public ShotResult findBestShot(
            PlayerState shooter,
            PlayerState target,
            TerrainProfile terrain,
            double wind
    ) {

        return findBestShot(
                shooter,
                target,
                terrain,
                wind,
                Collections.emptyList()
        );
    }


    // =========================================================
    // OPTIMIZER WITH BUMPERS
    // =========================================================

    public ShotResult findBestShot(
            PlayerState shooter,
            PlayerState target,
            TerrainProfile terrain,
            double wind,
            List<Bumper> bumpers
    ) {

        if (bumpers == null) {

            bumpers =
                    Collections.emptyList();
        }


        ShotResult bestResult =
                null;


        // =====================================================
        // ANGLE RANGE
        // =====================================================

        int minAngle;
        int maxAngle;


        if (!bumpers.isEmpty()) {


            /*
             * Mit Bumpern kann ein gültiger Shot auch
             * zuerst vom Gegner weg fliegen.
             */
            minAngle =
                    1;

            maxAngle =
                    179;


        } else {


            /*
             * Ohne Bumper behalten wir die bisherige
             * schnelle Suchlogik.
             */

            if (target.getX()
                    >=
                shooter.getX()) {


                minAngle =
                        1;

                maxAngle =
                        89;


            } else {


                minAngle =
                        91;

                maxAngle =
                        179;
            }
        }


        // =====================================================
        // SEARCH
        // =====================================================

        for (int power = 1;
             power <= 100;
             power++) {


            for (int angle = minAngle;
                 angle <= maxAngle;
                 angle++) {


                Shot shot =
                        new Shot(
                                power,
                                angle
                        );


                List<TrajectoryPoint> trajectory =
                        trajectoryCalculator.calculate(
                                shooter,
                                shot,
                                terrain,
                                wind,
                                bumpers
                        );


                ShotResult result =
                        evaluateTrajectory(
                                shot,
                                target,
                                trajectory
                        );


                if (bestResult == null ||
                    result.getClosestDistance()
                    <
                    bestResult.getClosestDistance()) {


                    bestResult =
                            result;
                }


                if (bestResult != null &&
                    bestResult.getClosestDistance()
                    <=
                    2.0) {


                    return bestResult;
                }
            }
        }


        return bestResult;
    }


    // =========================================================
    // EVALUATE
    // =========================================================

    private ShotResult evaluateTrajectory(
            Shot shot,
            PlayerState target,
            List<TrajectoryPoint> trajectory
    ) {

        double bestDistance =
                Double.MAX_VALUE;


        double bestX =
                0.0;

        double bestY =
                0.0;


        for (TrajectoryPoint point :
                trajectory) {


            double dx =
                    point.getX()
                    -
                    target.getX();


            double dy =
                    point.getY()
                    -
                    target.getY();


            double distance =
                    Math.sqrt(
                            dx * dx
                            +
                            dy * dy
                    );


            if (distance
                    <
                bestDistance) {


                bestDistance =
                        distance;


                bestX =
                        point.getX();


                bestY =
                        point.getY();
            }
        }


        return new ShotResult(
                shot,
                target,
                bestDistance,
                bestX,
                bestY
        );
    }
}