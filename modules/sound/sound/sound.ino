// USES TONEAC Library
// Must connect between pins 9 and 10
#include <toneAC.h>

#include "pitches.h"

int volume = 1;

boolean buzzerStatus = false;

byte incomingByteRead = 0;

int melody[] = {
  NOTE_G5, NOTE_E5, NOTE_D5, NOTE_C5, NOTE_D5, NOTE_E5, NOTE_G5, NOTE_E5, NOTE_D5, NOTE_C5, NOTE_D5, NOTE_E5, NOTE_D5, NOTE_E5, NOTE_G5, NOTE_E5, NOTE_G5, NOTE_A5, NOTE_E5, NOTE_A5, NOTE_G5, NOTE_E5, NOTE_D5, NOTE_C5, 0  
//    NOTE_C3, 0
};

// note durations: 4 = quarter note, 8 = eighth note, etc.:
int noteDurations[] = {
  4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 8, 8, 8, 8, 4, 4, 4, 4, 4, 4, 4, 4, 4, 2, 1
//    4, 1
};

int noteCount = 25;//2;

      int thisNote = 0;


long timer = -1;
long delayTimer = 0;
unsigned long startTime = 0;
unsigned long endTime = 0;
int startVolume = 0;
double gradient = 0;

int inChar =0;
String partStr[] = {"",""};
int part = 0;



void setup() {
  // put your setup code here, to run once:
  pinMode(3, OUTPUT);
  Serial.begin(9600);
}

void loop() {
  while (Serial.available() > 0) {
    inChar = Serial.read();
    if (incomingByteRead == '#') {
      //buzzerStatus = !buzzerStatus;
      part = 0;
    } else if (inChar == ';') {
      part++;
    } else {
      if (isDigit(inChar) || inChar == '-') {
        partStr[part] += (char)inChar;
      }
    }

    if (inChar == '\n') {
      startTime = millis();
      startVolume = volume;
      timer = partStr[1].toInt();
      delayTimer = partStr[0].toInt();
      endTime = startTime + (delayTimer * 1000) + (timer* 1000);
      gradient = (double)(10 - startVolume) / (double)(timer * 1000);
      startTime += (delayTimer * 1000);
      Serial.print(delayTimer); Serial.print(" "); Serial.println(timer);
     
      partStr[0] = "";
      partStr[1] = "";
      part = 0;

      delayTimer = max(0, delayTimer);
    }
  } 
  
  if (millis() >= startTime) {
    if (timer == -1) {
      volume = 0;
    } else if (timer == 0) {
      volume = 10;
    } else {
      volume = (int)(gradient * (millis() - startTime)) + startVolume;
      volume = min(volume, 10);
      volume = max(volume, 0);
    }  

    // to calculate the note duration, take one second divided by the note type.
    //e.g. quarter note = 1000 / 4, eighth note = 1000/8, etc.
    int noteDuration = 1000 / noteDurations[thisNote];
    
    toneAC(melody[thisNote], volume, noteDuration);
    // to distinguish the notes, set a minimum time between them.
    // the note's duration + 30% seems to work well:
    int pauseBetweenNotes = noteDuration * 1.30;
    delay(pauseBetweenNotes);
    // stop the tone playing:
    noToneAC();
    thisNote++;
    if (thisNote == noteCount) {
      thisNote = 0;
    }

  
  }
  


}
