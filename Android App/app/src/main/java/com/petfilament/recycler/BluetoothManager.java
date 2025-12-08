package com.petfilament.recycler;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.core.content.ContextCompat;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

/**
 * 藍牙管理類別，負責藍牙設備的掃描、連接、斷開以及數據收發。
 * 提供回調介面讓上層程式接收設備發現、連接狀態和數據事件。
 */
public class BluetoothManager {

    private static final String TAG = "BluetoothManager";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private ConnectedThread connectedThread;
    private final ArrayList<String> discoveredDevices = new ArrayList<>();
    private final BluetoothCallback callback;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isReceiverRegistered = false;

    private DatabaseHelper databaseHelper;

    /**
     * 藍牙事件回調介面，上層程式需實作以接收事件通知。
     */
    public interface BluetoothCallback {
        /** 當發現新設備或更新已配對設備列表時呼叫，傳遞設備名稱和 MAC 地址的列表 */
        void onDeviceFound(ArrayList<String> devices);
        /** 當藍牙連接成功時呼叫 */
        void onConnected();
        /** 當藍牙連接失敗或斷開時呼叫，傳遞錯誤訊息 */
        void onConnectionFailed(String error);
        /** 當接收到藍牙數據時呼叫，傳遞接收到的字串 */
        void onDataReceived(String data);
    }

