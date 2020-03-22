package edd.campuscompass01;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import static edd.campuscompass01.Constants.CAMPUS_NAVIGATION;
import static edd.campuscompass01.Constants.FEEDBACK;

public class FeedbackActivity extends AppCompatActivity {

    private Button sender,contact;
    private EditText nme, rev;
    private long count;
    private DatabaseReference myRef1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        final DatabaseReference mReference = FirebaseDatabase.getInstance().getReference(CAMPUS_NAVIGATION).child(FEEDBACK);

        nme = findViewById(R.id.name_review);
        rev = findViewById(R.id.review);
        sender = findViewById(R.id.send_feedback);
        contact=findViewById(R.id.contact_us_page);

        mReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                count = dataSnapshot.getChildrenCount();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        contact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(),ContactUsActivity.class));
            }
        });
        sender.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myRef1 = mReference.child(count + "");
                myRef1.child("Name").setValue(nme.getText().toString());
                myRef1.child("Review").setValue(rev.getText().toString());
                Toast.makeText(FeedbackActivity.this, "Review Entered.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });


    }
}
