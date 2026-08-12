package vision;

import model.Portal;
import model.PortalPair;

import java.awt.Color;
import java.awt.image.BufferedImage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;


public class PortalDetector {

    /*
     * Unteres HUD nicht analysieren.
     */
    private static final double MAX_Y_FACTOR =
            0.78;


    /*
     * Portalringe müssen eine vernünftige Anzahl
     * farbiger Pixel besitzen.
     */
    private static final int MIN_PIXELS =
            35;


    /*
     * Größenbereich großzügig halten.
     *
     * Deine bisherigen Screenshots sind ungefähr
     * 1920x1080 bzw. ähnlich skaliert.
     */
    private static final int MIN_DIAMETER =
            35;

    private static final int MAX_DIAMETER =
            150;


    /*
     * Portale sind kreisförmig.
     */
    private static final double MIN_ASPECT_RATIO =
            0.70;

    private static final double MAX_ASPECT_RATIO =
            1.35;


    /*
     * Der farbige Ring füllt seine Bounding Box
     * absichtlich nur teilweise.
     */
    private static final double MIN_FILL_RATIO =
            0.025;

    private static final double MAX_FILL_RATIO =
            0.60;


    /*
     * Der erkannte farbige Außenring ist etwas
     * größer als die schwarze Portalöffnung.
     *
     * Daher wird der Kollisionsradius etwas kleiner.
     *
     * Nach deinem Screenshot ist ~80 % ein sinnvoller
     * Startwert.
     */
    private static final double INNER_RADIUS_FACTOR =
            0.80;


    // =========================================================
    // PUBLIC API
    // =========================================================

    public List<PortalPair> detectPortalPairs(
            BufferedImage image
    ) {

        List<Portal> portals =
                detectPortals(
                        image
                );


        List<Portal> orange =
                new ArrayList<>();

        List<Portal> blue =
                new ArrayList<>();


        for (Portal portal :
                portals) {


            if (portal.getColor()
                    ==
                Portal.PortalColor.ORANGE) {


                orange.add(
                        portal
                );


            } else {


                blue.add(
                        portal
                );
            }
        }


        /*
         * Auf deinen Multi-Portal-Screenshots:
         *
         * oben: Pair 2
         * unten: Pair 1
         *
         * Für die Physik ist die konkrete Nummer egal,
         * solange die Zuordnung stimmt.
         *
         * Deshalb paaren wir zunächst nach Y-Reihenfolge.
         */
        orange.sort(
                Comparator.comparingDouble(
                        Portal::getCenterY
                )
        );


        blue.sort(
                Comparator.comparingDouble(
                        Portal::getCenterY
                )
        );


        List<PortalPair> pairs =
                new ArrayList<>();


        int pairCount =
                Math.min(
                        orange.size(),
                        blue.size()
                );


        for (int i = 0;
             i < pairCount;
             i++) {


            /*
             * Oben bekommt höhere sichtbare Nummer.
             *
             * Bei 2 Paaren:
             *
             * oben  -> 2
             * unten -> 1
             */
            int pairId =
                    pairCount - i;


            pairs.add(
                    new PortalPair(
                            pairId,
                            orange.get(i),
                            blue.get(i)
                    )
            );
        }


        return pairs;
    }


    public List<Portal> detectPortals(
            BufferedImage image
    ) {

        int width =
                image.getWidth();

        int height =
                image.getHeight();


        int maxY =
                (int) Math.round(
                        height
                        *
                        MAX_Y_FACTOR
                );


        List<Component> orangeComponents =
                findComponents(
                        image,
                        maxY,
                        true
                );


        List<Component> blueComponents =
                findComponents(
                        image,
                        maxY,
                        false
                );


        List<Portal> portals =
                new ArrayList<>();


        for (Component component :
                orangeComponents) {


            Portal portal =
                    componentToPortal(
                            component,
                            Portal.PortalColor.ORANGE
                    );


            if (portal != null) {

                portals.add(
                        portal
                );
            }
        }


        for (Component component :
                blueComponents) {


            Portal portal =
                    componentToPortal(
                            component,
                            Portal.PortalColor.BLUE
                    );


            if (portal != null) {

                portals.add(
                        portal
                );
            }
        }


        return portals;
    }


    // =========================================================
    // COMPONENT SEARCH
    // =========================================================

