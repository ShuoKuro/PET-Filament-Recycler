package com.petfilament.recycler;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class LogsAdapter extends RecyclerView.Adapter<LogsAdapter.ViewHolder> {

    private final ArrayList<DatabaseHelper.LogEntry> logs;

    public LogsAdapter(ArrayList<DatabaseHelper.LogEntry> logs) {
        this.logs = logs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DatabaseHelper.LogEntry log = logs.get(position);
        holder.textViewPrimary.setText("[" + log.timestamp + "] " + log.direction + ":");
        holder.textViewSecondary.setText(log.message);
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewPrimary;
        TextView textViewSecondary;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewPrimary = itemView.findViewById(android.R.id.text1);
            textViewSecondary = itemView.findViewById(android.R.id.text2);
        }
    }
}