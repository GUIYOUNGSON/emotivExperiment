
# coding: utf-8

# In[65]:

import numpy as np
import matplotlib.pyplot as plt
from scipy import signal
from sklearn.decomposition import FastICA, PCA
from scipy import fft
print "Done importing"

participant_num = 1

# In[66]:

# Load journal data
f = open("../participant_data/log/journal_1_2015.01.28.05.06")
ar = [line for line in f]
ar = ar[ar.index("-endheader-\n")+1:]
# faces are positive 1, places are -1
trialType = []
trialStart = []
trialEnd = []
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

trialEnd = [data + 1000*10 for data in trialStart]



# In[81]:

# Load EEG Data and save labels, timestamps

f = open("../realdata/eeg_100_2014.12.22.09.05")
eegdata = []
timestamps = []

datafile = [line for line in f]
for line in datafile:
    if len(line) > 1:
        datapoints = line.split(",")
        timestamps.append(long(datapoints[-1]))
        eegdata.append([float(data) for data in datapoints[:-1]])

globaltimestamps = timestamps
globaleegdata = eegdata
eegArray = np.array(eegdata)
eegArray = eegArray[:,3:17]
eegdata = eegArray.tolist()


# In[74]:

# Break into epoch arrays
epocharrays = []
timestamparrays = []
for i in range(0, len(trialStart)):
    thisEpoch = [data for index, data in enumerate(eegdata) if ((timestamps[index] >= trialStart[i] and timestamps[index] <= trialEnd[i]))]
    epocharrays.append(thisEpoch)
    timestamparrays.append([timestamps[index] for index, data in enumerate(eegdata) if (timestamps[index] >= trialStart[i] and timestamps[index] <= trialEnd[i])])



# In[75]:

print "Done loading"


# #Data Analysis

# In[76]:

# Block data into windows
def window(windowLen, epocharrays, timestamparrays, minsize):
    windowedEpochs = []
    for idx, thisEpoch in enumerate(epocharrays):
        thistimestamps = timestamparrays[idx]
        windows = []
        startTime = thistimestamps[0]
        thisWindow = []
        endTime = startTime + windowLen
        numPoints = len(thisEpoch)
        for idx2 in range(0, numPoints):
            if thistimestamps[idx2] > endTime:
                startTime = endTime + 1
                endTime = startTime + windowLen
                if len(thisWindow) > minsize:
                    windows.append(thisWindow)
                thisWindow = []
            if thistimestamps[idx2] >= startTime and thistimestamps[idx2] <= endTime:
                thisWindow.append(data)
        windowedEpochs.append(windows)
    return windowedEpochs

windowedEpochs = window(3000, epocharrays, timestamparrays, 100)
for epoch in windowedEpochs:
    print "Epoch of length %d:" % (len(epoch))
    for window in epoch:
        print "Window has length " + str(len(window))



# In[77]:

#Features are 14-dimensional vectors that are the value of each channel
features = []
labels = []
numpointsavg = 10
gc = mdp.nodes.GaussianClassifier()
# average every ten points together, save the results in a feature vector
for idx, epoch in enumerate(windowedEpochs):
    for window in epoch:
        windowarray = np.array(window)
        while windowarray.shape[0] >= numpointsavg:
            thisFeature = windowarray[:numpointsavg].mean(0)
            windowarray = windowarray[numpointsavg:]
            thisFeature = np.asarray([thisFeature])
            gc.train(thisFeature, trialType[idx])
           # features.append(windowarray)
           # labels.append(trialType[idx])

#features = np.array(features)
#print features.shape


# In[78]:

print np.array(windowedEpochs[0][0][30]).shape
gc.label(np.array([windowedEpochs[0][0][30]]note))


# In[88]:


eegArray = np.array(globaleegdata)
print eegArray.shape
eegArray = eegArray[:,3:17]
eegArray = eegArray.transpose()
f, arrs = plt.subplots(14, sharex = True, sharey = True)
i = 0
fig = plt.gcf()
fig.set_size_inches(15,30)
for data in eegArray:
    arrs[i].plot(globaltimestamps, eegArray[i], 'o')
    i += 1

arrs[0].set_title("Raw Channels")


# In[48]:

ica = FastICA()
S_ = ica.fit_transform(eegArray.T)  # Reconstruct signals
A_ = ica.mixing_  # Get estimated mixing matrix
assert np.allclose(eegArray.T, np.dot(S_, A_.T) + ica.mean_)
assert np.allclose(S_, ica.transform(eegArray.T))


# In[49]:

f, arrs = plt.subplots(14, sharex = True, sharey = True)
fig = plt.gcf()
fig.set_size_inches(15,30)
print arrs.shape
print S_.transpose().shape
i = 0
plt.ylim((-0.12, 0.12))
for data in S_.transpose():
    arrs[i].plot(data)
    arrs[i].set_title("Component " + str(i + 1))
    i += 1

arrs[0].set_title(trialName + "_ICA_Components")


# In[50]:

import sys
sys.path.append('..')
import parseEEGRaw
parseEEGRaw = reload(parseEEGRaw)
f, arrs = plt.subplots(nrows=3, ncols=5, figsize=(100,100))
f.set_size_inches(20,20)
i = 0
j = 0
markers = [(86,56),(40,65), (70,75), (53, 100), (25,125),
           (29,165), (80, 215), (135,215), (193,165),
           (200,125), (170,100), (150,75),(185,65), (135, 56)]
x,y = zip(*markers)

for component in ica.components_.T:
    parseEEGRaw.plotScalpContour(component, arrs[i][j])
    arrs[i][j].set_title("Component " + str(i*5 + j + 1))
    arrs[i][j].plot(x,y,'o')
    j += 1
    if j == 5:
        j = 0
        i += 1

plt.savefig(trialName + "_Topo_Plot")


# In[18]:




# In[ ]:
