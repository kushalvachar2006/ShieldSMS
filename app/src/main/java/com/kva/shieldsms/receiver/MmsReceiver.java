package com.kva.shieldsms.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Android requires an app to declare a WAP_PUSH_DELIVER receiver to be
 * eligible as the default SMS app. This demo is scoped to SMS only (per
 * the assignment), so MMS messages are intentionally not processed here.
 */
public class MmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Intentionally left minimal - MMS handling is out of scope for this demo.
    }
}
