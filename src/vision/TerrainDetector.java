package vision;

import model.Portal;
import model.PortalPair;
import model.TerrainProfile;

import java.awt.Color;
import java.awt.image.BufferedImage;

import java.util.Collections;
import java.util.List;


public class TerrainDetector {

    /*
     * Blue portals have a strong cyan glow that otherwise satisfies
     * the terrain color thresholds.
     *
     * We therefore ignore the whole visible portal region while
     * searching for the terrain surface.
     */
    private static final double PORTAL_EXCLUSION_PADDING =
            28.0;


    // =========================================================
    // OLD API
    // =========================================================

    public TerrainProfile detectTerrain(
            BufferedImage image
    ) {

        return detectTerrain(
                image,
                Collections.emptyList()
        );
    }


    // =========================================================
    // TERRAIN DETECTION WITH PORTAL MASK
    // =========================================================

    public TerrainProfile detectTerrain(
            BufferedImage image,
            List<PortalPair> portalPairs
    ) {

        if (portalPairs == null) {

            portalPairs =
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


        /*
         * Bottom HUD ignored.
         */
        int maxY =
                (int) (
                        height
                        *
                        0.83
                );


        /*
         * Upper HUD ignored.
         */
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
                            portalPairs
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
            List<PortalPair> portalPairs
    ) {

        /*
         * Search from top to bottom.
         *
         * The first sufficiently terrain-like pixel is considered the
         * surface, unless it belongs to a portal exclusion region.
         */
        for (int y = minY;
             y < maxY;
             y++) {


            if (isInsidePortalExclusion(
                    x,
                    y,
                    portalPairs
            )) {

                continue;
            }


            if (isTerrainPixel(
                    image.getRGB(
                            x,
                            y
                    )
            )) {


                /*
                 * A single pixel can be an effect.
                 *
                 * Require multiple terrain pixels below it as well.
                 */
                if (hasTerrainBelow(
                        image,
                        x,
                        y,
                        maxY,
                        portalPairs
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
            List<PortalPair> portalPairs
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


            if (isInsidePortalExclusion(
                    x,
                    checkY,
                    portalPairs
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


        /*
         * Existing ShellShock terrain baseline.
         */
        return b >= 110
                &&
                g >= 75
                &&
                b > r * 1.5
                &&
                g > r * 1.3;
    }


    // =========================================================
    // SMOOTHING
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