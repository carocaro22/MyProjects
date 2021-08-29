
#include <Arduino.h>
#include <ESP8266WiFi.h>
#include <ESP8266WiFiMulti.h>
#include <ESP8266HTTPClient.h>
#include <WiFiClient.h>
#include <ArduinoJson.h>

//Your WiFi
String wifi_name = "FPH-Mieter";
String password = "k85zu235emyz7";

//Your Read a Channel Field, change the number on the end after "results=" to 1
String readUrl = "http://api.thingspeak.com/channels/1450229/fields/1.json?api_key=EJAVXIPRP0F5KND3&results=1";

//Initializing Variables
ESP8266WiFiMulti WiFiMulti;
DynamicJsonBuffer jb;

//Setup GPIO2
int pinGPIO2 = 2; //To control LED
int ledStatus = 0; //0=off,1=on


void setup() {
  Serial.begin(115200);

  for (uint8_t t = 4; t > 0; t--) {
    Serial.printf("[SETUP] WAIT %d...\n", t);
    Serial.flush();
    delay(1000);
  }

  WiFi.mode(WIFI_STA);
  WiFiMulti.addAP("FPH-Mieter", "k85zu235emyz7");

}

void loop() {
  if ((WiFiMulti.run() == WL_CONNECTED)) {

    WiFiClient client;

    HTTPClient http;

    Serial.print("[HTTP] begin...\n");
    if (http.begin(client, readUrl)) {

      Serial.print("[HTTP] GET...\n");
      // start connection and send HTTP header
      int httpCode = http.GET();

      // httpCode will be negative on error
      if (httpCode > 0) {

        // HTTP header has been send and Server response header has been handled
        Serial.printf("[HTTP] GET... code: %d\n", httpCode);
        // file found at server
        if (httpCode == HTTP_CODE_OK || httpCode == HTTP_CODE_MOVED_PERMANENTLY) {
          String payload = http.getString();

          //Parse Payload
          JsonObject& obj = jb.parseObject(payload);
          String field1 = obj["feeds"][0]["field1"];
          Serial.print("field1: ");
          Serial.println(field1);

          //evaluate field1
          if (field1 == "1") {
            analogWrite(pinGPIO2, 1023);
            ledStatus = 1;
          }
          if (field1 == "0") {
            analogWrite(pinGPIO2, 0);
            ledStatus = 0;
          }
        }

      } else {
        Serial.printf("[HTTP] GET... failed, error: %s\n", http.errorToString(httpCode).c_str());
      }

      http.end();
    } else {
      Serial.printf("[HTTP} Unable to connect\n");
    }
  }
  delay(1000);
}
