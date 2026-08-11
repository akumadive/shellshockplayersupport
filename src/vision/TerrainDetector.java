package vision;

import model.TerrainProfile;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class TerrainDetector {

    public TerrainProfile detectTerrain(
            BufferedImage image
    ) {

        int width = image.getWidth();
        int height = image.getHeight();

        TerrainProfile terrain =
                new TerrainProfile(width);

        // HUD unten nicht untersuchen
        int maxY =
                (int) (height * 0.83);

        // oberen HUD-Bereich ignorieren
        int minY = 150;

        for (int x = 0; x < width; x++) {

            int surfaceY =
                    findSurface(
                            image,
                            x,
                            minY,
                            maxY
                    );

            terrain.setY(
                    x,
                    surfaceY
            );
        }

        smoothTerrain(terrain);

        return terrain;
    }

    private int findSurface(
            BufferedImage image,
            int x,
            int minY,
            int maxY
    ) {

        /*
         * Wir suchen von oben nach unten.
         *
         * Der erste Pixel, der stark genug
         * nach ShellShock-Terrain aussieht,
         * wird als Oberfläche genommen.
         */

        for (int y = minY; y < maxY; y++) {

            if (isTerrainPixel(
                    image.getRGB(x, y)
            )) {

                /*
                 * Ein einzelner Pixel könnte
                 * irgendein Effekt sein.
                 *
                 * Deshalb verlangen wir,
                 * dass darunter ebenfalls
                 * Terrain vorhanden ist.
                 */
                if (hasTerrainBelow(
                        image,
                        x,
                        y,
                        maxY
                )) {

                    return y;
                }
            }
        }

        return -1;
    }

    private boolean hasTerrainBelow(
            BufferedImage image,
            int x,
            int y,
            int maxY
    ) {

        int requiredPixels = 4;
        int found = 0;

        for (int offset = 1;
             offset <= 8;
             offset++) {

            int checkY = y + offset;

            if (checkY >= maxY) {
                break;
            }

            if (isTerrainPixel(
                    image.getRGB(
                            x,
                            checkY
                    )
            )) {

                found++;
            }
        }

        return found >= requiredPixels;
    }

    private boolean isTerrainPixel(int rgb) {

        Color color = new Color(rgb);

        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        /*
         * ShellShock Terrain ist in unserem
         * Screenshot deutlich heller/blauer
         * als der Hintergrund.
         */

        return
                b >= 110 &&
                g >= 75 &&
                b > r * 1.5 &&
                g > r * 1.3;
    }

    private void smoothTerrain(
            TerrainProfile terrain
    ) {

        int width = terrain.getWidth();

        int[] smoothed =
                new int[width];

        int radius = 2;

        for (int x = 0; x < width; x++) {

            int total = 0;
            int count = 0;

            for (int offset = -radius;
                 offset <= radius;
                 offset++) {

                int nx = x + offset;

                if (nx < 0 ||
                    nx >= width) {

                    continue;
                }

                int y = terrain.getY(nx);

                if (y >= 0) {
                    total += y;
                    count++;
                }
            }

            if (count > 0) {
                smoothed[x] =
                        total / count;
            } else {
                smoothed[x] = -1;
            }
        }

        for (int x = 0; x < width; x++) {
            terrain.setY(
                    x,
                    smoothed[x]
            );
        }
    }
}