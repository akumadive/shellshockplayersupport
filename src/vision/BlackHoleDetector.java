package vision;

import model.BlackHole;

import java.awt.Color;
import java.awt.image.BufferedImage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;


public class BlackHoleDetector {

    // =========================================================
    // SCREEN AREA
    // =========================================================

    /*
     * HUD unten ignorieren.
     */
    private static final double MAX_Y_FACTOR =
            0.76;


    /*
     * Oberen Edit-/Fensterbereich ignorieren.
     */
    private static final int MIN_Y =
            90;


    // =========================================================
    // BLACK CORE
    // =========================================================

    /*
     * Ein Kernpixel muss wirklich sehr dunkel sein.
     */
    private static final int MAX_CORE_BRIGHTNESS =
            48;


    /*
     * Mindestgröße einer schwarzen Kernkomponente.
     */
    private static final int MIN_CORE_PIXELS =
            120;


    /*
     * Möglicher Kerndurchmesser.
     *
     * Großzügig, weil die Black-Hole-Größe
     * variabel ist.
     */
    private static final int MIN_CORE_DIAMETER =
            12;

    private static final int MAX_CORE_DIAMETER =
            65;


    /*
     * Schwarzer Kern sollte ungefähr rund sein.
     */
    private static final double MIN_ASPECT_RATIO =
            0.70;

    private static final double MAX_ASPECT_RATIO =
            1.40;


    /*
     * Runde Fläche sollte ihre Bounding Box
     * vernünftig ausfüllen.
     *
     * Kreisfüllung theoretisch ~0.785.
     * Wegen Effekten / Antialiasing großzügiger.
     */
    private static final double MIN_CORE_FILL_RATIO =
            0.45;


    // =========================================================
    // SWIRL VALIDATION
    // =========================================================

    /*
     * Anzahl Proben rund um den Kern.
     */
    private static final int SWIRL_SAMPLES =
            48;


    /*
     * Wir prüfen mehrere Ringe außerhalb des Kerns.
     */
    private static final double[] SWIRL_RADIUS_FACTORS = {

            1.35,
            1.65,
            2.00,
            2.35
    };


    /*
     * Mindestens dieser Anteil aller Proben
     * muss blau/violett wirken.
     */
    private static final double MIN_SWIRL_RATIO =
            0.16;


    /*
     * Zusätzlich muss der Bereich außerhalb des
     * Kerns merklich heller als der Kern sein.
     */
    private static final double MIN_BRIGHT_RING_RATIO =
            0.25;


    // =========================================================
    // INFLUENCE RADIUS
    // =========================================================

    /*
     * Aus deinen beiden Referenzen wirkt der
     * äußere Wirkungsradius ungefähr proportional
     * zur Kern-/Gesamtgröße.
     *
     * Das wird beim Physics-Test noch kalibriert.
     */
    private static final double INFLUENCE_CORE_FACTOR =
            10.5;


    /*
     * Sicherheitsgrenzen.
     */
    private static final double MIN_INFLUENCE_RADIUS =
            65.0;

    private static final double MAX_INFLUENCE_RADIUS =
            210.0;


    // =========================================================
    // PUBLIC API
    // =========================================================

    public List<BlackHole> detect(
            BufferedImage image
    ) {

        int width =
                image.getWidth();


        int maxY =
                (int) Math.round(
                        image.getHeight()
                        *
                        MAX_Y_FACTOR
                );


        boolean[][] visited =
                new boolean[width][maxY];


        List<BlackHole> result =
                new ArrayList<>();


        for (int y = MIN_Y;
             y < maxY;
             y++) {


            for (int x = 0;
                 x < width;
                 x++) {


                if (visited[x][y]) {

                    continue;
                }


                visited[x][y] =
                        true;


                if (!isCorePixel(
                        image.getRGB(
                                x,
                                y
                        )
                )) {

                    continue;
                }


                Component component =
                        floodFillCore(
                                image,
                                x,
                                y,
                                maxY,
                                visited
                        );


                BlackHole blackHole =
                        classifyComponent(
                                image,
                                component
                        );


                if (blackHole != null) {


                    if (!isDuplicate(
                            result,
                            blackHole
                    )) {


                        result.add(
                                blackHole
                        );
                    }
                }
            }
        }


        return result;
    }


