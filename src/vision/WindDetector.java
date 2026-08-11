package vision;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class WindDetector {

    /*
     * Bereich der Windanzeige oben mittig.
     *
     * Alles relativ zur Auflösung, damit wir
     * nicht komplett an 1920x1080 gebunden sind.
     */
    private static final double MIN_X_PERCENT = 0.42;
    private static final double MAX_X_PERCENT = 0.58;

    private static final double MIN_Y_PERCENT = 0.05;
    private static final double MAX_Y_PERCENT = 0.16;


    // =========================================================
    // COMPLETE DETECTION RESULT
    // =========================================================

    public WindResult detect(
            BufferedImage image
    ) {

        WindDirection direction =
                detectDirection(image);

        BufferedImage region =
                getWindRegion(image);

        /*
         * Stärke ist NOCH nicht implementiert.
         *
         * -1 bedeutet:
         * "nicht automatisch erkannt"
         */
        int strength = -1;

        return new WindResult(
                direction,
                strength,
                region
        );
    }


    // =========================================================
    // WIND REGION
    // =========================================================

    public BufferedImage getWindRegion(
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


        return image.getSubimage(
                minX,
                minY,
                maxX - minX,
                maxY - minY
        );
    }


    // =========================================================
    // WIND DIRECTION
    // =========================================================

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


                int distance =
                        x - centerX;


                /*
                 * Der Windpfeil befindet sich
                 * links oder rechts der Zahl.
                 *
                 * Der mittlere Bereich wird
                 * bewusst ignoriert, weil dort
                 * die eigentlichen Ziffern sitzen.
                 */
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


        if (leftPixels >
            rightPixels * 1.25) {

            return WindDirection.LEFT;
        }


        if (rightPixels >
            leftPixels * 1.25) {

            return WindDirection.RIGHT;
        }


        return WindDirection.UNKNOWN;
    }


    // =========================================================
    // DEBUG IMAGE
    // =========================================================

    public BufferedImage createDebugImage(
            BufferedImage image
    ) {

        BufferedImage region =
                getWindRegion(image);


        BufferedImage debug =
                new BufferedImage(
                        region.getWidth(),
                        region.getHeight(),
                        BufferedImage.TYPE_INT_RGB
                );


        /*
         * Binary Image:
         *
         * Wind-HUD Pixel -> weiß
         * alles andere   -> schwarz
         *
         * Genau dieses Bild benutzen wir danach
         * für die Ziffernerkennung.
         */
        for (int y = 0;
             y < region.getHeight();
             y++) {

            for (int x = 0;
                 x < region.getWidth();
                 x++) {

                int rgb =
                        region.getRGB(
                                x,
                                y
                        );


                if (isWindUiPixel(rgb)) {

                    debug.setRGB(
                            x,
                            y,
                            Color.WHITE.getRGB()
                    );

                } else {

                    debug.setRGB(
                            x,
                            y,
                            Color.BLACK.getRGB()
                    );
                }
            }
        }


        return debug;
    }


    // =========================================================
    // COLOR FILTER
    // =========================================================

    private boolean isWindUiPixel(
            int rgb
    ) {

        Color color =
                new Color(rgb);


        int r =
                color.getRed();

        int g =
                color.getGreen();

        int b =
                color.getBlue();


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


        /*
         * ShellShock Wind-HUD:
         * helles Grau / Weiß.
         *
         * Gleichzeitig verlangen wir eine
         * geringe Farbsättigung, damit z.B.
         * blaue Map-Elemente nicht dazugehören.
         */
        return brightness >= 150 &&
               max - min <= 55;
    }


    // =========================================================
    // RESULT OBJECT
    // =========================================================

    public static class WindResult {

        private final WindDirection direction;
        private final int strength;
        private final BufferedImage region;


        public WindResult(
                WindDirection direction,
                int strength,
                BufferedImage region
        ) {

            this.direction =
                    direction;

            this.strength =
                    strength;

            this.region =
                    region;
        }


        public WindDirection getDirection() {
            return direction;
        }


        public int getStrength() {
            return strength;
        }


        public BufferedImage getRegion() {
            return region;
        }
    }


    // =========================================================
    // DIRECTION ENUM
    // =========================================================

    public enum WindDirection {

        LEFT,
        RIGHT,
        UNKNOWN
    }
}