// 測試步進馬達控制
#include <AccelStepper.h>

AccelStepper stepper1(1, 3, 4);  // 驅動器類型，STEP 3，DIR 4
const int EN = 2;                // 啟用引腳 (假設 active low)
const int LED = 13;              // LED 指示
const int speed_pot = A1;        // 電位器
const int max_speed = 1000;      // 最大速度

unsigned long lastPrintTime = 0;  // 用於非阻塞 Serial 输出
const unsigned long printInterval = 500;  // 每 500ms 输出一次

void setup() {
  pinMode(EN, OUTPUT);
  digitalWrite(EN, HIGH);  // 初始停用驅動器 (HIGH 停用，視驅動器而定)
  pinMode(LED, OUTPUT);
  digitalWrite(LED, LOW);  // 初始關閉 LED
  pinMode(speed_pot, INPUT);
  
  stepper1.setMaxSpeed(max_speed);
  
  Serial.begin(9600);
  Serial.println("Stepper Test Started");
}

void loop() {
  int speed = map(analogRead(speed_pot), 0, 1023, 0, max_speed);  // 從電位器讀速度
  stepper1.setSpeed(speed);  // 設定速度 (正轉；若需反轉，用 -speed)
  
  // 根據速度啟用/停用驅動器和 LED
  if (speed > 0) {
    digitalWrite(EN, LOW);   // 啟用驅動器
    digitalWrite(LED, HIGH); // 開啟 LED 表示運行
  } else {
    digitalWrite(EN, HIGH);  // 停用驅動器
    digitalWrite(LED, LOW);  // 關閉 LED
  }
  
  stepper1.runSpeed();  // 產生脈衝 (在 loop 中頻繁呼叫以確保平順)
  
  // 非阻塞 Serial 输出：每 500ms 檢查一次
  unsigned long currentTime = millis();
  if (currentTime - lastPrintTime >= printInterval) {
    lastPrintTime = currentTime;
    Serial.print("Speed: ");
    Serial.println(speed);
  }
}