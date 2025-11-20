// 測試 PID 計算邏輯
const float set_temperature = 200.0;  // 設定點
float temperature_read = 180.0;       // 模擬初始溫度（可手動改變測試）
float PID_error = 0.0;
float previous_error = 0.0;
float PID_value = 0.0;
float PID_p = 0.0, PID_i = 0.0, PID_d = 0.0;
const int kp = 90, ki = 30, kd = 80;
const int max_PWM = 255;

void setup() {
  Serial.begin(9600);
  Serial.println("PID Test Started");
}

void loop() {
  // 模擬溫度逐漸變化（例如每秒增加 5°C 測試收斂）
  temperature_read += 5.0;  // 模擬加熱（可註解或修改）
  if (temperature_read > 300) {  // 安全檢查
    PID_value = 0;
    Serial.println("Overheat! PWM=0");
    return;
  }
  
  PID_error = set_temperature - temperature_read + 6;  // 誤差
  float dt = 0.25;  // 固定時間差
  
  PID_p = kp * PID_error;
  PID_i += ki * PID_error * dt;
  PID_i = constrain(PID_i, -max_PWM, max_PWM);
  PID_d = kd * (PID_error - previous_error) / dt;
  
  PID_value = constrain(PID_p + PID_i + PID_d, 0, max_PWM);
  
  Serial.print("Temp: "); Serial.print(temperature_read, 1);
  Serial.print(" Error: "); Serial.print(PID_error, 1);
  Serial.print(" PID: "); Serial.println(PID_value, 1);
  
  previous_error = PID_error;
  delay(250);  // 模擬更新率
}