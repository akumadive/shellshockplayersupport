package vision;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class WindDetector {

    private static final double MIN_X_PERCENT = 0.42;
    private static final double MAX_X_PERCENT = 0.58;

    private static final double MIN_Y_PERCENT = 0.05;
    private static final double MAX_Y_PERCENT = 0.16;

    private static final int NORMALIZED_WIDTH = 10;
    private static final int NORMALIZED_HEIGHT = 14;

    /*
     * Die Templates sind nach der gleichen Normalisierung aufgebaut,
     * die wir auch auf die gefundenen ShellShock-Ziffern anwenden.
     *
     * Einige davon stammen bereits direkt aus unseren echten
     * ShellShock-Screenshots.
     */
    private static final String[][] DIGIT_TEMPLATES = {

            // 0
            {
                    "..######..",
                    "#########.",
                    "##########",
                    "####..####",
                    "###....###",
                    "###....###",
                    "###....###",
                    "###....###",
                    "###....###",
                    "####..####",
                    "##########",
                    "#########.",
                    "..######..",
                    "...####..."
            },

            // 1
            {
                    ".....#####",
                    ".....#####",
                    "..########",
                    "##########",
                    "##########",
                    ".....#####",
                    ".....#####",
                    ".....#####",
                    ".....#####",
                    ".....#####",
                    ".....#####",
                    ".....#####",
                    ".....#####",
                    ".....#####"
            },

            // 2
            {
                    "..######..",
                    "#########.",
                    "##########",
                    "......####",
                    "......####",
                    ".....####.",
                    "....####..",
                    "...####...",
                    "..####....",
                    ".####.....",
                    "####......",
                    "##########",
                    "##########",
                    "##########"
            },

            // 3
            {
                    "#########.",
                    "#########.",
                    "##########",
                    "##########",
                    "......####",
                    "......####",
                    "..########",
                    "..########",
                    "..########",
                    "........##",
                    "......####",
                    "##########",
                    "##########",
                    "..######.."
            },

            // 4
            {
                    ".....####.",
                    ".....####.",
                    ".....####.",
                    "....#####.",
                    "...######.",
                    "...######.",
                    "..###.###.",
                    "####..###.",
                    "####..###.",
                    "##########",
                    "##########",
                    "##########",
                    "......###.",
                    "......###."
            },

            // 5
            {
                    "..########",
                    "..########",
                    "..########",
                    "..########",
                    "..###.....",
                    "..###.....",
                    "..#######.",
                    "..########",
                    ".....#####",
                    ".......###",
                    ".......###",
                    "..########",
                    "##########",
                    "....###..."
            },

            // 6
            {
                    "...######.",
                    "...######.",
                    "#########.",
                    "#########.",
                    "####......",
                    "####......",
                    "#########.",
                    "##########",
                    "####.#####",
                    "####...###",
                    "####...###",
                    "##########",
                    "#########.",
                    "...####..."
            },

            // 7
            {
                    "##########",
                    "##########",
                    "##########",
                    "......####",
                    ".....####.",
                    ".....####.",
                    "....####..",
                    "...####...",
                    "...####...",
                    "..####....",
                    "..####....",
                    ".####.....",
                    ".####.....",
                    ".####....."
            },

            // 8
            {
                    "..######..",
                    "#########.",
                    "##########",
                    "####..####",
                    "####..####",
                    "##########",
                    ".########.",
                    "##########",
                    "####..####",
                    "###....###",
                    "####..####",
                    "##########",
                    "#########.",
                    "..######.."
            },

            // 9
            {
                    "...#####..",
                    "...#####..",
                    "#########.",
                    "####...###",
                    "####...###",
                    "####...###",
                    "##########",
                    "##########",
                    "##########",
                    "...##..###",
                    ".......###",
                    ".......###",
                    "#########.",
                    "#########."
            }
    };


    // =========================================================
    // COMPLETE DETECTION
    // =========================================================

    public WindResult detect(
            BufferedImage image
    ) {

        WindDirection direction =
                detectDirection(image);

        BufferedImage debugImage =
                createDebugImage(image);

        int strength =
                detectStrength(debugImage);

        double signedWind = 0.0;

        if (strength >= 0) {

            if (direction == WindDirection.LEFT) {

                signedWind =
                        -strength;

            } else if (direction == WindDirection.RIGHT) {

                signedWind =
                        strength;
            }
        }

        boolean valid =
                direction != WindDirection.UNKNOWN
                &&
                strength >= 0;

        return new WindResult(
                direction,
                strength,
                signedWind,
                valid
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
    // DIRECTION
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
    // STRENGTH
    // =========================================================

    private int detectStrength(
            BufferedImage binaryImage
    ) {

        boolean[][] white =
                createBooleanImage(
                        binaryImage
                );

        Component bubble =
                findWindBubble(
                        white
                );

        if (bubble == null) {

            return -1;
        }

        List<DigitBlob> digits =
                extractDigits(
                        white,
                        bubble
                );

        if (digits.isEmpty()) {

            return -1;
        }

        /*
         * Wind geht in ShellShock maximal
         * in den zweistelligen Bereich bzw.
         * bis 100.
         *
         * Der normale Fall sind 1-2 Ziffern.
         */
        if (digits.size() > 3) {

            return -1;
        }

        int value = 0;

        for (DigitBlob digit :
                digits) {

            int recognized =
                    recognizeDigit(
                            digit.pixels
                    );

            if (recognized < 0) {

                return -1;
            }

            value =
                    value * 10
                    +
                    recognized;
        }

        return value;
    }


    // =========================================================
    // FIND WHITE WIND BUBBLE
    // =========================================================

    private Component findWindBubble(
            boolean[][] image
    ) {

        List<Component> components =
                findComponents(
                        image,
                        true
                );

        int imageWidth =
                image.length;

        double centerX =
                imageWidth / 2.0;

        Component best = null;

        double bestScore =
                Double.MAX_VALUE;

        for (Component component :
                components) {

            int width =
                    component.getWidth();

            int height =
                    component.getHeight();

            if (width < 30 ||
                width > 60) {

                continue;
            }

            if (height < 18 ||
                height > 35) {

                continue;
            }

            if (component.pixelCount < 250) {

                continue;
            }

            double componentCenter =
                    component.getCenterX();

            double centerDistance =
                    Math.abs(
                            componentCenter
                            -
                            centerX
                    );

            if (centerDistance <
                bestScore) {

                best =
                        component;

                bestScore =
                        centerDistance;
            }
        }

        return best;
    }


    // =========================================================
    // EXTRACT BLACK DIGITS INSIDE WHITE BUBBLE
    // =========================================================

    private List<DigitBlob> extractDigits(
            boolean[][] image,
            Component bubble
    ) {

        int width =
                bubble.getWidth();

        int height =
                bubble.getHeight();

        boolean[][] blackInside =
                new boolean[
                        width
                ][
                        height
                ];

        for (int x = 0;
             x < width;
             x++) {

            for (int y = 0;
                 y < height;
                 y++) {

                int sourceX =
                        bubble.minX + x;

                int sourceY =
                        bubble.minY + y;

                blackInside[x][y] =
                        !image[sourceX][sourceY];
            }
        }

        List<Component> blackComponents =
                findComponents(
                        blackInside,
                        true
                );

        List<DigitBlob> digits =
                new ArrayList<>();

        for (Component component :
                blackComponents) {

            /*
             * Schwarzer Hintergrund außerhalb der
             * weißen Blase berührt deren Rand.
             *
             * Ziffern liegen vollständig IN der Blase.
             */
            if (component.minX == 0 ||
                component.minY == 0 ||
                component.maxX ==
                    width - 1 ||
                component.maxY ==
                    height - 1) {

                continue;
            }

            int digitWidth =
                    component.getWidth();

            int digitHeight =
                    component.getHeight();

            if (digitWidth < 3 ||
                digitWidth > 16) {

                continue;
            }

            if (digitHeight < 8 ||
                digitHeight > 20) {

                continue;
            }

            if (component.pixelCount < 10) {

                continue;
            }

            boolean[][] digitPixels =
                    new boolean[
                            digitWidth
                    ][
                            digitHeight
                    ];

            for (Point point :
                    component.points) {

                int dx =
                        point.x
                        -
                        component.minX;

                int dy =
                        point.y
                        -
                        component.minY;

                digitPixels[dx][dy] =
                        true;
            }

            digits.add(
                    new DigitBlob(
                            component.minX,
                            digitPixels
                    )
            );
        }

        digits.sort(
                Comparator.comparingInt(
                        digit ->
                                digit.x
                )
        );

        return digits;
    }


    // =========================================================
    // DIGIT RECOGNITION
    // =========================================================

    private int recognizeDigit(
            boolean[][] original
    ) {

        boolean[][] normalized =
                normalize(
                        original,
                        NORMALIZED_WIDTH,
                        NORMALIZED_HEIGHT
                );

        int bestDigit = -1;

        double bestScore =
                Double.MAX_VALUE;

        double secondBest =
                Double.MAX_VALUE;

        for (int digit = 0;
             digit <= 9;
             digit++) {

            boolean[][] template =
                    templateToBoolean(
                            DIGIT_TEMPLATES[
                                    digit
                            ]
                    );

            double score =
                    difference(
                            normalized,
                            template
                    );

            if (score < bestScore) {

                secondBest =
                        bestScore;

                bestScore =
                        score;

                bestDigit =
                        digit;

            } else if (score <
                       secondBest) {

                secondBest =
                        score;
            }
        }

        /*
         * Falls das Bild überhaupt nicht wie
         * irgendeine bekannte Ziffer aussieht,
         * lieber UNKNOWN zurückgeben.
         */
        if (bestScore > 0.45) {

            return -1;
        }

        /*
         * Bei extrem knappem Ergebnis ebenfalls
         * lieber nicht raten.
         *
         * Das können wir später anhand weiterer
         * echter Wind-Screenshots verfeinern.
         */
        if (secondBest -
            bestScore < 0.01) {

            return -1;
        }

        return bestDigit;
    }


    // =========================================================
    // NORMALIZE DIGIT
    // =========================================================

    private boolean[][] normalize(
            boolean[][] source,
            int targetWidth,
            int targetHeight
    ) {

        int sourceWidth =
                source.length;

        int sourceHeight =
                source[0].length;

        boolean[][] result =
                new boolean[
                        targetWidth
                ][
                        targetHeight
                ];

        for (int x = 0;
             x < targetWidth;
             x++) {

            for (int y = 0;
                 y < targetHeight;
                 y++) {

                int sourceX =
                        (int) (
                                (
                                    (double) x
                                    /
                                    targetWidth
                                )
                                *
                                sourceWidth
                        );

                int sourceY =
                        (int) (
                                (
                                    (double) y
                                    /
                                    targetHeight
                                )
                                *
                                sourceHeight
                        );

                sourceX =
                        Math.min(
                                sourceX,
                                sourceWidth - 1
                        );

                sourceY =
                        Math.min(
                                sourceY,
                                sourceHeight - 1
                        );

                result[x][y] =
                        source[
                                sourceX
                        ][
                                sourceY
                        ];
            }
        }

        return result;
    }


    // =========================================================
    // TEMPLATE
    // =========================================================

    private boolean[][] templateToBoolean(
            String[] template
    ) {

        boolean[][] result =
                new boolean[
                        NORMALIZED_WIDTH
                ][
                        NORMALIZED_HEIGHT
                ];

        for (int y = 0;
             y < NORMALIZED_HEIGHT;
             y++) {

            for (int x = 0;
                 x < NORMALIZED_WIDTH;
                 x++) {

                result[x][y] =
                        template[y]
                                .charAt(x)
                        ==
                        '#';
            }
        }

        return result;
    }


    private double difference(
            boolean[][] first,
            boolean[][] second
    ) {

        int different = 0;

        int total =
                NORMALIZED_WIDTH
                *
                NORMALIZED_HEIGHT;

        for (int x = 0;
             x < NORMALIZED_WIDTH;
             x++) {

            for (int y = 0;
                 y < NORMALIZED_HEIGHT;
                 y++) {

                if (first[x][y]
                    !=
                    second[x][y]) {

                    different++;
                }
            }
        }

        return (double) different
               /
               total;
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
    // BOOLEAN IMAGE
    // =========================================================

    private boolean[][] createBooleanImage(
            BufferedImage image
    ) {

        boolean[][] result =
                new boolean[
                        image.getWidth()
                ][
                        image.getHeight()
                ];

        for (int x = 0;
             x < image.getWidth();
             x++) {

            for (int y = 0;
                 y < image.getHeight();
                 y++) {

                Color color =
                        new Color(
                                image.getRGB(
                                        x,
                                        y
                                )
                        );

                int brightness =
                        (
                            color.getRed()
                            +
                            color.getGreen()
                            +
                            color.getBlue()
                        )
                        /
                        3;

                result[x][y] =
                        brightness >= 128;
            }
        }

        return result;
    }


    // =========================================================
    // CONNECTED COMPONENTS
    // =========================================================

    private List<Component> findComponents(
            boolean[][] image,
            boolean targetValue
    ) {

        int width =
                image.length;

        int height =
                image[0].length;

        boolean[][] visited =
                new boolean[
                        width
                ][
                        height
                ];

        List<Component> components =
                new ArrayList<>();

        for (int x = 0;
             x < width;
             x++) {

            for (int y = 0;
                 y < height;
                 y++) {

                if (visited[x][y]) {

                    continue;
                }

                if (image[x][y]
                    !=
                    targetValue) {

                    continue;
                }

                Component component =
                        floodFill(
                                image,
                                visited,
                                x,
                                y,
                                targetValue
                        );

                components.add(
                        component
                );
            }
        }

        return components;
    }


    private Component floodFill(
            boolean[][] image,
            boolean[][] visited,
            int startX,
            int startY,
            boolean targetValue
    ) {

        int width =
                image.length;

        int height =
                image[0].length;

        List<Point> queue =
                new ArrayList<>();

        queue.add(
                new Point(
                        startX,
                        startY
                )
        );

        int index = 0;

        Component component =
                new Component();

        while (index <
               queue.size()) {

            Point point =
                    queue.get(
                            index++
                    );

            int x =
                    point.x;

            int y =
                    point.y;

            if (x < 0 ||
                y < 0 ||
                x >= width ||
                y >= height) {

                continue;
            }

            if (visited[x][y]) {

                continue;
            }

            if (image[x][y]
                !=
                targetValue) {

                continue;
            }

            visited[x][y] =
                    true;

            component.add(
                    x,
                    y
            );

            for (int dx = -1;
                 dx <= 1;
                 dx++) {

                for (int dy = -1;
                     dy <= 1;
                     dy++) {

                    if (dx == 0 &&
                        dy == 0) {

                        continue;
                    }

                    queue.add(
                            new Point(
                                    x + dx,
                                    y + dy
                            )
                    );
                }
            }
        }

        return component;
    }


    // =========================================================
    // WIND UI COLOR
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
                        Math.max(
                                g,
                                b
                        )
                );

        int min =
                Math.min(
                        r,
                        Math.min(
                                g,
                                b
                        )
                );

        return brightness >= 150 &&
               max - min <= 55;
    }


    // =========================================================
    // HELPER CLASSES
    // =========================================================

    private static class Point {

        private final int x;
        private final int y;

        public Point(
                int x,
                int y
        ) {

            this.x = x;
            this.y = y;
        }
    }


    private static class Component {

        private int minX =
                Integer.MAX_VALUE;

        private int minY =
                Integer.MAX_VALUE;

        private int maxX =
                Integer.MIN_VALUE;

        private int maxY =
                Integer.MIN_VALUE;

        private int pixelCount = 0;

        private final List<Point> points =
                new ArrayList<>();

        public void add(
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

            points.add(
                    new Point(
                            x,
                            y
                    )
            );
        }

        public int getWidth() {

            return maxX
                    -
                    minX
                    +
                    1;
        }

        public int getHeight() {

            return maxY
                    -
                    minY
                    +
                    1;
        }

        public double getCenterX() {

            return minX
                    +
                    getWidth()
                    /
                    2.0;
        }
    }


    private static class DigitBlob {

        private final int x;

        private final boolean[][] pixels;

        public DigitBlob(
                int x,
                boolean[][] pixels
        ) {

            this.x =
                    x;

            this.pixels =
                    pixels;
        }
    }


    // =========================================================
    // PUBLIC RESULT
    // =========================================================

    public static class WindResult {

        private final WindDirection direction;

        private final int strength;

        private final double signedWind;

        private final boolean valid;

        public WindResult(
                WindDirection direction,
                int strength,
                double signedWind,
                boolean valid
        ) {

            this.direction =
                    direction;

            this.strength =
                    strength;

            this.signedWind =
                    signedWind;

            this.valid =
                    valid;
        }

        public WindDirection getDirection() {

            return direction;
        }

        public int getStrength() {

            return strength;
        }

        public double getSignedWind() {

            return signedWind;
        }

        public boolean isValid() {

            return valid;
        }
    }


    public enum WindDirection {

        LEFT,
        RIGHT,
        UNKNOWN
    }
}