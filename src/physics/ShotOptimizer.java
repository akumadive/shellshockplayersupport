package physics;

import model.PlayerState;
import model.Shot;
import model.ShotResult;
import model.TerrainProfile;
import model.TrajectoryPoint;

import java.util.List;

public class ShotOptimizer {

    private final TrajectoryCalculator trajectoryCalculator;


    public ShotOptimizer(
            TrajectoryCalculator trajectoryCalculator
    ) {

        this.trajectoryCalculator =
                trajectoryCalculator;
    }


    /*
     * Alte Version bleibt bestehen.
     * Ohne angegebenen Wind -> Wind 0.
     */
    public ShotResult findBestShot(
            PlayerState shooter,
            PlayerState target,
            TerrainProfile terrain
    ) {

        return findBestShot(
                shooter,
                target,
                terrain,
                0.0
        );
    }


    // =========================================================
    // OPTIMIZER WITH WIND
    // =========================================================

    public ShotResult findBestShot(
            PlayerState shooter,
            PlayerState target,
            TerrainProfile terrain,
            double wind
    ) {

        ShotResult bestResult = null;


        int minAngle;
        int maxAngle;


        if (target.getX()
                >=
            shooter.getX()) {

            minAngle = 1;
            maxAngle = 89;

        } else {

            minAngle = 91;
            maxAngle = 179;
        }


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
                                wind
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

                    bestResult = result;
                }


                if (bestResult != null &&
                    bestResult.getClosestDistance()
                    <= 2.0) {

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

        double bestX = 0;
        double bestY = 0;


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