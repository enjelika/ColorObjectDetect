package fall2015.uco.robotics.colorobjectdetect;
 
 import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
 import org.opencv.imgproc.Imgproc;

 import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
//import android.content.Intent;
 import android.content.Intent;
 import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
//import android.os.Handler;
 import android.os.Handler;
 import android.os.Message;
 import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

public class camera_activity extends Activity implements CvCameraViewListener2 {

//   int send;
    private Mat mRgba;
    Bitmap bitmap;
    int x_center;
    int y_center;
    int points;
   
  	private BluetoothAdapter mBluetoothAdapter = null;
    private NXTBluetoothService mNXTService = null;
  	private static String nxt;

    private CameraBridgeViewBase  mOpenCvCameraView;
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
   
    public camera_activity() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
    
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.surface_view);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial2_activity_surface_view);
        mOpenCvCameraView.setMaxFrameSize(176, 144);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
            while(!(mBluetoothAdapter.isEnabled())) {
                System.out.println("Trying to enable BlueTooth...");
            }
        }
     
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            nxt = extras.getString("address");
            mNXTService = new NXTBluetoothService(mHandler);
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(nxt);
            mNXTService.connect(device);
        }
        else{
            Intent i = new Intent(camera_activity.this, Devices_list.class);
            startActivity(i);
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }
   
    @Override
    public void onResume(){
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
        } catch (CvException e){
       	    Log.d("Exception",e.getMessage());
        }

        int x = 0;
        int y = 0;

        int all_x = 0;
        int all_y = 0;
                    
        while(x < 176) {
            	  
            while(y < 144){
                int pixel = bitmap.getPixel(x, y);
                int redValue = Color.red(pixel);
                int blueValue = Color.blue(pixel);
                int greenValue = Color.green(pixel);

                if(redValue > 200 && blueValue < 70 && greenValue < 70){
                	   points++;
                	   all_x = all_x + x;
                	   all_y = all_y + y;
                }
                   
                y++;
            }
            x++;
            y = 0;
       }

       y = 0;
       x = 0;
                   
       if(points > 200){
           x_center = all_x / points;
           y_center = all_y / points;

           Point center= new Point(x_center, y_center);
           //Imgproc.ellipse(mRgba, center, new Size(20, 20), 0, 0, 360, new Scalar(255, 0, 0), 4, 8, 0);

           int direction = 0;

           /** MOVEMENT DIRECTIONS BASED ON VALUES FROM CAMERA FEED */
           if(points < 7000){
             	   //forward
              	   direction = 1;
           }

           if(points > 7800 && points < 17200){
                	   //stop
                	   direction = 0;
           }

           if(points > 18000){
             	   //back
               	   direction = 2;
           }

//           writeData("x" + Float.toString(x_center) + "y" + Integer.toString(y_center) + direction);
           Log.d("points", Integer.toString(points));

           points = 0;
//           all_x = 0;
//           all_y = 0;
       }
       return mRgba;
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
                    switch(message){
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

//   private void CheckBt() {
//       mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//
//       if (!mBluetoothAdapter.isEnabled()) startActivity(i);
//
//       if (mBluetoothAdapter == null) {}
//    }

//    public void Connect() {
//
//	   if(mBluetoothAdapter.isEnabled()){
//			BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
//			Toast.makeText(getApplicationContext(), "Connecting...", Toast.LENGTH_LONG).show();
//			mBluetoothAdapter.cancelDiscovery();
//
//			try {
//				btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
//				btSocket.connect();
//
//				Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
//
//			} catch (IOException e) {
//				try {
//					btSocket.close();
//				} catch (IOException e2) {
//					Toast.makeText(getApplicationContext(), "Failed", Toast.LENGTH_SHORT).show();
//				}
//			}
//	   }
//	   else {
//		   Toast.makeText(getApplicationContext(), "Bluetooth is disabled", Toast.LENGTH_SHORT).show();
//	   }
//	}

//     private void writeData(String data) {
//
//	   if(mBluetoothAdapter.isEnabled()){
//			try {
//				outStream = btSocket.getOutputStream();
//			} catch (IOException e) {
//                Log.d("writeData", e.getMessage());
//			}
//
//			try {
//				outStream.write(data.getBytes());
//			} catch (IOException e) {
//				Log.d("writeData", e.getMessage());
//			}
//	   } else {
//           //Something...
//           Log.d("WDelse", "In else block within writeData...");
//	   }
//	}
 }