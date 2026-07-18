package com.kva.shieldsms.ui;

import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.kva.shieldsms.R;
import com.kva.shieldsms.data.AppDatabase;
import com.kva.shieldsms.data.ContactHelper;
import com.kva.shieldsms.data.MessageEntity;
import com.kva.shieldsms.vault.CryptoHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Biometric-gated screen that reveals ALL sensitive messages from one sender.
 *  - Receives EXTRA_SENDER (raw phone/service address) instead of a single
 *    message ID, so every protected message from that sender is shown in one
 *    authenticated view — matching how ConversationActivity works for normal msgs.
 *  - After successful auth, fetches all sensitive messages for the sender,
 *    decrypts each one, and displays them as a bottom-anchored list (oldest
 *    at top, newest at bottom) identical in behaviour to ConversationActivity.
 *  - FLAG_SECURE keeps screenshots / screen-recorders out.
 */
public class UnlockActivity extends AppCompatActivity {

    /** Raw sender address (phone number or service ID like "JD-KARONE-S"). */
    public static final String EXTRA_SENDER = "extra_sender";

    /**
     * Legacy single-message extra — kept for backward compatibility so any
     * existing pending intents (e.g. old notifications) don't crash.
     * When present and EXTRA_SENDER is absent we fall back to single-message mode.
     */
    public static final String EXTRA_MESSAGE_ID = "message_id";

    private ListView listView;
    private DecryptedMessageAdapter adapter;
    private String sender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Block screenshots and screen recording on this sensitive screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.activity_unlock);

        // Wire toolbar
        Toolbar toolbar = findViewById(R.id.unlockToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        listView = findViewById(R.id.unlockMessageList);
        listView.setStackFromBottom(true);
        listView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);

        adapter = new DecryptedMessageAdapter(this, new ArrayList<>());
        listView.setAdapter(adapter);

        // Prefer sender-based mode; fall back to legacy single-message mode
        sender = getIntent().getStringExtra(EXTRA_SENDER);
        if (sender == null || sender.isEmpty()) {
            // Legacy path: single message id
            long messageId = getIntent().getLongExtra(EXTRA_MESSAGE_ID, -1);
            if (messageId == -1) { finish(); return; }
            showBiometricPrompt(() -> decryptSingleAndShow(messageId));
        } else {
            // Resolve contact name for the toolbar title
            String title = ContactHelper.resolveDisplayName(this, sender);
            if (getSupportActionBar() != null) getSupportActionBar().setTitle(title);
            showBiometricPrompt(() -> decryptAllAndShow(sender));
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // -------------------------------------------------------------------------
    // Biometric prompt
    // -------------------------------------------------------------------------

    private void showBiometricPrompt(Runnable onSuccess) {
        Executor executor = ContextCompat.getMainExecutor(this);

        BiometricPrompt prompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {
                        onSuccess.run();
                    }

                    @Override
                    public void onAuthenticationError(int code, @NonNull CharSequence msg) {
                        Toast.makeText(UnlockActivity.this,
                                R.string.auth_cancelled, Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        Toast.makeText(UnlockActivity.this,
                                R.string.auth_failed, Toast.LENGTH_SHORT).show();
                    }
                });

        prompt.authenticate(new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.unlock_title))
                .setSubtitle(getString(R.string.waiting_auth))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build());
    }

    // -------------------------------------------------------------------------
    // Decrypt & display — all sensitive messages from this sender
    // -------------------------------------------------------------------------

    private void decryptAllAndShow(String senderAddress) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<MessageEntity> rows = AppDatabase.getInstance(this)
                        .messageDao().getSensitiveBySender(senderAddress);

                List<DecryptedMessage> result = new ArrayList<>();
                for (MessageEntity row : rows) {
                    try {
                        String plain = CryptoHelper.decrypt(row.body);
                        result.add(new DecryptedMessage(plain, row.timestampMillis));
                    } catch (Exception ignored) {
                        // If one message fails to decrypt, skip it rather than crashing
                        result.add(new DecryptedMessage(
                                getString(R.string.decrypt_failed), row.timestampMillis));
                    }
                }

                runOnUiThread(() -> {
                    if (result.isEmpty()) {
                        Toast.makeText(this, R.string.message_unavailable,
                                Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    adapter.clear();
                    adapter.addAll(result);
                    adapter.notifyDataSetChanged();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.decrypt_failed,
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    // -------------------------------------------------------------------------
    // Legacy: decrypt a single message by ID (kept for old notification taps)
    // -------------------------------------------------------------------------

    private void decryptSingleAndShow(long messageId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                MessageEntity message = AppDatabase.getInstance(this)
                        .messageDao().getById(messageId);
                if (message == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, R.string.message_unavailable,
                                Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }
                String plain = CryptoHelper.decrypt(message.body);
                runOnUiThread(() -> {
                    adapter.clear();
                    adapter.add(new DecryptedMessage(plain, message.timestampMillis));
                    adapter.notifyDataSetChanged();
                    // Set toolbar title to sender for legacy path too
                    String title = ContactHelper.resolveDisplayName(this, message.sender);
                    if (getSupportActionBar() != null) getSupportActionBar().setTitle(title);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.decrypt_failed,
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    // -------------------------------------------------------------------------
    // Simple data holder for decrypted messages
    // -------------------------------------------------------------------------

    private static class DecryptedMessage {
        final String body;
        final long timestampMillis;

        DecryptedMessage(String body, long timestampMillis) {
            this.body = body;
            this.timestampMillis = timestampMillis;
        }
    }

    // -------------------------------------------------------------------------
    // Adapter for the decrypted message list
    // -------------------------------------------------------------------------

    private static class DecryptedMessageAdapter extends ArrayAdapter<DecryptedMessage> {

        DecryptedMessageAdapter(android.content.Context ctx, List<DecryptedMessage> items) {
            super(ctx, 0, items);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.item_conversation_message, parent, false);
            }

            DecryptedMessage msg = getItem(position);
            if (msg == null) return convertView;

            TextView bodyText = convertView.findViewById(R.id.convBodyText);
            TextView timeText = convertView.findViewById(R.id.convTimeText);
            View lockIcon     = convertView.findViewById(R.id.convLockIcon);

            bodyText.setText(msg.body);
            timeText.setText(DateUtils.getRelativeTimeSpanString(msg.timestampMillis));
            lockIcon.setVisibility(View.GONE); // already decrypted

            return convertView;
        }
    }
}