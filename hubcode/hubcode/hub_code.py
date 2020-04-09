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
light_module_port = 'COM2'
sound_module_port = 'COM3'


TEST_USERNAME = "jack"
TEST_PASSWORD =  "21ysxqkcl1i3c3h8ou7l"
TEST_URL = "http://localhost"

def grab_alarms():
    schedule.clear()
    alarms_json = fetch(TEST_URL, TEST_USERNAME, TEST_PASSWORD)
    # GET ALARM TIMES
    alarms = []
    print("I have no idea what the JSON for this looks like")
    # TODO:GET TIMES FROM JSON
    for alarm in alarms:
        schedule.every().day.at(alarm.time).do(alarm_once)
    schedule.every().hour.at.do(grab_alarms())


def alarm_once():
    alarm()
    return schedule.cancel_job()


def alarm():
    print("send an alarm to the modules")

def main(argv):
    print("Attempting connection")
    #if len(argv) != 2:
    #    print("Usage: " + argv[0] + "[COM port number]")
    arduino = serial.Serial(display_module_port, 9600, timeout=.1)
    arduino.write("Welcome to drift")
    grab_alarms()
    while 1:
        schedule.run_pending()
        time.sleep(1)









if __name__ == "__main__":
    main(sys.argv)