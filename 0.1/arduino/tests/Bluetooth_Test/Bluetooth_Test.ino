// 包含必要的函式庫
#include <EEPROM.h> // 用於保存設定到 EEPROM

// 變數定義（模擬原系統的相關變數）
float set_temperature = 200.0; // 溫度設定點（預設 200°C）
int max_speed = 1000; // 馬達最大速度（步/秒）
int rotating_speed = 0; // 當前馬達速度（模擬值）
bool activate_stepper = false; // 馬達啟用旗標

// 初始化設定
void setup() {
  // 初始化串行監視器，用於調試（可選）
  Serial.begin(9600);
  Serial.println("Bluetooth Test Started");

  // 初始化硬體串口Serial1（引腳0=TX, 1=RX）並載入保存的設定
  Serial1.begin(9600);
  EEPROM.get(0, set_temperature); // 載入保存的溫度 (地址 0)
  EEPROM.get(4, max_speed); // 載入保存的速度 (地址 4, int 佔 4 bytes)
}

// 主迴圈：持續處理藍牙輸入
void loop() {
  handleBluetooth(); // 處理藍牙輸入和命令
  updateSimulatedSpeed(); // 模擬更新速度（為了 GET_STATUS）
  delay(100); // 短暫延遲，避免過度循環
}

// 函式：處理藍牙輸入和命令解析
void handleBluetooth() {
  if (Serial1.available() > 0) {
    String command = Serial1.readStringUntil('\n');
    command.trim();
    if (command.startsWith("SET_TEMP:")) {
      String valueStr = command.substring(9);
      float newTemp = valueStr.toFloat();
      if (newTemp >= 0 && newTemp <= 300) {
        set_temperature = newTemp;
        Serial1.println("OK: Temp set to " + String(newTemp));
        Serial.println("Temp set to " + String(newTemp)); // 調試輸出
      } else {
        Serial1.println("ERROR: Invalid temp");
        Serial.println("Invalid temp");
      }
    } else if (command.startsWith("SET_SPEED:")) {
      String valueStr = command.substring(10);
      int newSpeed = valueStr.toInt();
      if (newSpeed >= 0 && newSpeed <= 1000) {
        max_speed = newSpeed;
        Serial1.println("OK: Speed set to " + String(newSpeed));
        Serial.println("Speed set to " + String(newSpeed)); // 調試輸出
      } else {
        Serial1.println("ERROR: Invalid speed");
        Serial.println("Invalid speed");
      }
    } else if (command == "START") {
      activate_stepper = true;
      Serial1.println("OK: Motor started");
      Serial.println("Motor started"); // 調試輸出
    } else if (command == "STOP") {
      activate_stepper = false;
      Serial1.println("OK: Motor stopped");
      Serial.println("Motor stopped"); // 調試輸出
    } else if (command == "GET_STATUS") {
      String status = "TEMP:" + String(set_temperature) + ",SPEED:" + String(rotating_speed) + ",STATUS:" + (activate_stepper ? "ON" : "OFF") + ",CONNECTED:yes";
      Serial1.println(status); // 發送機器狀態
      Serial.println("Status sent: " + status); // 調試輸出
    } else if (command == "SAVE") {
      EEPROM.put(0, set_temperature); // 保存溫度
      EEPROM.put(4, max_speed); // 保存速度
      Serial1.println("OK: Settings saved");
      Serial.println("Settings saved"); // 調試輸出
    } else {
      Serial1.println("ERROR: Unknown command");
      Serial.println("Unknown command: " + command);
    }
  }
}

// 函式：模擬更新速度（無硬體時，簡單設定 rotating_speed 為 max_speed 或 0）
void updateSimulatedSpeed() {
  rotating_speed = activate_stepper ? max_speed : 0;
}