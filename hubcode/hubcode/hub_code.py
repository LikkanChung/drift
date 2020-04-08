import serial
import sys

def main(argv):
    print("Attempting connection")
    #if len(argv) != 2:
    #    print("Usage: " + argv[0] + "[COM port number]")
    arduino = serial.Serial('COM1', 9600, timeout=.1)




if __name__ == "__main__":
    main(sys.argv)