package com.petfilament.recycler;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

// LogsAdapter：RecyclerView適配器，用於綁定日誌數據到列表項目
public class LogsAdapter extends RecyclerView.Adapter<LogsAdapter.ViewHolder> {

    // 儲存日誌數據的列表，從DatabaseHelper獲取
    private final ArrayList<DatabaseHelper.LogEntry> logs;

    // 建構子：初始化日誌列表
    public LogsAdapter(ArrayList<DatabaseHelper.LogEntry> logs) {
        this.logs = logs;
    }

    // onCreateViewHolder：創建新的ViewHolder，使用系統預設的simple_list_item_2布局（兩行文字）
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ViewHolder(view);
    }

    // onBindViewHolder：綁定數據到ViewHolder，將日誌的時間戳+方向顯示在第一行，訊息顯示在第二行
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DatabaseHelper.LogEntry log = logs.get(position);
        holder.textViewPrimary.setText("[" + log.timestamp + "] " + log.direction + ":");
        holder.textViewSecondary.setText(log.message);
    }

    // getItemCount：返回日誌列表的大小
    @Override
    public int getItemCount() {
        return logs.size();
    }

    // ViewHolder：內部類別，持有列表項目的視圖元件
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewPrimary;  // 第一行文字（時間戳 + 方向）
        TextView textViewSecondary;  // 第二行文字（訊息）

        // 建構子：初始化TextView，從系統ID獲取
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewPrimary = itemView.findViewById(android.R.id.text1);
            textViewSecondary = itemView.findViewById(android.R.id.text2);
        }
    }
}