package model;

public class PortalPair {

    private final int id;

    private final Portal orangePortal;
    private final Portal bluePortal;


    public PortalPair(
            int id,
            Portal orangePortal,
            Portal bluePortal
    ) {

        this.id = id;

        this.orangePortal =
                orangePortal;

        this.bluePortal =
                bluePortal;
    }


    public int getId() {

        return id;
    }


    public Portal getOrangePortal() {

        return orangePortal;
    }


    public Portal getBluePortal() {

        return bluePortal;
    }


    /*
     * Liefert bei einem Portal das andere Portal
     * desselben Paares.
     */
    public Portal getOtherPortal(
            Portal portal
    ) {

        if (portal == orangePortal) {

            return bluePortal;
        }


        if (portal == bluePortal) {

            return orangePortal;
        }


        return null;
    }


    public boolean containsPortal(
            Portal portal
    ) {

        return portal == orangePortal
                ||
                portal == bluePortal;
    }


    @Override
    public String toString() {

        return
                "PortalPair{"
                +
                "id="
                +
                id
                +
                ", orange="
                +
                orangePortal
                +
                ", blue="
                +
                bluePortal
                +
                '}';
    }
}