package com.kva.shieldsms.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * One row = one incoming SMS message.
 *
 * For a SENSITIVE message, `body` holds AES-encrypted text (see
 * vault/CryptoHelper.java) and stays encrypted until the user authenticates.
 * For a normal message, `body` is stored as plain text - there's nothing to
 * protect, so encrypting it would add complexity with no benefit.
 */
@Entity(tableName = "messages")
public class MessageEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String sender;

    @NonNull
    public String body;

    public boolean isSensitive;

    public long timestampMillis;

    public MessageEntity(@NonNull String sender, @NonNull String body,
                          boolean isSensitive, long timestampMillis) {
        this.sender = sender;
        this.body = body;
        this.isSensitive = isSensitive;
        this.timestampMillis = timestampMillis;
    }
}
