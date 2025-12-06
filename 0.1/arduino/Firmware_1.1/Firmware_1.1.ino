// 包含必要的函式庫
#include <Wire.h> // 用於 I2C 通訊
#include <hd44780.h> // 用於控制 HD44780 LCD 顯示器
#include <hd44780ioClass/hd44780_I2Cexp.h> // 用於 I2C 擴展
#include <AccelStepper.h> // 用於步進馬達控制
#include <EEPROM.h> // 用於保存設定到 EEPROM
#include "pwm.h" // 用於 UNO R4 PWM 頻率控制 (內建於核心)
#include <math.h>

// 定義 LCD 物件：使用 I2C 擴展
hd44780_I2Cexp lcd;

// 定義 PWM 物件：用於加熱元件 (pin 5, 頻率 ~7812 Hz)
PwmOut heater_pwm(5);

// 定義步進馬達物件：驅動器類型 1，STEP 引腳 3，DIR 引腳 4
AccelStepper stepper1(1, 3, 4);

// 硬體引腳定義（集中管理，便於修改）
const int PWM_pin = 5; // PWM 輸出給加熱元件
const int but1 = 7; // 按鈕輸入，用於切換馬達
const int EN = 2; // 步進驅動器啟用引腳
const int LED = 13; // LED 指示燈

// 熱敏電阻參數 (NTC 100k, beta=3950, 系列電阻=4.7k, 用於高溫測量)
const double BETA = 3950.0; // Beta 值
const double R0 = 100000.0; // 25°C 時電阻 (100k)
const double SERIES_R = 4700.0; // 系列/上拉電阻 (4.7k)
const double T0 = 298.15; // 25°C in Kelvin

// PID 和溫度相關變數
float set_temperature = 200.0; // 溫度設定點（預設 200°C）
float temperature_read = 0.0; // 當前讀取溫度
float PID_error = 0.0; // 當前誤差
float previous_error = 0.0; // 上次誤差（用於微分項）
float PID_value = 0.0; // 最終 PID 輸出
float PID_p = 0.0; // 比例項
float PID_i = 0.0; // 積分項
float PID_d = 0.0; // 微分項
const int kp = 90; // PID 比例增益
const int ki = 30; // PID 積分增益
const int kd = 80; // PID 微分增益
const int max_PWM = 255; // PWM 最大值

// 步進馬達相關變數
int max_speed = 1000; // 馬達最大速度（步/秒），移除 const 以允許藍牙修改
int rotating_speed = 0; // 當前馬達速度
bool activate_stepper = false; // 馬達啟用旗標

// 時間和去彈跳相關變數
unsigned long lastDebounceTime = 0; // 上次按鈕去彈跳時間
unsigned long lastUpdateTime = 0; // 上次 PID/LCD 更新時間
const int debounceDelay = 50; // 去彈跳延遲（ms）
const int updateInterval = 250; // 更新間隔（ms）

// 手動熱敏電阻溫度轉換函式 (取代 thermistor 函式庫)
double readTemp() {
  int adc = analogRead(A0); // 讀取 ADC 值 (0-1023)
  if (adc == 0) return 0.0; // 避免除零
  double R = SERIES_R / (1023.0 / adc - 1.0); // 計算熱敏電阻電阻 (電壓分壓器)
  double lnR = log(R / R0);
  double tempK = 1.0 / (1.0 / T0 + lnR / BETA); // Beta 近似公式
  return tempK - 273.15; // 轉為攝氏
}

// 初始化設定
void setup() {
  // 設定步進驅動器引腳並初始停用
  pinMode(EN, OUTPUT);
  digitalWrite(EN, HIGH); // 高電位：停用驅動器

  // 設定步進馬達最大速度
  stepper1.setMaxSpeed(max_speed);

  // 設定輸入/輸出引腳
  pinMode(but1, INPUT_PULLUP); // 按鈕：輸入並啟用內部上拉
  pinMode(LED, OUTPUT);
  digitalWrite(LED, LOW); // 初始關閉 LED
  pinMode(PWM_pin, OUTPUT); // PWM：輸出

  // 初始化 PWM 物件：設定頻率 ~7812 Hz (原 AVR 設定)，初始占空比 0%
  heater_pwm.begin(7812.5f, 0.0f);

  // 初始化 LCD 並開啟背光
  lcd.begin(16, 2);
  lcd.backlight();

  // 初始化藍牙並載入保存的設定
  Serial1.begin(9600);
  EEPROM.get(0, set_temperature); // 載入保存的溫度 (地址 0)
  EEPROM.get(4, max_speed); // 載入保存的速度 (地址 4, int 佔 4 bytes)

  // 記錄初始時間
  lastUpdateTime = millis();
}

