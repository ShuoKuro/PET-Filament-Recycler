#include <AccelStepper.h> // 用於步進馬達控制

// 定義步進馬達物件：驅動器類型 1，STEP 引腳 3，DIR 引腳 4
AccelStepper stepper1(1, 3, 4);

// 硬體引腳定義
const int but1 = 7; // 按鈕輸入，用於切換馬達
const int EN = 2; // 步進驅動器啟用引腳
const int LED = 13; // LED 指示燈

// 步進馬達相關變數
int max_speed = 3000; // 馬達最大速度（步/秒）
int rotating_speed = 0; // 當前馬達速度
bool activate_stepper = false; // 馬達啟用旗標

// 時間和去彈跳相關變數
unsigned long lastDebounceTime = 0; // 上次按鈕去彈跳時間
const int debounceDelay = 50; // 去彈跳延遲（ms）

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

  // 初始化序列通訊，用於調試輸出和速度輸入
  Serial.begin(9600);
  Serial.println("Motor Test Started. Press button to toggle motor.");
  Serial.println("Enter 'speed:VALUE' in Serial Monitor to set speed (e.g., speed:500).");
}

void loop() {
  handleButton(); // 處理按鈕輸入和去彈跳
  handleSerialInput(); // 處理序列監控器輸入以設定速度
  updateStepper(); // 更新步進馬達狀態和速度

  // 每 500ms 輸出狀態到序列監控器，用於調試
  static unsigned long lastPrintTime = 0;
  if (millis() - lastPrintTime >= 500) {
    lastPrintTime = millis();
    Serial.print("Motor Enabled: ");
    Serial.print(activate_stepper ? "Yes" : "No");
    Serial.print(" | Speed: ");
    Serial.println(rotating_speed);
  }
}

// 函式：處理按鈕輸入和去彈跳邏輯
void handleButton() {
  if (!digitalRead(but1) && (millis() - lastDebounceTime) > debounceDelay) {
    activate_stepper = !activate_stepper; // 切換馬達啟用狀態
    lastDebounceTime = millis(); // 更新去彈跳時間
    Serial.println("Button pressed: Motor toggled.");
  }
}

// 函式：處理序列監控器輸入以設定速度
void handleSerialInput() {
  if (Serial.available() > 0) {
    String input = Serial.readStringUntil('\n');
    input.trim();
    if (input.startsWith("speed:")) {
      String valueStr = input.substring(6);
      int newSpeed = valueStr.toInt();
      if (newSpeed >= 0 && newSpeed <= max_speed) {
        rotating_speed = newSpeed; // 直接設定當前速度
        Serial.println("OK: Speed set to " + String(newSpeed));
      } else {
        Serial.println("ERROR: Invalid speed (0-" + String(max_speed) + ")");
      }
    } else {
      Serial.println("ERROR: Unknown command. Use 'speed:VALUE'");
    }
  }
}

// 函式：更新步進馬達狀態（LED、啟用、速度）
void updateStepper() {
  digitalWrite(LED, activate_stepper ? HIGH : LOW); // 根據狀態控制 LED
  digitalWrite(EN, activate_stepper ? LOW : HIGH); // 控制驅動器啟用（低電位啟用）

  // 如果啟用，使用設定的速度；否則設為 0
  if (!activate_stepper) {
    rotating_speed = 0;
  }

  stepper1.setSpeed(rotating_speed); // 設定馬達速度
  stepper1.runSpeed(); // 產生脈衝（非阻塞）
}