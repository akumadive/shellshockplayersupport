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
    // TEST SHOT
    // =========================================================

    /*
     * Genau dein echter Vergleichsschuss:
     *
     * Power 55
     * Angle 33
     */
    private static final double TEST_POWER =
            55.0;

    private static final double TEST_ANGLE =
            33.0;


    // =========================================================
    // PHYSICS
    // =========================================================

    /*
     * Muss mit dem normalen Calculator übereinstimmen.
     */
    private static final double TIME_STEP =
            0.05;


    private static final int MAX_STEPS =
            2500;


    /*
     * Noch NICHT kalibriert.
     *
     * Wir testen mehrere Strength/Exponent-Kombinationen
     * gleichzeitig.
     */
private static final CalibrationVariant[] VARIANTS = {

        new CalibrationVariant(
                "S16 E1.0",
                16.0,
                1.0,
                Color.WHITE
        ),

        new CalibrationVariant(
                "S18 E1.0",
                18.0,
                1.0,
                Color.YELLOW
        ),

        new CalibrationVariant(
                "S20 E1.0",
                20.0,
                1.0,
                Color.ORANGE
        ),

        new CalibrationVariant(
                "S20 E1.25",
                20.0,
                1.25,
                Color.CYAN
        ),

        new CalibrationVariant(
                "S20 E1.5",
                20.0,
                1.5,
                Color.GREEN
        ),

        new CalibrationVariant(
                "S24 E1.5",
                24.0,
                1.5,
                Color.MAGENTA
        )
};;


    public static void main(String[] args) {

        try {


            // =====================================================
            // SCREENSHOT
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
            // SELF
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
                    "BLACK HOLE CALIBRATION"
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
                    "Test Shot: Power="
                    +
                    TEST_POWER
                    +
                    " Angle="
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
            // PHYSICS MODEL
            // =====================================================

            PhysicsModel physicsModel =
                    new PhysicsModel();


            // =====================================================
            // SIMULATE ALL VARIANTS
            // =====================================================

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


                System.out.println();


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
            // DEBUG IMAGE
            // =====================================================

            BufferedImage debug =
                    drawCalibration(
                            screenshot,
                            results,
                            blackHoles
                    );


            ImageUtils.saveImage(
                    debug,
                    "data/screenshots/black_hole_calibration.png"
            );


            System.out.println();


            System.out.println(
                    "black_hole_calibration.png gespeichert."
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
            // CORE HIT
            // =================================================

            if (isInsideAnyCore(
                    x,
                    y,
                    blackHoles
            )) {


                break;
            }


            // =================================================
            // BLACK HOLE ACCELERATION
            // =================================================

            double[] acceleration =
                    calculateBlackHoleAcceleration(
                            x,
                            y,
                            blackHoles,
                            variant.strength,
                            variant.exponent
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
                    acceleration[0]
                    *
                    TIME_STEP;


            vy +=
                    acceleration[1]
                    *
                    TIME_STEP;


            // =================================================
            // POSITION
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


            /*
             * Exakte Segment/Core-Prüfung.
             *
             * Sonst könnte ein schneller Schuss zwischen
             * zwei Simulationspunkten durch den schwarzen
             * Kern springen.
             */
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
            // SCREEN BOUNDS
            // =================================================

            if (x < -200 ||
                x > 2120 ||
                y < -500 ||
                y > 1200) {


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
            double strength,
            double exponent
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


            if (distanceSquared <= 0.000001) {

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


            /*
             * 0 am äußeren Rand
             * 1 am Center
             */
            double normalized =
                    1.0
                    -
                    distance
                    /
                    influenceRadius;


            double acceleration =
                    strength
                    *
                    Math.pow(
                            normalized,
                            exponent
                    );


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
    // CORE TEST
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
    // SEGMENT / CORE
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
        // BLACK HOLE RADII
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
                    calibrationResult
                            .variant
                            .color
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
                100;


        graphics.setColor(
                Color.BLACK
        );


        graphics.fillRect(
                legendX - 10,
                legendY - 30,
                230,
                VARIANTS.length * 26 + 45
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

                    legendX + 30,
                    legendY - 5
            );


            graphics.drawString(
                    variant.name,
                    legendX + 40,
                    legendY
            );


            legendY +=
                    26;
        }


        graphics.dispose();


        return result;
    }


    // =========================================================
    // INTERNAL TYPES
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