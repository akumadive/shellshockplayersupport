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
     * Weiterhin exakt dein Referenzschuss:
     *
     * Power 55
     * Angle 33
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


    // =========================================================
    // FORCE MODELS
    // =========================================================

    /*
     * Wir testen diesmal verschiedene SHAPES.
     *
     * Alle sind echte radiale Beschleunigung.
     *
     * Unterschied:
     * Wie verändert sich die Kraft mit der Entfernung?
     */
    private enum ForceModel {

        /*
         * Glocke:
         *
         * Rand   -> 0
         * Mitte  -> Maximum
         * Core   -> wieder schwächer
         */
        BELL,

        /*
         * Glocke, aber Richtung Core bleibt
         * eine kleine Restkraft übrig.
         */
        BELL_WITH_FLOOR,

        /*
         * Kraft steigt nach innen an,
         * wird aber bei einem Maximum gedeckelt.
         */
        CAPPED_LINEAR
    }


    // =========================================================
    // VARIANTS
    // =========================================================

    private static final CalibrationVariant[] VARIANTS = {

            new CalibrationVariant(
                    "BELL S8",
                    ForceModel.BELL,
                    8.0,
                    Color.WHITE
            ),

            new CalibrationVariant(
                    "BELL S12",
                    ForceModel.BELL,
                    12.0,
                    Color.YELLOW
            ),

            new CalibrationVariant(
                    "BELL S16",
                    ForceModel.BELL,
                    16.0,
                    Color.ORANGE
            ),

            new CalibrationVariant(
                    "FLOOR S12",
                    ForceModel.BELL_WITH_FLOOR,
                    12.0,
                    Color.CYAN
            ),

            new CalibrationVariant(
                    "FLOOR S16",
                    ForceModel.BELL_WITH_FLOOR,
                    16.0,
                    Color.GREEN
            ),

            new CalibrationVariant(
                    "CAP S12",
                    ForceModel.CAPPED_LINEAR,
                    12.0,
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


            System.out.println();

            System.out.println(
                    "=============================="
            );

            System.out.println(
                    "BLACK HOLE CALIBRATION V3"
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
            // PHYSICS MODEL
            // =====================================================

            PhysicsModel physicsModel =
                    new PhysicsModel();


            // =====================================================
            // RUN VARIANTS
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
                    "data/screenshots/black_hole_calibration_v3.png"
            );


            System.out.println();

            System.out.println(
                    "black_hole_calibration_v3.png gespeichert."
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
            // GRAVITY
            // =================================================

            vx +=
                    blackHoleAcceleration[0]
                    *
                    TIME_STEP;


            vy +=
                    physicsModel.getGravity()
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
    // FORCE
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
            // NORMALIZED DISTANCE
            // =================================================

            /*
             * q:
             *
             * 0 = Center
             * 1 = Influence-Rand
             */
            double q =
                    distance
                    /
                    influenceRadius;


            q =
                    clamp(
                            q,
                            0.0,
                            1.0
                    );


            double factor;


            // =================================================
            // BELL
            // =================================================

            if (variant.model
                    ==
                ForceModel.BELL) {


                /*
                 * 0 am Center
                 * 1 ungefähr bei q = 0.5
                 * 0 am Rand
                 */
                factor =
                        4.0
                        *
                        q
                        *
                        (
                                1.0
                                -
                                q
                        );


            // =================================================
            // BELL WITH FLOOR
            // =================================================

            } else if (variant.model
                       ==
                       ForceModel.BELL_WITH_FLOOR) {


                /*
                 * Gleiches Bell-Modell,
                 * aber nahe Core bleibt ca. 20 %
                 * der Maximalwirkung bestehen.
                 *
                 * Damit vermeiden wir:
                 *
                 * "direkt am Core plötzlich gar keine Kraft".
                 */
                double bell =
                        4.0
                        *
                        q
                        *
                        (
                                1.0
                                -
                                q
                        );


                factor =
                        0.20
                        +
                        0.80
                        *
                        bell;


                /*
                 * Am äußeren Rand trotzdem auf 0 ziehen.
                 */
                factor *=
                        (
                                1.0
                                -
                                Math.pow(
                                        q,
                                        6.0
                                )
                        );


            // =================================================
            // CAPPED LINEAR
            // =================================================

            } else {


                /*
                 * Wie das alte lineare Modell:
                 *
                 * weiter innen -> stärker
                 *
                 * ABER:
                 * maximal 55 %.
                 *
                 * Dadurch gibt es keinen explosiven
                 * Nahbereich mehr.
                 */
                double inward =
                        1.0
                        -
                        q;


                factor =
                        Math.min(
                                inward,
                                0.55
                        );
            }


            factor =
                    Math.max(
                            0.0,
                            factor
                    );


            // =================================================
            // ACCELERATION
            // =================================================

            double acceleration =
                    variant.strength
                    *
                    factor;


            // =================================================
            // DIRECTION TO CENTER
            // =================================================

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
                        2.0 * a
                );


        double t2 =
                (
                        -b
                        +
                        sqrt
                )
                /
                (
                        2.0 * a
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
        // BLACK HOLE DEBUG
        // =====================================================

        for (BlackHole blackHole :
                blackHoles) {


            int cx =
                    (int) Math.round(
                            blackHole.getCenterX()
                    );


            int cy =
                    (int) Math.round(
                            blackHole.getCenterY()
                    );


            int influence =
                    (int) Math.round(
                            blackHole.getInfluenceRadius()
                    );


            int core =
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
                    cx - influence,
                    cy - influence,

                    influence * 2,
                    influence * 2
            );


            graphics.setColor(
                    Color.RED
            );


            graphics.drawOval(
                    cx - core,
                    cy - core,

                    core * 2,
                    core * 2
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
                        16
                )
        );


        int legendX =
                24;


        int legendY =
                94;


        graphics.setColor(
                Color.BLACK
        );


        graphics.fillRect(
                legendX - 12,
                legendY - 34,

                255,
                VARIANTS.length * 27 + 50
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
                32;


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

        private final ForceModel model;

        private final double strength;

        private final Color color;


        private CalibrationVariant(
                String name,
                ForceModel model,
                double strength,
                Color color
        ) {

            this.name =
                    name;


            this.model =
                    model;


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