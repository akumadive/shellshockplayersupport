package util;

import model.BlackHole;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import java.awt.image.BufferedImage;

import java.util.List;


public class BlackHoleDebugRenderer {

    public BufferedImage draw(
            BufferedImage source,
            List<BlackHole> blackHoles
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


        graphics.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.BOLD,
                        15
                )
        );


        for (BlackHole blackHole :
                blackHoles) {


            drawBlackHole(
                    graphics,
                    blackHole
            );
        }


        graphics.dispose();


        return result;
    }


    private void drawBlackHole(
            Graphics2D graphics,
            BlackHole blackHole
    ) {

        int centerX =
                (int) Math.round(
                        blackHole.getCenterX()
                );


        int centerY =
                (int) Math.round(
                        blackHole.getCenterY()
                );


        // =====================================================
        // INFLUENCE
        // =====================================================

        int influenceRadius =
                (int) Math.round(
                        blackHole.getInfluenceRadius()
                );


        graphics.setStroke(
                new BasicStroke(
                        2.0f
                )
        );


        graphics.setColor(
                Color.CYAN
        );


        graphics.drawOval(
                centerX - influenceRadius,
                centerY - influenceRadius,

                influenceRadius * 2,
                influenceRadius * 2
        );


        // =====================================================
        // CORE
        // =====================================================

        int coreRadius =
                (int) Math.round(
                        blackHole.getCoreRadius()
                );


        graphics.setStroke(
                new BasicStroke(
                        3.0f
                )
        );


        graphics.setColor(
                Color.RED
        );


        graphics.drawOval(
                centerX - coreRadius,
                centerY - coreRadius,

                coreRadius * 2,
                coreRadius * 2
        );


        // =====================================================
        // CENTER
        // =====================================================

        graphics.drawLine(
                centerX - 6,
                centerY,

                centerX + 6,
                centerY
        );


        graphics.drawLine(
                centerX,
                centerY - 6,

                centerX,
                centerY + 6
        );


        // =====================================================
        // LABEL
        // =====================================================

        graphics.setColor(
                Color.WHITE
        );


        graphics.drawString(
                "BLACK HOLE"
                +
                " CORE="
                +
                String.format(
                        "%.1f",
                        blackHole.getCoreRadius()
                )
                +
                " RANGE="
                +
                String.format(
                        "%.1f",
                        blackHole.getInfluenceRadius()
                ),

                centerX + coreRadius + 10,
                centerY
        );
    }
}