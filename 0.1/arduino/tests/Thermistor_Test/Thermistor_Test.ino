// 測試熱敏電阻讀取
#include <thermistor.h>

thermistor therm1(A0, 0);  // A0 引腳，配置 0

void setup() {
  Serial.begin(9600);  // 啟用序列輸出
  Serial.println("Thermistor Test Started");
}

void loop() {
  float temp = therm1.analog2temp();  // 讀取並轉換溫度
  Serial.print("Temperature: ");
  Serial.print(temp, 1);  // 顯示 1 小數位
  Serial.println(" C");
  
  delay(500);  // 每 0.5 秒讀一次
}