package com.petfilament.recycler;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

/**
 * LogsAdapter class is a RecyclerView adapter for binding log data to list items.
 */
public class LogsAdapter extends RecyclerView.Adapter<LogsAdapter.ViewHolder> {

    /**
     * List of log data from DatabaseHelper.
     */
    private final ArrayList<DatabaseHelper.LogEntry> logs;

    /**
     * Constructor to initialize log list.
     * @param logs List of log entries.
     */
    public LogsAdapter(ArrayList<DatabaseHelper.LogEntry> logs) {
        this.logs = logs;
    }

    /**
     * Creates a new ViewHolder using simple_list_item_2 layout.
     * @param parent Parent ViewGroup.
     * @param viewType View type.
     * @return New ViewHolder.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds data to ViewHolder, displaying timestamp + direction and message.
     * @param holder ViewHolder.
     * @param position Position.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DatabaseHelper.LogEntry log = logs.get(position);
        holder.textViewPrimary.setText("[" + log.timestamp + "] " + log.direction + ":");
        holder.textViewSecondary.setText(log.message);
    }

    /**
     * Returns the size of the log list.
     * @return Item count.
     */
    @Override
    public int getItemCount() {
        return logs.size();
    }

    /**
     * Inner ViewHolder class holding list item views.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        /**
         * Primary TextView for timestamp + direction.
         */
        TextView textViewPrimary;

        /**
         * Secondary TextView for message.
         */
        TextView textViewSecondary;

        /**
         * Constructor to initialize TextViews.
         * @param itemView Item view.
         */
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewPrimary = itemView.findViewById(android.R.id.text1);
            textViewSecondary = itemView.findViewById(android.R.id.text2);
        }
    }
}