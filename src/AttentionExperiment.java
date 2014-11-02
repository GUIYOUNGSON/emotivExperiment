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
    File thisFile = new File(fileName);
    thisFile.createNewFile();
    writer = new PrintWriter(fileName);
  }

  public void addEpoch(String epochType) throws Exception{
    boolean validEpochType = false;
    for(int i = 0; i < epochTypes.length; i++){
      if(epochTypes[i].equals(epochType)){
        validEpochType = true;
        break;
      }
    }
    if(!validEpochType) throw new Exception("Invalid epoch type given: " + epochType);

    thisEpoch = new Epoch(epochType, numEpochs);
    numEpochs++;
    epochQueue.add(thisEpoch);
  }

  public void addTrials(Iterable<double[][]> trials, int numTrials){
    for(double[][] trial : trials){
      thisEpoch.addTrial(trial, "12345");
    }
  }



  public class Epoch{

    String epochType;
    String numPics;
    Queue<Trial> trialQueue;
    int epochNum;

    public Epoch(String epochType, int epochNum){
      this.epochType = epochType;
      this.epochNum = this.epochNum;
      trialQueue = new LinkedList<Trial>();
    }

    public void addTrial(double[][] data, String timeStamp){
      trialQueue.add(new Trial(data, timeStamp));
    }

    public void flushEpochToFile(){
      writer.println("Epoch: " + epochNum + ", EpochType: " + epochType);
      for(Trial tri : trialQueue){
        writer.println(tri.timeStamp);
      }
    }

    public class Trial{

      double[][] data;
      String timeStamp;
      public Trial(double[][] data, String timeStamp){
        this.data = data;
        this.timeStamp = timeStamp;
      }
    }

  }

}
