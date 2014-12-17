channelNames = ['AF3', 'F7', 'F3', 'FC5', 'T7', 'P7', 'O1', 'O2', 'P8', 'T8', 'FC6',
       'F4', 'F8', 'AF4']

class EmotivEEG:
  def __init__(self, dataFile, journalFile):
      self.eegdata = parseToArray(dataFile)
      self.trialTypes, self.trialStarts, self.trialEnds = journalToArray(journalFile)
      self.labels = makeLabels(self.eegdata, (self.trialTypes, self.trialStarts, self.trialEnds))
      # remove time stamps
      self.eegdata = [data[1:-1] for data in self.eegdata]

def parseToArray(fileName):
  f = open(fileName)
  eegData = []
  for line in f:
    if line != '\n':
        eegData.append( [float(x) for x in line.split(',')]  )
  return eegData

def parseCedToArray(fileName):
     f = open(fileName)
     f.readline()
     return [chan.split()[1] for chan in f]

def journalToArray(journalFile):
    f = open(journalFile)
    ar = [line for line in f]
    ar = ar[ar.index("-endheader-\n")+1:]
    # faces are positive 1, places are -1
    trialType = []
    trialStart = []
    trialEnd = []
    # trials are arbitrarily ended at trialstart + dur
    # TODO: fix
    DUR = 5000
    stimOnsetIdx = len("StimOnset:")
    i = 0
    for line in ar:
        if "Epoch " in line:
            if "faces" in line:
                trialType.append(1)
            else:
                assert ("places" in line)
                trialType.append(-1)
        elif "Trial:0" in line:
            var = long(line.split(',')[2][stimOnsetIdx:])
            trialStart.append(var)
            trialEnd.append(var + DUR)
    return (trialType, trialStart, trialEnd)

def makeLabels(eegData, journalTuple):
    trialTypes, trialStarts, trialEnds = journalTuple
    labels = []
    for dataPoint in eegData:
        timestamp = int(dataPoint[-1])
        i = 0
        try:
            while (timestamp >= trialStarts[i] and timestamp <= trialEnds[i]):
                i += 1
        except IndexError:
            continue
        labels.append(trialTypes[i])
    return labels
