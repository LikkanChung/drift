import urllib.parse
import urllib.request
import json
import requests

PORT = "3333"
TEST_USERNAME = "max"
TEST_PASSWORD =  "12345678"

def old_fetch(server_address, username, password):
    # Authorise
    print("connecting to: " + server_address + ":" + PORT)
    values = {'username': username, 'password' : password}
    data = urllib.parse.urlencode(values)
    data = data.encode('utf-8')  # data should be bytes
    print("request:" + server_address + ":" + PORT + "/login")
    login_req = urllib.request.Request(server_address + ":" + PORT + "/login", data)
    login_req.add_header('Content-Type', 'application/json')
    login_resp = urllib.request.urlopen(login_req)
    login_json = login_resp.read()
    auth_data = json.loads(login_json)
    token = auth_data["token"]
    #Fetch alarms
    alarms_req = urllib.request.Request(server_address + ":" + PORT + "/alarms")
    alarms_req.add_header("X-Auth-Token", token)
    alarms_json = urllib.request.urlopen(alarms_req).read()
    print("Alarms fetched:" + alarms_json)
    #format
    #return
    return alarms_json

def fetch(server_address, username, password):
    # Authorise
    #print("connecting to: " + server_address + ":" + PORT)
    #values = {'username': username, 'password' : password}
    #data = urllib.parse.urlencode(values)
    #data = data.encode('utf-8')  # data should be bytes
    login_resp = requests.post("http://77.97.250.202:3333/login", json={"username":"max", "password":"12345678"})
    login_json = login_resp.json()
    token = login_json["token"]
    #Fetch alarms

    parameters = {'X-Auth-Token':token}
    alarms_resp = requests.get("http://77.97.250.202:3333/alarms", headers = parameters)
    print(alarms_resp.text)

    print(alarms_resp.json())
    #return
    return alarms_resp.json()


if __name__ == "__main__":
    fetch("http://127.0.0.1", TEST_USERNAME, TEST_PASSWORD)