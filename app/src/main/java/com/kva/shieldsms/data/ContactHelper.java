package com.kva.shieldsms.data;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import androidx.core.content.ContextCompat;

/**
 * Looks up a phone number in the device's contacts and returns the
 * display name if found, or the raw number if not (or if permission
 * hasn't been granted yet).
 *
 * READ_CONTACTS permission is checked at runtime before any lookup —
 * the app degrades gracefully (shows numbers) if it isn't granted.
 */
public class ContactHelper {

    private ContactHelper() {}

    /**
     * Returns the contact display name for {@code phoneNumber}, or
     * {@code phoneNumber} itself if no match is found.
     */
    public static String resolveDisplayName(Context context, String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) return phoneNumber;

        if (ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted — fall back to raw number
            return phoneNumber;
        }

        Uri lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber));

        try (Cursor cursor = context.getContentResolver().query(
                lookupUri,
                new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME},
                null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(0);
                if (name != null && !name.trim().isEmpty()) {
                    return name;
                }
            }
        } catch (Exception ignored) {
            // Any lookup failure is non-fatal — just show the number
        }

        return phoneNumber;
    }
}