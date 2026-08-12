package physics;

import model.Bumper;
import model.DamageMultiplier;
import model.PlayerState;
import model.PortalPair;
import model.Shot;
import model.ShotResult;
import model.TerrainProfile;
import model.TrajectoryPoint;

import java.util.Collections;
import java.util.List;

public class ShotOptimizer {

    private final TrajectoryCalculator trajectoryCalculator;

    private static final double TARGET_HIT_RADIUS = 15.0;

    public ShotOptimizer(
            TrajectoryCalculator trajectoryCalculator
    ) {
        this.trajectoryCalculator =
                trajectoryCalculator;
    }

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
                Collections.emptyList(),
                Collections.emptyList(),
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
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    public ShotResult findBestShot(
            PlayerState shooter,
            PlayerState target,
            TerrainProfile terrain,
            double wind,
            List<Bumper> bumpers
    ) {
        return findBestShot(
                shooter,
                target,
                terrain,
                wind,
                bumpers,
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    public ShotResult findBestShot(
            PlayerState shooter,
            PlayerState target,
            TerrainProfile terrain,
            double wind,
            List<Bumper> bumpers,
            List<DamageMultiplier> damageMultipliers
    ) {
        return findBestShot(
                shooter,
                target,
                terrain,
                wind,
                bumpers,
                damageMultipliers,
                Collections.emptyList()
        );
    }

    // =========================================================
    // FULL OPTIMIZER
    // =========================================================

    public ShotResult findBestShot(
            PlayerState shooter,
            PlayerState target,
            TerrainProfile terrain,
            double wind,
            List<Bumper> bumpers,
            List<DamageMultiplier> damageMultipliers,
            List<PortalPair> portalPairs
    ) {

        if (bumpers == null) {
            bumpers = Collections.emptyList();
        }

        if (damageMultipliers == null) {
            damageMultipliers = Collections.emptyList();
        }

        if (portalPairs == null) {
            portalPairs = Collections.emptyList();
        }

        ShotResult bestResult = null;

        int minAngle;
        int maxAngle;

        /*
         * A bumper OR portal can make a valid path initially travel in
         * an unintuitive direction, so search the full angular range.
         */
        if (!bumpers.isEmpty() ||
            !portalPairs.isEmpty()) {

            minAngle = 1;
            maxAngle = 179;

        } else if (target.getX()
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
                                wind,
                                bumpers,
                                portalPairs
                        );

                ShotResult result =
                        evaluateTrajectory(
                                shot,
                                target,
                                trajectory,
                                damageMultipliers
                        );

                if (isBetterResult(
                        result,
                        bestResult
                )) {

                    bestResult = result;
                }
            }
        }

        return bestResult;
    }

    public boolean isBetterResult(
            ShotResult candidate,
            ShotResult currentBest
    ) {

        if (candidate == null) {
            return false;
        }

        if (currentBest == null) {
            return true;
        }

        boolean candidateHit =
                candidate.isDirectHit();

        boolean bestHit =
                currentBest.isDirectHit();

        if (candidateHit &&
            !bestHit) {

            return true;
        }

        if (!candidateHit &&
            bestHit) {

            return false;
        }

        if (candidateHit) {

            if (candidate.getDamageMultiplier()
                    >
                currentBest.getDamageMultiplier()) {

                return true;
            }

            if (candidate.getDamageMultiplier()
                    <
                currentBest.getDamageMultiplier()) {

                return false;
            }

            return candidate.getClosestDistance()
                    <
                    currentBest.getClosestDistance();
        }

        return candidate.getClosestDistance()
                <
                currentBest.getClosestDistance();
    }

