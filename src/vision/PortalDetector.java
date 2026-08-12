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

    private static final double MAX_Y_FACTOR = 0.78;

    private static final int MIN_PIXELS = 35;
    private static final int MIN_DIAMETER = 35;
    private static final int MAX_DIAMETER = 150;

    private static final double MIN_ASPECT_RATIO = 0.70;
    private static final double MAX_ASPECT_RATIO = 1.35;

    private static final double MIN_FILL_RATIO = 0.025;
    private static final double MAX_FILL_RATIO = 0.60;

    /*
     * Fallback only. Normally the collision radius is now measured from
     * the DARK INNER OPENING -> colored ring transition.
     */
    private static final double FALLBACK_INNER_RADIUS_FACTOR = 0.80;

    private static final int RADIUS_RAYS = 64;
    private static final double MIN_VALID_RAY_FACTOR = 0.45;

    public List<PortalPair> detectPortalPairs(BufferedImage image) {

        List<Portal> portals = detectPortals(image);

        List<Portal> orange = new ArrayList<>();
        List<Portal> blue = new ArrayList<>();

        for (Portal portal : portals) {
            if (portal.getColor() == Portal.PortalColor.ORANGE) {
                orange.add(portal);
            } else {
                blue.add(portal);
            }
        }

        /*
         * Current pairing strategy:
         * same vertical order on both colors.
         *
         * This matches the verified practice-range cases:
         * top orange <-> top blue, bottom orange <-> bottom blue.
         *
         * The visible number recognition can be added later if we find
         * a layout where the orders differ.
         */
        orange.sort(Comparator.comparingDouble(Portal::getCenterY));
        blue.sort(Comparator.comparingDouble(Portal::getCenterY));

        int pairCount = Math.min(orange.size(), blue.size());
        List<PortalPair> pairs = new ArrayList<>();

        for (int i = 0; i < pairCount; i++) {
            int pairId = pairCount - i;

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

    public List<Portal> detectPortals(BufferedImage image) {

        int maxY = (int) Math.round(
                image.getHeight() * MAX_Y_FACTOR
        );

        List<Component> orangeComponents =
                findComponents(image, maxY, true);

        List<Component> blueComponents =
                findComponents(image, maxY, false);

        List<Portal> portals = new ArrayList<>();

        for (Component component : orangeComponents) {
            Portal portal = componentToPortal(
                    image,
                    component,
                    Portal.PortalColor.ORANGE
            );

            if (portal != null) {
                portals.add(portal);
            }
        }

        for (Component component : blueComponents) {
            Portal portal = componentToPortal(
                    image,
                    component,
                    Portal.PortalColor.BLUE
            );

            if (portal != null) {
                portals.add(portal);
            }
        }

        return portals;
    }

    private List<Component> findComponents(
            BufferedImage image,
            int maxY,
            boolean orange
    ) {

        int width = image.getWidth();

        boolean[][] visited =
                new boolean[width][maxY];

        List<Component> result =
                new ArrayList<>();

        for (int y = 0; y < maxY; y++) {
            for (int x = 0; x < width; x++) {

                if (visited[x][y]) {
                    continue;
                }

                visited[x][y] = true;

                if (!isPortalPixel(
                        image.getRGB(x, y),
                        orange
                )) {
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

                if (component.pixelCount >= MIN_PIXELS) {
                    result.add(component);
                }
            }
        }

        return result;
    }

    private Portal componentToPortal(
            BufferedImage image,
            Component component,
            Portal.PortalColor color
    ) {

        int width = component.getWidth();
        int height = component.getHeight();

        if (width < MIN_DIAMETER ||
            height < MIN_DIAMETER ||
            width > MAX_DIAMETER ||
            height > MAX_DIAMETER) {

            return null;
        }

        double aspectRatio =
                (double) width / height;

        if (aspectRatio < MIN_ASPECT_RATIO ||
            aspectRatio > MAX_ASPECT_RATIO) {

            return null;
        }

        double boxArea =
                width * (double) height;

        double fillRatio =
                component.pixelCount / boxArea;

        if (fillRatio < MIN_FILL_RATIO ||
            fillRatio > MAX_FILL_RATIO) {

            return null;
        }

        /*
         * Bounding-box center is less affected by an asymmetric glow
         * than the mean of all colored pixels.
         */
        double centerX =
                (component.minX + component.maxX) / 2.0;

        double centerY =
                (component.minY + component.maxY) / 2.0;

        double outerDiameter =
                (width + height) / 2.0;

        double outerRadius =
                outerDiameter / 2.0;

        double measuredInnerRadius =
                estimateInnerRadius(
                        image,
                        centerX,
                        centerY,
                        outerRadius,
                        color
                );

        double collisionRadius;

        if (Double.isFinite(measuredInnerRadius)) {
            collisionRadius = measuredInnerRadius;
        } else {
            collisionRadius =
                    outerRadius
                    *
                    FALLBACK_INNER_RADIUS_FACTOR;
        }

        return new Portal(
                color,
                centerX,
                centerY,
                collisionRadius
        );
    }

    /*
     * Measures the first strong colored-ring pixel while walking from
     * the dark portal center outward. This makes orange and blue use
     * the real black opening instead of different glow sizes.
     */
    private double estimateInnerRadius(
            BufferedImage image,
            double centerX,
            double centerY,
            double outerRadius,
            Portal.PortalColor color
    ) {

        List<Double> samples =
                new ArrayList<>();

        double maxRadius =
                Math.max(
                        8.0,
                        outerRadius * 1.15
                );

        for (int ray = 0;
             ray < RADIUS_RAYS;
             ray++) {

            double angle =
                    2.0
                    *
                    Math.PI
                    *
                    ray
                    /
                    RADIUS_RAYS;

            double cos =
                    Math.cos(angle);

            double sin =
                    Math.sin(angle);

            double found =
                    Double.NaN;

            /*
             * Starting a few pixels away from the center avoids any
             * number/text rendered inside the portal.
             */
            for (double radius = 8.0;
                 radius <= maxRadius;
                 radius += 0.5) {

                int x =
                        (int) Math.round(
                                centerX + cos * radius
                        );

                int y =
                        (int) Math.round(
                                centerY + sin * radius
                        );

                if (x < 0 ||
                    y < 0 ||
                    x >= image.getWidth() ||
                    y >= image.getHeight()) {

                    break;
                }

                int rgb =
                        image.getRGB(x, y);

                boolean ringPixel;

                if (color ==
                    Portal.PortalColor.ORANGE) {

                    ringPixel =
                            isOrangePortalPixel(rgb);

                } else {

                    ringPixel =
                            isBluePortalPixel(rgb);
                }

                if (ringPixel) {
                    found = radius;
                    break;
                }
            }

            if (Double.isFinite(found)) {
                samples.add(found);
            }
        }

        if (samples.size()
                <
            RADIUS_RAYS
            *
            MIN_VALID_RAY_FACTOR) {

            return Double.NaN;
        }

        samples.sort(Double::compareTo);

        /*
         * Median is robust against rays crossing the portal number,
         * sparks or stronger glow streaks.
         */
        int middle =
                samples.size() / 2;

        double median;

        if (samples.size() % 2 == 0) {
            median =
                    (
                            samples.get(middle - 1)
                            +
                            samples.get(middle)
                    )
                    /
                    2.0;
        } else {
            median =
                    samples.get(middle);
        }

        /*
         * Stay one pixel inside the visual ring so the trigger area
         * corresponds to the black opening rather than its bright edge.
         */
        return Math.max(
                5.0,
                median - 1.0
        );
    }

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

            component.add(x, y);

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
            y >= maxY ||
            visited[x][y]) {

            return;
        }

        visited[x][y] = true;

        if (isPortalPixel(
                image.getRGB(x, y),
                orange
        )) {

            queue.add(
                    new int[]{
                            x,
                            y
                    }
            );
        }
    }

    private boolean isPortalPixel(
            int rgb,
            boolean orange
    ) {

        if (orange) {
            return isOrangePortalPixel(rgb);
        }

        return isBluePortalPixel(rgb);
    }

    private boolean isOrangePortalPixel(
            int rgb
    ) {

        Color color =
                new Color(rgb);

        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        return r >= 145
                &&
                g >= 55
                &&
                g <= 190
                &&
                b <= 105
                &&
                r >= g * 1.25
                &&
                r >= b * 1.70;
    }

    private boolean isBluePortalPixel(
            int rgb
    ) {

        Color color =
                new Color(rgb);

        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        return b >= 135
                &&
                g >= 70
                &&
                r <= 110
                &&
                b >= r * 1.60
                &&
                b >= g * 1.05;
    }

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

            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);

            pixelCount++;
        }

        private int getWidth() {
            return maxX - minX + 1;
        }

        private int getHeight() {
            return maxY - minY + 1;
        }
    }
}
