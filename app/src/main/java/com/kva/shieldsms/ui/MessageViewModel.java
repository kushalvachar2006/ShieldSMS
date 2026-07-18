package com.kva.shieldsms.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.kva.shieldsms.data.AppDatabase;
import com.kva.shieldsms.data.MessageEntity;

import java.util.List;
import java.util.concurrent.Executors;

public class MessageViewModel extends AndroidViewModel {

    // Main chat list: one entry per unique sender (latest message only)
    private final MutableLiveData<List<MessageEntity>> latestPerSender = new MutableLiveData<>();

    // Conversation thread: all messages from a specific sender
    private final MutableLiveData<List<MessageEntity>> conversationMessages = new MutableLiveData<>();

    public MessageViewModel(@NonNull Application application) {
        super(application);
        refresh();
    }

    /** Used by MainActivity — grouped one-per-sender list. */
    public LiveData<List<MessageEntity>> getLatestPerSender() {
        return latestPerSender;
    }

    /** Used by ConversationActivity — all messages from one sender. */
    public LiveData<List<MessageEntity>> getConversationMessages() {
        return conversationMessages;
    }

    /** Refreshes the main (grouped) chat list. */
    public void refresh() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<MessageEntity> rows = AppDatabase.getInstance(getApplication())
                    .messageDao().getLatestPerSender();
            latestPerSender.postValue(rows);
        });
    }

    /** Loads all messages for a specific sender into conversationMessages LiveData. */
    public void loadConversation(String sender) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<MessageEntity> rows = AppDatabase.getInstance(getApplication())
                    .messageDao().getBySender(sender);
            conversationMessages.postValue(rows);
        });
    }
}