    /**
     * 建構子，初始化藍牙管理器。
     * @param context 應用程式上下文（通常是 Activity），用於權限檢查和廣播註冊
     * @param callback 回調介面，上層程式用於接收藍牙事件
     */
    public BluetoothManager(Context context, BluetoothCallback callback) {
        this.context = context;
        databaseHelper = new DatabaseHelper(context);
        this.callback = callback;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            callback.onConnectionFailed("設備不支援藍牙");
        }
    }

    /**
     * 檢查藍牙是否啟用。
     * @return true 如果藍牙已啟用，否則 false
     */
    public boolean isBluetoothEnabled() {
        if (bluetoothAdapter == null) return false;
        if (!hasConnectPermission()) {
            callback.onConnectionFailed("無 BLUETOOTH_CONNECT 權限，無法檢查藍牙狀態");
            return false;
        }
        try {
            return bluetoothAdapter.isEnabled();
        } catch (SecurityException e) {
            callback.onConnectionFailed("安全異常，無法檢查藍牙狀態: " + e.getMessage());
            Log.e(TAG, "檢查藍牙啟用狀態失敗", e);
            return false;
        }
    }

    /**
     * 啟用藍牙（如果尚未啟用）。
     * 需要 BLUETOOTH_CONNECT 權限（Android 12+）。
     * @return true 如果成功啟用或已啟用，false 如果權限不足
     */
    public boolean enableBluetooth() {
        if (bluetoothAdapter == null) return false;
        if (!hasConnectPermission()) {
            callback.onConnectionFailed("無 BLUETOOTH_CONNECT 權限，無法啟用藍牙");
            return false;
        }
        try {
            if (!bluetoothAdapter.isEnabled()) {
                bluetoothAdapter.enable();
            }
            return true;
        } catch (SecurityException e) {
            callback.onConnectionFailed("安全異常，無法啟用藍牙: " + e.getMessage());
            Log.e(TAG, "啟用藍牙失敗", e);
            return false;
        }
    }

    /**
     * 開始掃描藍牙設備，並顯示已配對設備。
     * 需要 BLUETOOTH_SCAN 權限。
     * 掃描結果透過 callback.onDeviceFound() 回傳。
     */
    public void startDiscovery() {
        if (bluetoothAdapter == null) {
            callback.onConnectionFailed("藍牙適配器不可用");
            return;
        }
        if (!hasScanPermission()) {
            callback.onConnectionFailed("無 BLUETOOTH_SCAN 權限，無法掃描");
            return;
        }
        try {
            discoveredDevices.clear();
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            bluetoothAdapter.startDiscovery();
            showBondedDevices();
            Log.d(TAG, "開始掃描藍牙設備");
        } catch (SecurityException e) {
            callback.onConnectionFailed("安全異常，無法掃描: " + e.getMessage());
            Log.e(TAG, "掃描藍牙設備失敗", e);
        }
    }

    /**
     * 停止藍牙掃描。
     * 需要 BLUETOOTH_SCAN 權限。
     */
    public void stopDiscovery() {
        if (!hasScanPermission() || bluetoothAdapter == null) {
            callback.onConnectionFailed("無 BLUETOOTH_SCAN 權限或藍牙適配器不可用");
            return;
        }
        try {
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
                Log.d(TAG, "停止藍牙掃描");
            }
        } catch (SecurityException e) {
            callback.onConnectionFailed("安全異常，無法停止掃描: " + e.getMessage());
            Log.e(TAG, "停止掃描失敗", e);
        }
    }

    /**
     * 連接指定 MAC 地址的藍牙設備。
     * @param macAddress 目標設備的 MAC 地址（格式如 "XX:XX:XX:XX:XX:XX"）
     */
    public void connect(String macAddress) {
        if (!hasConnectPermission() || !hasScanPermission()) {
            callback.onConnectionFailed("缺少藍牙權限，無法連接");
            return;
        }
        if (bluetoothAdapter == null) {
            callback.onConnectionFailed("藍牙適配器不可用");
            return;
        }
        try {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(macAddress);
            bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            bluetoothAdapter.cancelDiscovery();
            bluetoothSocket.connect();
            connectedThread = new ConnectedThread(bluetoothSocket);
            connectedThread.start();
            callback.onConnected();
            Log.d(TAG, "成功連接到 " + macAddress);
        } catch (IOException e) {
            callback.onConnectionFailed("連接失敗: " + e.getMessage());
            Log.e(TAG, "連接失敗", e);
            try {
                if (bluetoothSocket != null) bluetoothSocket.close();
            } catch (IOException ignored) {}
        } catch (SecurityException e) {
            callback.onConnectionFailed("安全異常: " + e.getMessage());
            Log.e(TAG, "安全異常", e);
        }
    }

    /**
     * 斷開當前藍牙連接。
     */
    public void disconnect() {
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
                bluetoothSocket = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "關閉 Socket 失敗", e);
        }
        callback.onConnectionFailed("已斷開連接");
    }

    /**
     * 發送數據到已連接的藍牙設備。
     * @param data 要發送的字串，自動附加換行符 \n
     */
    public void sendData(String data) {
        if (connectedThread != null) {
            connectedThread.write((data + "\n").getBytes());
            databaseHelper.insertLog("OUT", data);
            Log.d(TAG, "發送數據: " + data);
        } else {
            callback.onConnectionFailed("未連接任何設備，無法發送");
        }
    }

    /**
     * 註冊藍牙廣播接收器，用於監聽設備發現事件。
     * 應在 Activity 的 onStart() 或類似生命週期中呼叫。
     */
    public void registerReceiver() {
        if (!isReceiverRegistered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            context.registerReceiver(discoveryReceiver, filter);
            isReceiverRegistered = true;
            Log.d(TAG, "已註冊藍牙廣播接收器");
        }
    }

    /**
     * 取消註冊藍牙廣播接收器。
     * 應在 Activity 的 onStop() 或 onDestroy() 中呼叫。
     */
    public void unregisterReceiver() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(discoveryReceiver);
                isReceiverRegistered = false;
                Log.d(TAG, "已取消註冊藍牙廣播接收器");
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "接收器未註冊", e);
            }
        }
    }

    /**
     * 檢查是否具有 BLUETOOTH_SCAN 權限。
     * @return true 如果權限已授予，否則 false
     */
    private boolean hasScanPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * 檢查是否具有 BLUETOOTH_CONNECT 權限。
     * @return true 如果權限已授予，否則 false
     */
    private boolean hasConnectPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // 舊版 Android，BLUETOOTH 權限為 normal
    }

    /**
     * 顯示已配對的藍牙設備，結果透過 callback.onDeviceFound() 回傳。
     */
    private void showBondedDevices() {
        if (!hasConnectPermission()) {
            callback.onConnectionFailed("無 BLUETOOTH_CONNECT 權限，無法獲取已配對設備");
            return;
        }
        if (bluetoothAdapter == null) {
            callback.onConnectionFailed("藍牙適配器不可用");
            return;
        }
        try {
            Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
            for (BluetoothDevice device : bondedDevices) {
                String name = "Unknown";
                String address = "Unknown";
                if (hasConnectPermission()) {
                    try {
                        name = device.getName() != null ? device.getName() : "Unknown";
                        address = device.getAddress();
                    } catch (SecurityException e) {
                        Log.e(TAG, "獲取設備資訊失敗", e);
                    }
                }
                String item = name + " - " + address;
                if (!discoveredDevices.contains(item)) {
                    discoveredDevices.add(item);
                }
            }
            callback.onDeviceFound(new ArrayList<>(discoveredDevices));
            if (bondedDevices.isEmpty()) {
                callback.onConnectionFailed("未找到已配對的設備");
            }
        } catch (SecurityException e) {
            callback.onConnectionFailed("安全異常，無法獲取已配對設備: " + e.getMessage());
            Log.e(TAG, "獲取已配對設備失敗", e);
        }
    }

    // 藍牙設備發現的廣播接收器
    private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && hasConnectPermission()) {
                    String name = "Unknown";
                    String address = "Unknown";
                    try {
                        name = device.getName() != null ? device.getName() : "Unknown";
                        address = device.getAddress();
                    } catch (SecurityException e) {
                        Log.e(TAG, "獲取設備資訊失敗", e);
                        callback.onConnectionFailed("安全異常，無法獲取設備資訊: " + e.getMessage());
                    }
                    String item = name + " - " + address;
                    if (!discoveredDevices.contains(item)) {
                        discoveredDevices.add(item);
                        callback.onDeviceFound(new ArrayList<>(discoveredDevices));
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                callback.onDeviceFound(new ArrayList<>(discoveredDevices));
            }
        }
    };

    // 負責數據收發的內部線程
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "初始化流失敗", e);
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String data = new String(buffer, 0, bytes);
                    databaseHelper.insertLog("IN", data);
                    handler.post(() -> callback.onDataReceived(data));
                } catch (IOException e) {
                    handler.post(() -> callback.onConnectionFailed("連接斷開: " + e.getMessage()));
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "發送數據失敗", e);
                handler.post(() -> callback.onConnectionFailed("發送失敗: " + e.getMessage()));
            }
        }

        public void cancel() {
            try {
                mmInStream.close();
                mmOutStream.close();
            } catch (IOException e) {
                Log.e(TAG, "關閉流失敗", e);
            }
        }
    }
}