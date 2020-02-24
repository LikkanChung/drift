// this sketch reads a serial message and turns the light on if read
// using a custom protocol '#' to toggle the LED


const int rgb[] = {3, 2, 4};
boolean ledStatus = false;

byte incomingByteRead = 0;
String incomingBytes = "";
int byteCount = 0;

void setup() {
  // put your setup code here, to run once:
  for (int i = 0; i < 3; i++) {
    pinMode(i, OUTPUT);
  }
  Serial.begin(9600);
}

void loop() {
  if (Serial.available() > 0) {
    incomingByteRead = Serial.read();
    if (incomingByteRead == '#') {
      //Serial.write(incomingByteRead);
      ledStatus = !ledStatus;
    }
  }
  if (ledStatus) {
    digitalWrite(rgb[1], HIGH);
  } else {
    digitalWrite(rgb[1], LOW);
  }
}
