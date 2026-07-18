package com.kva.shieldsms.ui;

import android.app.role.RoleManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.kva.shieldsms.R;

/**
 * The app's very first screen. Explains why default-SMS-app status is
 * needed, then requests it. The user can ONLY move to MainActivity once
 * Android confirms this app is actually the default - there's no skip.
 */
public class OnboardingActivity extends AppCompatActivity {

    private TextView statusText;

    private final ActivityResultLauncher<Intent> roleRequestLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> checkStatusAndProceed());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        statusText = findViewById(R.id.statusText);
        Button grantButton = findViewById(R.id.grantAccessButton);
        grantButton.setOnClickListener(v -> requestDefaultSmsRole());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Covers the case where the user grants the role from a system
        // dialog that returns here without going through the launcher
        // callback (happens on some OEM SMS-role pickers).
        checkStatusAndProceed();
    }

    private void requestDefaultSmsRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = getSystemService(RoleManager.class);
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                if (roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                    checkStatusAndProceed();
                    return;
                }
                Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS);
                roleRequestLauncher.launch(intent);
                return;
            }
        }
        // Fallback for older Android versions
        Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName());
        roleRequestLauncher.launch(intent);
    }

    private void checkStatusAndProceed() {
        if (isDefaultSmsApp()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            statusText.setText(R.string.onboarding_still_needed);
        }
    }

    private boolean isDefaultSmsApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = getSystemService(RoleManager.class);
            if (roleManager != null) {
                return roleManager.isRoleHeld(RoleManager.ROLE_SMS);
            }
        }
        return getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(this));
    }
}
