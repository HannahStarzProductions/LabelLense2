import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

// OpenCV imports
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.core.CvType;
import org.opencv.core.Size;

// ZXing imports
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;

// For converting Mat -> BufferedImage -> JavaFX Image
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class LabelLenseApp extends Application {

    // Load the OpenCV native library
    static {
        // If you’re using -Djava.library.path=<folder>, you can load by name:
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // Alternatively, if you prefer an explicit path:
        // System.load("C:/path/to/opencv_java460.dll");
    }

    private VideoCapture capture;
    private boolean cameraActive = false;
    private Thread cameraThread;

    // GUI controls
    private ImageView cameraView;
    private TextArea scanResultsArea;
    private Button startStopCameraBtn;
    private Button scanBarcodeBtn;

    @Override
    public void start(Stage primaryStage) {
        // Build the GUI
        cameraView = new ImageView();
        cameraView.setFitWidth(640);
        cameraView.setFitHeight(480);
        cameraView.setPreserveRatio(true);

        scanResultsArea = new TextArea();
        scanResultsArea.setPrefRowCount(8);
        scanResultsArea.setWrapText(true);

        startStopCameraBtn = new Button("Start Camera");
        startStopCameraBtn.setOnAction(e -> onStartStopCamera());

        scanBarcodeBtn = new Button("Scan Barcode");
        scanBarcodeBtn.setDisable(true);
        scanBarcodeBtn.setOnAction(e -> onScanBarcode());

        HBox buttonBox = new HBox(10, startStopCameraBtn, scanBarcodeBtn);

        VBox rightPane = new VBox(10, new Label("Scan Results:"), scanResultsArea);

        BorderPane root = new BorderPane();
        root.setCenter(cameraView);
        root.setBottom(buttonBox);
        root.setRight(rightPane);

        Scene scene = new Scene(root, 900, 500);
        primaryStage.setTitle("LabelLense - Desktop App");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Handle the Start/Stop Camera button.
     */
    private void onStartStopCamera() {
        if (!cameraActive) {
            // Start camera
            capture = new VideoCapture();
            // 0 -> default webcam index
            capture.open(0);

            if (capture.isOpened()) {
                cameraActive = true;
                startStopCameraBtn.setText("Stop Camera");
                scanBarcodeBtn.setDisable(false);

                // Start the video capture thread
                cameraThread = new Thread(() -> {
                    Mat frame = new Mat();
                    while (cameraActive) {
                        if (capture.read(frame)) {
                            // Optionally, do some processing on frame
                            // e.g. Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);

                            Image fxImage = mat2Image(frame);
                            // Show the image in the GUI
                            Platform.runLater(() -> cameraView.setImage(fxImage));
                        }
                    }
                });
                cameraThread.setDaemon(true);
                cameraThread.start();
            } else {
                System.err.println("Could not open the camera...");
            }
        } else {
            // Stop camera
            cameraActive = false;
            startStopCameraBtn.setText("Start Camera");
            scanBarcodeBtn.setDisable(true);

            // Release the capture and let the thread exit
            if (capture != null && capture.isOpened()) {
                capture.release();
            }
            cameraView.setImage(null);
        }
    }

    /**
     * Handle the "Scan Barcode" button: take a single frame and attempt to decode it.
     */
    private void onScanBarcode() {
        if (cameraActive && capture != null && capture.isOpened()) {
            Mat frame = new Mat();
            if (capture.read(frame)) {
                // Convert frame to a BufferedImage
                BufferedImage buffered = mat2BufferedImage(frame);

                // Attempt to decode the barcode/QR in this image
                String result = decodeBarcode(buffered);
                if (result != null) {
                    scanResultsArea.appendText("Scanned Barcode: " + result + "\n");
                    // Fake database / mock call
                    String productInfo = fetchProductInfo(result);
                    scanResultsArea.appendText("Product Info:\n" + productInfo + "\n\n");
                } else {
                    scanResultsArea.appendText("No barcode detected in this frame.\n");
                }
            }
        }
    }

    /**
     * Stub method to fetch product info from a database or API, given a barcode string.
     * Replace this with a real lookup (Open Food Facts, USDA, etc.).
     */
    private String fetchProductInfo(String barcode) {
        return "Mock Product Name: Example Food\n"
                + "Calories: 200\n"
                + "Total Fat: 8g\n"
                + "Sugars: 12g\n"
                + "(Barcode: " + barcode + ")\n";
    }

    /**
     * Use ZXing to decode a barcode (UPC/EAN/QR/etc.) from a BufferedImage.
     * Returns the text if found; otherwise null.
     */
    private String decodeBarcode(BufferedImage image) {
        if (image == null) return null;
        try {
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result result = new MultiFormatReader().decode(bitmap);
            return result.getText();
        } catch (NotFoundException e) {
            // No barcode found
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Convert an OpenCV Mat to a JavaFX Image.
     */
    private Image mat2Image(Mat frame) {
        try {
            return SwingFXUtilsExtension.toFXImage(mat2BufferedImage(frame));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Convert an OpenCV Mat to a BufferedImage (for ZXing or other Java APIs).
     */
    private BufferedImage mat2BufferedImage(Mat original) {
        // Convert BGR -> RGB
        Mat rgb = new Mat();
        Imgproc.cvtColor(original, rgb, Imgproc.COLOR_BGR2RGB);

        int width = rgb.width();
        int height = rgb.height();
        int channels = rgb.channels();
        byte[] source = new byte[width * height * channels];
        rgb.get(0, 0, source);

        // Create new BufferedImage
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        byte[] target = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(source, 0, target, 0, source.length);

        return image;
    }

    /**
     * A minimal wrapper to convert a BufferedImage to a JavaFX Image without using
     * the entire SwingFXUtils class (which is part of javafx.swing).
     * If you prefer, you can import javafx.embed.swing.SwingFXUtils directly instead.
     */
    private static class SwingFXUtilsExtension {
        static Image toFXImage(BufferedImage bImage) throws IOException {
            if (bImage == null) return null;
            ByteArrayInputStream inputStream;
            try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
                ImageIO.write(bImage, "png", out);
                inputStream = new ByteArrayInputStream(out.toByteArray());
            }
            return new Image(inputStream);
        }
    }

    @Override
    public void stop() {
        // Clean up camera if the app is closed
        cameraActive = false;
        if (capture != null && capture.isOpened()) {
            capture.release();
        }
        if (cameraThread != null && cameraThread.isAlive()) {
            cameraThread.interrupt();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