    private ShotResult evaluateTrajectory(
            Shot shot,
            PlayerState target,
            List<TrajectoryPoint> trajectory,
            List<DamageMultiplier> damageMultipliers
    ) {

        double bestDistance =
                Double.MAX_VALUE;

        double bestX = 0.0;
        double bestY = 0.0;

        int activeMultiplier = 1;
        int hitMultiplier = 1;

        boolean targetHit = false;

        if (trajectory == null ||
            trajectory.isEmpty()) {

            return new ShotResult(
                    shot,
                    target,
                    bestDistance,
                    bestX,
                    bestY,
                    1
            );
        }

        for (int i = 0;
             i < trajectory.size();
             i++) {

            TrajectoryPoint point =
                    trajectory.get(i);

            if (!targetHit) {

                activeMultiplier =
                        Math.max(
                                activeMultiplier,
                                getHighestMultiplierAtPoint(
                                        point,
                                        damageMultipliers
                                )
                        );

                if (i > 0) {

                    TrajectoryPoint previous =
                            trajectory.get(i - 1);

                    /*
                     * Important portal detail:
                     *
                     * A teleport is represented by two points with the
                     * SAME timestamp. Do NOT treat that visual jump as a
                     * physical segment, otherwise an X2 sitting between
                     * the two portals would falsely count as collected.
                     */
                    if (Math.abs(
                            previous.getTime()
                            -
                            point.getTime()
                    ) > 0.000001) {

                        activeMultiplier =
                                Math.max(
                                        activeMultiplier,
                                        getHighestMultiplierOnSegment(
                                                previous,
                                                point,
                                                damageMultipliers
                                        )
                                );
                    }
                }
            }

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

            if (distance < bestDistance) {

                bestDistance = distance;
                bestX = point.getX();
                bestY = point.getY();
            }

            if (!targetHit &&
                distance <= TARGET_HIT_RADIUS) {

                targetHit = true;
                hitMultiplier = activeMultiplier;
            }
        }

        if (!targetHit) {
            hitMultiplier = 1;
        }

        return new ShotResult(
                shot,
                target,
                bestDistance,
                bestX,
                bestY,
                hitMultiplier
        );
    }

    private int getHighestMultiplierAtPoint(
            TrajectoryPoint point,
            List<DamageMultiplier> damageMultipliers
    ) {

        int highest = 1;

        for (DamageMultiplier multiplier :
                damageMultipliers) {

            if (multiplier.contains(
                    point.getX(),
                    point.getY()
            )) {

                highest =
                        Math.max(
                                highest,
                                multiplier.getValue()
                        );
            }
        }

        return highest;
    }

    private int getHighestMultiplierOnSegment(
            TrajectoryPoint start,
            TrajectoryPoint end,
            List<DamageMultiplier> damageMultipliers
    ) {

        int highest = 1;

        for (DamageMultiplier multiplier :
                damageMultipliers) {

            if (segmentIntersectsCircle(
                    start.getX(),
                    start.getY(),
                    end.getX(),
                    end.getY(),
                    multiplier.getCenterX(),
                    multiplier.getCenterY(),
                    multiplier.getRadius()
            )) {

                highest =
                        Math.max(
                                highest,
                                multiplier.getValue()
                        );
            }
        }

        return highest;
    }

    private boolean segmentIntersectsCircle(
            double startX,
            double startY,
            double endX,
            double endY,
            double centerX,
            double centerY,
            double radius
    ) {

        double dx = endX - startX;
        double dy = endY - startY;

        double lengthSquared =
                dx * dx
                +
                dy * dy;

        if (lengthSquared <= 0.000001) {

            double pointDx =
                    startX - centerX;

            double pointDy =
                    startY - centerY;

            return pointDx * pointDx
                    +
                    pointDy * pointDy
                    <=
                    radius * radius;
        }

        double t =
                (
                        (centerX - startX) * dx
                        +
                        (centerY - startY) * dy
                )
                /
                lengthSquared;

        t =
                Math.max(
                        0.0,
                        Math.min(
                                1.0,
                                t
                        )
                );

        double closestX =
                startX
                +
                dx * t;

        double closestY =
                startY
                +
                dy * t;

        double distanceX =
                closestX - centerX;

        double distanceY =
                closestY - centerY;

        return distanceX * distanceX
                +
                distanceY * distanceY
                <=
                radius * radius;
    }
}
