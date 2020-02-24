#include "pitches.h"

const int pin = 5;

int loops = 1;
int mult = 4.2;

boolean buzzerStatus = false;

byte incomingByteRead = 0;

int melody[] = {
  NOTE_G5, NOTE_E5, NOTE_D5, NOTE_C5, NOTE_D5, NOTE_E5, NOTE_G5, NOTE_E5, NOTE_D5, NOTE_C5, NOTE_D5, NOTE_E5, NOTE_D5, NOTE_E5, NOTE_G5, NOTE_E5, NOTE_G5, NOTE_A5, NOTE_E5, NOTE_A5, NOTE_G5, NOTE_E5, NOTE_D5, NOTE_C5, 0  
};

// note durations: 4 = quarter note, 8 = eighth note, etc.:
int noteDurations[] = {
  4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 8, 8, 8, 8, 4, 4, 4, 4, 4, 4, 4, 4, 4, 2, 1
};


void setup() {
  // put your setup code here, to run once:
  pinMode(pin, OUTPUT);
  Serial.begin(9600);
}

void loop() {
  if (Serial.available() > 0) {
    incomingByteRead = Serial.read();
    if (incomingByteRead == '#') {
      buzzerStatus = !buzzerStatus;
    }
  } else {
    if (buzzerStatus) {
//      digitalWrite(5, HIGH);
//      delay(200);
//      digitalWrite(5, LOW);
//      delay(100);
//      digitalWrite(5, HIGH);
//      delay(200);
//      digitalWrite(5, LOW);
//      delay(100);
//      digitalWrite(5, HIGH);
//      delay(200);
//      digitalWrite(5, LOW);
//      delay(200);
//        tone(5, 1000);
//        delay(500);
//        noTone(5);
//        delay(500);
        for (int thisNote = 0; thisNote < 25; thisNote++) {

    // to calculate the note duration, take one second divided by the note type.
    //e.g. quarter note = 1000 / 4, eighth note = 1000/8, etc.
    int noteDuration = 1000 / noteDurations[thisNote];
    tone(pin, melody[thisNote] * mult , noteDuration);

    // to distinguish the notes, set a minimum time between them.
    // the note's duration + 30% seems to work well:
    int pauseBetweenNotes = noteDuration * 1.30;
    delay(pauseBetweenNotes);
    // stop the tone playing:
    noTone(pin);
    loops++;
  }
        
    }
  }
   
}
