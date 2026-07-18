package com.kva.shieldsms.ui;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.kva.shieldsms.R;
import com.kva.shieldsms.data.ContactHelper;
import com.kva.shieldsms.data.MessageEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter for the main chat list — one row per unique sender.
 *
 * updateData() deduplicates by sender before binding so even if the DB
 * query returns unexpected duplicates, the UI stays clean.
 */
public class MessageAdapter extends ArrayAdapter<MessageEntity> {

    public interface OnMessageClickListener {
        void onClick(MessageEntity message);
    }

    private final OnMessageClickListener clickListener;

    public MessageAdapter(Context context, List<MessageEntity> messages,
                          OnMessageClickListener listener) {
        super(context, 0, messages);
        this.clickListener = listener;
    }

    /**
     * Deduplicates by sender (keeps the first occurrence, which is the
     * latest message because the query orders by timestampMillis DESC),
     * then refreshes the list.
     */
    public void updateData(List<MessageEntity> newMessages) {
        Map<String, MessageEntity> seen = new LinkedHashMap<>();
        for (MessageEntity msg : newMessages) {
            // putIfAbsent keeps the first (= most recent) entry per sender
            if (!seen.containsKey(msg.sender)) {
                seen.put(msg.sender, msg);
            }
        }
        List<MessageEntity> deduped = new ArrayList<>(seen.values());
        clear();
        addAll(deduped);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_message, parent, false);
        }

        MessageEntity message = getItem(position);
        if (message == null) return convertView;

        TextView senderText = convertView.findViewById(R.id.senderText);
        TextView bodyText   = convertView.findViewById(R.id.bodyText);
        TextView timeText   = convertView.findViewById(R.id.timeText);
        View     lockIcon   = convertView.findViewById(R.id.lockIcon);

        String displayName = ContactHelper.resolveDisplayName(getContext(), message.sender);
        senderText.setText(displayName);
        timeText.setText(DateUtils.getRelativeTimeSpanString(message.timestampMillis));

        if (message.isSensitive) {
            bodyText.setText(R.string.content_hidden);
            lockIcon.setVisibility(View.VISIBLE);
        } else {
            bodyText.setText(message.body);
            lockIcon.setVisibility(View.GONE);
        }

        convertView.setOnClickListener(v -> clickListener.onClick(message));
        return convertView;
    }
}