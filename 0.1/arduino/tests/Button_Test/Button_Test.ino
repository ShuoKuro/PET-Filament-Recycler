#include <AccelStepper.h> // 用於步進馬達控制 (保留以測試啟用)

// 硬體引腳定義
const int but1 = 7; // 按鈕輸入，用於切換馬達
const int EN = 2; // 步進驅動器啟用引腳
const int LED = 13; // LED 指示燈

// 步進馬達相關變數 (簡化測試用)
bool activate_stepper = false; // 馬達啟用旗標

// 時間和去彈跳相關變數
unsigned long lastDebounceTime = 0; // 上次按鈕去彈跳時間
const int debounceDelay = 50; // 去彈跳延遲（ms）

void setup() {
  // 設定步進驅動器引腳並初始停用
  pinMode(EN, OUTPUT);
  digitalWrite(EN, HIGH); // 高電位：停用驅動器

  // 設定輸入/輸出引腳
  pinMode(but1, INPUT_PULLUP); // 按鈕：輸入並啟用內部上拉
  pinMode(LED, OUTPUT);
  digitalWrite(LED, LOW); // 初始關閉 LED

  // 初始化序列通訊，用於調試輸出
  Serial.begin(9600);
  Serial.println("Button Test Started. Press button to toggle state.");
}

void loop() {
  handleButton(); // 處理按鈕輸入和去彈跳

  // 每 500ms 輸出狀態到序列監控器，用於調試
  static unsigned long lastPrintTime = 0;
  if (millis() - lastPrintTime >= 500) {
    lastPrintTime = millis();
    Serial.print("Button State: ");
    Serial.print(activate_stepper ? "Enabled (EN Low, LED On)" : "Disabled (EN High, LED Off)");
    Serial.println();
  }
}

// 函式：處理按鈕輸入和去彈跳邏輯
void handleButton() {
  if (!digitalRead(but1) && (millis() - lastDebounceTime) > debounceDelay) {
    activate_stepper = !activate_stepper; // 切換啟用狀態
    digitalWrite(EN, activate_stepper ? LOW : HIGH); // 控制 EN 引腳
    digitalWrite(LED, activate_stepper ? HIGH : LOW); // 控制 LED
    lastDebounceTime = millis(); // 更新去彈跳時間
    Serial.println("Button pressed: State toggled.");
  }
}