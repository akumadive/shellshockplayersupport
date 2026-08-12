package model;

public class Bumper {

    public enum BumperType {
        LINE,
        CIRCLE
    }


    private final BumperType type;

    private final double centerX;
    private final double centerY;

    private final double radius;

    private final double startX;
    private final double startY;

    private final double endX;
    private final double endY;


    /*
     * =========================================================
     * CIRCLE BUMPER
     * =========================================================
     */
    public static Bumper circle(
            double centerX,
            double centerY,
            double radius
    ) {

        return new Bumper(
                BumperType.CIRCLE,

                centerX,
                centerY,

                radius,

                0,
                0,

                0,
                0
        );
    }


    /*
     * =========================================================
     * LINE BUMPER
     * =========================================================
     */
    public static Bumper line(
            double startX,
            double startY,
            double endX,
            double endY
    ) {

        double centerX =
                (startX + endX)
                /
                2.0;

        double centerY =
                (startY + endY)
                /
                2.0;


        return new Bumper(
                BumperType.LINE,

                centerX,
                centerY,

                0,

                startX,
                startY,

                endX,
                endY
        );
    }


    private Bumper(
            BumperType type,

            double centerX,
            double centerY,

            double radius,

            double startX,
            double startY,

            double endX,
            double endY
    ) {

        this.type = type;

        this.centerX = centerX;
        this.centerY = centerY;

        this.radius = radius;

        this.startX = startX;
        this.startY = startY;

        this.endX = endX;
        this.endY = endY;
    }


    public BumperType getType() {
        return type;
    }


    public double getCenterX() {
        return centerX;
    }


    public double getCenterY() {
        return centerY;
    }


    public double getRadius() {
        return radius;
    }


    public double getStartX() {
        return startX;
    }


    public double getStartY() {
        return startY;
    }


    public double getEndX() {
        return endX;
    }


    public double getEndY() {
        return endY;
    }


    public double getLength() {

        double dx =
                endX
                -
                startX;

        double dy =
                endY
                -
                startY;


        return Math.sqrt(
                dx * dx
                +
                dy * dy
        );
    }


    @Override
    public String toString() {

        if (type == BumperType.CIRCLE) {

            return
                    "Bumper{"
                    +
                    "type=CIRCLE"
                    +
                    ", center=("
                    +
                    centerX
                    +
                    ", "
                    +
                    centerY
                    +
                    ")"
                    +
                    ", radius="
                    +
                    radius
                    +
                    '}';
        }


        return
                "Bumper{"
                +
                "type=LINE"
                +
                ", start=("
                +
                startX
                +
                ", "
                +
                startY
                +
                ")"
                +
                ", end=("
                +
                endX
                +
                ", "
                +
                endY
                +
                ")"
                +
                '}';
    }
}