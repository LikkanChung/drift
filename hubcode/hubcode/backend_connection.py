import http.client
import urllib.parse
import urllib.request


PORT = "3333"
TEST_USERNAME = "jack"
TEST_PASSWORD =  "21ysxqkcl1i3c3h8ou7l"

def fetch(server_address, username, password):
    #connect
    print("connecting to: " + server_address + ":" + PORT)
    #http.client.HTTPconnection(server_address + ":" + PORT)
    values = {'username': username, 'password' : password}
    data = urllib.parse.urlencode(values)
    data = data.encode('utf-8')  # data should be bytes
    req = urllib.request.Request(server_address + ":" + PORT + "/login", data)
    resp = urllib.request.urlopen(req)
    respData = resp.read()
    print(respData)


    #Authorise

    #Fetch alarms
    #format
    #return


if __name__ == "__main__":
    fetch("http://127.0.0.1", TEST_USERNAME, TEST_PASSWORD)