package com.kva.shieldsms.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MessageDao {

    @Insert
    long insert(MessageEntity message);

    @Query("SELECT * FROM messages ORDER BY timestampMillis DESC")
    List<MessageEntity> getAll();

    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    MessageEntity getById(long id);

    // One row per sender (latest message) — drives the main chat list.
    // Using MAX(id) rather than MAX(timestampMillis) avoids ties and is faster.
    @Query("SELECT * FROM messages WHERE id IN " +
            "(SELECT MAX(id) FROM messages GROUP BY sender) " +
            "ORDER BY timestampMillis DESC")
    List<MessageEntity> getLatestPerSender();

    // Full thread for one sender, oldest first (conversation + unlock views).
    @Query("SELECT * FROM messages WHERE sender = :sender ORDER BY timestampMillis ASC")
    List<MessageEntity> getBySender(String sender);

    // Sensitive-only thread for one sender, oldest first — used by UnlockActivity
    // so the user sees every protected message from that sender after one auth.
    @Query("SELECT * FROM messages WHERE sender = :sender AND isSensitive = 1 ORDER BY timestampMillis ASC")
    List<MessageEntity> getSensitiveBySender(String sender);
}