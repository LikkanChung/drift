import serial
import sys
import time

#The hub should
#authenticate from the back end
#Take alarms from the back end
#activate light and sound when the alarm is received
#deactivate afterwards

def main(argv):
    print("Attempting connection")
    #if len(argv) != 2:
    #    print("Usage: " + argv[0] + "[COM port number]")
    arduino = serial.Serial('COM1', 9600, timeout=.1)
    arduino.write("Welcome to drift")





if __name__ == "__main__":
    main(sys.argv)