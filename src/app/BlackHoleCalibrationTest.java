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

    /*
     * Dein echter Referenzschuss.
     */
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
            2500;


    /*
     * Wir vergleichen diesmal NICHT verschiedene Exponenten
     * des alten Modells.
     *
     * Stattdessen:
     *
     * LINEAR:
     * alter S20-Test als Referenz
     *
     * BELL:
     * neue Kraftkurve
     *
     *   Außenrand -> 0
     *   mittlerer Bereich -> maximale Kraft
     *   Richtung Kern -> wieder schwächer
     *
     * Der schwarze Core bleibt trotzdem tödlich.
     */
    private static final CalibrationVariant[] VARIANTS = {

            new CalibrationVariant(
                    "OLD S20",
                    ForceModel.OLD_LINEAR,
                    20.0,
                    Color.WHITE
            ),

            new CalibrationVariant(
                    "BELL S15",
                    ForceModel.BELL,
                    15.0,
                    Color.YELLOW
            ),

            new CalibrationVariant(
                    "BELL S20",
                    ForceModel.BELL,
                    20.0,
                    Color.ORANGE
            ),

            new CalibrationVariant(
                    "BELL S25",
                    ForceModel.BELL,
                    25.0,
                    Color.CYAN
            ),

            new CalibrationVariant(
                    "BELL S30",
                    ForceModel.BELL,
                    30.0,
                    Color.GREEN
            ),

            new CalibrationVariant(
                    "BELL S40",
                    ForceModel.BELL,
                    40.0,
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
            // PLAYERS
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


            System.out.println();


            System.out.println(
                    "=============================="
            );


            System.out.println(
                    "BLACK HOLE CALIBRATION V2"
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
                    "Power: "
                    +
                    TEST_POWER
                    +
                    " Angle: "
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
                            results,
                            blackHoles
                    );


            ImageUtils.saveImage(
                    debug,
                    "data/screenshots/black_hole_calibration_v2.png"
            );


            System.out.println();


            System.out.println(
                    "black_hole_calibration_v2.png gespeichert."
            );


        } catch (Exception e) {


            e.printStackTrace();
        }
    }


    // =========================================================
    // SIMULATE
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
            // ALREADY INSIDE CORE
            // =================================================

            if (isInsideAnyCore(
                    x,
                    y,
                    blackHoles
            )) {


                break;
            }


            // =================================================
            // BLACK HOLE FORCE
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
            // BLACK HOLE ACCELERATION
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
            // CORE SEGMENT COLLISION
            // =================================================

            CoreCollision coreCollision =
                    findCoreCollision(
                            x,
                            y,

                            nextX,
                            nextY,

                            blackHoles
                    );


            if (coreCollision != null) {


                points.add(
                        new TrajectoryPoint(
                                coreCollision.x,
                                coreCollision.y,

                                time
                                +
                                TIME_STEP
                                *
                                coreCollision.segmentFraction
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

            if (x < -300 ||
                x > 2300 ||
                y < -600 ||
                y > 1300) {


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


            double forceFactor;


            if (variant.forceModel
                    ==
                ForceModel.OLD_LINEAR) {


                /*
                 * ALTER ANSATZ:
                 *
                 * Rand   -> 0
                 * Center -> 1
                 *
                 * Genau dieser Ansatz hat bei S20
                 * den zu engen Orbit erzeugt.
                 */
                double normalized =
                        1.0
                        -
                        distance
                        /
                        influenceRadius;


                forceFactor =
                        normalized;


            } else {


                /*
                 * NEUER BELL-ANSATZ
                 *
                 * q:
                 *
                 * 0 = Center
                 * 1 = Influence-Rand
                 *
                 *
                 *       force
                 *
                 *         ^
                 *         |       /\
                 *         |      /  \
                 *         |     /    \
                 *         |____/______\____> q
                 *             0.5      1
                 *
                 *
                 * Formel:
                 *
                 * 4 * q * (1-q)
                 *
                 *
                 * q=1.0 -> 0
                 * q=0.5 -> 1
                 * q=0.0 -> 0
                 *
                 *
                 * Dadurch steigt die Kraft beim Eintritt
                 * zunächst an, explodiert aber NICHT mehr
                 * Richtung Core.
                 */
                double q =
                        distance
                        /
                        influenceRadius;


                forceFactor =
                        4.0
                        *
                        q
                        *
                        (
                                1.0
                                -
                                q
                        );


                forceFactor =
                        Math.max(
                                0.0,
                                Math.min(
                                        1.0,
                                        forceFactor
                                )
                        );
            }


            double acceleration =
                    variant.strength
                    *
                    forceFactor;


            /*
             * Richtung bleibt radial zum Mittelpunkt.
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
    // CORE
    // =========================================================

    private static boolean isInsideAnyCore(
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


            CoreCollision collision =
                    intersectCircle(
                            startX,
                            startY,

                            endX,
                            endY,

                            blackHole.getCenterX(),
                            blackHole.getCenterY(),

                            blackHole.getCoreRadius()
                    );


            if (collision == null) {

                continue;
            }


            if (best == null ||
                collision.segmentFraction
                        <
                best.segmentFraction) {


                best =
                        collision;
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
                dx
                *
                t,

                startY
                +
                dy
                *
                t,

                t
        );
    }


    // =========================================================
    // DRAW
    // =========================================================

    private static BufferedImage drawCalibration(
            BufferedImage source,
            List<CalibrationResult> results,
            List<BlackHole> blackHoles
    ) {

        BufferedImage result =
                new BufferedImage(
                        source.getWidth(),
                        source.getHeight(),
                        BufferedImage.TYPE_INT_ARGB
                );


        Graphics2D graphics =
                result.createGraphics();


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
        // BLACK HOLES
        // =====================================================

        graphics.setStroke(
                new BasicStroke(
                        1.5f
                )
        );


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


        for (CalibrationResult calibrationResult :
                results) {


            graphics.setColor(
                    calibrationResult.variant.color
            );


            List<TrajectoryPoint> trajectory =
                    calibrationResult.trajectory;


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
                        16
                )
        );


        int legendX =
                25;


        int legendY =
                95;


        graphics.setColor(
                Color.BLACK
        );


        graphics.fillRect(
                legendX - 12,
                legendY - 32,

                245,
                VARIANTS.length * 27 + 48
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
                31;


        for (CalibrationVariant variant :
                VARIANTS) {


            graphics.setColor(
                    variant.color
            );


            graphics.drawLine(
                    legendX,
                    legendY - 5,

                    legendX + 32,
                    legendY - 5
            );


            graphics.drawString(
                    variant.name,
                    legendX + 43,
                    legendY
            );


            legendY +=
                    27;
        }


        graphics.dispose();


        return result;
    }


    // =========================================================
    // TYPES
    // =========================================================

    private enum ForceModel {

        OLD_LINEAR,

        BELL
    }


    private static class CalibrationVariant {

        private final String name;

        private final ForceModel forceModel;

        private final double strength;

        private final Color color;


        private CalibrationVariant(
                String name,
                ForceModel forceModel,
                double strength,
                Color color
        ) {

            this.name =
                    name;


            this.forceModel =
                    forceModel;


            this.strength =
                    strength;


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

        private final double segmentFraction;


        private CoreCollision(
                double x,
                double y,
                double segmentFraction
        ) {

            this.x =
                    x;


            this.y =
                    y;


            this.segmentFraction =
                    segmentFraction;
        }
    }
}