    // =========================================================
    // COMPONENT CLASSIFICATION
    // =========================================================

    private BlackHole classifyComponent(
            BufferedImage image,
            Component component
    ) {

        if (component.pixelCount
                <
            MIN_CORE_PIXELS) {

            return null;
        }


        int width =
                component.getWidth();


        int height =
                component.getHeight();


        if (width < MIN_CORE_DIAMETER ||
            height < MIN_CORE_DIAMETER) {

            return null;
        }


        if (width > MAX_CORE_DIAMETER ||
            height > MAX_CORE_DIAMETER) {

            return null;
        }


        double aspectRatio =
                (double) width
                /
                height;


        if (aspectRatio < MIN_ASPECT_RATIO ||
            aspectRatio > MAX_ASPECT_RATIO) {

            return null;
        }


        double boxArea =
                width
                *
                (double) height;


        double fillRatio =
                component.pixelCount
                /
                boxArea;


        if (fillRatio
                <
            MIN_CORE_FILL_RATIO) {

            return null;
        }


        // =====================================================
        // CENTER
        // =====================================================

        double centerX =
                (
                        component.minX
                        +
                        component.maxX
                )
                /
                2.0;


        double centerY =
                (
                        component.minY
                        +
                        component.maxY
                )
                /
                2.0;


        // =====================================================
        // CORE RADIUS
        // =====================================================

        /*
         * Durchschnittlicher Durchmesser aus
         * Bounding-Box-Breite/Höhe.
         */
        double coreDiameter =
                (
                        width
                        +
                        height
                )
                /
                2.0;


        double coreRadius =
                coreDiameter
                /
                2.0;


        // =====================================================
        // SWIRL VALIDATION
        // =====================================================

        if (!hasBlackHoleSwirl(
                image,
                centerX,
                centerY,
                coreRadius
        )) {

            return null;
        }


        // =====================================================
        // INFLUENCE
        // =====================================================

        double influenceRadius =
                coreRadius
                *
                INFLUENCE_CORE_FACTOR;


        influenceRadius =
                Math.max(
                        MIN_INFLUENCE_RADIUS,
                        Math.min(
                                MAX_INFLUENCE_RADIUS,
                                influenceRadius
                        )
                );


        return new BlackHole(
                centerX,
                centerY,
                coreRadius,
                influenceRadius
        );
    }


    // =========================================================
    // SWIRL CHECK
    // =========================================================

    private boolean hasBlackHoleSwirl(
            BufferedImage image,

            double centerX,
            double centerY,

            double coreRadius
    ) {

        int swirlMatches =
                0;


        int brightMatches =
                0;


        int total =
                0;


        /*
         * Durchschnittliche Helligkeit des Kerns.
         */
        double coreBrightness =
                sampleCoreBrightness(
                        image,
                        centerX,
                        centerY,
                        coreRadius
                );


        for (double factor :
                SWIRL_RADIUS_FACTORS) {


            double radius =
                    coreRadius
                    *
                    factor;


            for (int sample = 0;
                 sample < SWIRL_SAMPLES;
                 sample++) {


                double angle =
                        2.0
                        *
                        Math.PI
                        *
                        sample
                        /
                        SWIRL_SAMPLES;


                int x =
                        (int) Math.round(
                                centerX
                                +
                                Math.cos(
                                        angle
                                )
                                *
                                radius
                        );


                int y =
                        (int) Math.round(
                                centerY
                                +
                                Math.sin(
                                        angle
                                )
                                *
                                radius
                        );


                if (!insideImage(
                        image,
                        x,
                        y
                )) {

                    continue;
                }


                total++;


                int rgb =
                        image.getRGB(
                                x,
                                y
                        );


                if (isSwirlPixel(
                        rgb
                )) {


                    swirlMatches++;
                }


                if (brightness(
                        rgb
                )
                        >=
                    coreBrightness
                    +
                    25.0) {


                    brightMatches++;
                }
            }
        }


        if (total == 0) {

            return false;
        }


        double swirlRatio =
                (double) swirlMatches
                /
                total;


        double brightRatio =
                (double) brightMatches
                /
                total;


        return swirlRatio
                >=
                MIN_SWIRL_RATIO

                &&

                brightRatio
                >=
                MIN_BRIGHT_RING_RATIO;
    }


