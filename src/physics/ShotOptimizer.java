package physics;

import model.Bumper;
import model.DamageMultiplier;
import model.PlayerState;
import model.Shot;
import model.ShotResult;
import model.TerrainProfile;
import model.TrajectoryPoint;

import java.util.Collections;
import java.util.List;


public class ShotOptimizer {

    private final TrajectoryCalculator trajectoryCalculator;


    /*
     * Abstand, bei dem wir einen Tank als
     * tatsächlich getroffen betrachten.
     *
     * Entspricht der bisherigen ShotResult-
     * Direct-Hit-Grenze.
     */
    private static final double TARGET_HIT_RADIUS =
            15.0;


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
                Collections.emptyList()
        );
    }


    // =========================================================
    // OPTIMIZER WITH BUMPERS + MULTIPLIERS
    // =========================================================

    public ShotResult findBestShot(
            PlayerState shooter,
            PlayerState target,
            TerrainProfile terrain,
            double wind,
            List<Bumper> bumpers,
            List<DamageMultiplier> damageMultipliers
    ) {

        if (bumpers == null) {

            bumpers =
                    Collections.emptyList();
        }


        if (damageMultipliers == null) {

            damageMultipliers =
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
             * Mit Bumpern kann ein Shot zunächst
             * vom Gegner weg fliegen.
             */
            minAngle =
                    1;

            maxAngle =
                    179;


        } else {


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
                                trajectory,
                                damageMultipliers
                        );


                if (isBetterResult(
                        result,
                        bestResult
                )) {


                    bestResult =
                            result;
                }
            }
        }


        /*
         * WICHTIG:
         *
         * Hier gibt es absichtlich KEINEN frühen
         * return mehr bei <= 2 Pixel.
         *
         * Früher war das okay:
         *
         * Treffer gefunden -> fertig.
         *
         * Jetzt könnte aber später noch ein genauso
         * guter X2- oder X3-Shot existieren.
         *
         * Deshalb muss der komplette Suchraum
         * untersucht werden.
         */

        return bestResult;
    }


    // =========================================================
    // RESULT RANKING
    // =========================================================

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


        // =====================================================
        // 1. HIT ALWAYS BEATS MISS
        // =====================================================

        if (candidateHit &&
            !bestHit) {

            return true;
        }


        if (!candidateHit &&
            bestHit) {

            return false;
        }


        // =====================================================
        // 2. BOTH ARE VALID HITS
        // =====================================================

        if (candidateHit) {


            /*
             * Unter tatsächlichen Treffern:
             *
             * X3 > X2 > normal.
             */
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


            /*
             * Gleicher Multiplier:
             * genaueren Treffer bevorzugen.
             */
            return candidate.getClosestDistance()
                    <
                    currentBest.getClosestDistance();
        }


        // =====================================================
        // 3. BOTH MISS
        // =====================================================

        /*
         * Bei Misses interessiert uns X2/X3 nicht.
         *
         * Ein Shot, der durch X2 fliegt und danach
         * 50 Pixel am Gegner vorbeigeht, ist nicht
         * besser als ein normaler Shot mit 5 Pixel
         * Abstand.
         */
        return candidate.getClosestDistance()
                <
                currentBest.getClosestDistance();
    }


    // =========================================================
    // EVALUATE
    // =========================================================

    private ShotResult evaluateTrajectory(
            Shot shot,
            PlayerState target,
            List<TrajectoryPoint> trajectory,
            List<DamageMultiplier> damageMultipliers
    ) {

        double bestDistance =
                Double.MAX_VALUE;


        double bestX =
                0.0;

        double bestY =
                0.0;


        /*
         * Höchster Multiplier, den das Projektil
         * BIS ZUM TREFFER passiert hat.
         */
        int activeMultiplier =
                1;


        /*
         * Der Multiplier beim ersten tatsächlichen
         * Gegnerkontakt.
         */
        int hitMultiplier =
                1;


        boolean targetHit =
                false;


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


        // =====================================================
        // WALK ALONG TRAJECTORY
        // =====================================================

        for (int i = 0;
             i < trajectory.size();
             i++) {


            TrajectoryPoint point =
                    trajectory.get(i);


            // =================================================
            // MULTIPLIER
            // =================================================

            /*
             * Nur solange der Gegner noch nicht getroffen
             * wurde, können weitere X2/X3 aktiviert werden.
             */
            if (!targetHit) {


                activeMultiplier =
                        Math.max(
                                activeMultiplier,
                                getHighestMultiplierAtPoint(
                                        point,
                                        damageMultipliers
                                )
                        );


                /*
                 * Zusätzlich das Segment vom vorherigen
                 * Trajectory-Punkt prüfen.
                 *
                 * Dadurch kann selbst ein kleiner X3 nicht
                 * zwischen zwei Simulationspunkten
                 * übersprungen werden.
                 */
                if (i > 0) {


                    activeMultiplier =
                            Math.max(
                                    activeMultiplier,
                                    getHighestMultiplierOnSegment(
                                            trajectory.get(
                                                    i - 1
                                            ),
                                            point,
                                            damageMultipliers
                                    )
                            );
                }
            }


            // =================================================
            // TARGET DISTANCE
            // =================================================

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


            // =================================================
            // FIRST TARGET HIT
            // =================================================

            if (!targetHit &&
                distance <= TARGET_HIT_RADIUS) {


                targetHit =
                        true;


                /*
                 * Genau der Multiplier, der VOR diesem
                 * Treffer eingesammelt wurde.
                 */
                hitMultiplier =
                        activeMultiplier;
            }
        }


        /*
         * Wenn der Gegner nie tatsächlich getroffen wurde,
         * darf ein zufällig durchflogener X2/X3 für die
         * Bewertung keine Rolle spielen.
         */
        if (!targetHit) {

            hitMultiplier =
                    1;
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


    // =========================================================
    // MULTIPLIER AT POINT
    // =========================================================

    private int getHighestMultiplierAtPoint(
            TrajectoryPoint point,
            List<DamageMultiplier> damageMultipliers
    ) {

        int highest =
                1;


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


    // =========================================================
    // MULTIPLIER SEGMENT COLLISION
    // =========================================================

    private int getHighestMultiplierOnSegment(
            TrajectoryPoint start,
            TrajectoryPoint end,
            List<DamageMultiplier> damageMultipliers
    ) {

        int highest =
                1;


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


    // =========================================================
    // SEGMENT / CIRCLE
    // =========================================================

    private boolean segmentIntersectsCircle(
            double startX,
            double startY,

            double endX,
            double endY,

            double centerX,
            double centerY,

            double radius
    ) {

        double dx =
                endX
                -
                startX;


        double dy =
                endY
                -
                startY;


        double lengthSquared =
                dx * dx
                +
                dy * dy;


        /*
         * Degeneriertes Segment.
         */
        if (lengthSquared
                <=
            0.000001) {


            double pointDx =
                    startX
                    -
                    centerX;


            double pointDy =
                    startY
                    -
                    centerY;


            return pointDx * pointDx
                    +
                    pointDy * pointDy
                    <=
                    radius * radius;
        }


        /*
         * Projektion des Kreismittelpunkts auf
         * das Trajectory-Segment.
         */
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
                closestX
                -
                centerX;


        double distanceY =
                closestY
                -
                centerY;


        return distanceX * distanceX
                +
                distanceY * distanceY
                <=
                radius * radius;
    }
}