package util;

import model.PlayerState;
import model.ShotResult;
import model.TerrainProfile;
import model.TrajectoryPoint;
import vision.Blob;

import javax.imageio.ImageIO;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import java.io.File;
import java.io.IOException;

import java.util.List;


public class ImageUtils {

    // =========================================================
    // SAVE IMAGE
    // =========================================================

    public static void saveImage(
            BufferedImage image,
            String path
    ) throws IOException {

        File file =
                new File(path);

        File parent =
                file.getParentFile();

        if (parent != null &&
            !parent.exists()) {

            parent.mkdirs();
        }

        ImageIO.write(
                image,
                "png",
                file
        );
    }


    // =========================================================
    // CROP
    // =========================================================

    public static BufferedImage crop(
            BufferedImage image,
            int x,
            int y,
            int width,
            int height
    ) {

        return image.getSubimage(
                x,
                y,
                width,
                height
        );
    }


    // =========================================================
    // PLAYER DEBUG
    // =========================================================

    public static BufferedImage drawPlayerMarkers(
            BufferedImage source,
            List<PlayerState> players
    ) {

        BufferedImage copy =
                copyImage(source);

        Graphics2D g =
                copy.createGraphics();

        prepareGraphics(g);

        for (PlayerState player :
                players) {

            int x =
                    player.getX();

            int y =
                    player.getY();

            g.drawOval(
                    x - 20,
                    y - 20,
                    40,
                    40
            );

            g.drawLine(
                    x - 25,
                    y,
                    x + 25,
                    y
            );

            g.drawLine(
                    x,
                    y - 25,
                    x,
                    y + 25
            );

            String text =
                    player.getType()
                    + " ("
                    + x
                    + ", "
                    + y
                    + ")";

            g.drawString(
                    text,
                    x + 25,
                    y
            );
        }

        g.dispose();

        return copy;
    }


    // =========================================================
    // BLOB DEBUG
    // =========================================================

    public static BufferedImage drawBlobMarkers(
            BufferedImage source,
            List<Blob> blobs
    ) {

        BufferedImage copy =
                copyImage(source);

        Graphics2D g =
                copy.createGraphics();

        prepareGraphics(g);

        for (Blob blob :
                blobs) {

            int x =
                    blob.getMinX();

            int y =
                    blob.getMinY();

            int width =
                    blob.getWidth();

            int height =
                    blob.getHeight();

            g.drawRect(
                    x,
                    y,
                    width,
                    height
            );

            String info =
                    width
                    + "x"
                    + height
                    + " | "
                    + blob.getPixelCount();

            g.drawString(
                    info,
                    x,
                    Math.max(
                            10,
                            y - 3
                    )
            );
        }

        g.dispose();

        return copy;
    }


    // =========================================================
    // TERRAIN DEBUG
    // =========================================================

    public static BufferedImage drawTerrain(
            BufferedImage source,
            TerrainProfile terrain
    ) {

        BufferedImage copy =
                copyImage(source);

        Graphics2D g =
                copy.createGraphics();

        prepareGraphics(g);

        for (int x = 0;
             x < terrain.getWidth();
             x++) {

            int y =
                    terrain.getY(x);

            if (y < 0) {

                continue;
            }

            g.fillRect(
                    x,
                    y - 1,
                    1,
                    3
            );
        }

        g.dispose();

        return copy;
    }


    // =========================================================
    // TRAJECTORY
    // =========================================================

    public static BufferedImage drawTrajectory(
            BufferedImage source,
            List<TrajectoryPoint> points
    ) {

        BufferedImage copy =
                copyImage(source);

        Graphics2D g =
                copy.createGraphics();

        prepareGraphics(g);

        for (TrajectoryPoint point :
                points) {

            int x =
                    (int) Math.round(
                            point.getX()
                    );

            int y =
                    (int) Math.round(
                            point.getY()
                    );

            if (x < 0 ||
                y < 0 ||
                x >= source.getWidth() ||
                y >= source.getHeight()) {

                continue;
            }

            g.fillOval(
                    x - 2,
                    y - 2,
                    4,
                    4
            );
        }

        g.dispose();

        return copy;
    }


    // =========================================================
    // COMPLETE SHOT RECOMMENDATION
    // =========================================================

