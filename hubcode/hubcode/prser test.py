import dateutil.parser
from dateutil.parser import *
from dateutil.tz import *
from datetime import datetime
import pytz
import time


def main():
    #print("ahh")
    #print(time.time())
    #TZOFFSETS = {"BRST": -10800}
    #BRSTTZ = tzoffset("BRST", -10800)
    #DEFAULT = datetime(2003, 9, 25)
    #p = parse("2020-04-10T10:00:00Z")
    #print(p)
    #too_early = datetime(2000,1,1)
    #print(too_early)
    #then = datetime.now()
    #long_time = then-too_early
    #print(long_time.total_seconds())
    #time.sleep(2)
    #now = datetime.now()
    #passed = now - then
    #print(passed.seconds)
    #print(then)
    #print(now)
    #print(passed)
    print(61 % 60)
    print(datetime.utcnow().replace(tzinfo=pytz.utc))


if __name__ == "__main__":
    main()