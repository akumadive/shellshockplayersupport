package app;

import capture.CaptureRegion;
import capture.ScreenCapture;

import model.BlackHole;

import util.BlackHoleDebugRenderer;
import util.ImageUtils;

import vision.BlackHoleDetector;

import java.awt.image.BufferedImage;

import java.util.List;


public class BlackHoleDetectionTest {

    public static void main(String[] args) {

        try {


            // =====================================================
            // SCREENSHOT
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
            // DETECT
            // =====================================================

            BlackHoleDetector detector =
                    new BlackHoleDetector();


            List<BlackHole> blackHoles =
                    detector.detect(
                            screenshot
                    );


            // =====================================================
            // OUTPUT
            // =====================================================

            System.out.println();


            System.out.println(
                    "=============================="
            );


            System.out.println(
                    "BLACK HOLES"
            );


            System.out.println(
                    "=============================="
            );


            System.out.println(
                    "Gefunden: "
                    +
                    blackHoles.size()
            );


            for (BlackHole blackHole :
                    blackHoles) {


                System.out.println(
                        blackHole
                );
            }


            // =====================================================
            // DEBUG IMAGE
            // =====================================================

            BlackHoleDebugRenderer renderer =
                    new BlackHoleDebugRenderer();


            BufferedImage debug =
                    renderer.draw(
                            screenshot,
                            blackHoles
                    );


            ImageUtils.saveImage(
                    debug,
                    "data/screenshots/black_hole_debug.png"
            );


            System.out.println();


            System.out.println(
                    "black_hole_debug.png gespeichert."
            );


        } catch (Exception e) {


            e.printStackTrace();
        }
    }
}