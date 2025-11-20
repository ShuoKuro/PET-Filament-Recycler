// 包含必要的函式庫
#include <Wire.h>               // 用於 I2C 通訊
#include <LiquidCrystal_I2C.h>  // 用於控制 I2C LCD 顯示器
#include <thermistor.h>         // 用於熱敏電阻溫度轉換 (從 https://electronoobs.com/eng_arduino_thermistor.php 下載)
#include <AccelStepper.h>       // 用於步進馬達控制

// 定義 LCD 物件：地址 0x3F，尺寸 16x2
LiquidCrystal_I2C lcd(0x3f, 16, 2);

// 定義熱敏電阻物件：連接到 A0，使用配置 0
thermistor therm1(A0, 0);

// 定義步進馬達物件：驅動器類型 1，STEP 引腳 3，DIR 引腳 4
AccelStepper stepper1(1, 3, 4);

// 硬體引腳定義（集中管理，便於修改）
const int PWM_pin = 5;       // PWM 輸出給加熱元件
const int speed_pot = A1;    // 電位器輸入，用於調整馬達速度
const int but1 = 7;          // 按鈕輸入，用於切換馬達
const int EN = 2;            // 步進驅動器啟用引腳
const int LED = 13;          // LED 指示燈

// PID 和溫度相關變數
float set_temperature = 200.0;  // 溫度設定點（預設 200°C）
float temperature_read = 0.0;   // 當前讀取溫度
float PID_error = 0.0;          // 當前誤差
float previous_error = 0.0;     // 上次誤差（用於微分項）
float PID_value = 0.0;          // 最終 PID 輸出
float PID_p = 0.0;              // 比例項
float PID_i = 0.0;              // 積分項
float PID_d = 0.0;              // 微分項
const int kp = 90;              // PID 比例增益
const int ki = 30;              // PID 積分增益
const int kd = 80;              // PID 微分增益
const int max_PWM = 255;        // PWM 最大值

// 步進馬達相關變數
const int max_speed = 1000;     // 馬達最大速度（步/秒）
int rotating_speed = 0;         // 當前馬達速度
bool activate_stepper = false;  // 馬達啟用旗標

// 時間和去彈跳相關變數
unsigned long lastDebounceTime = 0;  // 上次按鈕去彈跳時間
unsigned long lastUpdateTime = 0;    // 上次 PID/LCD 更新時間
const int debounceDelay = 50;        // 去彈跳延遲（ms）
const int updateInterval = 250;      // 更新間隔（ms）

// 初始化設定
void setup() {
  // 設定步進驅動器引腳並初始停用
  pinMode(EN, OUTPUT);
  digitalWrite(EN, HIGH);  // 高電位：停用驅動器
  
  // 設定步進馬達最大速度
  stepper1.setMaxSpeed(max_speed);
  
  // 設定輸入/輸出引腳
  pinMode(but1, INPUT_PULLUP);  // 按鈕：輸入並啟用內部上拉
  pinMode(speed_pot, INPUT);    // 電位器：輸入
  pinMode(LED, OUTPUT);
  digitalWrite(LED, LOW);       // 初始關閉 LED
  pinMode(PWM_pin, OUTPUT);     // PWM：輸出
  
  // 調整 Timer0 PWM 頻率為 ~7812 Hz（用於更好加熱控制）
  TCCR0B = TCCR0B & B11111000 | B00000010;
  
  // 設定 Timer1 用於中斷（~1kHz，CTC 模式，/8 預分頻）
  TCCR1A = 0;                  // 清空 TCCR1A
  TCCR1B = 0;                  // 清空 TCCR1B
  TCNT1 = 0;                   // 重置計數器
  OCR1A = 2000;                // 比較值：決定中斷頻率
  TCCR1B |= (1 << WGM12);      // 啟用 CTC 模式
  TCCR1B |= (1 << CS11);       // 預分頻 /8
  TIMSK1 |= (1 << OCIE1A);     // 啟用比較匹配 A 中斷
  
  // 初始化 LCD 並開啟背光
  lcd.init();
  lcd.backlight();
  
  // 記錄初始時間
  lastUpdateTime = millis();
}

