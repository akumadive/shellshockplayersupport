package physics;

import model.BlackHole;
import model.Bumper;
import model.PlayerState;
import model.Portal;
import model.PortalPair;
import model.Shot;
import model.TerrainProfile;
import model.TrajectoryPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class TrajectoryCalculator {

    private final PhysicsModel physicsModel;


    // =========================================================
    // GENERAL
    // =========================================================

    private static final double TIME_STEP =
            0.05;

    private static final int MAX_STEPS =
            10000;

    private static final double MAX_SCREEN_Y =
            1200.0;

    private static final double COLLISION_SAMPLE_DISTANCE =
            0.5;

    private static final double EVENT_EPSILON =
            0.000001;

    private static final int MAX_EVENTS_PER_STEP =
            16;


    // =========================================================
    // TERRAIN
    // =========================================================

    private static final double SHOOTER_IGNORE_RADIUS =
            14.0;


    // =========================================================
    // BUMPER
    // =========================================================

    private static final double LINE_BUMPER_RADIUS =
            5.0;

    private static final double CIRCLE_BUMPER_EXTRA_RADIUS =
            2.0;

    private static final double BUMPER_PUSH_OUT =
            1.5;

    private static final int BUMPER_COOLDOWN_STEPS =
            4;

    private static final int MAX_BOUNCES =
            20;


    // =========================================================
    // BLACK HOLE
    // =========================================================

    /*
     * ERSTER Kalibrierwert.
     *
     * Das ist aktuell der einzige Wert, den wir nach deinem
     * nächsten Test wahrscheinlich noch verändern müssen.
     *
     * Wir vergleichen unsere berechnete Kurve danach mit
     * deinen echten gestrichelten ShellShock-Trajectories.
     */
    private static final double BLACK_HOLE_MAX_ACCELERATION =
            8.0;


    /*
     * Stärke:
     *
     * Rand des Influence-Radius:
     * praktisch 0
     *
     * Richtung Core:
     * zunehmend stärker
     *
     * exponent = 2:
     * weicher Einstieg, starke Kurve innen.
     */
    private static final double BLACK_HOLE_FORCE_EXPONENT =
            2.0;


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
                Collections.emptyList(),
                Collections.emptyList(),
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
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }


    public List<TrajectoryPoint> calculate(
            PlayerState shooter,
            Shot shot,
            TerrainProfile terrain,
            double wind,
            List<Bumper> bumpers
    ) {

        return calculate(
                shooter,
                shot,
                terrain,
                wind,
                bumpers,
                Collections.emptyList(),
                Collections.emptyList()
        );
    }


    public List<TrajectoryPoint> calculate(
            PlayerState shooter,
            Shot shot,
            TerrainProfile terrain,
            double wind,
            List<Bumper> bumpers,
            List<PortalPair> portalPairs
    ) {

        return calculate(
                shooter,
                shot,
                terrain,
                wind,
                bumpers,
                portalPairs,
                Collections.emptyList()
        );
    }


    // =========================================================
    // FULL CALCULATION
    // =========================================================

    public List<TrajectoryPoint> calculate(
            PlayerState shooter,
            Shot shot,
            TerrainProfile terrain,
            double wind,
            List<Bumper> bumpers,
            List<PortalPair> portalPairs,
            List<BlackHole> blackHoles
    ) {

        if (bumpers == null) {

            bumpers =
                    Collections.emptyList();
        }


        if (portalPairs == null) {

            portalPairs =
                    Collections.emptyList();
        }


        if (blackHoles == null) {

            blackHoles =
                    Collections.emptyList();
        }


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

        double time =
                0.0;


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
        // PORTAL STATE
        // =====================================================

        PortalPair lockedPortalPair =
                null;


        // =====================================================
        // SIMULATION
        // =====================================================

        for (int step = 0;
             step < MAX_STEPS;
             step++) {


            points.add(
                    new TrajectoryPoint(
                            x,
                            y,
                            time
                    )
            );


            double remainingTime =
                    TIME_STEP;


            boolean destroyed =
                    false;


            int eventsThisStep =
                    0;


            while (remainingTime > EVENT_EPSILON &&
                   eventsThisStep < MAX_EVENTS_PER_STEP) {


                eventsThisStep++;


                // =============================================
                // PORTAL RE-ARM
                // =============================================

                if (lockedPortalPair != null &&
                    isOutsidePortalPair(
                            x,
                            y,
                            lockedPortalPair
                    )) {


                    lockedPortalPair =
                            null;
                }


                // =============================================
                // PREDICT NEXT POSITION
                // =============================================

                double nextX =
                        x
                        +
                        vx
                        *
                        remainingTime;


                double nextY =
                        y
                        +
                        vy
                        *
                        remainingTime;


                double nextTime =
                        time
                        +
                        remainingTime;


                // =============================================
                // COLLISION CANDIDATES
                // =============================================

                PortalCollision portalCollision =
                        findFirstPortalCollision(
                                x,
                                y,
                                time,

                                nextX,
                                nextY,
                                nextTime,

                                portalPairs,
                                lockedPortalPair
                        );


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


                BlackHoleCollision blackHoleCollision =
                        findFirstBlackHoleCoreCollision(
                                x,
                                y,
                                time,

                                nextX,
                                nextY,
                                nextTime,

                                blackHoles
                        );


                // =============================================
                // EVENT TIMES
                // =============================================

                double portalTime =
                        portalCollision == null
                                ?
                                Double.POSITIVE_INFINITY
                                :
                                portalCollision.time;


                double bumperTime =
                        bumperCollision == null
                                ?
                                Double.POSITIVE_INFINITY
                                :
                                bumperCollision.time;


                double terrainTime =
                        terrainCollision == null
                                ?
                                Double.POSITIVE_INFINITY
                                :
                                terrainCollision.time;


                double blackHoleTime =
                        blackHoleCollision == null
                                ?
                                Double.POSITIVE_INFINITY
                                :
                                blackHoleCollision.time;


                // =============================================
                // BLACK HOLE CORE
                // =============================================

                /*
                 * Core hat harte Kollisionslogik:
                 *
                 * Projektil wird verschluckt.
                 *
                 * Kein Bounce.
                 * Kein Durchflug.
                 * Keine Fortsetzung.
                 */
                if (blackHoleCollision != null &&
                    blackHoleTime <= portalTime &&
                    blackHoleTime <= bumperTime &&
                    blackHoleTime <= terrainTime) {


                    points.add(
                            new TrajectoryPoint(
                                    blackHoleCollision.x,
                                    blackHoleCollision.y,
                                    blackHoleCollision.time
                            )
                    );


                    destroyed =
                            true;


                    break;
                }


                // =============================================
                // PORTAL
                // =============================================

                if (portalCollision != null &&
                    portalTime <= bumperTime &&
                    portalTime <= terrainTime &&
                    portalTime <= blackHoleTime) {


                    double elapsed =
                            Math.max(
                                    0.0,
                                    portalCollision.time
                                    -
                                    time
                            );


                    remainingTime =
                            Math.max(
                                    0.0,
                                    remainingTime
                                    -
                                    elapsed
                            );


                    /*
                     * Exact entry point.
                     */
                    points.add(
                            new TrajectoryPoint(
                                    portalCollision.x,
                                    portalCollision.y,
                                    portalCollision.time
                            )
                    );


                    Portal destination =
                            portalCollision.pair
                                    .getOtherPortal(
                                            portalCollision.portal
                                    );


                    if (destination == null) {


                        x =
                                portalCollision.x;

                        y =
                                portalCollision.y;

                        time =
                                portalCollision.time;


                        continue;
                    }


                    /*
                     * Relative position stays identical.
                     */
                    double offsetX =
                            portalCollision.x
                            -
                            portalCollision.portal
                                    .getCenterX();


                    double offsetY =
                            portalCollision.y
                            -
                            portalCollision.portal
                                    .getCenterY();


                    x =
                            destination.getCenterX()
                            +
                            offsetX;


                    y =
                            destination.getCenterY()
                            +
                            offsetY;


                    time =
                            portalCollision.time;


                    /*
                     * vx / vy stay exactly unchanged.
                     */


                    lockedPortalPair =
                            portalCollision.pair;


                    /*
                     * Exact exit point.
                     *
                     * Same timestamp means:
                     * this visual teleport is NOT a physical line.
                     */
                    points.add(
                            new TrajectoryPoint(
                                    x,
                                    y,
                                    time
                            )
                    );


                    continue;
                }


                // =============================================
                // BUMPER
                // =============================================

                if (bumperCollision != null &&
                    bumperTime <= terrainTime &&
                    bumperTime <= blackHoleTime) {


                    double elapsed =
                            Math.max(
                                    0.0,
                                    bumperCollision.time
                                    -
                                    time
                            );


                    remainingTime =
                            Math.max(
                                    0.0,
                                    remainingTime
                                    -
                                    elapsed
                            );


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


                    if (bounceCount > MAX_BOUNCES) {


                        destroyed =
                                true;


                        break;
                    }


                    continue;
                }


                // =============================================
                // TERRAIN
                // =============================================

                if (terrainCollision != null &&
                    terrainTime <= blackHoleTime) {


                    points.add(
                            new TrajectoryPoint(
                                    terrainCollision.x,
                                    terrainCollision.y,
                                    terrainCollision.time
                            )
                    );


                    destroyed =
                            true;


                    break;
                }


                // =============================================
                // NO EVENT
                // =============================================

                x =
                        nextX;

                y =
                        nextY;

                time =
                        nextTime;


                remainingTime =
                        0.0;
            }


            if (destroyed) {

                break;
            }


            if (eventsThisStep >= MAX_EVENTS_PER_STEP) {

                break;
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
            // PORTAL RE-ARM
            // =================================================

            if (lockedPortalPair != null &&
                isOutsidePortalPair(
                        x,
                        y,
                        lockedPortalPair
                )) {


                lockedPortalPair =
                        null;
            }


            // =================================================
            // OUT OF BOUNDS
            // =================================================

            if (x < 0 ||
                x >= terrain.getWidth() ||
                y > MAX_SCREEN_Y) {


                break;
            }


            // =================================================
            // NORMAL ACCELERATION
            // =================================================

            vx +=
                    windAcceleration
                    *
                    TIME_STEP;


            vy +=
                    physicsModel.getGravity()
                    *
                    TIME_STEP;


            // =================================================
            // BLACK HOLE GRAVITY
            // =================================================

            double[] blackHoleAcceleration =
                    getBlackHoleAcceleration(
                            x,
                            y,
                            blackHoles
                    );


            vx +=
                    blackHoleAcceleration[0]
                    *
                    TIME_STEP;


            vy +=
                    blackHoleAcceleration[1]
                    *
                    TIME_STEP;
        }


        return points;
    }


    // =========================================================
    // BLACK HOLE FORCE
    // =========================================================

    private double[] getBlackHoleAcceleration(
            double x,
            double y,
            List<BlackHole> blackHoles
    ) {

        double totalAx =
                0.0;

        double totalAy =
                0.0;


        /*
         * Mehrere Black Holes:
         *
         * Kräfte addieren sich.
         */
        for (BlackHole blackHole :
                blackHoles) {


            double dx =
                    blackHole.getCenterX()
                    -
                    x;


            double dy =
                    blackHole.getCenterY()
                    -
                    y;


            double distanceSquared =
                    dx * dx
                    +
                    dy * dy;


            if (distanceSquared
                    <=
                EVENT_EPSILON) {


                continue;
            }


            double distance =
                    Math.sqrt(
                            distanceSquared
                    );


            double influenceRadius =
                    blackHole.getInfluenceRadius();


            if (distance >= influenceRadius) {

                continue;
            }


            /*
             * 0.0 = äußerster Rand
             * 1.0 = Zentrum
             */
            double normalized =
                    1.0
                    -
                    distance
                    /
                    influenceRadius;


            double acceleration =
                    BLACK_HOLE_MAX_ACCELERATION
                    *
                    Math.pow(
                            normalized,
                            BLACK_HOLE_FORCE_EXPONENT
                    );


            /*
             * Normalisierte Richtung zum Center.
             */
            double nx =
                    dx
                    /
                    distance;


            double ny =
                    dy
                    /
                    distance;


            totalAx +=
                    nx
                    *
                    acceleration;


            totalAy +=
                    ny
                    *
                    acceleration;
        }


        return new double[]{
                totalAx,
                totalAy
        };
    }


    // =========================================================
    // BLACK HOLE CORE COLLISION
    // =========================================================

    private BlackHoleCollision findFirstBlackHoleCoreCollision(
            double startX,
            double startY,
            double startTime,

            double endX,
            double endY,
            double endTime,

            List<BlackHole> blackHoles
    ) {

        BlackHoleCollision best =
                null;


        for (BlackHole blackHole :
                blackHoles) {


            BlackHoleCollision collision =
                    intersectBlackHoleCore(
                            startX,
                            startY,
                            startTime,

                            endX,
                            endY,
                            endTime,

                            blackHole
                    );


            if (collision != null &&
                (
                        best == null
                        ||
                        collision.time
                                <
                        best.time
                )) {


                best =
                        collision;
            }
        }


        return best;
    }


    private BlackHoleCollision intersectBlackHoleCore(
            double startX,
            double startY,
            double startTime,

            double endX,
            double endY,
            double endTime,

            BlackHole blackHole
    ) {

        /*
         * Falls wir bereits im Core starten:
         * sofort zerstört.
         */
        if (blackHole.containsCore(
                startX,
                startY
        )) {


            return new BlackHoleCollision(
                    blackHole,
                    startX,
                    startY,
                    startTime
            );
        }


        double dx =
                endX
                -
                startX;


        double dy =
                endY
                -
                startY;


        double fx =
                startX
                -
                blackHole.getCenterX();


        double fy =
                startY
                -
                blackHole.getCenterY();


        double a =
                dx * dx
                +
                dy * dy;


        if (a <= EVENT_EPSILON) {

            return null;
        }


        double b =
                2.0
                *
                (
                        fx * dx
                        +
                        fy * dy
                );


        double radius =
                blackHole.getCoreRadius();


        double c =
                fx * fx
                +
                fy * fy
                -
                radius * radius;


        double discriminant =
                b * b
                -
                4.0
                *
                a
                *
                c;


        if (discriminant < 0.0) {

            return null;
        }


        double sqrt =
                Math.sqrt(
                        discriminant
                );


        double t1 =
                (
                        -b
                        -
                        sqrt
                )
                /
                (
                        2.0
                        *
                        a
                );


        double t2 =
                (
                        -b
                        +
                        sqrt
                )
                /
                (
                        2.0
                        *
                        a
                );


        double hitFraction =
                Double.POSITIVE_INFINITY;


        if (t1 >= 0.0 &&
            t1 <= 1.0) {


            hitFraction =
                    t1;
        }


        if (t2 >= 0.0 &&
            t2 <= 1.0 &&
            t2 < hitFraction) {


            hitFraction =
                    t2;
        }


        if (!Double.isFinite(
                hitFraction
        )) {


            return null;
        }


        double hitX =
                startX
                +
                dx
                *
                hitFraction;


        double hitY =
                startY
                +
                dy
                *
                hitFraction;


        double hitTime =
                startTime
                +
                (
                        endTime
                        -
                        startTime
                )
                *
                hitFraction;


        return new BlackHoleCollision(
                blackHole,
                hitX,
                hitY,
                hitTime
        );
    }


    // =========================================================
    // PORTALS
    // =========================================================

    private PortalCollision findFirstPortalCollision(
            double startX,
            double startY,
            double startTime,

            double endX,
            double endY,
            double endTime,

            List<PortalPair> portalPairs,
            PortalPair lockedPortalPair
    ) {

        PortalCollision best =
                null;


        for (PortalPair pair :
                portalPairs) {


            if (pair == lockedPortalPair) {

                continue;
            }


            PortalCollision orangeHit =
                    intersectPortal(
                            startX,
                            startY,
                            startTime,

                            endX,
                            endY,
                            endTime,

                            pair,
                            pair.getOrangePortal()
                    );


            PortalCollision blueHit =
                    intersectPortal(
                            startX,
                            startY,
                            startTime,

                            endX,
                            endY,
                            endTime,

                            pair,
                            pair.getBluePortal()
                    );


            best =
                    earlierPortalCollision(
                            best,
                            orangeHit
                    );


            best =
                    earlierPortalCollision(
                            best,
                            blueHit
                    );
        }


        return best;
    }


    private PortalCollision earlierPortalCollision(
            PortalCollision current,
            PortalCollision candidate
    ) {

        if (candidate == null) {

            return current;
        }


        if (current == null ||
            candidate.time < current.time) {


            return candidate;
        }


        return current;
    }


    private PortalCollision intersectPortal(
            double startX,
            double startY,
            double startTime,

            double endX,
            double endY,
            double endTime,

            PortalPair pair,
            Portal portal
    ) {

        /*
         * Kein Retrigger solange wir bereits im Portal stehen.
         */
        if (portal.contains(
                startX,
                startY
        )) {


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


        double fx =
                startX
                -
                portal.getCenterX();


        double fy =
                startY
                -
                portal.getCenterY();


        double a =
                dx * dx
                +
                dy * dy;


        if (a <= EVENT_EPSILON) {

            return null;
        }


        double b =
                2.0
                *
                (
                        fx * dx
                        +
                        fy * dy
                );


        double radius =
                portal.getRadius();


        double c =
                fx * fx
                +
                fy * fy
                -
                radius * radius;


        double discriminant =
                b * b
                -
                4.0
                *
                a
                *
                c;


        if (discriminant < 0.0) {

            return null;
        }


        double sqrt =
                Math.sqrt(
                        discriminant
                );


        double t1 =
                (
                        -b
                        -
                        sqrt
                )
                /
                (
                        2.0
                        *
                        a
                );


        double t2 =
                (
                        -b
                        +
                        sqrt
                )
                /
                (
                        2.0
                        *
                        a
                );


        double hitFraction =
                Double.POSITIVE_INFINITY;


        if (t1 >= 0.0 &&
            t1 <= 1.0) {


            hitFraction =
                    t1;
        }


        if (t2 >= 0.0 &&
            t2 <= 1.0 &&
            t2 < hitFraction) {


            hitFraction =
                    t2;
        }


        if (!Double.isFinite(
                hitFraction
        )) {


            return null;
        }


        double hitX =
                startX
                +
                dx
                *
                hitFraction;


        double hitY =
                startY
                +
                dy
                *
                hitFraction;


        double hitTime =
                startTime
                +
                (
                        endTime
                        -
                        startTime
                )
                *
                hitFraction;


        return new PortalCollision(
                pair,
                portal,
                hitX,
                hitY,
                hitTime
        );
    }


    private boolean isOutsidePortalPair(
            double x,
            double y,
            PortalPair pair
    ) {

        return !pair.getOrangePortal()
                        .contains(
                                x,
                                y
                        )

                &&

                !pair.getBluePortal()
                        .contains(
                                x,
                                y
                        );
    }


    // =========================================================
    // BUMPERS
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


                if (bumper == ignoredBumper) {

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


        if (lengthSquared < EVENT_EPSILON) {

            return null;
        }


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


        double normalX;

        double normalY;


        double distance =
                Math.sqrt(
                        distanceSquared
                );


        if (distance > EVENT_EPSILON) {


            normalX =
                    diffX
                    /
                    distance;


            normalY =
                    diffY
                    /
                    distance;


        } else {


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


        if (distance > EVENT_EPSILON) {


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
    // TERRAIN
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


        return dx * dx
                +
                dy * dy
                <=
                SHOOTER_IGNORE_RADIUS
                *
                SHOOTER_IGNORE_RADIUS;
    }


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


        return y
                >=
                terrain.getY(
                        terrainX
                );
    }


    // =========================================================
    // INTERNAL COLLISION CLASSES
    // =========================================================

    private static class BlackHoleCollision {

        private final BlackHole blackHole;

        private final double x;
        private final double y;
        private final double time;


        private BlackHoleCollision(
                BlackHole blackHole,
                double x,
                double y,
                double time
        ) {

            this.blackHole =
                    blackHole;

            this.x =
                    x;

            this.y =
                    y;

            this.time =
                    time;
        }
    }


    private static class PortalCollision {

        private final PortalPair pair;
        private final Portal portal;

        private final double x;
        private final double y;
        private final double time;


        private PortalCollision(
                PortalPair pair,
                Portal portal,
                double x,
                double y,
                double time
        ) {

            this.pair =
                    pair;

            this.portal =
                    portal;

            this.x =
                    x;

            this.y =
                    y;

            this.time =
                    time;
        }
    }


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