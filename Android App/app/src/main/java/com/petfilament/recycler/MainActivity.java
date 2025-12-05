package com.petfilament.recycler;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button buttonNavigate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化 UI 元件 - 只有導航按鈕
        buttonNavigate = findViewById(R.id.button_navigate);

        // 導航按鈕點擊監聽器
        buttonNavigate.setOnClickListener(v -> {
            // 導航到 ControlActivity
            Intent intent = new Intent(MainActivity.this, ControlActivity.class);
            startActivity(intent);

            // 可選：添加過渡動畫
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 當返回 MainActivity 時，可以執行一些清理或初始化
    }
}