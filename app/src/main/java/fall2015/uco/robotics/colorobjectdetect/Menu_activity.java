package fall2015.uco.robotics.colorobjectdetect;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

public class Menu_activity extends AppCompatActivity {

    private ImageView start, about;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu_layout);

        about = (ImageView) findViewById(R.id.aboutButton);
        start = (ImageView)findViewById(R.id.connectButton);

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                context = getApplicationContext();
                Intent intent = new Intent(context, Devices_list.class);
                startActivity(intent);
            }
        });
;
        about.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog alertDialog = new AlertDialog.Builder(Menu_activity.this).create();
                alertDialog.setTitle("About this app");
                alertDialog.setMessage("NXT Color Object Detector by Debra H, Stan G, & Justin H \n"
                        + "This Android application works with a Lego NXT Mindstorm robot \n"
                        + "which needs to run the NXT side of this software.");
                alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
//                                Uri uri = Uri.parse("http://www.instructables.com/member/Nikus/");
//                                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
//                                startActivity(intent);
                            }
                        });

                alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                alertDialog.show();
            }
        });
    }
}
