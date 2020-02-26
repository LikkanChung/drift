// USES TONEAC Library
// Must connect between pins 9 and 10
#include <toneAC.h>

#include "pitches.h"

int volume = 1;

boolean buzzerStatus = false;

byte incomingByteRead = 0;

int melody[] = {
//  NOTE_G5, NOTE_E5, NOTE_D5, NOTE_C5, NOTE_D5, NOTE_E5, NOTE_G5, NOTE_E5, NOTE_D5, NOTE_C5, NOTE_D5, NOTE_E5, NOTE_D5, NOTE_E5, NOTE_G5, NOTE_E5, NOTE_G5, NOTE_A5, NOTE_E5, NOTE_A5, NOTE_G5, NOTE_E5, NOTE_D5, NOTE_C5, 0  
    NOTE_C3, 0
};

// note durations: 4 = quarter note, 8 = eighth note, etc.:
int noteDurations[] = {
//  4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 8, 8, 8, 8, 4, 4, 4, 4, 4, 4, 4, 4, 4, 2, 1
    4, 1
};

int noteCount = 2;//25;


void setup() {
  // put your setup code here, to run once:
  pinMode(3, OUTPUT);
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
      digitalWrite(3, HIGH);
      for (int thisNote = 0; thisNote < noteCount; thisNote++) {
    
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
  
        Serial.println(volume);
      }
      updateVolume();

    digitalWrite(3, LOW);
        
    }
  }
}

void updateVolume(){
  if (volume < 10) {
    volume++;
  }
}
