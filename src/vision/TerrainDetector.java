package vision;

import model.BlackHole;
import model.Portal;
import model.PortalPair;
import model.TerrainProfile;

import java.awt.Color;
import java.awt.image.BufferedImage;

import java.util.Collections;
import java.util.List;


public class TerrainDetector {

    /*
     * Portal-Glow darf nicht als Terrain gelten.
     */
    private static final double PORTAL_EXCLUSION_PADDING =
            28.0;


    /*
     * Beim Black Hole maskieren wir NICHT den kompletten
     * Influence-Radius.
     *
     * Sonst würden wir echtes Terrain unter einem Black Hole
     * wegwerfen.
     *
     * Wir maskieren nur den sichtbaren Swirl-/Particle-Bereich.
     */
    private static final double BLACK_HOLE_VISUAL_FACTOR =
            4.5;


    private static final double BLACK_HOLE_VISUAL_PADDING =
            12.0;


    // =========================================================
    // OLD API
    // =========================================================

    public TerrainProfile detectTerrain(
            BufferedImage image
    ) {

        return detectTerrain(
                image,
                Collections.emptyList(),
                Collections.emptyList()
        );
    }


    public TerrainProfile detectTerrain(
            BufferedImage image,
            List<PortalPair> portalPairs
    ) {

        return detectTerrain(
                image,
                portalPairs,
                Collections.emptyList()
        );
    }


    // =========================================================
    // FULL TERRAIN DETECTION
    // =========================================================

    public TerrainProfile detectTerrain(
            BufferedImage image,
            List<PortalPair> portalPairs,
            List<BlackHole> blackHoles
    ) {

        if (portalPairs == null) {


            portalPairs =
                    Collections.emptyList();
        }


        if (blackHoles == null) {


            blackHoles =
                    Collections.emptyList();
        }


        int width =
                image.getWidth();


        int height =
                image.getHeight();


        TerrainProfile terrain =
                new TerrainProfile(
                        width
                );


        int maxY =
                (int) (
                        height
                        *
                        0.83
                );


        int minY =
                150;


        for (int x = 0;
             x < width;
             x++) {


            int surfaceY =
                    findSurface(
                            image,
                            x,
                            minY,
                            maxY,
                            portalPairs,
                            blackHoles
                    );


            terrain.setY(
                    x,
                    surfaceY
            );
        }


        smoothTerrain(
                terrain
        );


        return terrain;
    }


    // =========================================================
    // FIND SURFACE
    // =========================================================

    private int findSurface(
            BufferedImage image,
            int x,
            int minY,
            int maxY,
            List<PortalPair> portalPairs,
            List<BlackHole> blackHoles
    ) {

        for (int y = minY;
             y < maxY;
             y++) {


            if (isInsideVisualExclusion(
                    x,
                    y,
                    portalPairs,
                    blackHoles
            )) {


                continue;
            }


            if (isTerrainPixel(
                    image.getRGB(
                            x,
                            y
                    )
            )) {


                if (hasTerrainBelow(
                        image,
                        x,
                        y,
                        maxY,
                        portalPairs,
                        blackHoles
                )) {


                    return y;
                }
            }
        }


        return -1;
    }


    // =========================================================
    // TERRAIN BELOW
    // =========================================================

    private boolean hasTerrainBelow(
            BufferedImage image,
            int x,
            int y,
            int maxY,
            List<PortalPair> portalPairs,
            List<BlackHole> blackHoles
    ) {

        int requiredPixels =
                4;


        int found =
                0;


        for (int offset = 1;
             offset <= 8;
             offset++) {


            int checkY =
                    y
                    +
                    offset;


            if (checkY >= maxY) {

                break;
            }


            if (isInsideVisualExclusion(
                    x,
                    checkY,
                    portalPairs,
                    blackHoles
            )) {


                continue;
            }


            if (isTerrainPixel(
                    image.getRGB(
                            x,
                            checkY
                    )
            )) {


                found++;
            }
        }


        return found
                >=
                requiredPixels;
    }


    // =========================================================
    // VISUAL EXCLUSION
    // =========================================================

    private boolean isInsideVisualExclusion(
            double x,
            double y,
            List<PortalPair> portalPairs,
            List<BlackHole> blackHoles
    ) {

        if (isInsidePortalExclusion(
                x,
                y,
                portalPairs
        )) {


            return true;
        }


        return isInsideBlackHoleExclusion(
                x,
                y,
                blackHoles
        );
    }


    // =========================================================
    // PORTAL MASK
    // =========================================================

    private boolean isInsidePortalExclusion(
            double x,
            double y,
            List<PortalPair> portalPairs
    ) {

        for (PortalPair pair :
                portalPairs) {


            if (isInsidePortalExclusion(
                    x,
                    y,
                    pair.getOrangePortal()
            )) {


                return true;
            }


            if (isInsidePortalExclusion(
                    x,
                    y,
                    pair.getBluePortal()
            )) {


                return true;
            }
        }


        return false;
    }


    private boolean isInsidePortalExclusion(
            double x,
            double y,
            Portal portal
    ) {

        if (portal == null) {

            return false;
        }


        double dx =
                x
                -
                portal.getCenterX();


        double dy =
                y
                -
                portal.getCenterY();


        double radius =
                portal.getRadius()
                +
                PORTAL_EXCLUSION_PADDING;


        return dx * dx
                +
                dy * dy
                <=
                radius * radius;
    }


    // =========================================================
    // BLACK HOLE MASK
    // =========================================================

    private boolean isInsideBlackHoleExclusion(
            double x,
            double y,
            List<BlackHole> blackHoles
    ) {

        for (BlackHole blackHole :
                blackHoles) {


            double dx =
                    x
                    -
                    blackHole.getCenterX();


            double dy =
                    y
                    -
                    blackHole.getCenterY();


            double visualRadius =
                    blackHole.getCoreRadius()
                    *
                    BLACK_HOLE_VISUAL_FACTOR
                    +
                    BLACK_HOLE_VISUAL_PADDING;


            /*
             * Niemals größer als der Influence-Bereich.
             */
            visualRadius =
                    Math.min(
                            visualRadius,
                            blackHole.getInfluenceRadius()
                    );


            if (dx * dx
                    +
                dy * dy
                    <=
                visualRadius
                *
                visualRadius) {


                return true;
            }
        }


        return false;
    }


    // =========================================================
    // COLOR
    // =========================================================

    private boolean isTerrainPixel(
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


        return b >= 110
                &&
                g >= 75
                &&
                b > r * 1.5
                &&
                g > r * 1.3;
    }


    // =========================================================
    // SMOOTH
    // =========================================================

    private void smoothTerrain(
            TerrainProfile terrain
    ) {

        int width =
                terrain.getWidth();


        int[] smoothed =
                new int[width];


        int radius =
                2;


        for (int x = 0;
             x < width;
             x++) {


            int total =
                    0;


            int count =
                    0;


            for (int offset = -radius;
                 offset <= radius;
                 offset++) {


                int nx =
                        x
                        +
                        offset;


                if (nx < 0 ||
                    nx >= width) {


                    continue;
                }


                int y =
                        terrain.getY(
                                nx
                        );


                if (y >= 0) {


                    total +=
                            y;


                    count++;
                }
            }


            if (count > 0) {


                smoothed[x] =
                        total
                        /
                        count;


            } else {


                smoothed[x] =
                        -1;
            }
        }


        for (int x = 0;
             x < width;
             x++) {


            terrain.setY(
                    x,
                    smoothed[x]
            );
        }
    }
}