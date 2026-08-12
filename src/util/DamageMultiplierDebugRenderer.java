package util;

import model.DamageMultiplier;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import java.awt.image.BufferedImage;

import java.util.List;


public class DamageMultiplierDebugRenderer {

    public BufferedImage draw(
            BufferedImage source,
            List<DamageMultiplier> multipliers
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


        for (DamageMultiplier multiplier :
                multipliers) {


            drawMultiplier(
                    graphics,
                    multiplier
            );
        }


        graphics.dispose();


        return result;
    }


    private void drawMultiplier(
            Graphics2D graphics,
            DamageMultiplier multiplier
    ) {

        int centerX =
                (int) Math.round(
                        multiplier.getCenterX()
                );


        int centerY =
                (int) Math.round(
                        multiplier.getCenterY()
                );


        int radius =
                (int) Math.round(
                        multiplier.getRadius()
                );


        int diameter =
                radius * 2;


        /*
         * X3 bekommt Cyan,
         * X2 Gelb.
         *
         * So sehen wir sofort, ob die
         * Größenklassifizierung stimmt.
         */
        if (multiplier.getType()
                ==
            DamageMultiplier.MultiplierType.X3) {


            graphics.setColor(
                    Color.CYAN
            );


        } else {


            graphics.setColor(
                    Color.YELLOW
            );
        }


        graphics.drawOval(
                centerX - radius,
                centerY - radius,
                diameter,
                diameter
        );


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


        String text =
                multiplier.getType()
                +
                "  r="
                +
                String.format(
                        "%.1f",
                        multiplier.getRadius()
                );


        graphics.drawString(
                text,
                centerX + radius + 5,
                centerY
        );
    }
}