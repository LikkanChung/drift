import dateutil.parser
from dateutil.parser import *
from dateutil.tz import *
from datetime import *
import time


def main():
    print("ahh")
    print(time.time())
    TZOFFSETS = {"BRST": -10800}
    BRSTTZ = tzoffset("BRST", -10800)
    DEFAULT = datetime(2003, 9, 25)
    p = parse("2020-04-10T10:00:00Z")
    print(p)

if __name__ == "__main__":
    main()