    // =========================================================
    // CORE BRIGHTNESS
    // =========================================================

    private double sampleCoreBrightness(
            BufferedImage image,

            double centerX,
            double centerY,

            double coreRadius
    ) {

        double radius =
                coreRadius
                *
                0.55;


        int samples =
                24;


        double total =
                0.0;


        int count =
                0;


        /*
         * Center.
         */
        int centerPixelX =
                (int) Math.round(
                        centerX
                );


        int centerPixelY =
                (int) Math.round(
                        centerY
                );


        if (insideImage(
                image,
                centerPixelX,
                centerPixelY
        )) {


            total +=
                    brightness(
                            image.getRGB(
                                    centerPixelX,
                                    centerPixelY
                            )
                    );


            count++;
        }


        /*
         * Ring innerhalb des schwarzen Kerns.
         */
        for (int i = 0;
             i < samples;
             i++) {


            double angle =
                    2.0
                    *
                    Math.PI
                    *
                    i
                    /
                    samples;


            int x =
                    (int) Math.round(
                            centerX
                            +
                            Math.cos(
                                    angle
                            )
                            *
                            radius
                    );


            int y =
                    (int) Math.round(
                            centerY
                            +
                            Math.sin(
                                    angle
                            )
                            *
                            radius
                    );


            if (!insideImage(
                    image,
                    x,
                    y
            )) {

                continue;
            }


            total +=
                    brightness(
                            image.getRGB(
                                    x,
                                    y
                            )
                    );


            count++;
        }


        if (count == 0) {

            return 0.0;
        }


        return total
                /
                count;
    }


    // =========================================================
    // CORE FLOOD FILL
    // =========================================================

    private Component floodFillCore(
            BufferedImage image,

            int startX,
            int startY,

            int maxY,

            boolean[][] visited
    ) {

        Queue<int[]> queue =
                new ArrayDeque<>();


        queue.add(
                new int[]{
                        startX,
                        startY
                }
        );


        Component component =
                new Component(
                        startX,
                        startY
                );


        while (!queue.isEmpty()) {


            int[] point =
                    queue.poll();


            int x =
                    point[0];


            int y =
                    point[1];


            component.add(
                    x,
                    y
            );


            /*
             * 8er-Nachbarschaft.
             */
            for (int dy = -1;
                 dy <= 1;
                 dy++) {


                for (int dx = -1;
                     dx <= 1;
                     dx++) {


                    if (dx == 0 &&
                        dy == 0) {

                        continue;
                    }


                    int nx =
                            x
                            +
                            dx;


                    int ny =
                            y
                            +
                            dy;


                    if (nx < 0 ||
                        ny < MIN_Y ||
                        nx >= image.getWidth() ||
                        ny >= maxY) {

                        continue;
                    }


                    if (visited[nx][ny]) {

                        continue;
                    }


                    visited[nx][ny] =
                            true;


                    if (isCorePixel(
                            image.getRGB(
                                    nx,
                                    ny
                            )
                    )) {


                        queue.add(
                                new int[]{
                                        nx,
                                        ny
                                }
                        );
                    }
                }
            }
        }


        return component;
    }


    // =========================================================
    // CORE PIXEL
    // =========================================================

