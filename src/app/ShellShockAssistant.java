package app;

import capture.CaptureRegion;
import capture.ScreenCapture;

import model.PlayerState;
import model.Shot;
import model.ShotResult;
import model.TerrainProfile;
import model.TrajectoryPoint;

import physics.PhysicsModel;
import physics.ShotOptimizer;
import physics.TrajectoryCalculator;

import util.ImageUtils;

import vision.Blob;
import vision.BlobDetector;
import vision.PlayerDetector;
import vision.TerrainDetector;
import vision.WindDetector;
import vision.WindDetector.WindDirection;

import java.awt.image.BufferedImage;
import java.util.List;


public class ShellShockAssistant {

    public static void main(String[] args) {

        try {

            // =====================================================
            // TEMPORARY WIND STRENGTH
            // =====================================================

            /*
             * Noch genau EIN Schritt manuell.
             *
             * Die Richtung wird bereits erkannt.
             * Die 50 wird nach unserem nächsten
             * Schritt durch automatische
             * Ziffernerkennung ersetzt.
             */
            double windStrength = 50.0;


            // =====================================================
            // SCREEN CAPTURE
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


            ImageUtils.saveImage(
                    screenshot,
                    "data/screenshots/original.png"
            );


            // =====================================================
            // WIND DETECTION
            // =====================================================

            WindDetector windDetector =
                    new WindDetector();


            WindDirection windDirection =
                    windDetector.detectDirection(
                            screenshot
                    );


            // =====================================================
            // WIND DEBUG IMAGE
            // =====================================================

            BufferedImage windDebug =
                    windDetector.createDebugImage(
                            screenshot
                    );


            ImageUtils.saveImage(
                    windDebug,
                    "data/screenshots/wind_debug.png"
            );


            // =====================================================
            // SIGNED WIND VALUE
            // =====================================================

            double wind;


            if (windDirection
                    ==
                WindDirection.LEFT) {

                wind =
                        -Math.abs(
                                windStrength
                        );

            } else if (windDirection
                    ==
                       WindDirection.RIGHT) {

                wind =
                        Math.abs(
                                windStrength
                        );

            } else {

                /*
                 * Falls die Richtung nicht sicher
                 * erkannt wird, rechnen wir lieber
                 * ohne Wind als mit falschem Wind.
                 */
                wind = 0.0;
            }


            System.out.println(
                    "Wind direction: "
                    + windDirection
            );


            System.out.println(
                    "Temporary wind strength: "
                    + windStrength
            );


            System.out.println(
                    "Wind used by physics: "
                    + wind
            );


            // =====================================================
            // BLOB DEBUG
            // =====================================================

            BlobDetector blobDetector =
                    new BlobDetector();


            List<Blob> blobs =
                    blobDetector.detectBlobs(
                            screenshot
                    );


            BufferedImage blobDebug =
                    ImageUtils.drawBlobMarkers(
                            screenshot,
                            blobs
                    );


            ImageUtils.saveImage(
                    blobDebug,
                    "data/screenshots/blob_debug.png"
            );


            // =====================================================
            // PLAYER DETECTION
            // =====================================================

            PlayerDetector playerDetector =
                    new PlayerDetector();


            List<PlayerState> players =
                    playerDetector.detectPlayers(
                            screenshot
                    );


            BufferedImage playerDebug =
                    ImageUtils.drawPlayerMarkers(
                            screenshot,
                            players
                    );


            ImageUtils.saveImage(
                    playerDebug,
                    "data/screenshots/player_debug.png"
            );


            // =====================================================
            // TERRAIN DETECTION
            // =====================================================

            TerrainDetector terrainDetector =
                    new TerrainDetector();


            TerrainProfile terrain =
                    terrainDetector.detectTerrain(
                            screenshot
                    );


            BufferedImage terrainDebug =
                    ImageUtils.drawTerrain(
                            screenshot,
                            terrain
                    );


            ImageUtils.saveImage(
                    terrainDebug,
                    "data/screenshots/terrain_debug.png"
            );


            // =====================================================
            // FIND SELF
            // =====================================================

            PlayerState self = null;


            for (PlayerState player : players) {

                if (player.getType()
                        ==
                    PlayerState.PlayerType.SELF) {

                    self = player;

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
            // PHYSICS
            // =====================================================

            PhysicsModel physicsModel =
                    new PhysicsModel();


            TrajectoryCalculator calculator =
                    new TrajectoryCalculator(
                            physicsModel
                    );


            ShotOptimizer optimizer =
                    new ShotOptimizer(
                            calculator
                    );


            // =====================================================
            // FIND BEST SHOT FOR EVERY ENEMY
            // =====================================================

            ShotResult overallBest = null;


            for (PlayerState player : players) {

                if (player.getType()
                        !=
                    PlayerState.PlayerType.ENEMY) {

                    continue;
                }


                ShotResult result =
                        optimizer.findBestShot(
                                self,
                                player,
                                terrain,
                                wind
                        );


                if (result == null) {

                    continue;
                }


                System.out.println();

                System.out.println(
                        "===== TARGET ====="
                );


                System.out.println(
                        "Target: "
                        + player.getX()
                        + ", "
                        + player.getY()
                );


                System.out.println(
                        "Power: "
                        + result
                                .getShot()
                                .getPower()
                );


                System.out.println(
                        "Angle: "
                        + result
                                .getShot()
                                .getAngle()
                );


                System.out.printf(
                        "Miss Distance: %.2f px%n",
                        result
                                .getClosestDistance()
                );


                if (overallBest == null ||
                    result.getClosestDistance()
                    <
                    overallBest.getClosestDistance()) {

                    overallBest =
                            result;
                }
            }


            // =====================================================
            // DRAW BEST TRAJECTORY
            // =====================================================

            if (overallBest != null) {

                Shot bestShot =
                        overallBest.getShot();


                List<TrajectoryPoint> bestTrajectory =
                        calculator.calculate(
                                self,
                                bestShot,
                                terrain,
                                wind
                        );


                BufferedImage trajectoryDebug =
                        ImageUtils.drawTrajectory(
                                screenshot,
                                bestTrajectory
                        );


                ImageUtils.saveImage(
                        trajectoryDebug,
                        "data/screenshots/trajectory_debug.png"
                );


                // =================================================
                // BEST SHOT OUTPUT
                // =================================================

                System.out.println();

                System.out.println(
                        "=============================="
                );


                System.out.println(
                        "BEST SHOT"
                );


                System.out.println(
                        "=============================="
                );


                System.out.println(
                        "Wind direction: "
                        + windDirection
                );


                System.out.println(
                        "Wind: "
                        + wind
                );


                System.out.println(
                        "Power: "
                        + bestShot.getPower()
                );


                System.out.println(
                        "Angle: "
                        + bestShot.getAngle()
                );


                System.out.println(
                        "Target: "
                        + overallBest
                                .getTarget()
                                .getX()
                        + ", "
                        + overallBest
                                .getTarget()
                                .getY()
                );


                System.out.printf(
                        "Miss Distance: %.2f px%n",
                        overallBest
                                .getClosestDistance()
                );


            } else {

                System.out.println(
                        "Kein gültiger Shot gefunden."
                );
            }


            // =====================================================
            // DEBUG OUTPUT
            // =====================================================

            System.out.println();

            System.out.println(
                    "=============================="
            );


            System.out.println(
                    "DEBUG"
            );


            System.out.println(
                    "=============================="
            );


            System.out.println(
                    "Blobs gefunden: "
                    + blobs.size()
            );


            System.out.println(
                    "Players gefunden: "
                    + players.size()
            );


            for (PlayerState player : players) {

                System.out.println(
                        player.getType()
                        + " -> x="
                        + player.getX()
                        + ", y="
                        + player.getY()
                );
            }


            System.out.println(
                    "Terrain erfolgreich analysiert."
            );


            System.out.println(
                    "Wind-Debug gespeichert unter "
                    + "data/screenshots/wind_debug.png"
            );


        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}