    private List<Component> findComponents(
            BufferedImage image,
            int maxY,
            boolean orange
    ) {

        int width =
                image.getWidth();


        boolean[][] visited =
                new boolean[width][maxY];


        List<Component> result =
                new ArrayList<>();


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


                boolean matching;


                if (orange) {


                    matching =
                            isOrangePortalPixel(
                                    image.getRGB(
                                            x,
                                            y
                                    )
                            );


                } else {


                    matching =
                            isBluePortalPixel(
                                    image.getRGB(
                                            x,
                                            y
                                    )
                            );
                }


                if (!matching) {

                    continue;
                }


                Component component =
                        floodFill(
                                image,
                                x,
                                y,
                                maxY,
                                visited,
                                orange
                        );


                if (component.pixelCount
                        >=
                    MIN_PIXELS) {


                    result.add(
                            component
                    );
                }
            }
        }


        return result;
    }


    // =========================================================
    // COMPONENT -> PORTAL
    // =========================================================

    private Portal componentToPortal(
            Component component,
            Portal.PortalColor color
    ) {

        int width =
                component.getWidth();

        int height =
                component.getHeight();


        if (width < MIN_DIAMETER ||
            height < MIN_DIAMETER) {

            return null;
        }


        if (width > MAX_DIAMETER ||
            height > MAX_DIAMETER) {

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


        if (fillRatio < MIN_FILL_RATIO ||
            fillRatio > MAX_FILL_RATIO) {

            return null;
        }


        /*
         * Mittelpunkt besser aus der Bounding Box als
         * aus dem Pixelmittelwert.
         *
         * Glow kann auf einer Seite stärker sein und
         * sonst den Mittelpunkt verschieben.
         */
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


        double outerDiameter =
                (
                        width
                        +
                        height
                )
                /
                2.0;


        double outerRadius =
                outerDiameter
                /
                2.0;


        double collisionRadius =
                outerRadius
                *
                INNER_RADIUS_FACTOR;


        return new Portal(
                color,
                centerX,
                centerY,
                collisionRadius
        );
    }


    // =========================================================
    // FLOOD FILL
    // =========================================================

    private Component floodFill(
            BufferedImage image,

            int startX,
            int startY,

            int maxY,

            boolean[][] visited,

            boolean orange
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
            for (int offsetY = -1;
                 offsetY <= 1;
                 offsetY++) {


                for (int offsetX = -1;
                     offsetX <= 1;
                     offsetX++) {


                    if (offsetX == 0 &&
                        offsetY == 0) {

                        continue;
                    }


                    addNeighbor(
                            image,

                            x + offsetX,
                            y + offsetY,

                            maxY,

                            visited,
                            queue,

                            orange
                    );
                }
            }
        }


        return component;
    }


    private void addNeighbor(
            BufferedImage image,

            int x,
            int y,

            int maxY,

            boolean[][] visited,

            Queue<int[]> queue,

            boolean orange
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


        int rgb =
                image.getRGB(
                        x,
                        y
                );


        boolean matching;


        if (orange) {


            matching =
                    isOrangePortalPixel(
                            rgb
                    );


        } else {


            matching =
                    isBluePortalPixel(
                            rgb
                    );
        }


        if (matching) {


            queue.add(
                    new int[]{
                            x,
                            y
                    }
            );
        }
    }


    // =========================================================
    // ORANGE
    // =========================================================

    private boolean isOrangePortalPixel(
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
         * Orange Portal:
         *
         * heller Orange-/Gold-Ring.
         *
         * Glow darf mit rein, aber dunkler Hintergrund
         * soll nicht verbunden werden.
         */
        boolean enoughRed =
                r >= 145;


        boolean enoughGreen =
                g >= 55;


        boolean notTooGreen =
                g <= 190;


        boolean lowBlue =
                b <= 105;


        boolean redDominates =
                r >= g * 1.25
                &&
                r >= b * 1.70;


        return enoughRed
                &&
                enoughGreen
                &&
                notTooGreen
                &&
                lowBlue
                &&
                redDominates;
    }


    // =========================================================
    // BLUE
    // =========================================================

    private boolean isBluePortalPixel(
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
         * Blaues Portal:
         *
         * kräftiges Cyan/Blau mit deutlich stärkerem
         * Blauanteil als Rot.
         */
        boolean enoughBlue =
                b >= 135;


        boolean enoughGreen =
                g >= 70;


        boolean lowRed =
                r <= 110;


        boolean blueDominates =
                b >= r * 1.60
                &&
                b >= g * 1.05;


        return enoughBlue
                &&
                enoughGreen
                &&
                lowRed
                &&
                blueDominates;
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