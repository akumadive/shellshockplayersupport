package app;

import capture.CaptureRegion;
import capture.ScreenCapture;

import model.BlackHole;
import model.PlayerState;
import model.TrajectoryPoint;

import physics.PhysicsModel;

import util.ImageUtils;

import vision.BlackHoleDetector;
import vision.PlayerDetector;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import java.awt.image.BufferedImage;

import java.util.ArrayList;
import java.util.List;


public class BlackHoleCalibrationTest {

    // =========================================================
    // REFERENCE SHOT
    // =========================================================

    private static final double TEST_POWER =
            55.0;

    private static final double TEST_ANGLE =
            33.0;


    // =========================================================
    // SIMULATION
    // =========================================================

    private static final double TIME_STEP =
            0.05;

    private static final int MAX_STEPS =
            1500;


    private static final double MIN_DEBUG_X =
            -100.0;

    private static final double MAX_DEBUG_X =
            2020.0;

    private static final double MIN_DEBUG_Y =
            -200.0;

    private static final double MAX_DEBUG_Y =
            950.0;


    // =========================================================
    // TEST VARIANTS
    // =========================================================

    /*
     * Jetzt testen wir erstmals ein echtes
     * inverse-distance-power Modell:
     *
     *          strength
     * a = --------------------
     *       normalizedR ^ p
     *
     *
     * normalizedR =
     *
     * distance / influenceRadius
     *
     *
     * p = 2.0 entspricht dem klassischen
     * inverse-square-artigen Verhalten.
     *
     *
     * Die Strength-Werte sehen dadurch numerisch
     * komplett anders aus als bei unseren alten Tests.
     */
    private static final CalibrationVariant[] VARIANTS = {

            new CalibrationVariant(
                    "INV2 S0.50",
                    0.50,
                    2.0,
                    Color.WHITE
            ),

            new CalibrationVariant(
                    "INV2 S0.75",
                    0.75,
                    2.0,
                    Color.YELLOW
            ),

            new CalibrationVariant(
                    "INV2 S1.00",
                    1.00,
                    2.0,
                    Color.ORANGE
            ),

            new CalibrationVariant(
                    "INV2 S1.25",
                    1.25,
                    2.0,
                    Color.CYAN
            ),

            new CalibrationVariant(
                    "INV1.5 S1.0",
                    1.00,
                    1.5,
                    Color.GREEN
            ),

            new CalibrationVariant(
                    "INV2.5 S0.5",
                    0.50,
                    2.5,
                    Color.MAGENTA
            )
    };


    public static void main(String[] args) {

        try {

            // =====================================================
            // CAPTURE
            // =====================================================

            CaptureRegion region =
                    new CaptureRegion(
                            0,
                            0,
                            1920,
                            1080
                    );


            ScreenCapture screenCapture =
                    new ScreenCapture(
                            region
                    );


            BufferedImage screenshot =
                    screenCapture.capture();


            // =====================================================
            // PLAYER
            // =====================================================

            PlayerDetector playerDetector =
                    new PlayerDetector();


            List<PlayerState> players =
                    playerDetector.detectPlayers(
                            screenshot
                    );


            PlayerState self =
                    null;


            for (PlayerState player :
                    players) {


                if (player.getType()
                        ==
                    PlayerState.PlayerType.SELF) {


                    self =
                            player;


                    break;
                }
            }


            if (self == null) {


                System.out.println(
                        "SELF konnte nicht erkannt werden."
                );


                return;
            }


            // =====================================================
            // BLACK HOLES
            // =====================================================

            BlackHoleDetector blackHoleDetector =
                    new BlackHoleDetector();


            List<BlackHole> blackHoles =
                    blackHoleDetector.detect(
                            screenshot
                    );


            // =====================================================
            // OUTPUT
            // =====================================================

            System.out.println();

            System.out.println(
                    "=============================="
            );

            System.out.println(
                    "BLACK HOLE CALIBRATION V5"
            );

            System.out.println(
                    "=============================="
            );


            System.out.println(
                    "SELF: "
                    +
                    self.getX()
                    +
                    ", "
                    +
                    self.getY()
            );


            System.out.println(
                    "Reference Shot: "
                    +
                    TEST_POWER
                    +
                    " / "
                    +
                    TEST_ANGLE
            );


            System.out.println(
                    "Black Holes: "
                    +
                    blackHoles.size()
            );


            for (BlackHole blackHole :
                    blackHoles) {


                System.out.println(
                        blackHole
                );
            }


            // =====================================================
            // PHYSICS
            // =====================================================

            PhysicsModel physicsModel =
                    new PhysicsModel();


            List<CalibrationResult> results =
                    new ArrayList<>();


            for (CalibrationVariant variant :
                    VARIANTS) {


                List<TrajectoryPoint> trajectory =
                        simulate(
                                self,
                                blackHoles,
                                physicsModel,
                                variant
                        );


                results.add(
                        new CalibrationResult(
                                variant,
                                trajectory
                        )
                );


                System.out.println(
                        variant.name
                        +
                        " -> "
                        +
                        trajectory.size()
                        +
                        " Punkte"
                );
            }


            // =====================================================
            // DRAW
            // =====================================================

            BufferedImage debug =
                    drawCalibration(
                            screenshot,
                            blackHoles,
                            results
                    );


            ImageUtils.saveImage(
                    debug,
                    "data/screenshots/black_hole_calibration_v5.png"
            );


            System.out.println();

            System.out.println(
                    "black_hole_calibration_v5.png gespeichert."
            );


        } catch (Exception e) {


            e.printStackTrace();
        }
    }


