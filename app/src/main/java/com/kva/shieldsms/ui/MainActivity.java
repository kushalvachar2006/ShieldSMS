package com.kva.shieldsms.ui;

import android.Manifest;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.kva.shieldsms.R;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_SENDER = "extra_sender";

    private static final int REQUEST_READ_CONTACTS = 101;

    private MessageViewModel viewModel;
    private MessageAdapter adapter;
    private TextView emptyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        emptyText = findViewById(R.id.emptyText);
        ListView listView = findViewById(R.id.messageListView);

        adapter = new MessageAdapter(this, new ArrayList<>(), message -> {
            if (message.isSensitive) {
                // Pass sender so UnlockActivity shows ALL sensitive messages from them
                Intent intent = new Intent(MainActivity.this, UnlockActivity.class);
                intent.putExtra(UnlockActivity.EXTRA_SENDER, message.sender);
                startActivity(intent);
            } else {
                openConversation(message.sender);
            }
        });
        listView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(MessageViewModel.class);
        viewModel.getLatestPerSender().observe(this, messages -> {
            if (messages == null || messages.isEmpty()) {
                emptyText.setVisibility(View.VISIBLE);
                adapter.clear();
            } else {
                emptyText.setVisibility(View.GONE);
                adapter.updateData(messages);
            }
        });

        requestContactsPermissionIfNeeded();
        handleIncomingIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIncomingIntent(intent);
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent == null) return;
        String sender = intent.getStringExtra(EXTRA_SENDER);
        if (sender != null && !sender.isEmpty()) {
            openConversation(sender);
        }
    }

    private void openConversation(String sender) {
        Intent intent = new Intent(this, ConversationActivity.class);
        intent.putExtra(ConversationActivity.EXTRA_SENDER, sender);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isDefaultSmsApp()) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }
        viewModel.refresh();
    }

    private void requestContactsPermissionIfNeeded() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    REQUEST_READ_CONTACTS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions,
                                           @androidx.annotation.NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_CONTACTS) {
            viewModel.refresh();
        }
    }

    private boolean isDefaultSmsApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = getSystemService(RoleManager.class);
            if (roleManager != null) return roleManager.isRoleHeld(RoleManager.ROLE_SMS);
        }
        return getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(this));
    }
}