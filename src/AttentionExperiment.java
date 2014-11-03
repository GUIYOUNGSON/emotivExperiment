import java.util.*;
import java.io.*;

public class AttentionExperiment{

  private int participantNum;
  private String fileName;
  private PrintWriter writer;
  private Queue<Epoch> epochQueue;
  private int numEpochs;
  private Epoch thisEpoch;
  private static int NUM_CHANNELS = 14;
  static String[] epochTypes =
    {"indoorClick", "outdoorClick", "femaleClick", "maleClick"};

  public AttentionExperiment(String outputDir, int participantNum) throws IOException{
    this.participantNum = participantNum;
    Date thisDate = new Date();
    this.fileName = outputDir + "/" + thisDate + "_" + participantNum;
    numEpochs = 0;
    epochQueue = new LinkedList<Epoch>();
    //File thisFile = new File(fileName);
    //thisFile.createNewFile();
    //writer = new PrintWriter(fileName);
  }

  public void addEpoch(String epochType){
    thisEpoch = new Epoch(epochType, numEpochs);
    numEpochs++;
    epochQueue.add(thisEpoch);
  }

  public void addTrial(String picture1, String picture2, float ratio){
    thisEpoch.addTrial(picture1, picture2, radio);
  }


  public class Epoch{

    String epochType;
    Queue<Trial> trialQueue;
    int epochNum;
    Trial thisTrial;
    int trialNum;
    boolean readyForNewTrial;
    ArrayList<ArrayList<Double>> channelData;
    ArrayList<Float> timeStamps;

    public Epoch(String epochType, int epochNum){
      this.epochType = epochType;
      this.epochNum = this.epochNum;
      this.readyForNewTrial = true;
      this.trialNum = 0;
      trialQueue = new LinkedList<Trial>();
      channelData = new ArrayList<ArrayList<Double>>();
      // add an array list for each channel
      for(int i = 0; i < NUM_CHANNELS; i++){
        channelData.add(i, new ArrayList<Double>());
      }
      timeStamps = new ArrayList<Float>();
    }

    public void addTrial(String picture1, String picture2, float ratio){
      if(!readyForNewTrial) endTrial();
      Trial thisTrial = new Trial(picture1, picture2, ratio, trialNum++);
      trialQueue.add(thisTrial);
    }

    public void addData(double[][] data, float timeStamp){

      assert(data[0] != null);

      // add timestamps
      for(int i = 0; i < data[0][0].length; i++){
        timeStamps.add(timeStamp);
      }

      for(int i = 0; i < NUM_CHANNELS; i++){
        double[] thisChannelData = data[i];
        // put data into channelData storage struct
        for(int j = 0; j < thisChannelData.length; j++){
          channelData.get(i).add(thisChannelData[j]);
        }
      }
    }

    // Flush all data from queue to trial, and clear channelsData.
    public void endTrial(){
      if(readyForNewTrial)  return;
      Double[][] trialData = new Double[NUM_CHANNELS];
      for(int i = 0; i < NUM_CHANNELS; i++){
        ArrayList<Double> thisChannelData = channelsData.get(i);
        trialData[i] = thisChannelData.toArray(new Double[thisChannelData.size()]);
        thisChannelData.clear();
      }

      thisTrial.addData(trialData, timeStamps.toArray(new Float[timeStamps.size()]));
      timeStamps.clear();

      readyForNewTrial = true;
    }

    public void flushEpochToFile(){
      writer.println("Epoch: " + epochNum + ", EpochType: " + epochType);
      for(Trial tri : trialQueue){
        writer.println(tri);
      }
    }

    public class Trial{
      Double[][] data;
      Float[] timeStamps;
      int trialNum;
      String picture1;
      String picture2;
      float ratio;

      public Trial(String picture1, String picture2, float ratio, int trialNum){
        this.trialNum = trialNum;
        this.picture1 = picture1;
        this.picture2 = picture2;
        this.ratio = ratio;
      }

      public void setData(Double[][] data, Float[] timeStamps){
        this.data = data;
        this.timeStamps = timeStamps;
      }


      public String toString(){
        String[] channels = EEGLog.channels;
        String outString = null;
        if(data[0] == null || data[0].length <= 0)   return null;
        int numMeasurements = data[0].length;
        for(int i = 0; i < numMeasurements; i++){
          for(int j = 0; j < channels.length; j++){
            outString += data[j][i];
            if(j != (channels.length - 1) )  outString += ",";
          }
          System.out.println();
        }

        return outString;
      }

    }

  }

}
