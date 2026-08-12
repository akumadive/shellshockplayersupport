package app;

import capture.CaptureRegion;
import capture.ScreenCapture;

import model.Bumper;
import model.DamageMultiplier;
import model.PlayerState;
import model.PortalPair;
import model.Shot;
import model.ShotResult;
import model.TerrainProfile;
import model.TrajectoryPoint;

import physics.PhysicsModel;
import physics.ShotOptimizer;
import physics.TrajectoryCalculator;

import util.BumperDebugRenderer;
import util.DamageMultiplierDebugRenderer;
import util.ImageUtils;

import vision.Blob;
import vision.BlobDetector;
import vision.BumperDetector;
import vision.DamageMultiplierDetector;
import vision.PlayerDetector;
import vision.PortalDetector;
import vision.TerrainDetector;
import vision.WindDetector;
import vision.WindDetector.WindResult;

import java.awt.image.BufferedImage;
import java.util.List;

public class ShellShockAssistant {

    public static void main(String[] args) {

        try {

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
            // WIND
            // =====================================================

            WindDetector windDetector =
                    new WindDetector();

            WindResult windResult =
                    windDetector.detect(
                            screenshot
                    );

            BufferedImage windDebug =
                    windDetector.createDebugImage(
                            screenshot
                    );

            ImageUtils.saveImage(
                    windDebug,
                    "data/screenshots/wind_debug.png"
            );

            double wind =
                    windResult.isValid()
                            ?
                            windResult.getSignedWind()
                            :
                            0.0;

            System.out.println();
            System.out.println("==============================");
            System.out.println("WIND");
            System.out.println("==============================");
            System.out.println("Direction: " + windResult.getDirection());
            System.out.println("Strength: " + windResult.getStrength());
            System.out.println("Valid: " + windResult.isValid());
            System.out.println("Physics Wind: " + wind);

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
            // PLAYERS
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
            // PORTALS
            // =====================================================

            /*
             * Portal detection MUST happen before terrain detection.
             *
             * The blue portal glow is cyan/blue enough to satisfy the
             * normal terrain color thresholds. The resulting portal
             * regions are therefore passed to TerrainDetector as masks.
             */
            PortalDetector portalDetector =
                    new PortalDetector();

            List<PortalPair> portalPairs =
                    portalDetector.detectPortalPairs(
                            screenshot
                    );

            System.out.println();
            System.out.println("==============================");
            System.out.println("PORTALS");
            System.out.println("==============================");
            System.out.println("Paare gefunden: " + portalPairs.size());

            for (PortalPair pair :
                    portalPairs) {

                System.out.println();
                System.out.println("PAIR " + pair.getId());
                System.out.println("Orange: " + pair.getOrangePortal());
                System.out.println("Blue:   " + pair.getBluePortal());
            }

            // =====================================================
            // TERRAIN
            // =====================================================

            TerrainDetector terrainDetector =
                    new TerrainDetector();

            TerrainProfile terrain =
                    terrainDetector.detectTerrain(
                            screenshot,
                            portalPairs
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
            // BUMPERS
            // =====================================================

            BumperDetector bumperDetector =
                    new BumperDetector();

            List<Bumper> bumpers =
                    bumperDetector.detectBumpers(
                            screenshot
                    );

            BumperDebugRenderer bumperDebugRenderer =
                    new BumperDebugRenderer();

            BufferedImage bumperDebug =
                    bumperDebugRenderer.drawBumpers(
                            screenshot,
                            bumpers
                    );

            ImageUtils.saveImage(
                    bumperDebug,
                    "data/screenshots/bumper_debug.png"
            );

            System.out.println();
            System.out.println("==============================");
            System.out.println("BUMPERS");
            System.out.println("==============================");
            System.out.println("Bumpers gefunden: " + bumpers.size());

            for (Bumper bumper : bumpers) {
                System.out.println(bumper);
            }

            // =====================================================
            // DAMAGE MULTIPLIERS
            // =====================================================

            DamageMultiplierDetector damageMultiplierDetector =
                    new DamageMultiplierDetector();

            List<DamageMultiplier> damageMultipliers =
                    damageMultiplierDetector.detect(
                            screenshot,
                            players
                    );

            DamageMultiplierDebugRenderer damageMultiplierDebugRenderer =
                    new DamageMultiplierDebugRenderer();

            BufferedImage damageMultiplierDebug =
                    damageMultiplierDebugRenderer.draw(
                            screenshot,
                            damageMultipliers
                    );

            ImageUtils.saveImage(
                    damageMultiplierDebug,
                    "data/screenshots/damage_multiplier_debug.png"
            );

            System.out.println();
            System.out.println("==============================");
            System.out.println("DAMAGE MULTIPLIERS");
            System.out.println("==============================");
            System.out.println("Gefunden: " + damageMultipliers.size());

            for (DamageMultiplier multiplier :
                    damageMultipliers) {

                System.out.println(multiplier);
            }

            // =====================================================
            // PORTALS
            // =====================================================

            PortalDetector portalDetector =
                    new PortalDetector();

            List<PortalPair> portalPairs =
                    portalDetector.detectPortalPairs(
                            screenshot
                    );

            System.out.println();
            System.out.println("==============================");
            System.out.println("PORTALS");
            System.out.println("==============================");
            System.out.println("Paare gefunden: " + portalPairs.size());

            for (PortalPair pair :
                    portalPairs) {

                System.out.println();
                System.out.println("PAIR " + pair.getId());
                System.out.println("Orange: " + pair.getOrangePortal());
                System.out.println("Blue:   " + pair.getBluePortal());
            }

            // =====================================================
            // FIND SELF
            // =====================================================

            PlayerState self = null;

            for (PlayerState player :
                    players) {

                if (player.getType()
                        ==
                    PlayerState.PlayerType.SELF) {

                    self = player;
                    break;
                }
            }

            if (self == null) {
                System.out.println("SELF konnte nicht erkannt werden.");
                return;
            }

            // =====================================================
            // PHYSICS / OPTIMIZER
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

            ShotResult overallBest = null;

            for (PlayerState player :
                    players) {

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
                                wind,
                                bumpers,
                                damageMultipliers,
                                portalPairs
                        );

                if (result == null) {
                    continue;
                }

                System.out.println();
                System.out.println("===== TARGET =====");
                System.out.println(
                        "Target: "
                        +
                        player.getX()
                        +
                        ", "
                        +
                        player.getY()
                );

                System.out.println(
                        "Power: "
                        +
                        result.getShot()
                                .getPower()
                );

                System.out.println(
                        "Angle: "
                        +
                        result.getShot()
                                .getAngle()
                );

                System.out.printf(
                        "Miss Distance: %.2f px%n",
                        result.getClosestDistance()
                );

                System.out.println(
                        "Damage Multiplier: X"
                        +
                        result.getDamageMultiplier()
                );

                if (optimizer.isBetterResult(
                        result,
                        overallBest
                )) {

                    overallBest = result;
                }
            }

            // =====================================================
            // BEST TRAJECTORY
            // =====================================================

            if (overallBest != null) {

                Shot bestShot =
                        overallBest.getShot();

                List<TrajectoryPoint> bestTrajectory =
                        calculator.calculate(
                                self,
                                bestShot,
                                terrain,
                                wind,
                                bumpers,
                                portalPairs
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

                System.out.println();
                System.out.println("==============================");
                System.out.println("BEST SHOT");
                System.out.println("==============================");
                System.out.println("Wind: " + wind);
                System.out.println("Power: " + bestShot.getPower());
                System.out.println("Angle: " + bestShot.getAngle());

                System.out.println(
                        "Target: "
                        +
                        overallBest.getTarget()
                                .getX()
                        +
                        ", "
                        +
                        overallBest.getTarget()
                                .getY()
                );

                System.out.printf(
                        "Miss Distance: %.2f px%n",
                        overallBest.getClosestDistance()
                );

                System.out.println(
                        "Damage Multiplier: X"
                        +
                        overallBest.getDamageMultiplier()
                );

            } else {

                System.out.println("Kein gültiger Shot gefunden.");
            }

            // =====================================================
            // GENERAL DEBUG
            // =====================================================

            System.out.println();
            System.out.println("==============================");
            System.out.println("DEBUG");
            System.out.println("==============================");
            System.out.println("Blobs gefunden: " + blobs.size());
            System.out.println("Players gefunden: " + players.size());
            System.out.println("Bumpers gefunden: " + bumpers.size());
            System.out.println(
                    "Damage Multipliers gefunden: "
                    +
                    damageMultipliers.size()
            );

            System.out.println(
                    "Portal-Paare gefunden: "
                    +
                    portalPairs.size()
            );

            for (PlayerState player :
                    players) {

                System.out.println(
                        player.getType()
                        +
                        " -> x="
                        +
                        player.getX()
                        +
                        ", y="
                        +
                        player.getY()
                );
            }

            System.out.println("Terrain erfolgreich analysiert.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}