    private boolean isCorePixel(
            int rgb
    ) {

        Color color =
                new Color(
                        rgb
                );


        int r =
                color.getRed();


        int g =
                color.getGreen();


        int b =
                color.getBlue();


        /*
         * Fast schwarz.
         *
         * Wichtig:
         * Hintergrund ist ebenfalls dunkel.
         *
         * Deshalb reicht diese Funktion allein
         * NICHT zur Klassifizierung. Erst Form +
         * Swirl machen daraus ein Black Hole.
         */
        int brightness =
                brightness(
                        rgb
                );


        int spread =
                Math.max(
                        r,
                        Math.max(
                                g,
                                b
                        )
                )
                -
                Math.min(
                        r,
                        Math.min(
                                g,
                                b
                        )
                );


        return brightness
                <=
                MAX_CORE_BRIGHTNESS

                &&

                spread
                <=
                45;
    }


    // =========================================================
    // SWIRL PIXEL
    // =========================================================

    private boolean isSwirlPixel(
            int rgb
    ) {

        Color color =
                new Color(
                        rgb
                );


        int r =
                color.getRed();


        int g =
                color.getGreen();


        int b =
                color.getBlue();


        /*
         * Black-Hole-Swirl:
         *
         * Blau/Violett.
         */
        boolean enoughBlue =
                b >= 75;


        boolean blueDominant =
                b >= r * 1.05;


        boolean someColor =
                g >= 20
                ||
                r >= 20;


        boolean notAlmostBlack =
                brightness(
                        rgb
                )
                >=
                35;


        return enoughBlue
                &&
                blueDominant
                &&
                someColor
                &&
                notAlmostBlack;
    }


    // =========================================================
    // DUPLICATE
    // =========================================================

    private boolean isDuplicate(
            List<BlackHole> existing,

            BlackHole candidate
    ) {

        for (BlackHole blackHole :
                existing) {


            double dx =
                    blackHole.getCenterX()
                    -
                    candidate.getCenterX();


            double dy =
                    blackHole.getCenterY()
                    -
                    candidate.getCenterY();


            double distance =
                    Math.sqrt(
                            dx * dx
                            +
                            dy * dy
                    );


            double threshold =
                    Math.max(
                            blackHole.getCoreRadius(),
                            candidate.getCoreRadius()
                    )
                    *
                    2.5;


            if (distance
                    <
                threshold) {


                return true;
            }
        }


        return false;
    }


    // =========================================================
    // HELPERS
    // =========================================================

    private boolean insideImage(
            BufferedImage image,

            int x,
            int y
    ) {

        return x >= 0
                &&
                y >= 0
                &&
                x < image.getWidth()
                &&
                y < image.getHeight();
    }


    private int brightness(
            int rgb
    ) {

        Color color =
                new Color(
                        rgb
                );


        return (int) Math.round(

                color.getRed()
                        *
                        0.2126

                +

                color.getGreen()
                        *
                        0.7152

                +

                color.getBlue()
                        *
                        0.0722
        );
    }


    // =========================================================
    // COMPONENT
    // =========================================================

    private static class Component {

        private int minX;
        private int minY;

        private int maxX;
        private int maxY;

        private int pixelCount;


        private Component(
                int x,
                int y
        ) {

            minX = x;
            minY = y;

            maxX = x;
            maxY = y;

            pixelCount = 0;
        }


        private void add(
                int x,
                int y
        ) {

            minX =
                    Math.min(
                            minX,
                            x
                    );


            minY =
                    Math.min(
                            minY,
                            y
                    );


            maxX =
                    Math.max(
                            maxX,
                            x
                    );


            maxY =
                    Math.max(
                            maxY,
                            y
                    );


            pixelCount++;
        }


        private int getWidth() {

            return maxX
                    -
                    minX
                    +
                    1;
        }


        private int getHeight() {

            return maxY
                    -
                    minY
                    +
                    1;
        }
    }
}