// 主迴圈：持續檢查按鈕、更新馬達、並定期更新 PID 和 LCD
void loop() {
  handleButton(); // 處理按鈕輸入和去彈跳
  handleBluetooth(); // 處理藍牙輸入和命令
  updateStepper(); // 更新步進馬達狀態和速度

  // 非阻塞更新：每 250ms 執行一次 PID 和顯示
  unsigned long currentTime = millis();
  if (currentTime - lastUpdateTime >= updateInterval) {
    lastUpdateTime = currentTime;
    updatePIDAndHeater(); // 更新 PID 計算和 PWM 輸出
    updateLCD(); // 更新 LCD 顯示
  }
}

// 函式：處理按鈕輸入和去彈跳邏輯
void handleButton() {
  if (!digitalRead(but1) && (millis() - lastDebounceTime) > debounceDelay) {
    activate_stepper = !activate_stepper; // 切換馬達啟用狀態
    lastDebounceTime = millis(); // 更新去彈跳時間
  }
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
      } else {
        Serial1.println("ERROR: Invalid temp");
      }
    } else if (command.startsWith("SET_SPEED:")) {
      String valueStr = command.substring(10);
      int newSpeed = valueStr.toInt();
      if (newSpeed >= 0 && newSpeed <= 1000) {
        max_speed = newSpeed; // 更新 max_speed
        Serial1.println("OK: Speed set to " + String(newSpeed));
      } else {
        Serial1.println("ERROR: Invalid speed");
      }
    } else if (command == "START") {
      activate_stepper = true;
      Serial1.println("OK: Motor started");
    } else if (command == "STOP") {
      activate_stepper = false;
      Serial1.println("OK: Motor stopped");
    } else if (command == "GET_STATUS") {
      String status = "TEMP:" + String(set_temperature) + ",SPEED:" + String(rotating_speed) + ",STATUS:" + (activate_stepper ? "ON" : "OFF") + ",CONNECTED:yes";
      Serial1.println(status); // 發送機器狀態
    } else if (command == "SAVE") {
      EEPROM.put(0, set_temperature); // 保存溫度
      EEPROM.put(4, max_speed); // 保存速度
      Serial1.println("OK: Settings saved");
    } else {
      Serial1.println("ERROR: Unknown command");
    }
  }
}

// 函式：更新步進馬達狀態（LED、啟用、速度）
void updateStepper() {
  digitalWrite(LED, activate_stepper ? HIGH : LOW); // 根據狀態控制 LED
  digitalWrite(EN, activate_stepper ? LOW : HIGH); // 控制驅動器啟用（低電位啟用）

  // 計算速度：如果啟用，直接使用 max_speed（透過藍牙設定）；否則設為 0
  rotating_speed = activate_stepper ? max_speed : 0;

  stepper1.setSpeed(rotating_speed); // 設定馬達速度
  stepper1.runSpeed(); // 產生脈衝（非阻塞，取代原 ISR）
}

// 函式：更新 PID 計算和 PWM 加熱輸出
void updatePIDAndHeater() {
  temperature_read = readTemp(); // 讀取溫度 (手動轉換)

  // 安全檢查：如果超過 300°C，關閉加熱並返回
  if (temperature_read > 300) {
    heater_pwm.pulse_perc(0.0f);
    return;
  }

  PID_error = set_temperature - temperature_read + 6; // 計算誤差（加 6 為校準偏移）

  float dt = updateInterval / 1000.0; // 時間差（秒），基於更新間隔

  PID_p = kp * PID_error; // 比例項
  PID_i += ki * PID_error * dt; // 積分項（累加，乘以 dt）
  PID_i = constrain(PID_i, -max_PWM, max_PWM); // 積分防飽和夾持
  PID_d = kd * (PID_error - previous_error) / dt; // 微分項

  PID_value = constrain(PID_p + PID_i + PID_d, 0, max_PWM); // 總 PID 值並夾持
  heater_pwm.pulse_perc((PID_value / 255.0f) * 100.0f); // 輸出 PWM (百分比)

  previous_error = PID_error; // 儲存本次誤差供下次使用
}

// 函式：更新 LCD 顯示（避免閃爍，直接覆寫）
void updateLCD() {
  lcd.setCursor(0, 0); // 第一行
  lcd.print("T: "); // 顯示 "T: "
  lcd.print(temperature_read, 1); // 溫度（1 小數位）
  lcd.print(" S: "); // 顯示 " S: "
  lcd.print(rotating_speed); // 速度
  lcd.print(" "); // 清除殘留字元

  lcd.setCursor(0, 1); // 第二行
  lcd.print("PID: "); // 顯示 "PID: "
  lcd.print(PID_value); // PID 值
  lcd.print(" "); // 清除殘留字元
}