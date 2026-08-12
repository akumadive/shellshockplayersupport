package vision;

import model.DamageMultiplier;
import model.PlayerState;

import java.awt.Color;
import java.awt.image.BufferedImage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;


public class DamageMultiplierDetector {

    private static final double MAX_Y_FACTOR =
            0.78;


    private static final int MIN_PIXELS =
            90;


    private static final int MIN_DIAMETER =
            24;

    private static final int MAX_DIAMETER =
            120;


    private static final double MIN_ASPECT_RATIO =
            0.65;

    private static final double MAX_ASPECT_RATIO =
            1.55;


    private static final double MIN_FILL_RATIO =
            0.18;


    private static final double X3_MAX_DIAMETER_FACTOR =
            0.045;


    /*
     * Enemy/self sprites and their red/green markers must never be
     * interpreted as a damage multiplier.
     *
     * The false X3 in the portal test was centered essentially exactly
     * on the detected enemy.
     */
    private static final double PLAYER_REJECTION_RADIUS =
            42.0;


    // =========================================================
    // OLD API
    // =========================================================

    public List<DamageMultiplier> detect(
            BufferedImage image
    ) {

        return detect(
                image,
                Collections.emptyList()
        );
    }


    // =========================================================
    // DETECT WITH PLAYER REJECTION
    // =========================================================

    public List<DamageMultiplier> detect(
            BufferedImage image,
            List<PlayerState> players
    ) {

        if (players == null) {

            players =
                    Collections.emptyList();
        }


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


        boolean[][] visited =
                new boolean[width][maxY];


        List<DamageMultiplier> result =
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


                if (!isMultiplierPixel(
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


                DamageMultiplier multiplier =
                        classify(
                                component,
                                image
                        );


                if (multiplier == null) {

                    continue;
                }


                if (isNearPlayer(
                        multiplier,
                        players
                )) {

                    continue;
                }


                result.add(
                        multiplier
                );
            }
        }


        return result;
    }


    // =========================================================
    // PLAYER REJECTION
    // =========================================================

    private boolean isNearPlayer(
            DamageMultiplier multiplier,
            List<PlayerState> players
    ) {

        for (PlayerState player :
                players) {


            double dx =
                    multiplier.getCenterX()
                    -
                    player.getX();


            double dy =
                    multiplier.getCenterY()
                    -
                    player.getY();


            double distanceSquared =
                    dx * dx
                    +
                    dy * dy;


            if (distanceSquared
                    <=
                PLAYER_REJECTION_RADIUS
                *
                PLAYER_REJECTION_RADIUS) {


                return true;
            }
        }


        return false;
    }


    // =========================================================
    // CLASSIFY
    // =========================================================

    private DamageMultiplier classify(
            Component component,
            BufferedImage image
    ) {

        if (component.pixelCount
                <
            MIN_PIXELS) {

            return null;
        }


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


        double boundingArea =
                width
                *
                (double) height;


        double fillRatio =
                component.pixelCount
                /
                boundingArea;


        if (fillRatio
                <
            MIN_FILL_RATIO) {

            return null;
        }


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


        double diameter =
                (
                        width
                        +
                        height
                )
                /
                2.0;


        double radius =
                diameter
                /
                2.0;


        double x3Limit =
                image.getHeight()
                *
                X3_MAX_DIAMETER_FACTOR;


        DamageMultiplier.MultiplierType type;


        if (diameter
                <=
            x3Limit) {


            type =
                    DamageMultiplier
                            .MultiplierType
                            .X3;


        } else {


            type =
                    DamageMultiplier
                            .MultiplierType
                            .X2;
        }


        return new DamageMultiplier(
                type,
                centerX,
                centerY,
                radius
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
                            queue
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


        if (isMultiplierPixel(
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

    private boolean isMultiplierPixel(
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


        boolean redStrong =
                r >= 105;


        boolean redDominant =
                r >= g * 1.45
                &&
                r >= b * 1.30;


        boolean notTooDark =
                r + g + b
                >=
                150;


        return redStrong
                &&
                redDominant
                &&
                notTooDark;
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