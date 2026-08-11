package vision;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class WindDetector {

    /*
     * ShellShock zeigt den Wind ungefähr
     * oben mittig an.
     *
     * Wir untersuchen bewusst nur einen
     * kleinen Bereich, damit HUD und Map
     * nicht stören.
     */
    private static final double MIN_X_PERCENT = 0.42;
    private static final double MAX_X_PERCENT = 0.58;

    private static final double MIN_Y_PERCENT = 0.05;
    private static final double MAX_Y_PERCENT = 0.16;


    public WindDirection detectDirection(
            BufferedImage image
    ) {

        int minX =
                (int) (
                        image.getWidth()
                        * MIN_X_PERCENT
                );

        int maxX =
                (int) (
                        image.getWidth()
                        * MAX_X_PERCENT
                );

        int minY =
                (int) (
                        image.getHeight()
                        * MIN_Y_PERCENT
                );

        int maxY =
                (int) (
                        image.getHeight()
                        * MAX_Y_PERCENT
                );


        int centerX =
                image.getWidth() / 2;


        int leftPixels = 0;
        int rightPixels = 0;


        for (int y = minY;
             y <= maxY;
             y++) {

            for (int x = minX;
                 x <= maxX;
                 x++) {

                if (!isWindUiPixel(
                        image.getRGB(
                                x,
                                y
                        )
                )) {
                    continue;
                }


                /*
                 * Die Ziffern selbst sitzen fast
                 * exakt in der Mitte.
                 *
                 * Uns interessieren hier nur die
                 * Pfeilpixel links/rechts davon.
                 */
                int distance =
                        x - centerX;


                if (distance < -20 &&
                    distance > -80) {

                    leftPixels++;
                }


                if (distance > 20 &&
                    distance < 80) {

                    rightPixels++;
                }
            }
        }


        if (leftPixels
                >
            rightPixels * 1.25) {

            return WindDirection.LEFT;
        }


        if (rightPixels
                >
            leftPixels * 1.25) {

            return WindDirection.RIGHT;
        }


        return WindDirection.UNKNOWN;
    }


    private boolean isWindUiPixel(
            int rgb
    ) {

        Color color =
                new Color(rgb);


        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();


        /*
         * Windanzeige ist hellgrau/weiß.
         */
        int brightness =
                (r + g + b) / 3;


        int max =
                Math.max(
                        r,
                        Math.max(g, b)
                );

        int min =
                Math.min(
                        r,
                        Math.min(g, b)
                );


        return brightness >= 150 &&
               max - min <= 55;
    }


    public enum WindDirection {

        LEFT,
        RIGHT,
        UNKNOWN
    }
}