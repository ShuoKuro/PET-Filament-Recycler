// 測試步進馬達控制
#include <AccelStepper.h>

AccelStepper stepper1(1, 3, 4);  // 驅動器類型，STEP 3，DIR 4
const int EN = 2;                // 啟用引腳
const int LED = 13;              // LED 指示
const int speed_pot = A1;        // 電位器
const int max_speed = 1000;      // 最大速度

void setup() {
  pinMode(EN, OUTPUT);
  digitalWrite(EN, LOW);  // 啟用驅動器
  pinMode(LED, OUTPUT);
  digitalWrite(LED, HIGH);  // 開啟 LED 表示運行
  pinMode(speed_pot, INPUT);
  
  stepper1.setMaxSpeed(max_speed);
  
  // 設定 Timer1 中斷 (~1kHz)
  TCCR1A = 0;
  TCCR1B = 0;
  TCNT1 = 0;
  OCR1A = 2000;
  TCCR1B |= (1 << WGM12) | (1 << CS11);
  TIMSK1 |= (1 << OCIE1A);
  
  Serial.begin(9600);
  Serial.println("Stepper Test Started");
}

void loop() {
  int speed = map(analogRead(speed_pot), 0, 1023, 0, max_speed);  // 從電位器讀速度
  stepper1.setSpeed(speed);
  stepper1.runSpeed();  // 備援呼叫
  
  Serial.print("Speed: ");
  Serial.println(speed);
  delay(500);  // 每 0.5 秒輸出
}

ISR(TIMER1_COMPA_vect) {
  TCNT1 = 0;
  stepper1.runSpeed();  // 中斷產生脈衝
}