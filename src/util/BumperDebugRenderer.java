package util;

import model.Bumper;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import java.awt.image.BufferedImage;

import java.util.List;


public class BumperDebugRenderer {

    public BufferedImage drawBumpers(
            BufferedImage source,
            List<Bumper> bumpers
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


        for (Bumper bumper :
                bumpers) {


            if (bumper.getType()
                    ==
                Bumper.BumperType.CIRCLE) {


                drawCircle(
                        graphics,
                        bumper
                );


            } else {


                drawLine(
                        graphics,
                        bumper
                );
            }
        }


        graphics.dispose();


        return result;
    }


    private void drawCircle(
            Graphics2D graphics,
            Bumper bumper
    ) {

        double radius =
                bumper.getRadius();


        int x =
                (int) Math.round(
                        bumper.getCenterX()
                        -
                        radius
                );

        int y =
                (int) Math.round(
                        bumper.getCenterY()
                        -
                        radius
                );


        int diameter =
                (int) Math.round(
                        radius
                        *
                        2.0
                );


        graphics.setColor(
                Color.MAGENTA
        );


        graphics.drawOval(
                x,
                y,
                diameter,
                diameter
        );


        graphics.drawString(
                "BUMPER CIRCLE",
                x,
                Math.max(
                        15,
                        y - 5
                )
        );
    }


    private void drawLine(
            Graphics2D graphics,
            Bumper bumper
    ) {

        graphics.setColor(
                Color.MAGENTA
        );


        int startX =
                (int) Math.round(
                        bumper.getStartX()
                );

        int startY =
                (int) Math.round(
                        bumper.getStartY()
                );


        int endX =
                (int) Math.round(
                        bumper.getEndX()
                );

        int endY =
                (int) Math.round(
                        bumper.getEndY()
                );


        graphics.drawLine(
                startX,
                startY,
                endX,
                endY
        );


        graphics.fillOval(
                startX - 4,
                startY - 4,
                8,
                8
        );


        graphics.fillOval(
                endX - 4,
                endY - 4,
                8,
                8
        );


        graphics.drawString(
                "BUMPER LINE",
                (startX + endX) / 2,
                Math.max(
                        15,
                        (startY + endY) / 2 - 8
                )
        );
    }
}