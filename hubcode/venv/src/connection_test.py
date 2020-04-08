import serial

def main(argv):
    print("make connection")
    if(len(argv) != 2):
        print("Usage: argv[0] [COM port number]")

if __name__ == "__main__":
    main(sys.argv)

