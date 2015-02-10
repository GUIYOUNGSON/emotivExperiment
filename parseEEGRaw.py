from pylab import *
import numpy as np
import scipy.interpolate as interpolate
import matplotlib.image as mpimg

markers = [(86,56),(40,65), (70,75), (53, 100), (25,125),
           (29,165), (80, 215), (135,215), (193,165),
           (200,125), (170,100), (150,75),(185,65), (135, 56)]
scalpFile = '/Users/Dale/Dropbox/code/neuromancer/scalp.png'
channels = ['AF3', 'F7', 'F3', 'FC5', 'T7', 'P7', 'O1', 'O2', 'P8', 'T8', 'FC6',
       'F4', 'F8', 'AF4']

def parseToArray(fileName):
  f = open(fileName)
  eegData = []
  for line in f:
    if line != '\n':
        eegData.append( [float(x) for x in line.split(',')]  )
  f.close()
  return eegData

def parseCedToArray(fileName):
     f = open(fileName)
     f.readline()
     return [chan.split()[1] for chan in f]

def plotScalpContour(component, axis):
    imp = mpimg.imread(scalpFile)
    axis.imshow(imp)
    X = [x[0] for x in markers]
    Y = [y[1] for y in markers]
    Z = [float(x)/component.sum() for x in component]
    f = interpolate.interp2d(X,Y,Z)
    X = np.linspace(0,200)
    Y = X
    Z = f(X,Y)
    #axis.pcolor(X,Y,Z)
    axis.contour(X, Y, Z)

class Journal:
    def __init__(self, journal_file):
        epochOnset = []
        epochOffset = []
        epochCorrect = []
        epochType = []
        # Load journal data
        f = open(journal_file)
        ar = [line for line in f]
        ar = ar[ar.index("-endheader-\n")+1:]
        stimOnsetIdx = len("StimOnset:")
        stimOffsetIdx = len("StimOffset:")
        correctIdx = len("Correct:")
        epochNum = -1
        f.close()
        for line in ar:
            if "Type" in line:
                epochType.append(line.split()[-1])
                epochNum += 1
                epochOnset.append([])
                epochOffset.append([])
                epochCorrect.append([])
            else:
                line = line.split(',')
                epochOnset[epochNum].append(long(line[2][stimOnsetIdx:]))
                epochOffset[epochNum].append(long(line[3][stimOffsetIdx:]))
                epochCorrect[epochNum].append((True if "true" in line[-1] else False))

        self.epochOnset = epochOnset
        self.epochOffset = epochOffset
        self.epochCorrect = epochCorrect
        self.epochType = epochType
        self.numEpochs = len(epochType)


class EEGData:
    def __init__(self, journal, eegdata):
        eegdata = sorted(eegdata, key=lambda point: long((point[-1])))
        self.epochs = []
        self.timestamps = []
        offset = 0
        for epochNum in range(0, journal.numEpochs):
            thisEpoch = []
            thisTimestamps = []
            epochStart = journal.epochOnset[epochNum][0]
            epochEnd = journal.epochOffset[epochNum][-1]
            while long(eegdata[offset][-1]) < epochStart:
                offset += 1
            thisTime = long(eegdata[offset][-1])
            while (thisTime >= epochStart) and (thisTime <= epochEnd):
                thisEpoch.append(eegdata[offset][3:17])
                thisTimestamps.append(eegdata[offset][-1])
                offset += 1
                thisTime = long(eegdata[offset][-1])

            self.epochs.append(thisEpoch)
            self.timestamps.append(thisTimestamps)

'''
class EEGData:
    def __init__(self, journal, eegdata):
        eegdata = sorted(eegdata, key=lambda point: long((point[-1])))
        self.epochs = []
        offset = 0
        for epochNum in range(0, journal.numEpochs):
            thisEpoch = []
            trial = []
            for (startTime, endTime) in zip(journal.epochOnset[epochNum], journal.epochOffset[epochNum]):
                while long(eegdata[offset][-1]) < startTime:
                    offset += 1
                thisTime = long(eegdata[offset][-1])
                while (thisTime >= startTime) and (thisTime <= endTime):
                    trial.append(eegdata[offset])
                    offset += 1
                    thisTime = long(eegdata[offset][-1])
                thisEpoch.append(np.array(trial))
                trial = []
            self.epochs.append(thisEpoch)
'''
