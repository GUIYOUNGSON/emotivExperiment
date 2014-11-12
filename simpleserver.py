#!/usr/bin/env python

import socket
import threading
import random

eegdata = " ".join([str(random.uniform(0, 400)) for r in xrange(14)])

host = '127.0.0.1'
port = 6789
backlog = 5
client = None
size = 1024
eegDataQueue = []
killThread = False

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

s.bind((host, port))
s.listen(backlog)

while 1:
    if not client:
        print "Got Client"
        client, address = s.accept()
    data = client.recv(size)
    if data:
        print "From client: "
        print data
'''
def sendData():
    if killThread:
        return
    threading.Timer(5.0, sendData).start()
    eegDataQueue.append(eegdata)
    print "Added data to queue"

try:
    sendData()
except(KeyboardInterrupt, SystemExit):
    killThread = True
    sys.exit()

while 1:
    if not client:
        client, address = s.accept()
    for dataPoint in eegDataQueue:
        client.send(dataPoint)
    data = client.recv(size)
    if data:
        print "Got data from client "
        print data
    eegDataQueue = []
'''
