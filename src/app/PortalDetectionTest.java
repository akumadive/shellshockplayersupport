package app;

import capture.CaptureRegion;
import capture.ScreenCapture;

import model.Portal;
import model.PortalPair;

import util.ImageUtils;

import vision.PortalDetector;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import java.awt.image.BufferedImage;

import java.util.List;


public class PortalDetectionTest {

    public static void main(String[] args) {

        try {


            // =====================================================
            // CAPTURE
            // =====================================================

            CaptureRegion region =
                    new CaptureRegion(
                            0,
                            0,
                            1920,
                            1080
                    );


            ScreenCapture screenCapture =
                    new ScreenCapture(
                            region
                    );


            BufferedImage screenshot =
                    screenCapture.capture();


            // =====================================================
            // DETECTION
            // =====================================================

            PortalDetector detector =
                    new PortalDetector();


            List<PortalPair> pairs =
                    detector.detectPortalPairs(
                            screenshot
                    );


            // =====================================================
            // CONSOLE DEBUG
            // =====================================================

            System.out.println();


            System.out.println(
                    "=============================="
            );


            System.out.println(
                    "PORTALS"
            );


            System.out.println(
                    "=============================="
            );


            System.out.println(
                    "Paare gefunden: "
                    +
                    pairs.size()
            );


            for (PortalPair pair :
                    pairs) {


                System.out.println();


                System.out.println(
                        "PAIR "
                        +
                        pair.getId()
                );


                System.out.println(
                        "Orange: "
                        +
                        pair.getOrangePortal()
                );


                System.out.println(
                        "Blue:   "
                        +
                        pair.getBluePortal()
                );
            }


            // =====================================================
            // DEBUG IMAGE
            // =====================================================

            BufferedImage debug =
                    drawPortalDebug(
                            screenshot,
                            pairs
                    );


            ImageUtils.saveImage(
                    debug,
                    "data/screenshots/portal_debug.png"
            );


            System.out.println();


            System.out.println(
                    "portal_debug.png gespeichert."
            );


        } catch (Exception e) {


            e.printStackTrace();
        }
    }


    // =========================================================
    // DEBUG RENDERER
    // =========================================================

    private static BufferedImage drawPortalDebug(
            BufferedImage source,
            List<PortalPair> pairs
    ) {

        BufferedImage result =
                new BufferedImage(
                        source.getWidth(),
                        source.getHeight(),
                        BufferedImage.TYPE_INT_ARGB
                );


        Graphics2D graphics =
                result.createGraphics();


        graphics.drawImage(
                source,
                0,
                0,
                null
        );


        graphics.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );


        graphics.setStroke(
                new BasicStroke(
                        3.0f
                )
        );


        graphics.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.BOLD,
                        16
                )
        );


        for (PortalPair pair :
                pairs) {


            Portal orange =
                    pair.getOrangePortal();


            Portal blue =
                    pair.getBluePortal();


            // =================================================
            // PAIR CONNECTION
            // =================================================

            graphics.setColor(
                    Color.WHITE
            );


            graphics.drawLine(
                    (int) Math.round(
                            orange.getCenterX()
                    ),
                    (int) Math.round(
                            orange.getCenterY()
                    ),

                    (int) Math.round(
                            blue.getCenterX()
                    ),
                    (int) Math.round(
                            blue.getCenterY()
                    )
            );


            // =================================================
            // ORANGE
            // =================================================

            drawPortal(
                    graphics,
                    orange,
                    Color.ORANGE,
                    "ORANGE "
                    +
                    pair.getId()
            );


            // =================================================
            // BLUE
            // =================================================

            drawPortal(
                    graphics,
                    blue,
                    Color.CYAN,
                    "BLUE "
                    +
                    pair.getId()
            );
        }


        graphics.dispose();


        return result;
    }


    private static void drawPortal(
            Graphics2D graphics,
            Portal portal,
            Color color,
            String label
    ) {

        int centerX =
                (int) Math.round(
                        portal.getCenterX()
                );


        int centerY =
                (int) Math.round(
                        portal.getCenterY()
                );


        int radius =
                (int) Math.round(
                        portal.getRadius()
                );


        int diameter =
                radius * 2;


        graphics.setColor(
                color
        );


        graphics.drawOval(
                centerX - radius,
                centerY - radius,
                diameter,
                diameter
        );


        /*
         * Mittelpunkt
         */
        graphics.drawLine(
                centerX - 7,
                centerY,
                centerX + 7,
                centerY
        );


        graphics.drawLine(
                centerX,
                centerY - 7,
                centerX,
                centerY + 7
        );


        graphics.drawString(
                label
                +
                " r="
                +
                String.format(
                        "%.1f",
                        portal.getRadius()
                ),

                centerX + radius + 6,
                centerY
        );
    }
}