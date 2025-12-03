// 測試熱敏電阻讀取

// 熱敏電阻參數 (NTC 100k, beta=3950, 系列電阻=4.7k, 用於高溫測量)
const double BETA = 3950.0; // Beta 值
const double R0 = 100000.0; // 25°C 時電阻 (100k)
const double SERIES_R = 4700.0; // 系列/上拉電阻 (4.7k)
const double T0 = 298.15; // 25°C in Kelvin

// 手動熱敏電阻溫度轉換函式
double readTemp() {
  int adc = analogRead(A0); // 讀取 ADC 值 (0-1023)
  if (adc == 0) return 0.0; // 避免除零
  double R = SERIES_R / (1023.0 / adc - 1.0); // 計算熱敏電阻電阻 (電壓分壓器)
  double lnR = log(R / R0);
  double tempK = 1.0 / (1.0 / T0 + lnR / BETA); // Beta 近似公式
  return tempK - 273.15; // 轉為攝氏
}

void setup() {
  Serial.begin(9600);  // 啟用序列輸出
  Serial.println("Thermistor Test Started");
}

void loop() {
  float temp = readTemp();  // 讀取並轉換溫度
  Serial.print("Temperature: ");
  Serial.print(temp, 1);  // 顯示 1 小數位
  Serial.println(" C");
  
  delay(500);  // 每 0.5 秒讀一次
}