    // =========================================================
    // SIMULATION
    // =========================================================

    private static List<TrajectoryPoint> simulate(
            PlayerState shooter,
            List<BlackHole> blackHoles,
            PhysicsModel physicsModel,
            CalibrationVariant variant
    ) {

        List<TrajectoryPoint> points =
                new ArrayList<>();


        double angle =
                Math.toRadians(
                        TEST_ANGLE
                );


        double velocity =
                TEST_POWER
                *
                physicsModel.getPowerScale();


        double vx =
                Math.cos(
                        angle
                )
                *
                velocity;


        double vy =
                -Math.sin(
                        angle
                )
                *
                velocity;


        double x =
                shooter.getX();


        double y =
                shooter.getY();


        double time =
                0.0;


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


            // =================================================
            // CORE
            // =================================================

            if (insideAnyCore(
                    x,
                    y,
                    blackHoles
            )) {


                break;
            }


            // =================================================
            // BLACK HOLE ACCELERATION
            // =================================================

            double[] blackHoleAcceleration =
                    calculateBlackHoleAcceleration(
                            x,
                            y,
                            blackHoles,
                            variant
                    );


            // =================================================
            // NORMAL GRAVITY
            // =================================================

            vy +=
                    physicsModel.getGravity()
                    *
                    TIME_STEP;


            // =================================================
            // BLACK HOLE FORCE
            // =================================================

            vx +=
                    blackHoleAcceleration[0]
                    *
                    TIME_STEP;


            vy +=
                    blackHoleAcceleration[1]
                    *
                    TIME_STEP;


            // =================================================
            // NEXT POSITION
            // =================================================

            double nextX =
                    x
                    +
                    vx
                    *
                    TIME_STEP;


            double nextY =
                    y
                    +
                    vy
                    *
                    TIME_STEP;


            // =================================================
            // CORE COLLISION
            // =================================================

            CoreCollision collision =
                    findCoreCollision(
                            x,
                            y,
                            nextX,
                            nextY,
                            blackHoles
                    );


            if (collision != null) {


                points.add(
                        new TrajectoryPoint(
                                collision.x,
                                collision.y,

                                time
                                +
                                TIME_STEP
                                *
                                collision.fraction
                        )
                );


                break;
            }


            x =
                    nextX;


            y =
                    nextY;


            time +=
                    TIME_STEP;


            // =================================================
            // DEBUG BOUNDS
            // =================================================

            if (x < MIN_DEBUG_X ||
                x > MAX_DEBUG_X ||
                y < MIN_DEBUG_Y ||
                y > MAX_DEBUG_Y) {


                break;
            }
        }


