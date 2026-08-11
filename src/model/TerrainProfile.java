package model;

public class TerrainProfile {

    private final int[] surfaceY;

    public TerrainProfile(int width) {
        this.surfaceY = new int[width];

        for (int x = 0; x < width; x++) {
            surfaceY[x] = -1;
        }
    }

    public int getWidth() {
        return surfaceY.length;
    }

    public int getY(int x) {

        if (x < 0 || x >= surfaceY.length) {
            return -1;
        }

        return surfaceY[x];
    }

    public void setY(int x, int y) {

        if (x < 0 || x >= surfaceY.length) {
            return;
        }

        surfaceY[x] = y;
    }

    public boolean hasTerrainAt(int x) {
        return getY(x) >= 0;
    }
}