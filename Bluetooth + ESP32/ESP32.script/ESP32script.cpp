#include <BluetoothSerial.h>

BluetoothSerial SerialBT;

void setup() {
  Serial.begin(115200);
  SerialBT.begin("ESP32test"); // Имя Bluetooth устройства
  Serial.println("The device started, now you can pair it with Bluetooth!");
}

void loop() {
  if (SerialBT.available()) {
    String message = SerialBT.readString();
    Serial.print("Received: ");
    Serial.println(message);
    SerialBT.print("Echo: ");
    SerialBT.println(message);
  }
}
