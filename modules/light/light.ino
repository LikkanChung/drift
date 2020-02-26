// this sketch reads a serial message and turns the light on if read
// using a custom protocol read the number of seconds <int> left to full brighness, from current, if -1 then off, if 0 then now

const int led = 5;

long timer = -1; // number of secs to go - starts the led off
unsigned long startTime = 0;
unsigned long endTime = 0;
int startBrightness = 0; // brighness 0-255
int currentBrightness = 0;
double gradient = 0;

int inChar = 0;
String inString = "";

void setup() {
  // put your setup code here, to run once:
  for (int i = 0; i < 3; i++) {
    pinMode(i, OUTPUT);
  }
  Serial.begin(9600);
}

void loop() {
  while (Serial.available() > 0) {
    startTime = millis();
    startBrightness = currentBrightness;
    inChar = Serial.read();
    if (isDigit(inChar) || inChar == '-') {
      inString += (char)inChar;
    }

    if (inChar == '\n') {
      timer = inString.toInt();
      endTime = startTime + (timer * 1000);
      gradient = (double)(255 - startBrightness) / (double)(timer * 1000);
      inString = "";
    }
  }

  if (timer == -1) {
    currentBrightness = 0;
  } else if (timer == 0) {
    currentBrightness = 255;
  } else {
    // modelled as mx+c
    currentBrightness = (int)(gradient * (millis() - startTime)) + startBrightness;  
    currentBrightness = min(currentBrightness, 255);
    currentBrightness = max(currentBrightness, 0);
    
  }
  analogWrite(led, currentBrightness);

}
