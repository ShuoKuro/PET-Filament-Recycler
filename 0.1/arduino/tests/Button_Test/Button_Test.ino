// 測試按鈕和去彈跳
const int but1 = 7;                // 按鈕引腳
const int LED = 13;                // LED 指示
bool activate_stepper = false;     // 狀態旗標
unsigned long lastDebounceTime = 0;
const int debounceDelay = 50;      // 去彈跳延遲

void setup() {
  pinMode(but1, INPUT_PULLUP);
  pinMode(LED, OUTPUT);
  digitalWrite(LED, LOW);
  Serial.begin(9600);
  Serial.println("Button Test Started");
}

void loop() {
  if (!digitalRead(but1) && (millis() - lastDebounceTime) > debounceDelay) {
    activate_stepper = !activate_stepper;  // 切換狀態
    lastDebounceTime = millis();
    Serial.print("State Changed: ");
    Serial.println(activate_stepper ? "ON" : "OFF");
  }
  
  digitalWrite(LED, activate_stepper ? HIGH : LOW);  // 更新 LED
  
  delay(10);  // 小延遲避免 CPU 過載
}