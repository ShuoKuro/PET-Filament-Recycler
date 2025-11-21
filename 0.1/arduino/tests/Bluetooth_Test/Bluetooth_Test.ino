// 測試藍牙模組功能
#include <SoftwareSerial.h>
#include <EEPROM.h>  // 用於模擬保存測試

// 定義藍牙串口（RX=8, TX=9）
SoftwareSerial bluetooth(8, 9);

// 模擬系統變數（從原代碼借用）
float set_temperature = 200.0;
int max_speed = 1000;
bool activate_stepper = false;

// 用於測試的序列輸出
void setup() {
  Serial.begin(9600);  // 用於除錯輸出到監控器
  bluetooth.begin(9600);  // 藍牙波特率
  
  // 模擬載入 EEPROM
  EEPROM.get(0, set_temperature);
  EEPROM.get(4, max_speed);
  
  Serial.println("Bluetooth Test Started. Connect via App and send commands.");
}

void loop() {
  handleBluetooth();  // 處理藍牙輸入
  
  // 模擬系統更新（每秒輸出一次狀態，用於驗證）
  delay(1000);
  Serial.print("Current State: TEMP=");
  Serial.print(set_temperature);
  Serial.print(", SPEED=");
  Serial.print(max_speed);
  Serial.print(", MOTOR=");
  Serial.println(activate_stepper ? "ON" : "OFF");
}

// 函式：處理藍牙輸入和命令解析（從整合代碼複製）
void handleBluetooth() {
  if (bluetooth.available() > 0) {
    String command = bluetooth.readStringUntil('\n');
    command.trim();
    Serial.print("Received: "); Serial.println(command);  // 除錯輸出

    if (command.startsWith("SET_TEMP:")) {
      String valueStr = command.substring(9);
      float newTemp = valueStr.toFloat();
      if (newTemp >= 0 && newTemp <= 300) {
        set_temperature = newTemp;
        bluetooth.println("OK: Temp set to " + String(newTemp));
        Serial.println("Temp updated");
      } else {
        bluetooth.println("ERROR: Invalid temp");
        Serial.println("Invalid temp");
      }
    } else if (command.startsWith("SET_SPEED:")) {
      String valueStr = command.substring(10);
      int newSpeed = valueStr.toInt();
      if (newSpeed >= 0 && newSpeed <= 1000) {
        max_speed = newSpeed;
        bluetooth.println("OK: Speed set to " + String(newSpeed));
        Serial.println("Speed updated");
      } else {
        bluetooth.println("ERROR: Invalid speed");
        Serial.println("Invalid speed");
      }
    } else if (command == "START") {
      activate_stepper = true;
      bluetooth.println("OK: Motor started");
      Serial.println("Motor started");
    } else if (command == "STOP") {
      activate_stepper = false;
      bluetooth.println("OK: Motor stopped");
      Serial.println("Motor stopped");
    } else if (command == "GET_STATUS") {
      String status = "TEMP:" + String(set_temperature) + ",SPEED:" + String(max_speed) + ",STATUS:" + (activate_stepper ? "ON" : "OFF") + ",CONNECTED:yes";
      bluetooth.println(status);
      Serial.println("Status sent: " + status);
    } else if (command == "SAVE") {
      EEPROM.put(0, set_temperature);
      EEPROM.put(4, max_speed);
      bluetooth.println("OK: Settings saved");
      Serial.println("Settings saved");
    } else {
      bluetooth.println("ERROR: Unknown command");
      Serial.println("Unknown command");
    }
  }
}