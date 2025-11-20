// 測試 LCD 顯示
#include <Wire.h>
#include <LiquidCrystal_I2C.h>

LiquidCrystal_I2C lcd(0x3f, 16, 2);  // 地址 0x3F，尺寸 16x2

void setup() {
  lcd.init();         // 初始化 LCD
  lcd.backlight();    // 開啟背光
  Serial.begin(9600); // 啟用序列輸出用於除錯
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