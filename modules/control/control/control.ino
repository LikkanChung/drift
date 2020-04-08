//  protocol 
// Write to arduino - #<k|h|m|s>;[<k|h|m|s>;...] where k,h,m,s are key, hour, min, sec respectively. kms can be chained to return 
// Return result <result>;[<result>;...]

// keypad stuff
#include<Keypad.h>
const byte ROWS = 4;
const byte COLS = 4;
char keys[ROWS][COLS] = {
  {'1','2','3','A'},
  {'4','5','6','B'},
  {'7','8','9','C'},
  {'*','0','#','D'}
};
byte pinRows[ROWS] = {2, 3, 4 ,5}; // pins 1-4 of keypad
byte pinCols[COLS] = {6, 7, 8 ,9}; // pins 5-8 of keypad
Keypad keypad = Keypad(makeKeymap(keys),pinRows, pinCols, ROWS, COLS);

// rtc stuff
#include<Wire.h>
#include<DS3231.h>
DS3231 clock;
RTCDateTime time;

void setup() {
  // put your setup code here, to run once:
  Serial.begin(9600);
  
  // keypad
  clock.begin();
  clock.setDateTime(__DATE__,__TIME__);

  // rtc
}

void loop() {
  // put your main code here, to run repeatedly:
  // keypad
  char keyPressed = keypad.getKey();
  if (keyPressed) {
    Serial.println(keyPressed);  
  }

  
  // rtc
  time = clock.getDateTime();
  Serial.print(time.hour);
  Serial.print(":");
  Serial.print(time.minute);
  Serial.print(":");
  Serial.println(time.second);
  delay(1000);
}
