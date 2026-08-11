package vision;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class BlobDetector {

    private int minBrightness = 180;

    public List<Blob> detectBlobs(BufferedImage image) {

        int width = image.getWidth();
        int height = image.getHeight();

        boolean[][] visited = new boolean[width][height];

        List<Blob> blobs = new ArrayList<>();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                if (visited[x][y]) {
                    continue;
                }

                visited[x][y] = true;

                if (!isCandidatePixel(image, x, y)) {
                    continue;
                }

                Blob blob = floodFill(
        image,
        x,
        y,
        visited
);

if (blob.getPixelCount() >= 5) {
    blobs.add(blob);
}
            }
        }

        return blobs;
    }

    private Blob floodFill(
            BufferedImage image,
            int startX,
            int startY,
            boolean[][] visited
    ) {

        Queue<int[]> queue = new ArrayDeque<>();

        queue.add(new int[]{startX, startY});

        Blob blob = new Blob(startX, startY);

        while (!queue.isEmpty()) {

            int[] point = queue.poll();

            int x = point[0];
            int y = point[1];

            blob.addPixel(x, y);

            checkNeighbor(
                    image,
                    x + 1,
                    y,
                    visited,
                    queue
            );

            checkNeighbor(
                    image,
                    x - 1,
                    y,
                    visited,
                    queue
            );

            checkNeighbor(
                    image,
                    x,
                    y + 1,
                    visited,
                    queue
            );

            checkNeighbor(
                    image,
                    x,
                    y - 1,
                    visited,
                    queue
            );
        }

        return blob;
    }

    private void checkNeighbor(
            BufferedImage image,
            int x,
            int y,
            boolean[][] visited,
            Queue<int[]> queue
    ) {

        if (x < 0 ||
            y < 0 ||
            x >= image.getWidth() ||
            y >= image.getHeight()) {

            return;
        }

        if (visited[x][y]) {
            return;
        }

        visited[x][y] = true;

        if (isCandidatePixel(image, x, y)) {
            queue.add(new int[]{x, y});
        }
    }

    private boolean isCandidatePixel(
            BufferedImage image,
            int x,
            int y
    ) {

        Color color = new Color(
                image.getRGB(x, y)
        );

        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        int brightness = (r + g + b) / 3;

        return brightness >= minBrightness;
    }

    public void setMinBrightness(int minBrightness) {
        this.minBrightness = minBrightness;
    }
}