// 主迴圈：持續檢查按鈕、更新馬達、並定期更新 PID 和 LCD
void loop() {
  handleButton();      // 處理按鈕輸入和去彈跳
  updateStepper();     // 更新步進馬達狀態和速度
  
  // 非阻塞更新：每 250ms 執行一次 PID 和顯示
  unsigned long currentTime = millis();
  if (currentTime - lastUpdateTime >= updateInterval) {
    lastUpdateTime = currentTime;
    updatePIDAndHeater();  // 更新 PID 計算和 PWM 輸出
    updateLCD();           // 更新 LCD 顯示
  }
}

// 函式：處理按鈕輸入和去彈跳邏輯
void handleButton() {
  if (!digitalRead(but1) && (millis() - lastDebounceTime) > debounceDelay) {
    activate_stepper = !activate_stepper;  // 切換馬達啟用狀態
    lastDebounceTime = millis();           // 更新去彈跳時間
  }
}

// 函式：更新步進馬達狀態（LED、啟用、速度）
void updateStepper() {
  digitalWrite(LED, activate_stepper ? HIGH : LOW);  // 根據狀態控制 LED
  digitalWrite(EN, activate_stepper ? LOW : HIGH);   // 控制驅動器啟用（低電位啟用）
  
  // 計算速度：如果啟用，從電位器映射；否則設為 0
  rotating_speed = activate_stepper ? map(analogRead(speed_pot), 0, 1023, 0, max_speed) : 0;
  
  stepper1.setSpeed(rotating_speed);  // 設定馬達速度
  stepper1.runSpeed();                // 產生脈衝（作為中斷的備援）
}

// 函式：更新 PID 計算和 PWM 加熱輸出
void updatePIDAndHeater() {
  temperature_read = therm1.analog2temp();  // 讀取溫度
  
  // 安全檢查：如果超過 300°C，關閉加熱並返回
  if (temperature_read > 300) {
    analogWrite(PWM_pin, 0);
    return;
  }
  
  PID_error = set_temperature - temperature_read + 6;  // 計算誤差（加 6 為校準偏移）
  
  float dt = updateInterval / 1000.0;  // 時間差（秒），基於更新間隔
  
  PID_p = kp * PID_error;                        // 比例項
  PID_i += ki * PID_error * dt;                  // 積分項（累加，乘以 dt）
  PID_i = constrain(PID_i, -max_PWM, max_PWM);   // 積分防飽和夾持
  PID_d = kd * (PID_error - previous_error) / dt;  // 微分項
  
  PID_value = constrain(PID_p + PID_i + PID_d, 0, max_PWM);  // 總 PID 值並夾持
  analogWrite(PWM_pin, PID_value);                           // 輸出 PWM
  
  previous_error = PID_error;  // 儲存本次誤差供下次使用
}

// 函式：更新 LCD 顯示（避免閃爍，直接覆寫）
void updateLCD() {
  lcd.setCursor(0, 0);                   // 第一行
  lcd.print("T: ");                      // 顯示 "T: "
  lcd.print(temperature_read, 1);        // 溫度（1 小數位）
  lcd.print(" S: ");                     // 顯示 " S: "
  lcd.print(rotating_speed);             // 速度
  lcd.print("  ");                       // 清除殘留字元
  
  lcd.setCursor(0, 1);                   // 第二行
  lcd.print("PID: ");                    // 顯示 "PID: "
  lcd.print(PID_value);                  // PID 值
  lcd.print("    ");                     // 清除殘留字元
}

// 中斷服務常式：Timer1 比較匹配，用於定期產生步進脈衝
ISR(TIMER1_COMPA_vect) {
  TCNT1 = 0;              // 重置計時器
  stepper1.runSpeed();    // 產生馬達脈衝（確保平滑運行）
}