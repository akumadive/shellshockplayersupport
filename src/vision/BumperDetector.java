package vision;

import model.Bumper;

import java.awt.Color;
import java.awt.image.BufferedImage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;


public class BumperDetector {

    /*
     * Unteren HUD-Bereich ignorieren.
     */
    private static final double MAX_Y_FACTOR =
            0.82;


    /*
     * Sehr kleine Farbkomponenten ignorieren.
     */
    private static final int MIN_PIXELS =
            18;


    /*
     * Mindestgröße einer erkannten Komponente.
     */
    private static final int MIN_SIZE =
            8;


    /*
     * Extrem große Komponenten sind sehr
     * wahrscheinlich UI / Effekt / Fehlklassifikation.
     */
    private static final int MAX_SIZE =
            500;


    /*
     * Verhältnis zwischen Haupt- und Nebenachse,
     * ab dem eine Komponente als LINE gilt.
     *
     * Ein langer dünner Bumper besitzt eine sehr
     * dominante Hauptachse.
     */
    private static final double LINE_EIGENVALUE_RATIO =
            4.0;


    /*
     * Kreis-Komponenten sollten nicht extrem
     * gestreckt sein.
     */
    private static final double CIRCLE_MIN_RATIO =
            0.60;

    private static final double CIRCLE_MAX_RATIO =
            1.67;


    /*
     * Minimaler Radius für Circle-Bumper.
     */
    private static final double MIN_CIRCLE_RADIUS =
            8.0;


    // =========================================================
    // DETECTION
    // =========================================================

    public List<Bumper> detectBumpers(
            BufferedImage image
    ) {

        int width =
                image.getWidth();

        int height =
                image.getHeight();


        int maxY =
                (int) (
                        height
                        *
                        MAX_Y_FACTOR
                );


        boolean[][] visited =
                new boolean[width][maxY];


        List<Component> components =
                new ArrayList<>();


        // =====================================================
        // CONNECTED COMPONENTS
        // =====================================================

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


                if (!isBumperPixel(
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


                if (component.pixelCount
                        <
                    MIN_PIXELS) {

                    continue;
                }


                components.add(
                        component
                );
            }
        }


        // =====================================================
        // COMPONENT -> BUMPER
        // =====================================================

        List<Bumper> bumpers =
                new ArrayList<>();


        for (Component component :
                components) {


            int componentWidth =
                    component.getWidth();

            int componentHeight =
                    component.getHeight();


            if (componentWidth < MIN_SIZE &&
                componentHeight < MIN_SIZE) {

                continue;
            }


            if (componentWidth > MAX_SIZE ||
                componentHeight > MAX_SIZE) {

                continue;
            }


            Bumper bumper =
                    classifyComponent(
                            component
                    );


            if (bumper != null) {

                bumpers.add(
                        bumper
                );
            }
        }


        return bumpers;
    }


    // =========================================================
    // CLASSIFICATION
    // =========================================================

    private Bumper classifyComponent(
            Component component
    ) {

        /*
         * Wir bestimmen nicht mehr einfach:
         *
         * width > height -> horizontal
         * height > width -> vertical
         *
         * Stattdessen berechnen wir aus ALLEN Pixeln
         * die tatsächliche Hauptachse der Komponente.
         *
         * Damit funktionieren auch schräge Bumper.
         */

        PrincipalAxis axis =
                calculatePrincipalAxis(
                        component
                );


        if (axis == null) {

            return null;
        }


        // =====================================================
        // LINE
        // =====================================================

        /*
         * Große erste Eigenwert-Komponente und kleine
         * zweite Eigenwert-Komponente:
         *
         *          =====================
         *
         * also eine lange dünne Struktur.
         */
        double eigenRatio;


        if (axis.minorEigenvalue
                <=
            0.000001) {

            eigenRatio =
                    Double.POSITIVE_INFINITY;

        } else {

            eigenRatio =
                    axis.majorEigenvalue
                    /
                    axis.minorEigenvalue;
        }


        if (eigenRatio
                >=
            LINE_EIGENVALUE_RATIO) {


            return createLineBumper(
                    component,
                    axis
            );
        }


        // =====================================================
        // CIRCLE
        // =====================================================

        double width =
                component.getWidth();

        double height =
                component.getHeight();


        double ratio =
                width
                /
                height;


        if (ratio >= CIRCLE_MIN_RATIO &&
            ratio <= CIRCLE_MAX_RATIO) {


            double radius =
                    (
                            width
                            +
                            height
                    )
                    /
                    4.0;


            if (radius
                    >=
                MIN_CIRCLE_RADIUS) {


                return Bumper.circle(
                        axis.centerX,
                        axis.centerY,
                        radius
                );
            }
        }


        /*
         * Uneindeutige Komponente lieber ignorieren,
         * als einen falschen Bumper in die spätere
         * Physik einzubauen.
         */
        return null;
    }


