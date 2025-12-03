#include <Wire.h>
#include <hd44780.h>
#include <hd44780ioClass/hd44780_I2Cexp.h>  // I2C expander i/o class header

hd44780_I2Cexp lcd;  // 自動偵測地址和pin mapping

void setup() {
  lcd.begin(16, 2);    // 初始化 LCD (16x2)
  lcd.backlight();     // 開啟背光
  Serial.begin(9600);  // 啟用序列輸出用於除錯
  Serial.println("LCD Test Started");
}

void loop() {
  lcd.setCursor(0, 0);  // 第一行
  lcd.print("Test LCD: "); 
  lcd.print(123.4, 1);  // 測試浮點數顯示（1 小數位）
  
  lcd.setCursor(0, 1);  // 第二行
  lcd.print("Line 2 OK");
  
  delay(1000);  // 每秒更新一次，觀察是否穩定
}