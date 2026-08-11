package vision;

import model.PlayerState;
import model.PlayerState.PlayerType;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class PlayerDetector {

    public List<PlayerState> detectPlayers(
            BufferedImage image
    ) {

        List<PlayerState> candidates =
                new ArrayList<>();

        boolean[][] visited =
                new boolean[
                        image.getWidth()
                ][
                        image.getHeight()
                ];

        // Unteres HUD ignorieren
        int maxY =
                (int) (
                        image.getHeight()
                        * 0.83
                );

        for (int y = 50;
             y < maxY;
             y++) {

            for (int x = 0;
                 x < image.getWidth();
                 x++) {

                if (visited[x][y]) {
                    continue;
                }

                PlayerType type =
                        getPlayerColor(
                                image.getRGB(
                                        x,
                                        y
                                )
                        );

                if (type == null) {
                    continue;
                }

                ColorBlob blob =
                        floodFill(
                                image,
                                x,
                                y,
                                type,
                                visited
                        );

                if (!isTankCandidate(blob)) {
                    continue;
                }

                PlayerState candidate =
                        new PlayerState(
                                blob.centerX(),
                                blob.centerY(),
                                type
                        );

                if (!isNearExistingPlayer(
                        candidates,
                        candidate
                )) {

                    candidates.add(
                            candidate
                    );
                }
            }
        }

        return cleanPlayers(candidates);
    }


    // =========================================================
    // COLOR DETECTION
    // =========================================================

    private PlayerType getPlayerColor(
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


        // Gegner = deutlich rot
        if (r > 140 &&
            r > g * 1.4 &&
            r > b * 1.4) {

            return PlayerType.ENEMY;
        }


        // Eigener Spieler = deutlich grün
        if (g > 140 &&
            g > r * 1.4 &&
            g > b * 1.2) {

            return PlayerType.SELF;
        }

        return null;
    }


    // =========================================================
    // FLOOD FILL
    // =========================================================

    private ColorBlob floodFill(
            BufferedImage image,
            int startX,
            int startY,
            PlayerType type,
            boolean[][] visited
    ) {

        ArrayList<int[]> queue =
                new ArrayList<>();

        queue.add(
                new int[]{
                        startX,
                        startY
                }
        );

        int index = 0;

        int minX = startX;
        int maxX = startX;

        int minY = startY;
        int maxY = startY;

        int pixels = 0;


        while (index < queue.size()) {

            int[] point =
                    queue.get(index++);

            int x = point[0];
            int y = point[1];


            if (x < 0 ||
                y < 0 ||
                x >= image.getWidth() ||
                y >= image.getHeight()) {

                continue;
            }


            if (visited[x][y]) {
                continue;
            }

            visited[x][y] = true;


            if (getPlayerColor(
                    image.getRGB(
                            x,
                            y
                    )
            ) != type) {

                continue;
            }


            pixels++;

            minX =
                    Math.min(
                            minX,
                            x
                    );

            maxX =
                    Math.max(
                            maxX,
                            x
                    );

            minY =
                    Math.min(
                            minY,
                            y
                    );

            maxY =
                    Math.max(
                            maxY,
                            y
                    );


            queue.add(
                    new int[]{
                            x + 1,
                            y
                    }
            );

            queue.add(
                    new int[]{
                            x - 1,
                            y
                    }
            );

            queue.add(
                    new int[]{
                            x,
                            y + 1
                    }
            );

            queue.add(
                    new int[]{
                            x,
                            y - 1
                    }
            );
        }


        return new ColorBlob(
                minX,
                minY,
                maxX,
                maxY,
                pixels
        );
    }


    // =========================================================
    // BASIC BLOB FILTER
    // =========================================================

    private boolean isTankCandidate(
            ColorBlob blob
    ) {

        int width =
                blob.width();

        int height =
                blob.height();


        if (height <= 0) {
            return false;
        }


        double aspectRatio =
                (double) width
                / height;


        /*
         * Entfernt u.a. lange HP-Balken.
         */
        return width >= 8 &&
               width <= 50 &&
               height >= 6 &&
               height <= 35 &&
               blob.pixels >= 20 &&
               aspectRatio <= 4.0;
    }


    // =========================================================
    // DUPLICATE FILTER
    // =========================================================

    private boolean isNearExistingPlayer(
            List<PlayerState> players,
            PlayerState candidate
    ) {

        final int minDistance = 30;

        for (PlayerState player :
                players) {

            if (player.getType()
                    !=
                candidate.getType()) {

                continue;
            }


            int dx =
                    player.getX()
                    -
                    candidate.getX();

            int dy =
                    player.getY()
                    -
                    candidate.getY();


            double distance =
                    Math.sqrt(
                            dx * dx
                            +
                            dy * dy
                    );


            if (distance
                    <
                minDistance) {

                return true;
            }
        }

        return false;
    }


    // =========================================================
    // POST PROCESSING
    // =========================================================

    private List<PlayerState> cleanPlayers(
            List<PlayerState> candidates
    ) {

        List<PlayerState> result =
                new ArrayList<>();


        /*
         * Es soll nur EIN SELF existieren.
         *
         * Falls mehrere grüne Kandidaten
         * übereinander erkannt werden,
         * ist der unterste normalerweise
         * der tatsächliche Tank.
         */
        PlayerState self = null;


        for (PlayerState player :
                candidates) {

            if (player.getType()
                    !=
                PlayerType.SELF) {

                continue;
            }


            if (self == null ||
                player.getY()
                >
                self.getY()) {

                self = player;
            }
        }


        if (self != null) {
            result.add(self);
        }


        // Gegner verarbeiten
        for (PlayerState player :
                candidates) {

            if (player.getType()
                    !=
                PlayerType.ENEMY) {

                continue;
            }


            /*
             * Der rote Pfeil über SELF
             * sieht farblich wie ein Enemy aus.
             *
             * Wenn ein roter Kandidat fast
             * exakt über SELF sitzt, ignorieren.
             */
            if (self != null &&
                isSelfMarker(
                        self,
                        player
                )) {

                continue;
            }


            result.add(player);
        }


        return result;
    }


    // =========================================================
    // SELF MARKER FILTER
    // =========================================================

    private boolean isSelfMarker(
            PlayerState self,
            PlayerState enemy
    ) {

        int dx =
                Math.abs(
                        enemy.getX()
                        -
                        self.getX()
                );

        int dy =
                self.getY()
                -
                enemy.getY();


        return dx <= 35 &&
               dy >= 35 &&
               dy <= 140;
    }


    // =========================================================
    // INTERNAL COLOR BLOB
    // =========================================================

    private static class ColorBlob {

        private final int minX;
        private final int minY;

        private final int maxX;
        private final int maxY;

        private final int pixels;


        public ColorBlob(
                int minX,
                int minY,
                int maxX,
                int maxY,
                int pixels
        ) {

            this.minX = minX;
            this.minY = minY;

            this.maxX = maxX;
            this.maxY = maxY;

            this.pixels = pixels;
        }


        public int width() {

            return maxX
                    -
                    minX
                    +
                    1;
        }


        public int height() {

            return maxY
                    -
                    minY
                    +
                    1;
        }


        public int centerX() {

            return minX
                    +
                    width() / 2;
        }


        public int centerY() {

            return minY
                    +
                    height() / 2;
        }
    }
}