        return points;
    }


    // =========================================================
    // BLACK HOLE FORCE
    // =========================================================

    private static double[] calculateBlackHoleAcceleration(
            double x,
            double y,
            List<BlackHole> blackHoles,
            CalibrationVariant variant
    ) {

        double totalAx =
                0.0;


        double totalAy =
                0.0;


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
                0.000001) {


                continue;
            }


            double distance =
                    Math.sqrt(
                            distanceSquared
                    );


            double influenceRadius =
                    blackHole.getInfluenceRadius();


            if (distance
                    >=
                influenceRadius) {


                continue;
            }


            // =================================================
            // NORMALIZED RADIUS
            // =================================================

            /*
             * q:
             *
             * 1.0 = äußerer Influence-Rand
             *
             * Richtung Core wird q kleiner.
             */
            double q =
                    distance
                    /
                    influenceRadius;


            /*
             * WICHTIG:
             *
             * Nicht bis q=0 laufen lassen.
             *
             * Wir begrenzen die maximale Kraft anhand
             * der Core-Größe.
             *
             * Das Projektil wird ohnehin zerstört,
             * sobald es den Core erreicht.
             */
            double coreQ =
                    blackHole.getCoreRadius()
                    /
                    influenceRadius;


            /*
             * Kraft-Singularity etwas VOR dem eigentlichen
             * Core vermeiden.
             */
            double minimumQ =
                    Math.max(
                            coreQ * 1.75,
                            0.12
                    );


            double effectiveQ =
                    Math.max(
                            q,
                            minimumQ
                    );


            // =================================================
            // INVERSE POWER
            // =================================================

            /*
             * Reine inverse Funktion hätte am Rand bereits
             * Strength.
             *
             * Wir wollen aber:
             *
             * influenceRadius -> exakt 0
             *
             * deshalb ziehen wir den Randwert 1 ab.
             *
             *
             * q = 1:
             *
             * 1 / 1^p - 1 = 0
             *
             *
             * q < 1:
             *
             * Kraft steigt zunehmend.
             */
            double inverse =
                    1.0
                    /
                    Math.pow(
                            effectiveQ,
                            variant.exponent
                    );


            double forceFactor =
                    inverse
                    -
                    1.0;


            // =================================================
            // SOFT EDGE
            // =================================================

            /*
             * Zusätzlich die äußersten 10 % weich einblenden.
             */
            double edgeFactor;


            if (q >= 0.90) {


                double t =
                        (
                                1.0
                                -
                                q
                        )
                        /
                        0.10;


                t =
                        clamp(
                                t,
                                0.0,
                                1.0
                        );


                edgeFactor =
                        t
                        *
                        t
                        *
                        (
                                3.0
                                -
                                2.0 * t
                        );


            } else {


                edgeFactor =
                        1.0;
            }


            forceFactor *=
                    edgeFactor;


            // =================================================
            // FINAL ACCELERATION
            // =================================================

            double acceleration =
                    variant.strength
                    *
                    forceFactor;


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
    // CORE
    // =========================================================

    private static boolean insideAnyCore(
            double x,
            double y,
            List<BlackHole> blackHoles
    ) {

        for (BlackHole blackHole :
                blackHoles) {


            if (blackHole.containsCore(
                    x,
                    y
            )) {


                return true;
            }
        }


        return false;
    }


    // =========================================================
    // CORE COLLISION
    // =========================================================

    private static CoreCollision findCoreCollision(
            double startX,
            double startY,
            double endX,
            double endY,
            List<BlackHole> blackHoles
    ) {

        CoreCollision best =
                null;


        for (BlackHole blackHole :
                blackHoles) {


            CoreCollision candidate =
                    intersectCircle(
                            startX,
                            startY,
                            endX,
                            endY,

                            blackHole.getCenterX(),
                            blackHole.getCenterY(),

                            blackHole.getCoreRadius()
                    );


            if (candidate != null &&
                (
                        best == null
                        ||
                        candidate.fraction
                                <
                        best.fraction
                )) {


                best =
                        candidate;
            }
        }


        return best;
    }


    private static CoreCollision intersectCircle(
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


        double fx =
                startX
                -
                centerX;


        double fy =
                startY
                -
                centerY;


        double a =
                dx * dx
                +
                dy * dy;


        if (a <= 0.000001) {


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


        double t =
                Double.POSITIVE_INFINITY;


        if (t1 >= 0.0 &&
            t1 <= 1.0) {


            t =
                    t1;
        }


        if (t2 >= 0.0 &&
            t2 <= 1.0 &&
            t2 < t) {


            t =
                    t2;
        }


        if (!Double.isFinite(
                t
        )) {


            return null;
        }


        return new CoreCollision(
                startX
                +
                dx * t,

                startY
                +
                dy * t,

                t
        );
    }


    // =========================================================
    // DRAW
    // =========================================================

    private static BufferedImage drawCalibration(
            BufferedImage source,
            List<BlackHole> blackHoles,
            List<CalibrationResult> results
    ) {

        BufferedImage output =
                new BufferedImage(
                        source.getWidth(),
                        source.getHeight(),
                        BufferedImage.TYPE_INT_ARGB
                );


        Graphics2D graphics =
                output.createGraphics();


        graphics.drawImage(
                source,
                0,
                0,
                null
        );


        graphics.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );


        // =====================================================
        // BLACK HOLE RADII
        // =====================================================

        for (BlackHole blackHole :
                blackHoles) {


            int centerX =
                    (int) Math.round(
                            blackHole.getCenterX()
                    );


            int centerY =
                    (int) Math.round(
                            blackHole.getCenterY()
                    );


            int influenceRadius =
                    (int) Math.round(
                            blackHole.getInfluenceRadius()
                    );


            int coreRadius =
                    (int) Math.round(
                            blackHole.getCoreRadius()
                    );


            graphics.setStroke(
                    new BasicStroke(
                            1.5f
                    )
            );


            graphics.setColor(
                    new Color(
                            0,
                            255,
                            255,
                            100
                    )
            );


            graphics.drawOval(
                    centerX - influenceRadius,
                    centerY - influenceRadius,

                    influenceRadius * 2,
                    influenceRadius * 2
            );


            graphics.setColor(
                    Color.RED
            );


            graphics.drawOval(
                    centerX - coreRadius,
                    centerY - coreRadius,

                    coreRadius * 2,
                    coreRadius * 2
            );
        }


        // =====================================================
        // TRAJECTORIES
        // =====================================================

        graphics.setStroke(
                new BasicStroke(
                        3.0f
                )
        );


        for (CalibrationResult result :
                results) {


            graphics.setColor(
                    result.variant.color
            );


            List<TrajectoryPoint> trajectory =
                    result.trajectory;


            for (int i = 1;
                 i < trajectory.size();
                 i++) {


                TrajectoryPoint previous =
                        trajectory.get(
                                i - 1
                        );


                TrajectoryPoint current =
                        trajectory.get(
                                i
                        );


                graphics.drawLine(
                        (int) Math.round(
                                previous.getX()
                        ),

                        (int) Math.round(
                                previous.getY()
                        ),

                        (int) Math.round(
                                current.getX()
                        ),

                        (int) Math.round(
                                current.getY()
                        )
                );
            }
        }


        // =====================================================
        // LEGEND
        // =====================================================

        graphics.setFont(
                new Font(
                        Font.MONOSPACED,
                        Font.BOLD,
                        15
                )
        );


        int legendX =
                20;

        int legendY =
                85;


        graphics.setColor(
                Color.BLACK
        );


        graphics.fillRect(
                legendX - 10,
                legendY - 30,

                230,
                VARIANTS.length * 25 + 50
        );


        graphics.setColor(
                Color.WHITE
        );


        graphics.drawString(
                "Power 55 / Angle 33",
                legendX,
                legendY
        );


        legendY +=
                30;


        for (CalibrationVariant variant :
                VARIANTS) {


            graphics.setColor(
                    variant.color
            );


            graphics.drawLine(
                    legendX,
                    legendY - 5,

                    legendX + 28,
                    legendY - 5
            );


            graphics.drawString(
                    variant.name,
                    legendX + 38,
                    legendY
            );


            legendY +=
                    25;
        }


        graphics.dispose();


        return output;
    }


    // =========================================================
    // HELPERS
    // =========================================================

    private static double clamp(
            double value,
            double min,
            double max
    ) {

        return Math.max(
                min,
                Math.min(
                        max,
                        value
                )
        );
    }


    // =========================================================
    // TYPES
    // =========================================================

    private static class CalibrationVariant {

        private final String name;

        private final double strength;

        private final double exponent;

        private final Color color;


        private CalibrationVariant(
                String name,
                double strength,
                double exponent,
                Color color
        ) {

            this.name =
                    name;

            this.strength =
                    strength;

            this.exponent =
                    exponent;

            this.color =
                    color;
        }
    }


    private static class CalibrationResult {

        private final CalibrationVariant variant;

        private final List<TrajectoryPoint> trajectory;


        private CalibrationResult(
                CalibrationVariant variant,
                List<TrajectoryPoint> trajectory
        ) {

            this.variant =
                    variant;

            this.trajectory =
                    trajectory;
        }
    }


    private static class CoreCollision {

        private final double x;

        private final double y;

        private final double fraction;


        private CoreCollision(
                double x,
                double y,
                double fraction
        ) {

            this.x =
                    x;

            this.y =
                    y;

            this.fraction =
                    fraction;
        }
    }
}