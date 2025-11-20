// 測試 PWM 輸出
const int PWM_pin = 5;  // PWM 引腳

void setup() {
  pinMode(PWM_pin, OUTPUT);
  // 調整 PWM 頻率 ~7812 Hz
  TCCR0B = TCCR0B & B11111000 | B00000010;
  Serial.begin(9600);
  Serial.println("PWM Test Started");
}

void loop() {
  for (int value = 0; value <= 255; value += 51) {  // 測試不同值
    analogWrite(PWM_pin, value);
    Serial.print("PWM Value: ");
    Serial.println(value);
    delay(1000);  // 每秒改變一次，觀察變化
  }
}