import serial
import sys
import time
import schedule

from backend_connection import fetch

#The hub should
#authenticate from the back end
#Take alarms from the back end
#activate light and sound when the alarm is received
#deactivate afterwards


display_module_port = '/dev/ttyUSB4'
keypad_module_port = '/dev/ttyUSB0'
light_module_port = '/dev/ttyUSB2'
sound_module_port = '/dev/ttyUSB3'


TEST_USERNAME = "jack"
TEST_PASSWORD =  "21ysxqkcl1i3c3h8ou7l"
TEST_URL = "http://localhost"
display_arduino = None
sound_arduino = None
light_arduino = None
keypad_arduino = None


def grab_alarms(arduinos):
    schedule.clear()
    alarms_json = fetch(TEST_URL, TEST_USERNAME, TEST_PASSWORD)
    # GET ALARM TIMES
    alarms = []
    print("I have no idea what the JSON for this looks like")
    # TODO:GET TIMES FROM JSON
    #TODO:ALSO MOVE THE EVENT FORWARD 5 minutes
    for alarm in alarms:
        schedule.every().day.at(alarm.time).do(alarm_once, arduinos)
    schedule.every().hour.at.do(grab_alarms, arduinos)

def shutdown_once(arduinos):
    shutdown(arduinos)
    return schedule.Cancel_Job

def shutdown(arduinos):
    arduinos["light"].write(b"#0;-1;\n")
    arduinos["sound"].write(b"#0;-1;\n")
    print("shutdown modules")

def alarm_once(arduinos):
    alarm(arduinos)
    return schedule.Cancel_Job


def alarm(arduinos):
    arduinos["light"].write(b"#10;10;\n")
    arduinos["sound"].write(b"#10;10;\n")
    print("send an alarm to the modules")

def main(argv):
    print("Attempting connection")
    #if len(argv) != 2:
    #    print("Usage: " + argv[0] + "[COM port number]")
    display_arduino = serial.Serial(display_module_port, 9600, timeout=0.1)
    sound_arduino = serial.Serial(sound_module_port, 9600, timeout=.1)
    light_arduino = serial.Serial(light_module_port, 9600, timeout=.1)
    keypad_arduino = serial.Serial(keypad_module_port, 9600, timeout=.1)
    arduinos = {"display": display_arduino, "sound": sound_arduino, "light": light_arduino, "keypad": keypad_arduino}
    time.sleep(2)

    display_arduino.write(b"#0;0;Welcome to drift;\n")
    #grab_alarms(arduinos)
    while 1:
        schedule.run_pending()
        time.sleep(1)









if __name__ == "__main__":
    main(sys.argv)