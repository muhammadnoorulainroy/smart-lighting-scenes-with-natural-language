# logger.py
import time
import sys

_t0 = time.ticks_ms()

def _ts():
    return time.ticks_diff(time.ticks_ms(), _t0) // 1000

def log(src, msg):
    try:
        print("[{:5d}s][{}] {}".format(_ts(), src, msg))
    except Exception:
        # Never let logging crash the app
        pass

def log_err(src, msg, exc=None):
    try:
        print("[{:5d}s][{}][ERR] {}".format(_ts(), src, msg))
        if exc is not None:
            sys.print_exception(exc)
    except Exception:
        pass
