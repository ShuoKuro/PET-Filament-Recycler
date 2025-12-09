package com.petfilament.recycler;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import android.content.Intent;

/**
 * ControlActivity class handles Bluetooth control interface.
 */
public class ControlActivity extends AppCompatActivity implements BluetoothManager.BluetoothCallback {

    /**
     * Bluetooth manager instance.
     */
    private BluetoothManager bluetoothManager;

    /**
     * Spinner for Bluetooth devices.
     */
    private Spinner spinnerBluetoothDevices;

    /**
     * TextView for connection status.
     */
    private TextView textViewConnectionStatus;

    /**
     * TextView for machine status.
     */
    private TextView textViewMachineStatus;

    /**
     * Button to connect.
     */
    private Button buttonConnect;

    /**
     * Button to disconnect.
     */
    private Button buttonDisconnect;

    /**
     * Button to refresh devices.
     */
    private Button buttonRefresh;

    /**
     * Button to start machine.
     */
    private Button buttonStart;

    /**
     * Button to stop machine.
     */
    private Button buttonStop;

    /**
     * Button to save settings.
     */
    private Button buttonSave;

    /**
     * EditText for temperature.
     */
    private EditText editTextTemperature;

    /**
     * EditText for speed.
     */
    private EditText editTextSpeed;

    /**
     * Adapter for device spinner.
     */
    private ArrayAdapter<String> deviceAdapter;

    /**
     * TextView for current temperature.
     */
    private TextView textViewCurrentTemperature;

    /**
     * TextView for current speed.
     */
    private TextView textViewCurrentSpeed;

    /**
     * Button to view logs.
     */
    private Button buttonViewLogs;

    /**
     * Permission request code.
     */
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 100;

    /**
     * Machine status variable.
     */
    private String machineStatus = "IDLE";

    /**
     * Current temperature.
     */
    private float currentTemperature = 0;

    /**
     * Current speed.
     */
    private int currentSpeed = 0;

