package vision;

import model.Portal;
import model.PortalPair;

import java.awt.Color;
import java.awt.image.BufferedImage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class PortalDetector {

    // =========================================================
    // SEARCH AREA
    // =========================================================

    /*
     * Unteres HUD ignorieren.
     */
    private static final double MAX_Y_FACTOR =
            0.78;


    /*
     * Portalradius in deinen bisherigen 1920x1080-Screenshots:
     *
     * ungefähr 40 px.
     *
     * Etwas Spielraum für Skalierung / Glow.
     */
    private static final int MIN_RADIUS =
            30;

    private static final int MAX_RADIUS =
            55;


    /*
     * Nicht jeden einzelnen Pixel als Center testen.
     *
     * 3 reicht bei einem ~40px Portal völlig.
     */
    private static final int CENTER_STEP =
            3;


    /*
     * Radius wird ebenfalls nicht in 1px-Schritten getestet.
     */
    private static final int RADIUS_STEP =
            2;


    // =========================================================
    // CENTER DARKNESS
    // =========================================================

    /*
     * Portalinnenraum ist fast schwarz.
     *
     * Wir prüfen nicht nur einen Pixel, sondern mehrere
     * Punkte um den Kandidatenmittelpunkt herum.
     */
    private static final int DARK_CENTER_MAX_BRIGHTNESS =
            115;


    /*
     * Mindestens dieser Anteil der geprüften inneren
     * Punkte muss dunkel sein.
     */
    private static final double MIN_DARK_CENTER_RATIO =
            0.78;


    // =========================================================
    // RING TEST
    // =========================================================

    /*
     * Anzahl Winkelproben rund um den Kreis.
     */
    private static final int RING_SAMPLES =
            32;


    /*
     * Mindestanteil der Ringpunkte, die zur Farbe
     * passen müssen.
     *
     * Absichtlich nicht 100 %, weil:
     *
     * - Terrain davor liegen kann
     * - Sparks existieren
     * - Anti-Aliasing
     * - sichtbare Nummer im Portal
     */
    private static final double MIN_RING_MATCH_RATIO =
            0.44;


    /*
     * Zusätzlich muss außerhalb des dunklen Innenraums
     * ein deutlicher Helligkeitsanstieg vorliegen.
     */
    private static final double MIN_RING_CONTRAST_RATIO =
            0.55;


    // =========================================================
    // CANDIDATE SCORE
    // =========================================================

    /*
     * Kandidaten unterhalb dieses Scores ignorieren.
     */
    private static final double MIN_SCORE =
            0.52;


    /*
     * Mehrere nahe Center-Kandidaten gehören zum
     * selben Portal.
     */
    private static final double DUPLICATE_DISTANCE =
            35.0;


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
         * Aktuelle Pair-Strategie:
         *
         * gleiche vertikale Reihenfolge.
         *
         * Das entspricht allen bisher gesehenen
         * Fällen:
         *
         * Orange oben <-> Blau oben
         * Orange unten <-> Blau unten
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


        int pairCount =
                Math.min(
                        orange.size(),
                        blue.size()
                );


        List<PortalPair> pairs =
                new ArrayList<>();


        for (int i = 0;
             i < pairCount;
             i++) {


            int pairId =
                    pairCount
                    -
                    i;


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

        List<PortalCandidate> candidates =
                findPortalCandidates(
                        image
                );


        /*
         * Beste Kandidaten zuerst.
         */
        candidates.sort(
                (
                        first,
                        second
                ) ->
                        Double.compare(
                                second.score,
                                first.score
                        )
        );


        List<PortalCandidate> accepted =
                new ArrayList<>();


        /*
         * Non-Maximum-Suppression.
         *
         * Ein echtes Portal erzeugt wegen CENTER_STEP
         * viele Kandidaten direkt nebeneinander.
         */
        for (PortalCandidate candidate :
                candidates) {


            boolean duplicate =
                    false;


            for (PortalCandidate existing :
                    accepted) {


                double dx =
                        candidate.centerX
                        -
                        existing.centerX;


                double dy =
                        candidate.centerY
                        -
                        existing.centerY;


                double distance =
                        Math.sqrt(
                                dx * dx
                                +
                                dy * dy
                        );


                if (distance
                        <
                    DUPLICATE_DISTANCE) {


                    duplicate =
                            true;


                    break;
                }
            }


            if (!duplicate) {


                accepted.add(
                        candidate
                );
            }
        }


        List<Portal> result =
                new ArrayList<>();


        for (PortalCandidate candidate :
                accepted) {


            result.add(
                    new Portal(
                            candidate.color,
                            candidate.centerX,
                            candidate.centerY,
                            candidate.radius
                    )
            );
        }


        return result;
    }


    // =========================================================
    // CANDIDATE SEARCH
    // =========================================================

    private List<PortalCandidate> findPortalCandidates(
            BufferedImage image
    ) {

        List<PortalCandidate> candidates =
                new ArrayList<>();


        int width =
                image.getWidth();


        int maxY =
                (int) Math.round(
                        image.getHeight()
                        *
                        MAX_Y_FACTOR
                );


        /*
         * Genug Abstand vom Bildrand für MAX_RADIUS.
         */
        for (int y = MAX_RADIUS;
             y < maxY - MAX_RADIUS;
             y += CENTER_STEP) {


            for (int x = MAX_RADIUS;
                 x < width - MAX_RADIUS;
                 x += CENTER_STEP) {


                // =============================================
                // FAST CENTER REJECTION
                // =============================================

                if (!looksLikeDarkPortalCenter(
                        image,
                        x,
                        y
                )) {


                    continue;
                }


                // =============================================
                // TEST RADII
                // =============================================

                PortalCandidate bestHere =
                        null;


                for (int radius = MIN_RADIUS;
                     radius <= MAX_RADIUS;
                     radius += RADIUS_STEP) {


                    PortalCandidate orangeCandidate =
                            evaluateCandidate(
                                    image,
                                    x,
                                    y,
                                    radius,
                                    Portal.PortalColor.ORANGE
                            );


                    if (orangeCandidate != null &&
                        (
                                bestHere == null
                                ||
                                orangeCandidate.score
                                        >
                                bestHere.score
                        )) {


                        bestHere =
                                orangeCandidate;
                    }


                    PortalCandidate blueCandidate =
                            evaluateCandidate(
                                    image,
                                    x,
                                    y,
                                    radius,
                                    Portal.PortalColor.BLUE
                            );


                    if (blueCandidate != null &&
                        (
                                bestHere == null
                                ||
                                blueCandidate.score
                                        >
                                bestHere.score
                        )) {


                        bestHere =
                                blueCandidate;
                    }
                }


                if (bestHere != null &&
                    bestHere.score
                            >=
                    MIN_SCORE) {


                    candidates.add(
                            bestHere
                    );
                }
            }
        }


        return candidates;
    }


    // =========================================================
    // DARK CENTER
    // =========================================================

    private boolean looksLikeDarkPortalCenter(
            BufferedImage image,
            int centerX,
            int centerY
    ) {

        /*
         * Punkte innerhalb des Portalinnenraums.
         *
         * Nicht zu weit außen testen, damit der Ring
         * noch nicht erfasst wird.
         */
        int[][] offsets = {

                {0, 0},

                {8, 0},
                {-8, 0},

                {0, 8},
                {0, -8},

                {12, 8},
                {-12, 8},

                {12, -8},
                {-12, -8},

                {18, 0},
                {-18, 0},

                {0, 18},
                {0, -18}
        };


        int dark =
                0;


        int total =
                0;


        for (int[] offset :
                offsets) {


            int x =
                    centerX
                    +
                    offset[0];


            int y =
                    centerY
                    +
                    offset[1];


            if (x < 0 ||
                y < 0 ||
                x >= image.getWidth() ||
                y >= image.getHeight()) {


                continue;
            }


            total++;


            int brightness =
                    brightness(
                            image.getRGB(
                                    x,
                                    y
                            )
                    );


            if (brightness
                    <=
                DARK_CENTER_MAX_BRIGHTNESS) {


                dark++;
            }
        }


        if (total == 0) {

            return false;
        }


        double ratio =
                (double) dark
                /
                total;


        return ratio
                >=
                MIN_DARK_CENTER_RATIO;
    }


    // =========================================================
    // EVALUATE PORTAL
    // =========================================================

    private PortalCandidate evaluateCandidate(
            BufferedImage image,

            double centerX,
            double centerY,

            double radius,

            Portal.PortalColor color
    ) {

        int ringMatches =
                0;


        int contrastMatches =
                0;


        int validSamples =
                0;


        /*
         * Wir testen drei Radien:
         *
         * innerRadius -> sollte dunkel sein
         * radius      -> farbiger Ring
         * outerRadius -> Glow / Umgebung
         */
        double innerRadius =
                radius
                *
                0.72;


        for (int sample = 0;
             sample < RING_SAMPLES;
             sample++) {


            double angle =
                    2.0
                    *
                    Math.PI
                    *
                    sample
                    /
                    RING_SAMPLES;


            double cos =
                    Math.cos(
                            angle
                    );


            double sin =
                    Math.sin(
                            angle
                    );


            int ringX =
                    (int) Math.round(
                            centerX
                            +
                            cos
                            *
                            radius
                    );


            int ringY =
                    (int) Math.round(
                            centerY
                            +
                            sin
                            *
                            radius
                    );


            int innerX =
                    (int) Math.round(
                            centerX
                            +
                            cos
                            *
                            innerRadius
                    );


            int innerY =
                    (int) Math.round(
                            centerY
                            +
                            sin
                            *
                            innerRadius
                    );


            if (!insideImage(
                    image,
                    ringX,
                    ringY
            ) ||
                !insideImage(
                        image,
                        innerX,
                        innerY
                )) {


                continue;
            }


            validSamples++;


            int ringRgb =
                    image.getRGB(
                            ringX,
                            ringY
                    );


            int innerRgb =
                    image.getRGB(
                            innerX,
                            innerY
                    );


            if (matchesPortalRing(
                    ringRgb,
                    color
            )) {


                ringMatches++;
            }


            int ringBrightness =
                    brightness(
                            ringRgb
                    );


            int innerBrightness =
                    brightness(
                            innerRgb
                    );


            /*
             * Portalring sollte deutlich heller sein
             * als seine Innenöffnung.
             */
            if (ringBrightness
                    >=
                innerBrightness
                +
                45) {


                contrastMatches++;
            }
        }


        if (validSamples == 0) {

            return null;
        }


        double ringRatio =
                (double) ringMatches
                /
                validSamples;


        double contrastRatio =
                (double) contrastMatches
                /
                validSamples;


        if (ringRatio
                <
            MIN_RING_MATCH_RATIO) {


            return null;
        }


        if (contrastRatio
                <
            MIN_RING_CONTRAST_RATIO) {


            return null;
        }


        /*
         * Ringfarbe ist wichtiger als Kontrast.
         */
        double score =
                ringRatio
                *
                0.70
                +
                contrastRatio
                *
                0.30;


        return new PortalCandidate(
                color,
                centerX,
                centerY,
                radius,
                score
        );
    }


    // =========================================================
    // PORTAL RING COLORS
    // =========================================================

    private boolean matchesPortalRing(
            int rgb,
            Portal.PortalColor color
    ) {

        if (color
                ==
            Portal.PortalColor.ORANGE) {


            return isOrangeRing(
                    rgb
            );
        }


        return isBlueRing(
                rgb
        );
    }


    private boolean isOrangeRing(
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
         * Orange / gold.
         *
         * Hier darf der Filter wieder relativ streng sein,
         * weil wir nicht mehr ganze Komponenten verbinden.
         */
        boolean redStrong =
                r >= 125;


        boolean greenPresent =
                g >= 45;


        boolean redDominates =
                r >= g * 1.15
                &&
                r >= b * 1.45;


        boolean notBlue =
                b <= 145;


        return redStrong
                &&
                greenPresent
                &&
                redDominates
                &&
                notBlue;
    }


    private boolean isBlueRing(
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
         * Blau / Cyan.
         *
         * Terrain kann dieselbe Farbe haben.
         * Das ist jetzt aber okay:
         *
         * Ein Terrainpixel allein reicht nicht mehr.
         * Es muss kreisförmig um eine dunkle Öffnung
         * verteilt sein.
         */
        boolean blueStrong =
                b >= 115;


        boolean greenPresent =
                g >= 55;


        boolean blueDominates =
                b >= r * 1.25;


        boolean notTooRed =
                r <= 125;


        return blueStrong
                &&
                greenPresent
                &&
                blueDominates
                &&
                notTooRed;
    }


    // =========================================================
    // HELPERS
    // =========================================================

    private boolean insideImage(
            BufferedImage image,
            int x,
            int y
    ) {

        return x >= 0
                &&
                y >= 0
                &&
                x < image.getWidth()
                &&
                y < image.getHeight();
    }


    private int brightness(
            int rgb
    ) {

        Color color =
                new Color(
                        rgb
                );


        /*
         * Perceptual-ish brightness.
         *
         * Wertebereich weiterhin ungefähr 0..255.
         */
        return (int) Math.round(
                color.getRed()
                        *
                        0.2126
                +
                color.getGreen()
                        *
                        0.7152
                +
                color.getBlue()
                        *
                        0.0722
        );
    }


    // =========================================================
    // INTERNAL CANDIDATE
    // =========================================================

    private static class PortalCandidate {

        private final Portal.PortalColor color;

        private final double centerX;
        private final double centerY;

        private final double radius;

        private final double score;


        private PortalCandidate(
                Portal.PortalColor color,

                double centerX,
                double centerY,

                double radius,

                double score
        ) {

            this.color =
                    color;


            this.centerX =
                    centerX;


            this.centerY =
                    centerY;


            this.radius =
                    radius;


            this.score =
                    score;
        }
    }
}