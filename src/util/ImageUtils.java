package util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class ImageUtils {

    public static void saveImage(BufferedImage image, String path) throws IOException {

        File file = new File(path);

        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        ImageIO.write(image, "png", file);
    }

    public static BufferedImage crop(
            BufferedImage image,
            int x,
            int y,
            int width,
            int height
    ) {
        return image.getSubimage(x, y, width, height);
    }
    public static BufferedImage drawPlayerMarkers(
        BufferedImage source,
        java.util.List<model.PlayerState> players
) {

    BufferedImage copy = new BufferedImage(
            source.getWidth(),
            source.getHeight(),
            BufferedImage.TYPE_INT_ARGB
    );

    java.awt.Graphics2D g = copy.createGraphics();
    g.drawImage(source, 0, 0, null);

    for (model.PlayerState player : players) {

        int x = player.getX();
        int y = player.getY();

        g.drawOval(
                x - 20,
                y - 20,
                40,
                40
        );

        g.drawLine(x - 25, y, x + 25, y);
        g.drawLine(x, y - 25, x, y + 25);

        String text =
                player.getType() +
                " (" +
                x + ", " +
                y + ")";

        g.drawString(
                text,
                x + 25,
                y
        );
    }

    g.dispose();

    return copy;
}
public static BufferedImage drawBlobMarkers(
        BufferedImage source,
        java.util.List<vision.Blob> blobs
) {

    BufferedImage copy = new BufferedImage(
            source.getWidth(),
            source.getHeight(),
            source.getType()
    );

    java.awt.Graphics2D g = copy.createGraphics();
    g.drawImage(source, 0, 0, null);

    for (vision.Blob blob : blobs) {

        int x = blob.getMinX();
        int y = blob.getMinY();
        int width = blob.getWidth();
        int height = blob.getHeight();

        g.drawRect(x, y, width, height);

        String info =
                width + "x" + height +
                " | " + blob.getPixelCount();

        g.drawString(
                info,
                x,
                Math.max(10, y - 3)
        );
    }

    g.dispose();

    return copy;
}
public static BufferedImage drawTerrain(
        BufferedImage source,
        model.TerrainProfile terrain
) {

    BufferedImage copy =
            new BufferedImage(
                    source.getWidth(),
                    source.getHeight(),
                    BufferedImage.TYPE_INT_ARGB
            );

    java.awt.Graphics2D g =
            copy.createGraphics();

    g.drawImage(
            source,
            0,
            0,
            null
    );

    for (int x = 0;
         x < terrain.getWidth();
         x++) {

        int y =
                terrain.getY(x);

        if (y < 0) {
            continue;
        }

        // etwas dicker sichtbar machen
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
public static BufferedImage drawTrajectory(
        BufferedImage source,
        java.util.List<model.TrajectoryPoint> points
) {

    BufferedImage copy =
            new BufferedImage(
                    source.getWidth(),
                    source.getHeight(),
                    BufferedImage.TYPE_INT_ARGB
            );

    java.awt.Graphics2D g =
            copy.createGraphics();

    g.drawImage(
            source,
            0,
            0,
            null
    );

    for (model.TrajectoryPoint point : points) {

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
}