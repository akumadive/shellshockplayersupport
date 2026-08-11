package vision;

public class Blob {

    private int minX;
    private int minY;
    private int maxX;
    private int maxY;
    private int pixelCount;

    public Blob(int x, int y) {
        this.minX = x;
        this.minY = y;
        this.maxX = x;
        this.maxY = y;
        this.pixelCount = 0;
    }

    public void addPixel(int x, int y) {
        minX = Math.min(minX, x);
        minY = Math.min(minY, y);
        maxX = Math.max(maxX, x);
        maxY = Math.max(maxY, y);
        pixelCount++;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getWidth() {
        return maxX - minX + 1;
    }

    public int getHeight() {
        return maxY - minY + 1;
    }

    public int getPixelCount() {
        return pixelCount;
    }

    public int getCenterX() {
        return minX + getWidth() / 2;
    }

    public int getCenterY() {
        return minY + getHeight() / 2;
    }
}