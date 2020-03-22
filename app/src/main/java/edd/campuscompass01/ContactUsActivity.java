package edd.campuscompass01;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import static android.content.Intent.ACTION_DIAL;

public class ContactUsActivity extends AppCompatActivity {

    Button call, mail, website;
    ImageButton inst, fb, linkdin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_us);

        inst = findViewById(R.id.insta);
        fb = findViewById(R.id.facebook);
        linkdin = findViewById(R.id.linkdn);
        call = findViewById(R.id.call);
        mail = findViewById(R.id.mail);
        website = findViewById(R.id.website);


        call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri number = Uri.parse("tel:02024202180");
                Intent callIntent = new Intent(ACTION_DIAL, number);
                startActivity(callIntent);
            }
        });
        mail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO,
                        Uri.fromParts("mailto", "admissions@vit.edu", null));
                startActivityForResult(emailIntent, 1);
            }
        });

        website.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Constants().webpage = "https://www.vit.edu";
                Intent i = new Intent(getApplicationContext(), Web.class);
                startActivity(i);

            }
        });

        fb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Constants().webpage = "https://www.facebook.com/VIT.Pune/";
                Intent i = new Intent(getApplicationContext(), Web.class);
                startActivity(i);
            }
        });

        inst.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Constants().webpage = "https://www.instagram.com/explore/locations/513139214/vishwakarma-institute-of-technology/";
                Intent i = new Intent(getApplicationContext(), Web.class);
                startActivity(i);

            }
        });

        linkdin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Constants().webpage = "https://www.linkedin.com/school/vishwakarma-institute-of-technology-pune/?originalSubdomain=in";
                Intent i = new Intent(getApplicationContext(), Web.class);
                startActivity(i);

            }
        });
    }
}