    /**
     * onCreate method initializes UI, Bluetooth, adapter, listeners, and permissions.
     * @param savedInstanceState Saved instance state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        // Initialize all UI components
        initializeViews();

        // Initialize Bluetooth manager
        bluetoothManager = new BluetoothManager(this, this);

        // Initialize device list adapter
        deviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        deviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBluetoothDevices.setAdapter(deviceAdapter);

        // Set button click listeners
        setupButtonListeners();

        // Check and request necessary permissions
        checkAndRequestPermissions();
    }


    private void checkAndRequestPermissions() {
        // 檢查當前已經擁有的權限
        boolean hasAllPermissions = true;
        ArrayList<String> permissionsNeeded = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ 需要 BLUETOOTH_SCAN 和 BLUETOOTH_CONNECT
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                hasAllPermissions = false;
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                hasAllPermissions = false;
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        }

        // 所有 Android 版本都需要位置權限來掃描藍牙設備
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            hasAllPermissions = false;
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (!hasAllPermissions && !permissionsNeeded.isEmpty()) {
            // 請求缺失的權限
            ActivityCompat.requestPermissions(
                    this,
                    permissionsNeeded.toArray(new String[0]),
                    REQUEST_BLUETOOTH_PERMISSIONS
            );
        } else {
            // 已有所有權限，開始藍牙掃描
            startBluetoothDiscovery();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                // 所有權限已授予
                Toast.makeText(this, "權限已授予，開始掃描藍牙設備", Toast.LENGTH_SHORT).show();
                startBluetoothDiscovery();
            } else {
                // 部分或全部權限被拒絕
                Toast.makeText(this, "需要權限才能掃描藍牙設備", Toast.LENGTH_LONG).show();

                // 檢查是否應該顯示解釋
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[i])) {
                            // 用戶拒絕了權限，但沒有選擇"不再詢問"
                            showPermissionExplanationDialog();
                            break;
                        } else {
                            // 用戶選擇了"不再詢問"或永久拒絕
                            showGoToSettingsDialog();
                            break;
                        }
                    }
                }
            }
        }
    }

    private void showPermissionExplanationDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("需要權限")
                .setMessage("應用需要以下權限來掃描藍牙設備：\n\n" +
                        "1. 位置權限 - 用於發現附近的藍牙設備\n" +
                        "2. 藍牙權限 - 用於連接和控制藍牙設備\n\n" +
                        "這是 Android 系統的安全要求，應用不會收集您的位置信息。")
                .setPositiveButton("授予權限", (dialog, which) -> checkAndRequestPermissions())
                .setNegativeButton("取消", null)
                .show();
    }

    private void showGoToSettingsDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("需要權限")
                .setMessage("您已永久拒絕了必要的權限。\n\n" +
                        "請到手機的「設置」→「應用」→「PET-Filament-Recycler」→「權限」中手動授予權限。")
                .setPositiveButton("打開設置", (dialog, which) -> {
                    // 打開應用設置頁面
                    android.content.Intent intent = new android.content.Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    );
                    intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void startBluetoothDiscovery() {
        runOnUiThread(() -> {
            if (hasBluetoothPermissions()) {
                bluetoothManager.registerReceiver();
                bluetoothManager.startDiscovery();
                Toast.makeText(this, "正在掃描藍牙設備...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "沒有藍牙掃描權限", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean hasBluetoothPermissions() {
        // 檢查所有必要的權限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+
            boolean hasScan = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
            boolean hasConnect = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
            boolean hasLocation = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            return hasScan && hasConnect && hasLocation;
        } else {
            // Android 6.0-11
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
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

        textViewCurrentTemperature = findViewById(R.id.textview_current_temperature);
        textViewCurrentSpeed = findViewById(R.id.textview_current_speed);
        buttonViewLogs = findViewById(R.id.button_view_logs);

        // 初始禁用控制按鈕
        setControlButtonsEnabled(false);
    }

    private void setupButtonListeners() {
        // 刷新按鈕
        buttonRefresh.setOnClickListener(v -> {
            if (hasBluetoothPermissions()) {
                deviceAdapter.clear();
                deviceAdapter.notifyDataSetChanged();
                bluetoothManager.startDiscovery();
                Toast.makeText(this, "重新掃描設備...", Toast.LENGTH_SHORT).show();
            } else {
                // 沒有權限，請求權限
                checkAndRequestPermissions();
            }
        });

        buttonViewLogs.setOnClickListener(v -> {
            Intent intent = new Intent(ControlActivity.this, LogActivity.class);
            startActivity(intent);
        });

        // 連接按鈕
        buttonConnect.setOnClickListener(v -> {
            if (spinnerBluetoothDevices.getSelectedItem() != null) {
                String selectedItem = spinnerBluetoothDevices.getSelectedItem().toString();
                String[] parts = selectedItem.split(" - ");
                if (parts.length > 1) {
                    String macAddress = parts[1].trim();
                    if (hasBluetoothPermissions()) {
                        bluetoothManager.connect(macAddress);
                    } else {
                        Toast.makeText(this, "需要藍牙連接權限", Toast.LENGTH_SHORT).show();
                        checkAndRequestPermissions();
                    }
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

        // 開始按鈕
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

        // 停止按鈕
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

        // 保存設置按鈕
        buttonSave.setOnClickListener(v -> {
            String tempStr = editTextTemperature.getText().toString().trim();
            String speedStr = editTextSpeed.getText().toString().trim();

            // 驗證溫度輸入
            if (tempStr.isEmpty()) {
                showToast("請輸入溫度");
                return;
            }

            float temperature;
            try {
                temperature = Float.parseFloat(tempStr);
            } catch (NumberFormatException e) {
                showToast("請輸入有效的溫度值");
                return;
            }

            if (temperature < 150 || temperature > 300) {
                showToast("溫度範圍應為 150-300°C");
                return;
            }

            // 驗證速度輸入
            if (speedStr.isEmpty()) {
                showToast("請輸入速度");
                return;
            }

            int speed;
            try {
                speed = Integer.parseInt(speedStr);
            } catch (NumberFormatException e) {
                showToast("請輸入有效的速度值");
                return;
            }

            if (speed < 0 || speed > 1000) {
                showToast("速度範圍應為 0-1000 mm/s");
                return;
            }

            if (bluetoothManager != null) {
                bluetoothManager.sendData("SET_TEMP:" + (int)temperature);
                bluetoothManager.sendData("SET_SPEED:" + speed);
                bluetoothManager.sendData("SAVE");
                showToast("參數設置已發送並保存");
            } else {
                showToast("未連接到設備");
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (hasBluetoothPermissions()) {
            bluetoothManager.registerReceiver();

            // 如果有連接，請求機器狀態
            if (bluetoothManager != null) {
                new android.os.Handler().postDelayed(() -> {
                    bluetoothManager.sendData("GET_STATUS");
                }, 1000);
            }
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
            if (devices != null && !devices.isEmpty()) {
                deviceAdapter.clear();
                deviceAdapter.addAll(devices);
                deviceAdapter.notifyDataSetChanged();
                showToast("找到 " + devices.size() + " 個設備");
            } else {
                deviceAdapter.clear();
                deviceAdapter.notifyDataSetChanged();
                showToast("未找到藍牙設備");
            }
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
            buttonViewLogs.setEnabled(true);

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
            showToast("連接失敗: " + error);

            // 禁用控制按鈕
            setControlButtonsEnabled(false);
            buttonViewLogs.setEnabled(false);
        });
    }

    @Override
    public void onDataReceived(String data) {
        runOnUiThread(() -> {
            if (data == null || data.isEmpty()) return;

            if (data.contains("OK:")) {
                // 成功響應
                showToast(data.substring(3).trim());
            } else if (data.contains("ERROR:")) {
                // 錯誤響應
                showToast("錯誤: " + data.substring(6).trim());
            } else if (data.contains("TEMP:") && data.contains("SPEED:") && data.contains("STATUS:")) {
                // 完整的狀態信息
                parseStatusData(data);
            } else if (data.contains("TEMP:")) {
                // 溫度信息
                String temp = data.replace("TEMP:", "").trim();
                try {
                    currentTemperature = Float.parseFloat(temp);
                    textViewCurrentTemperature.setText("Current: " + (int)currentTemperature + "°C");
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            } else if (data.contains("SPEED:")) {
                // 速度信息
                String speed = data.replace("SPEED:", "").trim();
                try {
                    currentSpeed = Integer.parseInt(speed);
                    textViewCurrentSpeed.setText("Current: " + currentSpeed + " mm/s");
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            } else if (data.contains("STATUS:")) {
                // 狀態信息
                String status = data.replace("STATUS:", "").trim();
                machineStatus = status;
                updateMachineStatusUI();
            } else {
                // 其他數據，記錄日誌
                Log.d("BluetoothData", "收到數據: " + data);
            }
        });
    }

    private void parseStatusData(String data) {
        try {
            String[] parts = data.split(",");
            for (String part : parts) {
                if (part.startsWith("TEMP:")) {
                    String tempStr = part.replace("TEMP:", "").trim();
                    currentTemperature = Float.parseFloat(tempStr);
                    textViewCurrentTemperature.setText("Current: " + (int)currentTemperature + "°C");
                } else if (part.startsWith("SPEED:")) {
                    String speedStr = part.replace("SPEED:", "").trim();
                    currentSpeed = Integer.parseInt(speedStr);
                    textViewCurrentSpeed.setText("Current: " + currentSpeed + " mm/s");
                } else if (part.startsWith("STATUS:")) {
                    String status = part.replace("STATUS:", "").trim();
                    machineStatus = status.equals("ON") ? "RUNNING" : "IDLE";
                    updateMachineStatusUI();
                }
            }
            showToast("狀態更新完成");
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
        } else if (machineStatus.contains("ERROR") || machineStatus.contains("STOPPED")) {
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
        buttonDisconnect.setAlpha(alpha);
    }

    private void showToast(String message) {
        Toast.makeText(ControlActivity.this, message, Toast.LENGTH_SHORT).show();
    }
}