    // =========================================================
    // CREATE LINE USING PCA
    // =========================================================

    private Bumper createLineBumper(
            Component component,
            PrincipalAxis axis
    ) {

        /*
         * Alle Bumper-Pixel werden auf die erkannte
         * Hauptachse projiziert.
         *
         * Kleinste Projektion = Start
         * Größte Projektion  = Ende
         *
         * Damit erhalten wir die tatsächlichen
         * Endpunkte eines schrägen Bumpers.
         */

        double minProjection =
                Double.POSITIVE_INFINITY;

        double maxProjection =
                Double.NEGATIVE_INFINITY;


        for (PixelPoint point :
                component.points) {


            double dx =
                    point.x
                    -
                    axis.centerX;

            double dy =
                    point.y
                    -
                    axis.centerY;


            double projection =
                    dx * axis.directionX
                    +
                    dy * axis.directionY;


            minProjection =
                    Math.min(
                            minProjection,
                            projection
                    );


            maxProjection =
                    Math.max(
                            maxProjection,
                            projection
                    );
        }


        if (!Double.isFinite(
                minProjection
        ) ||
            !Double.isFinite(
                    maxProjection
            )) {

            return null;
        }


        double startX =
                axis.centerX
                +
                axis.directionX
                *
                minProjection;

        double startY =
                axis.centerY
                +
                axis.directionY
                *
                minProjection;


        double endX =
                axis.centerX
                +
                axis.directionX
                *
                maxProjection;

        double endY =
                axis.centerY
                +
                axis.directionY
                *
                maxProjection;


        double dx =
                endX
                -
                startX;

        double dy =
                endY
                -
                startY;


        double length =
                Math.sqrt(
                        dx * dx
                        +
                        dy * dy
                );


        /*
         * Winzige "Linien" ignorieren.
         */
        if (length
                <
            MIN_SIZE) {

            return null;
        }


        return Bumper.line(
                startX,
                startY,
                endX,
                endY
        );
    }


    // =========================================================
    // PRINCIPAL COMPONENT ANALYSIS
    // =========================================================

