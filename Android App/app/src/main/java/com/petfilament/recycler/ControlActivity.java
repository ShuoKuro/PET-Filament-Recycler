package com.petfilament.recycler;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class ControlActivity extends AppCompatActivity implements BluetoothManager.BluetoothCallback {

    private BluetoothManager bluetoothManager;
    private Spinner spinnerBluetoothDevices;
    private TextView textViewConnectionStatus, textViewMachineStatus;
    private Button buttonConnect, buttonDisconnect, buttonRefresh;
    private Button buttonStart, buttonStop, buttonSave;
    private EditText editTextTemperature, editTextSpeed;
    private ArrayAdapter<String> deviceAdapter;

    // 機器狀態變數
    private String machineStatus = "IDLE";
    private float currentTemperature = 0;
    private int currentSpeed = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        // 初始化所有 UI 元件
        initializeViews();

        // 初始化藍牙管理器
        bluetoothManager = new BluetoothManager(this, this);

        // 初始化設備列表適配器
        deviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        deviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBluetoothDevices.setAdapter(deviceAdapter);

        // 設置按鈕點擊監聽器
        setupButtonListeners();

        // 開始掃描設備
        bluetoothManager.registerReceiver();
        bluetoothManager.startDiscovery();
    }

    private void initializeViews() {
        spinnerBluetoothDevices = findViewById(R.id.spinner_bluetooth_devices);
        textViewConnectionStatus = findViewById(R.id.textview_connection_status);
        textViewMachineStatus = findViewById(R.id.textview_machine_status);

        buttonConnect = findViewById(R.id.button_connect);
        buttonDisconnect = findViewById(R.id.button_disconnect);
        buttonRefresh = findViewById(R.id.button_refresh);
        buttonStart = findViewById(R.id.button_start);
        buttonStop = findViewById(R.id.button_stop);
        buttonSave = findViewById(R.id.button_save);

        editTextTemperature = findViewById(R.id.edittext_temperature);
        editTextSpeed = findViewById(R.id.edittext_speed);

        // 初始禁用控制按鈕
        setControlButtonsEnabled(false);
    }

    private void setupButtonListeners() {
        // 連接按鈕
        buttonConnect.setOnClickListener(v -> {
            if (spinnerBluetoothDevices.getSelectedItem() != null) {
                String selectedItem = spinnerBluetoothDevices.getSelectedItem().toString();
                String[] parts = selectedItem.split(" - ");
                if (parts.length > 1) {
                    String macAddress = parts[1].trim();
                    bluetoothManager.connect(macAddress);
                } else {
                    showToast("請選擇有效的設備");
                }
            } else {
                showToast("請先掃描並選擇設備");
            }
        });

        // 斷開連接按鈕
        buttonDisconnect.setOnClickListener(v -> {
            bluetoothManager.disconnect();
            textViewConnectionStatus.setText("Not Connected");
            textViewConnectionStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            showToast("已斷開連接");
            setControlButtonsEnabled(false);
        });

        // 刷新設備列表
        buttonRefresh.setOnClickListener(v -> {
            deviceAdapter.clear();
            deviceAdapter.notifyDataSetChanged();
            bluetoothManager.startDiscovery();
        });

        // 開始按鈕 - 根據 Arduino 協議發送 "START"
        buttonStart.setOnClickListener(v -> {
            if (bluetoothManager != null) {
                bluetoothManager.sendData("START");
                showToast("發送啟動指令");
                machineStatus = "RUNNING";
                updateMachineStatusUI();
            } else {
                showToast("未連接到設備");
            }
        });

        // 停止按鈕 - 根據 Arduino 協議發送 "STOP"
        buttonStop.setOnClickListener(v -> {
            if (bluetoothManager != null) {
                bluetoothManager.sendData("STOP");
                showToast("發送停止指令");
                machineStatus = "STOPPED";
                updateMachineStatusUI();
            } else {
                showToast("未連接到設備");
            }
        });

        // 保存設置按鈕 - 根據 Arduino 協議發送 SET_TEMP 和 SET_SPEED
        buttonSave.setOnClickListener(v -> {
            String tempStr = editTextTemperature.getText().toString().trim();
            String speedStr = editTextSpeed.getText().toString().trim();

            // 驗證溫度輸入
            if (tempStr.isEmpty()) {
                showToast("請輸入溫度");
                return;
            }

            float temperature = Float.parseFloat(tempStr);
            if (temperature < 220 || temperature > 260) {
                showToast("溫度範圍應為 220-260°C");
                return;
            }

            // 驗證速度輸入
            if (speedStr.isEmpty()) {
                showToast("請輸入速度");
                return;
            }

            int speed = Integer.parseInt(speedStr);
            if (speed < 0 || speed > 1000) {
                showToast("速度範圍應為 0-1000 mm/s");
                return;
            }

            // 發送設置指令 - 根據 Arduino 協議格式
            if (bluetoothManager != null) {
                // 發送溫度設置命令
                bluetoothManager.sendData("SET_TEMP:" + (int)temperature);

                // 發送速度設置命令
                bluetoothManager.sendData("SET_SPEED:" + speed);

                showToast("參數設置已發送");

                // 發送保存到 EEPROM 命令（可選）
                bluetoothManager.sendData("SAVE");
            } else {
                showToast("未連接到設備");
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        bluetoothManager.registerReceiver();

        // 請求機器狀態
        if (bluetoothManager != null) {
            // 延遲發送狀態請求，確保連接穩定
            new android.os.Handler().postDelayed(() -> {
                bluetoothManager.sendData("GET_STATUS");
            }, 1000);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        bluetoothManager.unregisterReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothManager.disconnect();
        bluetoothManager.unregisterReceiver();
    }

    // BluetoothCallback 介面實現
    @Override
    public void onDeviceFound(ArrayList<String> devices) {
        runOnUiThread(() -> {
            deviceAdapter.clear();
            deviceAdapter.addAll(devices);
            deviceAdapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onConnected() {
        runOnUiThread(() -> {
            textViewConnectionStatus.setText("Connected");
            textViewConnectionStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            showToast("藍牙連接成功");

            // 啟用控制按鈕
            setControlButtonsEnabled(true);

            // 連接成功後請求當前狀態
            new android.os.Handler().postDelayed(() -> {
                bluetoothManager.sendData("GET_STATUS");
            }, 500);
        });
    }

    @Override
    public void onConnectionFailed(String error) {
        runOnUiThread(() -> {
            textViewConnectionStatus.setText("Not Connected");
            textViewConnectionStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            showToast(error);

            // 禁用控制按鈕
            setControlButtonsEnabled(false);
        });
    }

    @Override
    public void onDataReceived(String data) {
        runOnUiThread(() -> {
            // 處理接收到的數據 - 根據 Arduino 協議
            if (data.contains("OK:")) {
                // 成功響應
                showToast(data);
            } else if (data.contains("ERROR:")) {
                // 錯誤響應
                showToast("錯誤: " + data.replace("ERROR:", ""));
            } else if (data.contains("TEMP:") && data.contains("SPEED:") && data.contains("STATUS:")) {
                // 解析狀態信息
                parseStatusData(data);
            } else if (data.contains("TEMP:")) {
                String temp = data.replace("TEMP:", "").trim();
                try {
                    currentTemperature = Float.parseFloat(temp);
                    editTextTemperature.setText(String.valueOf((int)currentTemperature));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            } else if (data.contains("SPEED:")) {
                String speed = data.replace("SPEED:", "").trim();
                try {
                    currentSpeed = Integer.parseInt(speed);
                    editTextSpeed.setText(String.valueOf(currentSpeed));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            } else if (data.contains("STATUS:")) {
                String status = data.replace("STATUS:", "").trim();
                machineStatus = status;
                updateMachineStatusUI();
            } else {
                // 其他數據，直接顯示
                showToast("收到: " + data);
            }
        });
    }

    private void parseStatusData(String data) {
        try {
            // 示例格式: "TEMP:200,SPEED:500,STATUS:ON,CONNECTED:yes"
            String[] parts = data.split(",");
            for (String part : parts) {
                if (part.startsWith("TEMP:")) {
                    String tempStr = part.replace("TEMP:", "").trim();
                    currentTemperature = Float.parseFloat(tempStr);
                    editTextTemperature.setText(String.valueOf((int)currentTemperature));
                } else if (part.startsWith("SPEED:")) {
                    String speedStr = part.replace("SPEED:", "").trim();
                    currentSpeed = Integer.parseInt(speedStr);
                    editTextSpeed.setText(String.valueOf(currentSpeed));
                } else if (part.startsWith("STATUS:")) {
                    String status = part.replace("STATUS:", "").trim();
                    machineStatus = status.equals("ON") ? "RUNNING" : "IDLE";
                    updateMachineStatusUI();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showToast("解析狀態數據失敗");
        }
    }

    private void updateMachineStatusUI() {
        String displayText = "機器狀態: " + machineStatus + "\n"
                + "溫度: " + (int)currentTemperature + "°C\n"
                + "速度: " + currentSpeed + " mm/s";

        textViewMachineStatus.setText(displayText);

        // 根據狀態改變文字顏色
        if (machineStatus.equalsIgnoreCase("RUNNING") || machineStatus.equalsIgnoreCase("ON")) {
            textViewMachineStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else if (machineStatus.equalsIgnoreCase("IDLE") || machineStatus.equalsIgnoreCase("OFF")) {
            textViewMachineStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
        } else if (machineStatus.contains("ERROR")) {
            textViewMachineStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    private void setControlButtonsEnabled(boolean enabled) {
        buttonStart.setEnabled(enabled);
        buttonStop.setEnabled(enabled);
        buttonSave.setEnabled(enabled);
        buttonDisconnect.setEnabled(enabled);

        // 設置按鈕透明度
        float alpha = enabled ? 1.0f : 0.5f;
        buttonStart.setAlpha(alpha);
        buttonStop.setAlpha(alpha);
        buttonSave.setAlpha(alpha);
    }

    private void showToast(String message) {
        Toast.makeText(ControlActivity.this, message, Toast.LENGTH_SHORT).show();
    }
}