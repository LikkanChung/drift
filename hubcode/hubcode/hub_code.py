import serial
import sys
import time

#The hub should
#authenticate from the back end
#Take alarms from the back end
#activate light and sound when the alarm is received
#deactivate afterwards

display_module_port = 'COM1'
light_module_port = 'COM2'
sound_module_port = 'COM3'


def main(argv):
    print("Attempting connection")
    #if len(argv) != 2:
    #    print("Usage: " + argv[0] + "[COM port number]")
    arduino = serial.Serial(display_module_port, 9600, timeout=.1)
    arduino.write("Welcome to drift")





if __name__ == "__main__":
    main(sys.argv)