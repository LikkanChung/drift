// this sketch reads a serial message and turns the light on if read
// custom protocol "#[delay];[glow-time];\n"
// using a custom protocol read the number of seconds <int> left to full brighness, from current, if -1 then off, if 0 then now

const int led[] = {3,5,6}; // use pwm pins

const double startColor[] = {1.0,0.9294,0.8706};; // white - not normalised
const double endColor[] = {1.0,0.8078,0.6510};
double colGradient[] = {0,0,0};

long delayTimer = 0;


long timer = -1; // number of secs to go - starts the led off
unsigned long startTime = 0;
unsigned long endTime = 0;
int startBrightness = 0; // brighness 0-255
int currentBrightness = 0;
double gradient = 0;

int inChar = 0;
String partStr[] = {"",""};
int part = 0;

void setup() {
  // put your setup code here, to run once:
  for (int i = 2; i <= 4; i++) {
    pinMode(i, OUTPUT);
  }
  Serial.begin(9600);
}

void loop() {
  while (Serial.available() > 0) {
    
    
    inChar = Serial.read();

    if (inChar == '#') {
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
      startBrightness = currentBrightness;
      timer = partStr[1].toInt();
      delayTimer = partStr[0].toInt();
      endTime = startTime + (delayTimer * 1000) + (timer * 1000);
      gradient = (double)(255 - startBrightness) / (double)(timer * 1000);
      startTime += (delayTimer * 1000);
      Serial.print(delayTimer); Serial.print(" "); Serial.println(timer);
      partStr[0] = "";
      partStr[1] = "";
      part = 0;

      delayTimer = max(0, delayTimer);

      // colour change gradient
      for (int i = 0; i < 3; i++) {
        colGradient[i] = (double)(endColor[i] - startColor[i])/(double)(timer*1000);
      }
    }
  }

  if (millis() >= startTime) {
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
  }

  int i = 0;
  for (i=0; i < 3; i++) {
    if (millis() > endTime) {
      analogWrite(led[i], currentBrightness * endColor[i]);
    } else if (millis() < startTime) {
      analogWrite(led[i], currentBrightness * startColor[i]);
    } else {
      analogWrite(led[i], currentBrightness * ((colGradient[i] * (millis() - startTime)) + startColor[i]));
    }
  }
  

}
