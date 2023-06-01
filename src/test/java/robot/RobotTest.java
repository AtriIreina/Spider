package robot;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class RobotTest {


    //横坐标. 纵坐标
    static int[] windowPosition = new int[]{1600, 850};

    static List<String> urlList = new ArrayList<String>();
    static {
        urlList.add("http://www.fmpan.com/#/s/da2bx66a");
        urlList.add("http://www.ibuspan.com/file/QUE3MjQ3MDI=.html");
    }

    public static void main(String[] args) throws AWTException {
        Robot robot = new Robot();
        for (String url : urlList) {
            ClipboardTest.setClipboardString(url);
            robot.mouseMove(windowPosition[0], windowPosition[1]);
//            robot.mousePress();
//            robot.keyPress();
        }
    }
}
