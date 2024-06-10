#include <WiFi.h>
#include <AsyncTCP.h>
#include <ESPAsyncWebServer.h>

const char* ssid = "your_SSID";
const char* password = "your_PASSWORD";

AsyncWebServer server(80);

void setup() {
  Serial.begin(115200);
  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.println("Connecting to WiFi...");
  }

  Serial.println("Connected to WiFi");

  server.on("/send_message", HTTP_POST, [](AsyncWebServerRequest *request){
    if (request->hasParam("message", true)) {
      String message = request->getParam("message", true)->value();
      Serial.println("Received message: " + message);
      request->send(200, "text/plain", "Message received");
    } else {
      request->send(400, "text/plain", "Message parameter missing");
    }
  });

  server.begin();
}

void loop() {
}