import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class Test {
    static {
        // Option 1: If using -Djava.library.path plus the NATIVE_LIBRARY_NAME:
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // Option 2: If loading the exact DLL or .so:
        // System.load("absolute/path/to/opencv_java460.dll");
    }

    public static void main(String[] args) {
        System.out.println("OpenCV version: " + Core.getVersionString());

        // Simple matrix test
        Mat mat = Mat.eye(3, 3, CvType.CV_8UC1);
        System.out.println("Matrix:\n" + mat.dump());
    }
}
