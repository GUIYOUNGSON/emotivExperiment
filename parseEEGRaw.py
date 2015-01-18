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
