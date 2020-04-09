import http.client
import urllib.parse
import urllib.request
import json


PORT = "3333"
TEST_USERNAME = "jack"
TEST_PASSWORD =  "21ysxqkcl1i3c3h8ou7l"

def fetch(server_address, username, password):
    # Authorise
    print("connecting to: " + server_address + ":" + PORT)
    #http.client.HTTPconnection(server_address + ":" + PORT)
    values = {'username': username, 'password' : password}
    data = urllib.parse.urlencode(values)
    data = data.encode('utf-8')  # data should be bytes
    login_req = urllib.request.Request(server_address + ":" + PORT + "/login", data)
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


if __name__ == "__main__":
    fetch("http://127.0.0.1", TEST_USERNAME, TEST_PASSWORD)