package model;

public class DamageMultiplier {

    public enum MultiplierType {
        X2(2),
        X3(3);

        private final int value;

        MultiplierType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }


    private final MultiplierType type;

    private final double centerX;
    private final double centerY;

    private final double radius;


    public DamageMultiplier(
            MultiplierType type,
            double centerX,
            double centerY,
            double radius
    ) {

        this.type = type;
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = radius;
    }


    public MultiplierType getType() {
        return type;
    }


    public int getValue() {
        return type.getValue();
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


    @Override
    public String toString() {

        return
                "DamageMultiplier{"
                +
                "type="
                +
                type
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