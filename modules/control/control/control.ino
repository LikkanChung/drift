//  protocol 
// Write to arduino - #time; or #key;
// Return result as a string where it is hh:mm:ss, or a string of the chars 01234..d*# etc

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
String inputChars = "";

// rtc stuff
#include<Wire.h>
#include<DS3231.h>
DS3231 clock;
RTCDateTime time;

// Serial Reading
char inChar = 0;
String partStr[] = {"","","","","","",""};
int part = 0;

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
  if (keyPressed != NO_KEY) {
    inputChars.concat(keyPressed);
  }
  
  // rtc
  time = clock.getDateTime();
  String timeStr = "";
  String timePart[] = {"","",""};
  timePart[0] = time.hour;
  timePart[1] = time.minute;
  timePart[2] = time.second;
  int i = 0;
  for (i = 0; i < 3; i++) {
    int j = 0;
    if (timePart[i].length() < 2) {
      timeStr += '0';
    }
    for (j = 0; j < timePart[i].length(); j++) {
      timeStr += timePart[i].charAt(j);
    }
    if (i < 2) {
      timeStr += ':';
    }
  }

  while (Serial.available() > 0) {
    inChar = Serial.read();

    if (inChar == '#') {
      part = 0;
    } else if (inChar == ';') {
      part++;
    } else {
      partStr[part] += (char)inChar;
    }
    if (inChar == '\n') {
      if (partStr[0] == "key") {
        inputChars.replace("\n","");
        Serial.println(inputChars);
        inputChars = "";
      } else if (partStr[0] == "time") {
        timeStr.replace("\n","");
        Serial.println(timeStr);
      } else if (partStr[0] == "set") {
        int y,mo,d,h,mi,s;
        y = partStr[1].toInt();
        mo = partStr[2].toInt();
        d = partStr[3].toInt();
        h = partStr[4].toInt();
        mi = partStr[5].toInt();
        s = partStr[6].toInt();
        clock.setDateTime(y,mo,d,h,mi,s);
      }
      partStr[0] = "";
      partStr[1] = "";
      partStr[2] = "";
      partStr[3] = "";
      partStr[4] = "";
      partStr[5] = "";
      partStr[6] = "";
      part = 0;
    }
  }
}
