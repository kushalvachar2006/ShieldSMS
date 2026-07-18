package com.kva.shieldsms.receiver;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.kva.shieldsms.R;
import com.kva.shieldsms.data.AppDatabase;
import com.kva.shieldsms.data.ContactHelper;
import com.kva.shieldsms.data.MessageEntity;
import com.kva.shieldsms.detector.SensitiveKeywordDetector;
import com.kva.shieldsms.ui.ConversationActivity;
import com.kva.shieldsms.ui.UnlockActivity;
import com.kva.shieldsms.vault.CryptoHelper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SmsDeliverReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsDeliverReceiver";
    private static final String SENSITIVE_CHANNEL_ID = "sensitive_channel";
    private static final String NORMAL_CHANNEL_ID = "normal_channel";

    private static final ExecutorService backgroundExecutor =
            Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Telephony.Sms.Intents.SMS_DELIVER_ACTION.equals(intent.getAction())) return;

        SmsMessage[] parts = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (parts == null || parts.length == 0) return;

        String sender = parts[0].getOriginatingAddress();
        StringBuilder fullBody = new StringBuilder();
        for (SmsMessage part : parts) {
            if (part.getMessageBody() != null) fullBody.append(part.getMessageBody());
        }
        String body = fullBody.toString();
        long timestamp = System.currentTimeMillis();

        if (sender == null) sender = "Unknown";
        Log.d(TAG, "SMS from " + sender + " | body=\"" + body + "\"");

        String finalSender = sender;
        createNotificationChannels(context);

        backgroundExecutor.execute(() -> {
            try {
                SensitiveKeywordDetector detector = new SensitiveKeywordDetector();
                boolean sensitive = detector.isSensitive(body);

                String storedBody = sensitive ? CryptoHelper.encrypt(body) : body;
                MessageEntity entity = new MessageEntity(finalSender, storedBody, sensitive, timestamp);
                AppDatabase.getInstance(context).messageDao().insert(entity);

                String displayName = ContactHelper.resolveDisplayName(context, finalSender);

                if (sensitive) {
                    postSensitiveNotification(context, finalSender, displayName);
                } else {
                    postNormalNotification(context, finalSender, displayName, body);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to process incoming SMS", e);
            }
        });
    }

    /**
     * Sensitive notification taps open UnlockActivity with EXTRA_SENDER so
     * ALL sensitive messages from this sender are shown after one auth.
     * Using the sender string (not a message ID) as the notification ID hash
     * means repeated messages from the same sender update the same notification
     * rather than stacking up multiple ones.
     */
    private void postSensitiveNotification(Context context, String rawSender,
                                           String displayName) {
        Intent target = new Intent(context, UnlockActivity.class);
        target.putExtra(UnlockActivity.EXTRA_SENDER, rawSender);
        target.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int notifId = rawSender.hashCode();
        PendingIntent pi = PendingIntent.getActivity(context, notifId, target,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, SENSITIVE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_shield)
                .setContentTitle(displayName)
                .setContentText(context.getString(R.string.content_hidden))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_MESSAGE);

        notify(context, notifId, builder);
    }

    private void postNormalNotification(Context context, String rawSender,
                                        String displayName, String previewText) {
        Intent target = new Intent(context, ConversationActivity.class);
        target.putExtra(ConversationActivity.EXTRA_SENDER, rawSender);
        target.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int notifId = rawSender.hashCode();
        PendingIntent pi = PendingIntent.getActivity(context, notifId, target,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, NORMAL_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_shield)
                .setContentTitle(displayName)
                .setContentText(previewText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_MESSAGE);

        notify(context, notifId, builder);
    }

    private void notify(Context context, int id, NotificationCompat.Builder builder) {
        NotificationManager mgr = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (mgr != null) mgr.notify(id, builder.build());
    }

    private void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager mgr = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (mgr == null) return;
            mgr.createNotificationChannel(new NotificationChannel(
                    SENSITIVE_CHANNEL_ID, "Protected Messages",
                    NotificationManager.IMPORTANCE_HIGH));
            mgr.createNotificationChannel(new NotificationChannel(
                    NORMAL_CHANNEL_ID, "Messages",
                    NotificationManager.IMPORTANCE_HIGH));
        }
    }
}