    public static BufferedImage drawShotRecommendation(
            BufferedImage source,
            List<TrajectoryPoint> trajectory,
            ShotResult result,
            double wind
    ) {

        BufferedImage copy =
                copyImage(source);

        Graphics2D g =
                copy.createGraphics();

        prepareGraphics(g);

        // -----------------------------------------------------
        // TRAJECTORY
        // -----------------------------------------------------

        g.setStroke(
                new BasicStroke(2.0f)
        );

        for (TrajectoryPoint point :
                trajectory) {

            int x =
                    (int) Math.round(
                            point.getX()
                    );

            int y =
                    (int) Math.round(
                            point.getY()
                    );

            if (x < 0 ||
                y < 0 ||
                x >= source.getWidth() ||
                y >= source.getHeight()) {

                continue;
            }

            g.fillOval(
                    x - 2,
                    y - 2,
                    4,
                    4
            );
        }


        // -----------------------------------------------------
        // TARGET
        // -----------------------------------------------------

        PlayerState target =
                result.getTarget();

        int targetX =
                target.getX();

        int targetY =
                target.getY();

        g.setStroke(
                new BasicStroke(3.0f)
        );

        g.drawOval(
                targetX - 28,
                targetY - 28,
                56,
                56
        );

        g.drawLine(
                targetX - 35,
                targetY,
                targetX + 35,
                targetY
        );

        g.drawLine(
                targetX,
                targetY - 35,
                targetX,
                targetY + 35
        );


        // -----------------------------------------------------
        // IMPACT / CLOSEST POINT
        // -----------------------------------------------------

        int impactX =
                (int) Math.round(
                        result.getClosestX()
                );

        int impactY =
                (int) Math.round(
                        result.getClosestY()
                );

        g.fillOval(
                impactX - 5,
                impactY - 5,
                10,
                10
        );


        // -----------------------------------------------------
        // INFORMATION BOX
        // -----------------------------------------------------

        int boxX = 25;
        int boxY = 110;

        int boxWidth = 330;
        int boxHeight = 185;

        g.setColor(
                new Color(
                        0,
                        0,
                        0,
                        185
                )
        );

        g.fillRoundRect(
                boxX,
                boxY,
                boxWidth,
                boxHeight,
                20,
                20
        );

        g.setColor(
                Color.WHITE
        );

        g.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.BOLD,
                        22
                )
        );

        g.drawString(
                "BEST SHOT",
                boxX + 20,
                boxY + 35
        );

        g.setFont(
                new Font(
                        Font.MONOSPACED,
                        Font.PLAIN,
                        18
                )
        );

        int textX =
                boxX + 20;

        int textY =
                boxY + 70;

        int lineHeight =
                25;


        g.drawString(
                "Power : "
                + formatNumber(
                        result
                                .getShot()
                                .getPower()
                ),
                textX,
                textY
        );

        g.drawString(
                "Angle : "
                + formatNumber(
                        result
                                .getShot()
                                .getAngle()
                )
                + "°",
                textX,
                textY + lineHeight
        );

        g.drawString(
                "Wind  : "
                + formatSignedNumber(
                        wind
                ),
                textX,
                textY + lineHeight * 2
        );

        g.drawString(
                String.format(
                        "Error : %.2f px",
                        result
                                .getClosestDistance()
                ),
                textX,
                textY + lineHeight * 3
        );

        g.dispose();

        return copy;
    }


    // =========================================================
    // INTERNAL HELPERS
    // =========================================================

    private static BufferedImage copyImage(
            BufferedImage source
    ) {

        BufferedImage copy =
                new BufferedImage(
                        source.getWidth(),
                        source.getHeight(),
                        BufferedImage.TYPE_INT_ARGB
                );

        Graphics2D g =
                copy.createGraphics();

        g.drawImage(
                source,
                0,
                0,
                null
        );

        g.dispose();

        return copy;
    }


    private static void prepareGraphics(
            Graphics2D g
    ) {

        g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        g.setColor(
                Color.WHITE
        );

        g.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.PLAIN,
                        14
                )
        );
    }


    private static String formatNumber(
            double value
    ) {

        if (value ==
            Math.rint(value)) {

            return Integer.toString(
                    (int) value
            );
        }

        return String.format(
                "%.2f",
                value
        );
    }


    private static String formatSignedNumber(
            double value
    ) {

        if (value > 0) {

            return "+"
                    + formatNumber(
                            value
                    );
        }

        return formatNumber(
                value
        );
    }
}