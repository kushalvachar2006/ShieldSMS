package com.kva.shieldsms.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.kva.shieldsms.R;
import com.kva.shieldsms.data.ContactHelper;
import com.kva.shieldsms.data.MessageEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows the full message history for a single sender.
 *
 * Fixes vs previous version:
 *  - Toolbar is now wired via setSupportActionBar() so the contact name
 *    actually appears in the title bar.
 *  - ListView uses setStackFromBottom(true) + setTranscriptMode(TRANSCRIPT_MODE_NORMAL)
 *    so messages render oldest-at-top / newest-at-bottom, and the view opens
 *    scrolled to the latest message — matching standard messaging app behaviour.
 *  - DB query changed to ASC order to match the stack-from-bottom layout.
 */
public class ConversationActivity extends AppCompatActivity {

    public static final String EXTRA_SENDER = "extra_sender";

    private MessageViewModel viewModel;
    private ConversationAdapter adapter;
    private ListView listView;
    private String sender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        sender = getIntent().getStringExtra(EXTRA_SENDER);
        if (sender == null || sender.isEmpty()) {
            finish();
            return;
        }

        // Wire up the Toolbar so setSupportActionBar works and the title is shown
        Toolbar toolbar = findViewById(R.id.conversationToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            // Resolve phone number → contact display name
            String title = ContactHelper.resolveDisplayName(this, sender);
            getSupportActionBar().setTitle(title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        listView = findViewById(R.id.conversationListView);

        // Stack from bottom: position 0 (oldest) is at the top, newest at the bottom.
        // The list opens scrolled to the last item automatically.
        listView.setStackFromBottom(true);
        listView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);

        adapter = new ConversationAdapter(this, new ArrayList<>());
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            MessageEntity message = adapter.getItem(position);
            if (message != null && message.isSensitive) {
                Intent intent = new Intent(this, UnlockActivity.class);
                intent.putExtra(UnlockActivity.EXTRA_MESSAGE_ID, message.id);
                startActivity(intent);
            }
            // Non-sensitive: body is already fully visible, nothing more to do
        });

        viewModel = new ViewModelProvider(this).get(MessageViewModel.class);
        viewModel.getConversationMessages().observe(this, messages -> {
            if (messages != null) {
                adapter.clear();
                adapter.addAll(messages);
                adapter.notifyDataSetChanged();
            }
        });

        viewModel.loadConversation(sender);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh after returning from UnlockActivity
        if (sender != null) viewModel.loadConversation(sender);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // -------------------------------------------------------------------------
    // Inner adapter for the conversation thread
    // -------------------------------------------------------------------------

    private static class ConversationAdapter extends ArrayAdapter<MessageEntity> {

        ConversationAdapter(android.content.Context context, List<MessageEntity> items) {
            super(context, 0, items);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.item_conversation_message, parent, false);
            }

            MessageEntity message = getItem(position);
            if (message == null) return convertView;

            TextView bodyText = convertView.findViewById(R.id.convBodyText);
            TextView timeText = convertView.findViewById(R.id.convTimeText);
            View lockIcon     = convertView.findViewById(R.id.convLockIcon);

            timeText.setText(DateUtils.getRelativeTimeSpanString(message.timestampMillis));

            if (message.isSensitive) {
                bodyText.setText(R.string.content_hidden);
                lockIcon.setVisibility(View.VISIBLE);
            } else {
                bodyText.setText(message.body);
                lockIcon.setVisibility(View.GONE);
            }

            return convertView;
        }
    }
}