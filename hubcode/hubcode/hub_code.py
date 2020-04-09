import serial
import sys
import time
import json
import schedule
import dateutil.parser

from backend_connection import fetch

#The hub should
#authenticate from the back end
#Take alarms from the back end
#activate light and sound when the alarm is received
#deactivate afterwards


arduinos_connected = False

display_module_port = '/dev/ttyUSB4'
keypad_module_port = '/dev/ttyUSB0'
light_module_port = '/dev/ttyUSB2'
sound_module_port = '/dev/ttyUSB3'


TEST_USERNAME = "jack"
TEST_PASSWORD =  "21ysxqkcl1i3c3h8ou7l"
TEST_URL = "http://77.97.250.202"
display_arduino = None
sound_arduino = None
light_arduino = None
keypad_arduino = None


def grab_alarms(arduinos):
    schedule.clear()
    alarms = fetch(TEST_URL, TEST_USERNAME, TEST_PASSWORD)
    print("fetched")
    print(alarms)
    # GET ALARM TIMES
    for alarm in alarms:
        print(alarm)
        hour = alarm["time"][11:13]
        minute = alarm["time"][14:16]
        print(hour + ":" + minute)
        schedule.every().day.at(hour + ":" + minute).do(alarm_once, arduinos)
        print("alarm entered")
        if arduinos_connected:
            alarm_notification = "Alarm at " + hour + ":" + minute
            display_arduino.write(bytes("#0;0;" + alarm_notification +";\n"))
    schedule.every().minute.at(":17").do(grab_alarms, arduinos)

def shutdown_once(arduinos):
    shutdown(arduinos)
    return schedule.CancelJob

def shutdown(arduinos):
    arduinos["light"].write(b"#0;-1;\n")
    arduinos["sound"].write(b"#0;-1;\n")
    print("shutdown modules")

def alarm_once(arduinos):
    alarm(arduinos)
    return schedule.CancelJob


def alarm(arduinos):
    print("An alarm is going off")
    print("Success")
    if(arduinos_connected):
        arduinos["light"].write(b"#10;10;\n")
        arduinos["sound"].write(b"#10;10;\n")
    print("send an alarm to the modules")

def main(argv):
    print("Attempting connection")
    if len(argv) != 2:
        print("Usage: " + argv[0] + "[COM port number]")
    if arduinos_connected:
        display_arduino = serial.Serial(display_module_port, 9600, timeout=0.1)
        sound_arduino = serial.Serial(sound_module_port, 9600, timeout=.1)
        light_arduino = serial.Serial(light_module_port, 9600, timeout=.1)
        keypad_arduino = serial.Serial(keypad_module_port, 9600, timeout=.1)
        arduinos = {"display": display_arduino, "sound": sound_arduino, "light": light_arduino, "keypad": keypad_arduino}
        display_arduino.write(b"#0;0;Welcome to drift;\n")
        time.sleep(2)
    else:
        arduinos = {"oof" : True}
    grab_alarms(arduinos)
    while 1:
        schedule.run_pending()
        time.sleep(1)









if __name__ == "__main__":
    main(sys.argv)