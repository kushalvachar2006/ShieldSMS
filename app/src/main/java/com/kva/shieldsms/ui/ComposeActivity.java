package com.kva.shieldsms.ui;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.kva.shieldsms.R;

/**
 * Android requires an activity that handles ACTION_SENDTO for this app to
 * be eligible as the default SMS app. This demo is scoped to INCOMING
 * messages only (per the assignment), so sending isn't implemented here -
 * this screen exists purely to satisfy that platform requirement.
 */
public class ComposeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);

        Button closeButton = findViewById(R.id.closeComposeButton);
        closeButton.setOnClickListener(v -> finish());
    }
}
