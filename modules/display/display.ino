/*
  LiquidCrystal Library - Hello World

 Demonstrates the use a 16x2 LCD display.  The LiquidCrystal
 library works with all LCD displays that are compatible with the
 Hitachi HD44780 driver. There are many of them out there, and you
 can usually tell them by the 16-pin interface.

 This sketch prints "Hello World!" to the LCD
 and shows the time.

  The circuit:
 * LCD RS pin to digital pin 12
 * LCD Enable pin to digital pin 11
 * LCD D4 pin to digital pin 5
 * LCD D5 pin to digital pin 4
 * LCD D6 pin to digital pin 3
 * LCD D7 pin to digital pin 2
 * LCD R/W pin to ground
 * LCD VSS pin to ground
 * LCD VCC pin to 5V
 * 10K resistor:
 * ends to +5V and ground
 * wiper to LCD VO pin (pin 3)

 Library originally added 18 Apr 2008
 by David A. Mellis
 library modified 5 Jul 2009
 by Limor Fried (http://www.ladyada.net)
 example added 9 Jul 2009
 by Tom Igoe
 modified 22 Nov 2010
 by Tom Igoe
 modified 7 Nov 2016
 by Arturo Guadalupi

 This example code is in the public domain.

 http://www.arduino.cc/en/Tutorial/LiquidCrystalHelloWorld

*/

// include the library code:
#include <LiquidCrystal.h>

// initialize the library by associating any needed LCD interface pin
// with the arduino pin number it is connected to
const int rs = 12, en = 11, d4 = 5, d5 = 4, d6 = 3, d7 = 2;

const int pinV0 = 7;
const int contrast = 50;
LiquidCrystal lcd(rs, en, d4, d5, d6, d7);

char inChar = 0;
String inStr  = "";
String partStr[] = {"","",""};
int part = 0;

void setup() {
  analogWrite(pinV0, contrast);
  // set up the LCD's number of columns and rows:
  lcd.begin(16, 2);
  Serial.begin(9600);
}

void loop() {
  while (Serial.available() > 0) {
    inChar = Serial.read();
    inStr += (char)inChar;
    if (inChar == '\n') {
      if (inStr.length() > 0 && inStr.charAt(0) == '#') {
          
          int i = 1;
          char readChar = inStr.charAt(i);
          while (i < inStr.length() && part < 3) {
            readChar = inStr.charAt(i);
            
            if (readChar == ';') {
              part++;
              
            } else {
              if ((part == 0 || part == 1) && !isDigit(readChar)) {
                // case indexes not +ve digits
        
              } else {
                partStr[part] += (char)readChar; 
       
             
              }
            }
            i++;
          }

          inStr = "";

          int x = partStr[0].toInt();
          x = min(x, 15);
          x = max(x, 0);
          int y = partStr[1].toInt();
          y = min(y, 1);
          y = max(y, 0);
          lcd.setCursor(x,y);
          lcd.print(partStr[2]);
          Serial.println(partStr[2]);
          part = 0;
          partStr[0] = "";
          partStr[1] = "";
          partStr[2] = "";
      }
    }
  }


 


  
}
