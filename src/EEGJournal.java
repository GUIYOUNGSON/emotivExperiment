import java.util.*;
import java.io.*;

public class EEGJournal{

  private static boolean DEBUG = true;
  private int participantNum;
  private String fileName = "testdata/testfile.csv";
  private PrintWriter writer;
  private Queue<Epoch> epochQueue;
  private int numEpochs;
  private Epoch thisEpoch;
  private ArrayList<String> userMetaData;

  static String[] channels = EEGLog.channels;

  public EEGJournal(String outputDir, int participantNum) throws IOException{
    this.participantNum = participantNum;
    Date thisDate = new Date();
    // this.fileName = outputDir + "/" + thisDate + "_" + participantNum;
    numEpochs = 0;
    epochQueue = new LinkedList<Epoch>();
    writer = new PrintWriter(fileName);
  }

  public String dumpAllData(){
    String outString = "";
    for(Epoch thisEpoch: epochQueue){
      outString += "Epoch " + thisEpoch.epochNum + ":\n";
      for(Trial trial : thisEpoch.trialQueue){
        outString += trial;
      }
    }

    return outString;
  }

  public void addEpoch(String epochType){
    thisEpoch = new Epoch(epochType, numEpochs);
    numEpochs++;
    epochQueue.add(thisEpoch);
  }

  // We only keep track of the ratio with which the pictures were displayed,
  // since which images were displayed is determined in advance.
  public void addTrial(float ratio){
    thisEpoch.addTrial(ratio);
  }

  public void endTrial(long timeImageOnset, long timeOfResponse, long resposeTime){
    if(DEBUG) System.out.println(thisTrial);
    thisEpoch.endTrial(timeImageOnset, timeOfResponse, responseTime);
  }

  public void addData(double[][] data, long timeStamp){
    thisEpoch.addData(data, timeStamp);
  }

  public void addMetaData(String metadata){
    userMetaData.add(metadata);
  }


  public class Epoch{

    String epochType;
    Queue<Trial> trialQueue;
    int epochNum;
    Trial thisTrial;
    int trialNum;
    ArrayList<ArrayList<Double>> channelData;
    ArrayList<Long> timeStamps;

    public Epoch(String epochType, int epochNum){
      this.epochType = epochType;
      this.epochNum = this.epochNum;
      this.trialNum = 0;
      trialQueue = new LinkedList<Trial>();
      channelData = new ArrayList<ArrayList<Double>>();
      // add an array list for each channel
      for(int i = 0; i < channels.length; i++){
        channelData.add(i, new ArrayList<Double>());
      }
      timeStamps = new ArrayList<Long>();
    }

    public void addTrial(float ratio){
      thisTrial = new Trial(ratio, trialNum++);
      trialQueue.add(thisTrial);
    }

    public void addData(double[][] data, Long timeStamp){

      assert(data[0] != null);

      // add timestamps
      for(int i = 0; i < data[0].length; i++){
        timeStamps.add(timeStamp);
      }

      for(int i = 0; i < channels.length; i++){
        double[] thisChannelData = data[i];
        // put data into channelData storage struct
        ArrayList<Double> thisChannelStorage = channelData.get(i);
        for(int j = 0; j < thisChannelData.length; j++){
          thisChannelStorage.add(thisChannelData[j]);
        }
      }
    }

    // Flush all data from queue to trial, and clear channelsData.
    public void endTrial(long timeImageOnset, long timeOfResponse, long resposeTime){
      if(thisTrial == null)  return;
      Double[][] trialData = new Double[channels.length][];
      for(int i = 0; i < channels.length; i++){
        ArrayList<Double> thisChannelData = channelData.get(i);
        trialData[i] = thisChannelData.toArray(new Double[thisChannelData.size()]);
        thisChannelData.clear();
      }

      thisTrial.setData(trialData, timeStamps.toArray(new Long[timeStamps.size()]));
      thisTrial.stimulusOnset = timeImageOnset;
      thisTrial.timeOfResponse = timeOfResponse;
      thisTrial.responseTime = responseTime;
      timeStamps.clear();

    }

    public void flushEpochToFile(){
      writer.println("Epoch: " + epochNum + ", EpochType: " + epochType);
      for(Trial tri : trialQueue){
        writer.println(tri);
      }
    }

  }

  public class Trial{
    Double[][] data;
    Long[] timeStamps;
    int trialNum;
    float ratio;
    long stimulusOnset;
    long timeOfResponse;
    long responseTime;
    boolean correctResponse;

    public Trial(float ratio, int trialNum){
      this.trialNum = trialNum;
      this.ratio = ratio;
    }

    public void setData(Double[][] data, Long[] timeStamps){
      this.data = data;
      this.timeStamps = timeStamps;
    }


    public String toString(){
      String[] channels = EEGLog.channels;
      String outString = "Stimulus Onset: " + stimulusOnset + ",Time of Response: " + ((timeOfResponse != null) ? timeOfResponse : "none") +
      ",Response Time: " + ((responseTime != null) ? responseTime : "none") + "\n";

      for(String channel : channels){
        outString += channel + ", ";
      }
      outString += "\n";
      if(data == null)  return null;
      if(data[0] == null || data[0].length <= 0)   return null;
      int numMeasurements = data[0].length;
      for(int i = 0; i < numMeasurements; i++){
        for(int j = 0; j < data.length; j++){
          outString += data[j][i];
          outString += ",";
        }
        outString += (timeStamps[i] + "\n");
      }
      return outString;
    }

  }

}
