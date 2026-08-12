package vision;

import model.Bumper;

import java.awt.Color;
import java.awt.image.BufferedImage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;


public class BumperDetector {

    /*
     * HUD ignorieren.
     *
     * Der untere UI-Bereich enthält Farben,
     * die wir nicht als Bumper erkennen wollen.
     */
    private static final double MAX_Y_FACTOR =
            0.82;


    /*
     * Extrem kleine Farbflecken ignorieren.
     */
    private static final int MIN_PIXELS =
            18;


    /*
     * Bumper sind keine winzigen Objekte.
     */
    private static final int MIN_SIZE =
            8;


    /*
     * Sehr große Komponenten sind fast sicher
     * UI / Effekte / falsche Erkennung.
     */
    private static final int MAX_SIZE =
            500;


    public List<Bumper> detectBumpers(
            BufferedImage image
    ) {

        int width =
                image.getWidth();

        int height =
                image.getHeight();


        int maxY =
                (int) (
                        height
                        *
                        MAX_Y_FACTOR
                );


        boolean[][] visited =
                new boolean[width][maxY];


        List<Component> components =
                new ArrayList<>();


        // =====================================================
        // CONNECTED COMPONENTS
        // =====================================================

        for (int y = 0;
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


                if (!isBumperPixel(
                        image.getRGB(
                                x,
                                y
                        )
                )) {

                    continue;
                }


                Component component =
                        floodFill(
                                image,
                                x,
                                y,
                                maxY,
                                visited
                        );


                if (component.pixelCount
                        <
                    MIN_PIXELS) {

                    continue;
                }


                components.add(
                        component
                );
            }
        }


        // =====================================================
        // COMPONENT -> BUMPER
        // =====================================================

        List<Bumper> bumpers =
                new ArrayList<>();


        for (Component component :
                components) {


            int componentWidth =
                    component.getWidth();

            int componentHeight =
                    component.getHeight();


            if (componentWidth
                    <
                MIN_SIZE &&
                componentHeight
                    <
                MIN_SIZE) {

                continue;
            }


            if (componentWidth
                    >
                MAX_SIZE ||
                componentHeight
                    >
                MAX_SIZE) {

                continue;
            }


            Bumper bumper =
                    classifyComponent(
                            component
                    );


            if (bumper != null) {

                bumpers.add(
                        bumper
                );
            }
        }


        return bumpers;
    }


    // =========================================================
    // CLASSIFY
    // =========================================================

    private Bumper classifyComponent(
            Component component
    ) {

        double width =
                component.getWidth();

        double height =
                component.getHeight();


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
        // CIRCLE
        // =====================================================

        /*
         * Ein Kreis hat ungefähr dieselbe
         * Breite und Höhe.
         */
        double ratio =
                width
                /
                height;


        if (ratio >= 0.65 &&
            ratio <= 1.50 &&
            width >= 25 &&
            height >= 25) {


            double radius =
                    (
                            width
                            +
                            height
                    )
                    /
                    4.0;


            return Bumper.circle(
                    centerX,
                    centerY,
                    radius
            );
        }


        // =====================================================
        // HORIZONTAL LINE
        // =====================================================

        if (width >=
                height * 2.0) {


            return Bumper.line(
                    component.minX,
                    centerY,

                    component.maxX,
                    centerY
            );
        }


        // =====================================================
        // VERTICAL LINE
        // =====================================================

        if (height >=
                width * 2.0) {


            return Bumper.line(
                    centerX,
                    component.minY,

                    centerX,
                    component.maxY
            );
        }


        /*
         * Später:
         *
         * schräge Bumper über PCA / line fitting.
         *
         * Im ersten Schritt ignorieren wir
         * uneindeutige Komponenten lieber,
         * anstatt falsche Bumper zu erzeugen.
         */
        return null;
    }


    // =========================================================
    // FLOOD FILL
    // =========================================================

    private Component floodFill(
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


            addNeighbor(
                    image,

                    x + 1,
                    y,

                    maxY,

                    visited,
                    queue
            );


            addNeighbor(
                    image,

                    x - 1,
                    y,

                    maxY,

                    visited,
                    queue
            );


            addNeighbor(
                    image,

                    x,
                    y + 1,

                    maxY,

                    visited,
                    queue
            );


            addNeighbor(
                    image,

                    x,
                    y - 1,

                    maxY,

                    visited,
                    queue
            );
        }


        return component;
    }


    private void addNeighbor(
            BufferedImage image,

            int x,
            int y,

            int maxY,

            boolean[][] visited,
            Queue<int[]> queue
    ) {

        if (x < 0 ||
            y < 0 ||
            x >= image.getWidth() ||
            y >= maxY) {

            return;
        }


        if (visited[x][y]) {

            return;
        }


        visited[x][y] =
                true;


        if (isBumperPixel(
                image.getRGB(
                        x,
                        y
                )
        )) {


            queue.add(
                    new int[]{
                            x,
                            y
                    }
            );
        }
    }


    // =========================================================
    // COLOR
    // =========================================================

    private boolean isBumperPixel(
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


        /*
         * ShellShock-Bumper:
         *
         * typischerweise pink / violett /
         * magenta mit starkem Rot- und Blauanteil.
         *
         * Schwellen absichtlich etwas großzügig,
         * da Glow und Anti-Aliasing die Farbe verändern.
         */
        boolean enoughRed =
                r >= 120;


        boolean enoughBlue =
                b >= 100;


        boolean purpleDominance =
                r >= g * 1.20
                &&
                b >= g * 1.10;


        boolean brightness =
                r + b >= 280;


        return enoughRed
                &&
                enoughBlue
                &&
                purpleDominance
                &&
                brightness;
    }


    // =========================================================
    // INTERNAL COMPONENT
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