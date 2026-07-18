package com.kva.shieldsms.receiver;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Android requires an app to declare a RESPOND_VIA_MESSAGE service (used for
 * "quick reply" from the phone dialer) to be eligible as the default SMS
 * app. This demo doesn't implement sending messages at all (per the
 * assignment - incoming only), so this is a minimal no-op stub.
 */
public class HeadlessSmsSendService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        stopSelf();
        return START_NOT_STICKY;
    }
}
