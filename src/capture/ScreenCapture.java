package capture;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;

public class ScreenCapture {

    private final Robot robot;
    private CaptureRegion captureRegion;

    public ScreenCapture(CaptureRegion captureRegion) throws AWTException {
        this.robot = new Robot();
        this.captureRegion = captureRegion;
    }

    public BufferedImage capture() {

        Rectangle rectangle = new Rectangle(
                captureRegion.getX(),
                captureRegion.getY(),
                captureRegion.getWidth(),
                captureRegion.getHeight()
        );

        return robot.createScreenCapture(rectangle);
    }

    public void setCaptureRegion(CaptureRegion captureRegion) {
        this.captureRegion = captureRegion;
    }

    public CaptureRegion getCaptureRegion() {
        return captureRegion;
    }
}