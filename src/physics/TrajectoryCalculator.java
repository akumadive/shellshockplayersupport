package physics;

import model.Bumper;
import model.PlayerState;
import model.Shot;
import model.TerrainProfile;
import model.TrajectoryPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class TrajectoryCalculator {

    private final PhysicsModel physicsModel;


    // =========================================================
    // GENERAL PHYSICS
    // =========================================================

    private static final double TIME_STEP =
            0.05;

    private static final int MAX_STEPS =
            10000;

    private static final double MAX_SCREEN_Y =
            1200.0;


    // =========================================================
    // TERRAIN
    // =========================================================

    private static final double SHOOTER_IGNORE_RADIUS =
            14.0;

    private static final double COLLISION_SAMPLE_DISTANCE =
            0.5;


    // =========================================================
    // BUMPER
    // =========================================================

    /*
     * Effektive Kollisionsdicke eines Line-Bumpers.
     *
     * Der Detector liefert die Mittellinie.
     * Das echte Objekt besitzt aber eine sichtbare Dicke.
     */
    private static final double LINE_BUMPER_RADIUS =
            5.0;


    /*
     * Circle Detector liefert bereits den ungefähren Radius.
     * Kleine zusätzliche Toleranz verhindert, dass ein
     * Projektil knapp durch Anti-Aliasing hindurchläuft.
     */
    private static final double CIRCLE_BUMPER_EXTRA_RADIUS =
            2.0;


    /*
     * Nach einem Bounce wird das Projektil minimal von der
     * Oberfläche weggeschoben.
     *
     * Sonst könnte es im nächsten Physics-Step denselben
     * Bumper sofort noch einmal treffen.
     */
    private static final double BUMPER_PUSH_OUT =
            1.5;


    /*
     * Derselbe Bumper wird für einige Schritte ignoriert,
     * nachdem wir ihn getroffen haben.
     */
    private static final int BUMPER_COOLDOWN_STEPS =
            4;


    /*
     * Schutz vor absurden Endlosschleifen zwischen
     * mehreren Bumpern.
     */
    private static final int MAX_BOUNCES =
            20;


    public TrajectoryCalculator(
            PhysicsModel physicsModel
    ) {

        this.physicsModel =
                physicsModel;
    }


    // =========================================================
    // OLD API
    // =========================================================

    public List<TrajectoryPoint> calculate(
            PlayerState shooter,
            Shot shot,
            TerrainProfile terrain
    ) {

        return calculate(
                shooter,
                shot,
                terrain,
                0.0,
                Collections.emptyList()
        );
    }


    public List<TrajectoryPoint> calculate(
            PlayerState shooter,
            Shot shot,
            TerrainProfile terrain,
            double wind
    ) {

        return calculate(
                shooter,
                shot,
                terrain,
                wind,
                Collections.emptyList()
        );
    }


    // =========================================================
    // CALCULATE WITH BUMPERS
    // =========================================================

    public List<TrajectoryPoint> calculate(
            PlayerState shooter,
            Shot shot,
            TerrainProfile terrain,
            double wind,
            List<Bumper> bumpers
    ) {

        List<TrajectoryPoint> points =
                new ArrayList<>();


        if (bumpers == null) {

            bumpers =
                    Collections.emptyList();
        }


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
                Math.cos(
                        angleRadians
                )
                *
                velocity;


        double vy =
                -Math.sin(
                        angleRadians
                )
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
        // BUMPER STATE
        // =====================================================

        Bumper lastHitBumper =
                null;

        int bumperCooldown =
                0;

        int bounceCount =
                0;


        // =====================================================
        // SIMULATION
        // =====================================================

        double time =
                0.0;


        for (int i = 0;
             i < MAX_STEPS;
             i++) {


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
            // BUMPER COLLISION
            // =================================================

            BumperCollision bumperCollision =
                    findBumperCollision(
                            x,
                            y,
                            time,

                            nextX,
                            nextY,
                            nextTime,

                            bumpers,

                            bumperCooldown > 0
                                    ?
                                    lastHitBumper
                                    :
                                    null
                    );


            if (bumperCollision != null) {


                // =============================================
                // MOVE TO COLLISION
                // =============================================

                x =
                        bumperCollision.x;

                y =
                        bumperCollision.y;

                time =
                        bumperCollision.time;


                points.add(
                        new TrajectoryPoint(
                                x,
                                y,
                                time
                        )
                );


                // =============================================
                // REFLECT VELOCITY
                // =============================================

                /*
                 * Reflection:
                 *
                 * v' = v - 2(v dot n)n
                 */

                double dot =
                        vx
                        *
                        bumperCollision.normalX
                        +
                        vy
                        *
                        bumperCollision.normalY;


                vx =
                        vx
                        -
                        2.0
                        *
                        dot
                        *
                        bumperCollision.normalX;


                vy =
                        vy
                        -
                        2.0
                        *
                        dot
                        *
                        bumperCollision.normalY;


                // =============================================
                // PUSH PROJECTILE AWAY
                // =============================================

                x +=
                        bumperCollision.normalX
                        *
                        BUMPER_PUSH_OUT;


                y +=
                        bumperCollision.normalY
                        *
                        BUMPER_PUSH_OUT;


                lastHitBumper =
                        bumperCollision.bumper;


                bumperCooldown =
                        BUMPER_COOLDOWN_STEPS;


                bounceCount++;


                if (bounceCount
                        >
                    MAX_BOUNCES) {

                    break;
                }


                /*
                 * Nach dem Bounce beginnt der nächste
                 * Physics-Step von der Kollisionsstelle.
                 *
                 * Gravity/Wind werden unten weiterhin
                 * normal angewendet.
                 */

            } else {


                // =============================================
                // TERRAIN COLLISION
                // =============================================

                TerrainCollision terrainCollision =
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


                if (terrainCollision != null) {


                    points.add(
                            new TrajectoryPoint(
                                    terrainCollision.x,
                                    terrainCollision.y,
                                    terrainCollision.time
                            )
                    );


                    break;
                }


                // =============================================
                // APPLY POSITION
                // =============================================

                x =
                        nextX;

                y =
                        nextY;

                time =
                        nextTime;
            }


            // =================================================
            // BUMPER COOLDOWN
            // =================================================

            if (bumperCooldown > 0) {

                bumperCooldown--;


                if (bumperCooldown == 0) {

                    lastHitBumper =
                            null;
                }
            }


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

            vx +=
                    windAcceleration
                    *
                    TIME_STEP;


            vy +=
                    physicsModel.getGravity()
                    *
                    TIME_STEP;
        }


        return points;
    }


    // =========================================================
    // BUMPER COLLISION
    // =========================================================

    private BumperCollision findBumperCollision(
            double startX,
            double startY,
            double startTime,

            double endX,
            double endY,
            double endTime,

            List<Bumper> bumpers,

            Bumper ignoredBumper
    ) {

        if (bumpers == null ||
            bumpers.isEmpty()) {

            return null;
        }


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
                    dx
                    *
                    progress;


            double sampleY =
                    startY
                    +
                    dy
                    *
                    progress;


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


            for (Bumper bumper :
                    bumpers) {


                if (bumper
                        ==
                    ignoredBumper) {

                    continue;
                }


                BumperCollision collision;


                if (bumper.getType()
                        ==
                    Bumper.BumperType.LINE) {


                    collision =
                            checkLineBumper(
                                    sampleX,
                                    sampleY,
                                    sampleTime,
                                    bumper
                            );


                } else {


                    collision =
                            checkCircleBumper(
                                    sampleX,
                                    sampleY,
                                    sampleTime,
                                    bumper
                            );
                }


                if (collision != null) {

                    return collision;
                }
            }
        }


        return null;
    }


    // =========================================================
    // LINE BUMPER
    // =========================================================

    private BumperCollision checkLineBumper(
            double x,
            double y,
            double time,
            Bumper bumper
    ) {

        double ax =
                bumper.getStartX();

        double ay =
                bumper.getStartY();


        double bx =
                bumper.getEndX();

        double by =
                bumper.getEndY();


        double lineX =
                bx
                -
                ax;


        double lineY =
                by
                -
                ay;


        double lengthSquared =
                lineX * lineX
                +
                lineY * lineY;


        if (lengthSquared
                <
            0.000001) {

            return null;
        }


        // =====================================================
        // CLOSEST POINT ON LINE SEGMENT
        // =====================================================

        double projection =
                (
                        (x - ax) * lineX
                        +
                        (y - ay) * lineY
                )
                /
                lengthSquared;


        projection =
                Math.max(
                        0.0,
                        Math.min(
                                1.0,
                                projection
                        )
                );


        double closestX =
                ax
                +
                lineX
                *
                projection;


        double closestY =
                ay
                +
                lineY
                *
                projection;


        double diffX =
                x
                -
                closestX;


        double diffY =
                y
                -
                closestY;


        double distanceSquared =
                diffX * diffX
                +
                diffY * diffY;


        if (distanceSquared
                >
            LINE_BUMPER_RADIUS
            *
            LINE_BUMPER_RADIUS) {

            return null;
        }


        // =====================================================
        // NORMAL
        // =====================================================

        double normalX;

        double normalY;


        double distance =
                Math.sqrt(
                        distanceSquared
                );


        if (distance
                >
            0.000001) {


            /*
             * Normal vom Bumper zum Projektil.
             */
            normalX =
                    diffX
                    /
                    distance;


            normalY =
                    diffY
                    /
                    distance;


        } else {


            /*
             * Projektil liegt exakt auf der Mittellinie.
             *
             * Dann normale Senkrechte zur Bumper-Achse.
             */
            double lineLength =
                    Math.sqrt(
                            lengthSquared
                    );


            normalX =
                    -lineY
                    /
                    lineLength;


            normalY =
                    lineX
                    /
                    lineLength;
        }


        return new BumperCollision(
                bumper,

                x,
                y,
                time,

                normalX,
                normalY
        );
    }


    // =========================================================
    // CIRCLE BUMPER
    // =========================================================

    private BumperCollision checkCircleBumper(
            double x,
            double y,
            double time,
            Bumper bumper
    ) {

        double radius =
                bumper.getRadius()
                +
                CIRCLE_BUMPER_EXTRA_RADIUS;


        double dx =
                x
                -
                bumper.getCenterX();


        double dy =
                y
                -
                bumper.getCenterY();


        double distanceSquared =
                dx * dx
                +
                dy * dy;


        if (distanceSquared
                >
            radius * radius) {

            return null;
        }


        double distance =
                Math.sqrt(
                        distanceSquared
                );


        double normalX;

        double normalY;


        if (distance
                >
            0.000001) {


            normalX =
                    dx
                    /
                    distance;


            normalY =
                    dy
                    /
                    distance;


        } else {


            normalX =
                    0.0;

            normalY =
                    -1.0;
        }


        return new BumperCollision(
                bumper,

                x,
                y,
                time,

                normalX,
                normalY
        );
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


            if (sampleX < 0 ||
                sampleX >= terrain.getWidth()) {

                continue;
            }


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
    // TERRAIN TEST
    // =========================================================

    private boolean hitsTerrain(
            double x,
            double y,
            TerrainProfile terrain
    ) {

        int terrainX =
                (int) Math.floor(
                        x
                );


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


        return y
                >=
                terrainY;
    }


    // =========================================================
    // INTERNAL BUMPER COLLISION
    // =========================================================

    private static class BumperCollision {

        private final Bumper bumper;

        private final double x;
        private final double y;
        private final double time;

        private final double normalX;
        private final double normalY;


        private BumperCollision(
                Bumper bumper,

                double x,
                double y,
                double time,

                double normalX,
                double normalY
        ) {

            this.bumper =
                    bumper;

            this.x =
                    x;

            this.y =
                    y;

            this.time =
                    time;

            this.normalX =
                    normalX;

            this.normalY =
                    normalY;
        }
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

            this.x =
                    x;

            this.y =
                    y;

            this.time =
                    time;
        }
    }
}