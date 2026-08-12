package model;

public class Portal {

    public enum PortalColor {
        ORANGE,
        BLUE
    }


    private final PortalColor color;

    private final double centerX;
    private final double centerY;

    /*
     * Radius der tatsächlich betretbaren Portalfläche.
     * NICHT Radius des großen Glows.
     */
    private final double radius;


    public Portal(
            PortalColor color,
            double centerX,
            double centerY,
            double radius
    ) {

        this.color = color;

        this.centerX = centerX;
        this.centerY = centerY;

        this.radius = radius;
    }


    public PortalColor getColor() {

        return color;
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


    public boolean contains(
            double x,
            double y
    ) {

        double dx =
                x - centerX;

        double dy =
                y - centerY;


        return dx * dx + dy * dy
                <=
                radius * radius;
    }


    public double distanceToCenter(
            double x,
            double y
    ) {

        double dx =
                x - centerX;

        double dy =
                y - centerY;


        return Math.sqrt(
                dx * dx
                +
                dy * dy
        );
    }


    @Override
    public String toString() {

        return
                "Portal{"
                +
                "color="
                +
                color
                +
                ", center=("
                +
                String.format("%.1f", centerX)
                +
                ", "
                +
                String.format("%.1f", centerY)
                +
                ")"
                +
                ", radius="
                +
                String.format("%.1f", radius)
                +
                '}';
    }
}