    private PrincipalAxis calculatePrincipalAxis(
            Component component
    ) {

        if (component.points.size()
                <
            2) {

            return null;
        }


        // =====================================================
        // CENTER / MEAN
        // =====================================================

        double centerX =
                0.0;

        double centerY =
                0.0;


        for (PixelPoint point :
                component.points) {


            centerX +=
                    point.x;

            centerY +=
                    point.y;
        }


        centerX /=
                component.points.size();

        centerY /=
                component.points.size();


        // =====================================================
        // COVARIANCE MATRIX
        // =====================================================

        double covarianceXX =
                0.0;

        double covarianceXY =
                0.0;

        double covarianceYY =
                0.0;


        for (PixelPoint point :
                component.points) {


            double dx =
                    point.x
                    -
                    centerX;

            double dy =
                    point.y
                    -
                    centerY;


            covarianceXX +=
                    dx * dx;

            covarianceXY +=
                    dx * dy;

            covarianceYY +=
                    dy * dy;
        }


        double count =
                component.points.size();


        covarianceXX /=
                count;

        covarianceXY /=
                count;

        covarianceYY /=
                count;


        // =====================================================
        // EIGENVALUES
        // =====================================================

        /*
         * Matrix:
         *
         * [ xx  xy ]
         * [ xy  yy ]
         *
         *
         * Eigenwerte analytisch bestimmen.
         */

        double trace =
                covarianceXX
                +
                covarianceYY;


        double difference =
                covarianceXX
                -
                covarianceYY;


        double root =
                Math.sqrt(
                        difference * difference
                        +
                        4.0
                        *
                        covarianceXY
                        *
                        covarianceXY
                );


        double majorEigenvalue =
                (
                        trace
                        +
                        root
                )
                /
                2.0;


        double minorEigenvalue =
                (
                        trace
                        -
                        root
                )
                /
                2.0;


        // =====================================================
        // PRINCIPAL EIGENVECTOR
        // =====================================================

        double directionX;

        double directionY;


        /*
         * Normalfall:
         *
         * Eigenvektor zur größten Varianz bestimmen.
         */
        if (Math.abs(
                covarianceXY
        ) > 0.000001) {


            directionX =
                    majorEigenvalue
                    -
                    covarianceYY;

            directionY =
                    covarianceXY;


        } else {


            /*
             * Perfekt horizontale bzw. vertikale
             * Komponente.
             */
            if (covarianceXX
                    >=
                covarianceYY) {


                directionX =
                        1.0;

                directionY =
                        0.0;


            } else {


                directionX =
                        0.0;

                directionY =
                        1.0;
            }
        }


        // =====================================================
        // NORMALIZE
        // =====================================================

        double directionLength =
                Math.sqrt(
                        directionX * directionX
                        +
                        directionY * directionY
                );


        if (directionLength
                <
            0.000001) {

            return null;
        }


        directionX /=
                directionLength;

        directionY /=
                directionLength;


        /*
         * Für reproduzierbare Richtung sorgen.
         *
         * Hauptsächlich fürs Debugging praktisch:
         * Start/Ende drehen dadurch nicht zufällig um.
         */
        if (directionX < 0.0 ||
            (
                    Math.abs(directionX)
                            <
                    0.000001
                    &&
                    directionY < 0.0
            )) {


            directionX =
                    -directionX;

            directionY =
                    -directionY;
        }


        return new PrincipalAxis(
                centerX,
                centerY,

                directionX,
                directionY,

                majorEigenvalue,
                minorEigenvalue
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


            /*
             * WICHTIG:
             *
             * Jetzt 8er-Nachbarschaft statt nur:
             *
             * oben / unten / links / rechts.
             *
             * Schräge Pixelketten sind häufig nur
             * diagonal miteinander verbunden.
             *
             * Ohne diagonale Nachbarn kann ein
             * schräger Bumper in mehrere Komponenten
             * zerfallen.
             */

            addNeighbor(
                    image,
                    x - 1,
                    y - 1,
                    maxY,
                    visited,
                    queue
            );


            addNeighbor(
                    image,
                    x,
                    y - 1,
                    maxY,
                    visited,
                    queue
            );


            addNeighbor(
                    image,
                    x + 1,
                    y - 1,
                    maxY,
                    visited,
                    queue
            );


            addNeighbor(
                    image,
                    x - 1,
                    y,
                    maxY,
                    visited,
                    queue
            );


            addNeighbor(
                    image,
                    x + 1,
                    y,
                    maxY,
                    visited,
                    queue
            );


            addNeighbor(
                    image,
                    x - 1,
                    y + 1,
                    maxY,
                    visited,
                    queue
            );


            addNeighbor(
                    image,
                    x,
                    y + 1,
                    maxY,
                    visited,
                    queue
            );


            addNeighbor(
                    image,
                    x + 1,
                    y + 1,
                    maxY,
                    visited,
                    queue
            );
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


        if (isBumperPixel(
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
    // BUMPER COLOR
    // =========================================================

    private boolean isBumperPixel(
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
         * Pink / violetter Glow der ShellShock-Bumper.
         *
         * Wir nehmen bewusst NICHT nur einen festen
         * RGB-Wert, weil:
         *
         * - Glow
         * - Anti-Aliasing
         * - Hintergrund
         *
         * unterschiedliche Pixelwerte erzeugen.
         */

        boolean enoughRed =
                r >= 120;


        boolean enoughBlue =
                b >= 100;


        boolean purpleDominance =
                r >= g * 1.20
                &&
                b >= g * 1.10;


        boolean brightness =
                r + b
                >=
                280;


        return enoughRed
                &&
                enoughBlue
                &&
                purpleDominance
                &&
                brightness;
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


        /*
         * Neu:
         *
         * Wir speichern sämtliche Pixel der
         * Komponente, damit anschließend die
         * tatsächliche Hauptachse berechnet
         * werden kann.
         */
        private final List<PixelPoint> points =
                new ArrayList<>();


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


            points.add(
                    new PixelPoint(
                            x,
                            y
                    )
            );
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


    // =========================================================
    // PIXEL POINT
    // =========================================================

    private static class PixelPoint {

        private final double x;
        private final double y;


        private PixelPoint(
                double x,
                double y
        ) {

            this.x = x;
            this.y = y;
        }
    }


    // =========================================================
    // PRINCIPAL AXIS
    // =========================================================

    private static class PrincipalAxis {

        private final double centerX;
        private final double centerY;

        private final double directionX;
        private final double directionY;

        private final double majorEigenvalue;
        private final double minorEigenvalue;


        private PrincipalAxis(
                double centerX,
                double centerY,

                double directionX,
                double directionY,

                double majorEigenvalue,
                double minorEigenvalue
        ) {

            this.centerX = centerX;
            this.centerY = centerY;

            this.directionX = directionX;
            this.directionY = directionY;

            this.majorEigenvalue =
                    majorEigenvalue;

            this.minorEigenvalue =
                    minorEigenvalue;
        }
    }
}