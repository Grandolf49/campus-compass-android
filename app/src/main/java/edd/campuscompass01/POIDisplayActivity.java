package edd.campuscompass01;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

public class POIDisplayActivity extends AppCompatActivity {

    private TextView tv_name, tv_desc, tv_time;
    private ImageView iv_poi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poidisplay);

        tv_desc = findViewById(R.id.poi_disp_desc);
        tv_name = findViewById(R.id.poi_disp_name);
        tv_time = findViewById(R.id.poi_disp_time);
        iv_poi = findViewById(R.id.poi_disp_iv);

        Intent intent = getIntent();
        String name = intent.getStringExtra("name");
        String desc = intent.getStringExtra("desc");
        String time = intent.getStringExtra("time");
        String image_id = intent.getStringExtra("image");
        tv_time.setText(time);
        tv_name.setText(name);
        tv_desc.setText(desc);
        iv_poi.setImageResource(Constants.ARRAY_ICONS[Integer.parseInt(image_id)]);

    }
}
