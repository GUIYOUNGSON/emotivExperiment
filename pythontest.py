import time;

def build_model(inputfile):

    f = open(inputfile)
    i = 0;
    for line in f:
	       i+= 1
    return "%d Lines" % (i)


command = input()
time.sleep(2)
print "Executed Command: " + str(command)
