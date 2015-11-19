package fall2015.uco.robotics.colorobjectdetect;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

public class camera_activity extends Activity implements CvCameraViewListener2 {

    private Mat mRgba;
    Bitmap bitmap;
    int x_center, y_center, points;

    private BluetoothAdapter mBluetoothAdapter = null;
    private NXTBluetoothService mNXTService = null;
    private static String nxt;

    private byte message = 0;

    public static final int FORWARD = -1;
    public static final int BACKWARD = -2;
    public static final int STOPPED = -3;

    int CURRENT_STATE = -3;
    long LAST_TIME_MESSAGE_SENT = System.currentTimeMillis() + 500;

    private CameraBridgeViewBase mOpenCvCameraView;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    mOpenCvCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    public camera_activity() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.surface_view);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial2_activity_surface_view);
        mOpenCvCameraView.setMaxFrameSize(176, 144); //Original setting: (176, 144)
        mOpenCvCameraView.setCvCameraViewListener(this);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
            while (!(mBluetoothAdapter.isEnabled())) {
                System.out.println("Trying to enable BlueTooth...");
            }
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            nxt = extras.getString("address");
//            mNXTService = new NXTBluetoothService(mHandler);
//            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(nxt);
//            mNXTService.connect(device);
        }
        else{
            Intent i = new Intent(camera_activity.this, Devices_list.class);
            startActivity(i);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    @Override
    public void onDestroy() {
        if (mOpenCvCameraView != null) mOpenCvCameraView.disableView();
        super.onDestroy();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        try {
            bitmap = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mRgba, bitmap);
        } catch (CvException e) {
            Log.d("Exception", e.getMessage());
        }

        int x = 0;
        int y = 0;

        int all_x = 0;
        int all_y = 0;

        int x_min = 176;
        int x_max = 0;
        int y_min = 144;
        int y_max = 0;

        int screen_center_x = 176/2;

        while (x < 176) {
            while (y < 144) {
                int pixel = bitmap.getPixel(x, y);
                int redValue = Color.red(pixel);
                int blueValue = Color.blue(pixel);
                int greenValue = Color.green(pixel);

                if(x == (176/2)){
                    if(y == (144/2)){
                        Log.d("CenterColor", "R:" + redValue + ", G:" + greenValue + ", B:" + blueValue);
                        bitmap.setPixel(x, y, Color.YELLOW);
                    }
                }
                //if (redValue > 245 && blueValue < 243 && greenValue < 178) { // redValue > 200 && blueValue < 70 && greenValue < 70
                if(checkColorRange(redValue, greenValue, blueValue)){
                    points++;
                    all_x = all_x + x;
                    all_y = all_y + y;
                    if(x_min > x){ x_min = x; }
                    if(x_max < x){ x_max = x; }
                    if(y_min > y){ y_min = y; }
                    if(y_max < y){ y_max = y; }
                    //bitmap.setPixel(x, y, Color.CYAN);
                }else{
                    if(x != (176/2) && y != (144/2)){
                        bitmap.setPixel(x, y, Color.BLACK);
                    }
                }

                y++;
            }
            x++;
            y = 0;
        }

        Log.d("POINTS", Integer.toString(points));
        if (points > 50) {
//            x_center = all_x / points;
//            y_center = all_y / points;
            x_center = (x_max + x_min) / 2;
            y_center = (y_max + y_min) / 2;

            Point center = new Point(x_center, y_center);

            bitmap.setPixel(x_center, y_center, Color.CYAN);

            //Imgproc.ellipse(mRgba, center, new Size(20, 20), 0, 0, 360, new Scalar(255, 0, 0), 4, 8, 0);

            int direction = 0;

            /** MOVEMENT DIRECTIONS BASED ON VALUES FROM CAMERA FEED */
            Log.d("CurrentState", Integer.toString(CURRENT_STATE));
            if (points < 2000) { //original: (points < 7000)
                //forward
                if(CURRENT_STATE != FORWARD &&  getElapsedTime() >= 500){
                    message = 19;
                    sendMessage(message);
                    CURRENT_STATE = FORWARD;
                    LAST_TIME_MESSAGE_SENT = System.currentTimeMillis();
                    Log.d("MOVE", "forward");
                }else if(getElapsedTime() >= 500){
                    if(screen_center_x > x_center){
                        //turn right
                        Log.d("MOVE", "right forward turn");
                        if(screen_center_x - x_center > 70){
                            //fastest 56
                            message = 56;
                            sendMessage(message);
                        }else if(screen_center_x - x_center > 60){
                            //2nd fastest
                            message = 55;
                            sendMessage(message);
                        }else if(screen_center_x - x_center > 50){
                            //3rd fastest
                            message = 54;
                            sendMessage(message);
                        }else if(screen_center_x - x_center > 40){
                            //middle speed
                            message = 53;
                            sendMessage(message);
                        }else if(screen_center_x - x_center > 30){
                            //3rd slowest
                            message = 52;
                            sendMessage(message);
                        }else if(screen_center_x - x_center > 20){
                            //2nd slowest
                            message = 51;
                            sendMessage(message);
                        }else if(screen_center_x - x_center > 10){
                            //slowest 50
                            message = 50;
                            sendMessage(message);
                        }
                    }else{
                        //turn left
                        Log.d("MOVE", "left forward turn");
                        if(x_center - screen_center_x > 70){
                            //fastest 46
                            message = 46;
                            sendMessage(message);
                        }else if(x_center - screen_center_x > 60){
                            //2nd fastest
                            message = 45;
                            sendMessage(message);
                        }else if(x_center - screen_center_x > 50){
                            //3rd fastest
                            message = 44;
                            sendMessage(message);
                        }else if(x_center - screen_center_x > 40){
                            //middle speed
                            message = 43;
                            sendMessage(message);
                        }else if(x_center - screen_center_x > 30){
                            //3rd slowest
                            message = 42;
                            sendMessage(message);
                        }else if(x_center - screen_center_x > 20){
                            //2nd slowest
                            message = 41;
                            sendMessage(message);
                        }else if(x_center - screen_center_x > 10){
                            //slowest 40
                            message = 40;
                            sendMessage(message);
                        }
                    }
                }
            }

            if (points >= 2000 && points < 2500) { //original: (points > 7800 && points < 17200)
                //stop
                if(CURRENT_STATE != STOPPED && getElapsedTime() >= 500){
                    message = 59;
                    sendMessage(message);
                    CURRENT_STATE = STOPPED;
                    Log.d("MOVE", "stop");
                }else{
                    if(screen_center_x > x_center){
                        //turn right
                        Log.d("MOVE", "right stopped turn");
                        if(screen_center_x - x_center > 70){
                            //fastest
                        }else if(screen_center_x - x_center > 60){
                            //2nd fastest
                        }else if(screen_center_x - x_center > 50){
                            //3rd fastest
                        }else if(screen_center_x - x_center > 40){
                            //middle speed
                        }else if(screen_center_x - x_center > 30){
                            //3rd slowest
                        }else if(screen_center_x - x_center > 20){
                            //2nd slowest
                        }else if(screen_center_x - x_center > 10){
                            //slowest
                        }
                    }else{
                        //turn left
                        Log.d("MOVE", "left stopped turn");
                        if(x_center - screen_center_x > 70){
                            //fastest
                        }else if(x_center - screen_center_x > 60){
                            //2nd fastest
                        }else if(x_center - screen_center_x > 50){
                            //3rd fastest
                        }else if(x_center - screen_center_x > 40){
                            //middle speed
                        }else if(x_center - screen_center_x > 30){
                            //3rd slowest
                        }else if(x_center - screen_center_x > 20){
                            //2nd slowest
                        }else if(x_center - screen_center_x > 10){
                            //slowest
                        }
                    }
                }
            }

            if (points > 2500) { //original: (points < 18000)
                //back
                if(CURRENT_STATE != BACKWARD && getElapsedTime() >= 500){
                    message = 29;
                    sendMessage(message);
                    CURRENT_STATE = BACKWARD;
                    Log.d("MOVE", "back");
                }else if (CURRENT_STATE == BACKWARD && getElapsedTime() >= 500){
                    if(screen_center_x > x_center){
                        //turn right
                        Log.d("MOVE", "reverse right turn");
                        if(screen_center_x - x_center > 70){
                            //fastest
                        }else if(screen_center_x - x_center > 60){
                            //2nd fastest
                        }else if(screen_center_x - x_center > 50){
                            //3rd fastest
                        }else if(screen_center_x - x_center > 40){
                            //middle speed
                        }else if(screen_center_x - x_center > 30){
                            //3rd slowest
                        }else if(screen_center_x - x_center > 20){
                            //2nd slowest
                        }else if(screen_center_x - x_center > 10){
                            //slowest
                        }
                    }else{
                        //turn left
                        Log.d("MOVE", "reverse left turn");
                        if(x_center - screen_center_x > 70){
                            //fastest
                        }else if(x_center - screen_center_x > 60){
                            //2nd fastest
                        }else if(x_center - screen_center_x > 50){
                            //3rd fastest
                        }else if(x_center - screen_center_x > 40){
                            //middle speed
                        }else if(x_center - screen_center_x > 30){
                            //3rd slowest
                        }else if(x_center - screen_center_x > 20){
                            //2nd slowest
                        }else if(x_center - screen_center_x > 10){
                            //slowest
                        }
                    }
                }
            }

            Log.d("X/Y", "x" + Float.toString(x_center) + "y" + Integer.toString(y_center) + direction);
//            Log.d("POINTS", Integer.toString(points));


        }
        points = 0;
//        return mRgba;
        Utils.bitmapToMat(bitmap, mRgba);
        return mRgba;
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(byte message) {
        // Check that we're actually connected before trying anything
//        if (mNXTService.getState() != NXTBluetoothService.STATE_CONNECTED) {
//            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
//            return;
//        }
//        mNXTService.write(message);
    }

    private long getElapsedTime(){
        return LAST_TIME_MESSAGE_SENT - System.currentTimeMillis();
    }

    private boolean checkColorRange(int r, int g, int b){
        if(r > 220){
            if(g < 100){
                if(b > 175){
                    return true;
                }
            }
        }
        if(r > 170){
            if(g < 5){
                if(b > 60 && b < 100){
                    return true;
                }
            }
        }
        if(r >= 255){
            if(g < 190 && g > 155){
                if(b >= 255){
                    return true;
                }
            }
        }
        return false;
    }


    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case NXTBluetoothService.STATE_CONNECTED:
                            setTitle("STATUS: Connected");
                            break;
                        case NXTBluetoothService.STATE_CONNECTING:
                            setTitle("STATUS: Waiting...");
                            break;
                        case NXTBluetoothService.STATE_LISTEN:
                            break;
                        case NXTBluetoothService.STATE_NONE:
                            setTitle("STATUS: Disconnected");
                            break;
                    }
                    break;

                case Constants.MESSAGE_WRITE:
                    break;

                case Constants.MESSAGE_READ:
                    /**
                     * Gets int value of message from NXT that is passed from NXTBluetoothService
                     */
                    int message = msg.arg1;
                    /**
                     * TODO: add cases for each message to perform tasks
                     */
                    switch (message) {
                        default:
                            Toast.makeText(camera_activity.this, "Message received int = " + Integer.toString(message), Toast.LENGTH_LONG).show();
                    }
                    break;

                case Constants.MESSAGE_DEVICE_NAME:
                    Toast.makeText(camera_activity.this, "Connected to NXT", Toast.LENGTH_SHORT).show();
                    break;

                case Constants.MESSAGE_TOAST:
                    if(msg.getData().getString(Constants.TOAST) == "Unable to connect device"){
                        Intent i = new Intent(camera_activity.this, Devices_list.class);
                        startActivity(i);
                    }else if(msg.getData().getString(Constants.TOAST) == "Device connection was lost"){
                        Intent i = new Intent(camera_activity.this, Devices_list.class);
                        startActivity(i);
                    }
                    Toast.makeText(camera_activity.this, msg.getData().getString(Constants.TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
}
