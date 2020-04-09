import urllib.parse
import urllib.request
import json
import requests
from datetime import datetime


PORT = "3333"
TEST_USERNAME = "max"
TEST_PASSWORD =  "12345678"
TIME_OUT_SECONDS = 3300

def fetch(server_address, username, password, token, received):
    # Authorise
    rerequest_time = datetime.now()
    time_gap = rerequest_time - received
    if time_gap.total_seconds() > TIME_OUT_SECONDS:
        login_resp = requests.post("http://77.97.250.202:3333/login", json={"username":"max", "password":"12345678"})
        login_json = login_resp.json()
        token = login_json["token"]
        received = rerequest_time
    #Fetch alarms
    parameters = {'X-Auth-Token':token}
    alarms_resp = requests.get("http://77.97.250.202:3333/alarms", headers = parameters)
    print(alarms_resp.json())
    #return
    return alarms_resp.json(), token, received


if __name__ == "__main__":
    fetch("http://127.0.0.1", TEST_USERNAME, TEST_PASSWORD)