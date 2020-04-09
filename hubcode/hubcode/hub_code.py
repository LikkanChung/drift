import serial
import sys
import time
import schedule

from hubcode.backend_connection import fetch

#The hub should
#authenticate from the back end
#Take alarms from the back end
#activate light and sound when the alarm is received
#deactivate afterwards


display_module_port = 'COM1'
keypad_module_port = 'COM4'
light_module_port = 'COM2'
sound_module_port = 'COM3'


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
    for alarm in alarms:
        schedule.every().day.at(alarm.time).do(alarm_once, arduinos)
    schedule.every().hour.at.do(grab_alarms, arduinos)


def alarm_once(arduinos):
    alarm(arduinos)
    return schedule.cancel_job()


def alarm(arduinos):
    arduinos["light"].write("#1;20;\n")
    arduinos["sound"].write("#1:20;\n")
    time.sleep(30)
    arduinos["light"].write("#0;-1;\n")
    arduinos["sound"].write("#0:-1;\n")
    # TODO:SEND ALARMS TO MODULES
    print("send an alarm to the modules")

def main(argv):
    print("Attempting connection")
    #if len(argv) != 2:
    #    print("Usage: " + argv[0] + "[COM port number]")
    display_arduino = serial.Serial(display_module_port, 9600, timeout=.1)
    sound_arduino = serial.Serial(sound_module_port, 9600, timeout=.1)
    light_arduino = serial.Serial(light_module_port, 9600, timeout=.1)
    keypad_arduino = serial.Serial(keypad_module_port, 9600, timeout=.1)
    arduinos = {"display": display_arduino, "sound": sound_arduino, "light": light_arduino, "keypad": keypad_arduino}
    time.sleep(1)
    display_arduino.write("Welcome to drift")
    grab_alarms(arduinos)
    while 1:
        schedule.run_pending()
        time.sleep(1)









if __name__ == "__main__":
    main(sys.argv)