import java.util.*;
import java.io.*;

public class EEGJournal{

  private static boolean DEBUG = true;
  private int participantNum;
  private String fileName = "testdata/testfile.csv"; // in the future, do not hard code
  private PrintWriter writer;
  private Queue<Epoch> epochQueue;
  private int numEpochs;
  private Epoch thisEpoch;
  private ArrayList<String> userMetaData;

  static String[] channels = EEGLog.channels;

  public EEGJournal(String outputDir, int participantNum) throws IOException{
    this.participantNum = participantNum;
    Date thisDate = new Date();
    numEpochs = 0;
    epochQueue = new LinkedList<Epoch>();
    writer = new PrintWriter(fileName);
    userMetaData = new ArrayList<String>();
  }


  public void addEpoch(String epochType){
    thisEpoch = new Epoch(epochType, numEpochs);
    numEpochs++;
    epochQueue.add(thisEpoch);
  }

  // We only keep track of the ratio with which the pictures were displayed,
  // since which images were displayed is determined in advance.
  public void addTrial(double ratio, String im1, String im2){
    thisEpoch.addTrial(ratio, im1, im2);
  }

  public void endTrial(long timeImageOnset, long timeOfResponse, long responseTime){
    if(DEBUG) System.out.println(thisEpoch.thisTrial);
    thisEpoch.endTrial(timeImageOnset, timeOfResponse, responseTime);
  }

  public void endTrial(long timeImageOnset){
    if(DEBUG) System.out.println(thisEpoch.thisTrial);
    thisEpoch.endTrial(timeImageOnset, -1, -1);
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

    public Epoch(String epochType, int epochNum){
      this.epochType = epochType;
      this.epochNum = this.epochNum;
      this.trialNum = 0;
      trialQueue = new LinkedList<Trial>();
    }

    public void addTrial(double ratio, String im1, String im2){
      thisTrial = new Trial(ratio, trialNum++, im1, im2);
      trialQueue.add(thisTrial);
    }

    // Set image onset details, response times, etc.
    public void endTrial(long timeImageOnset, long timeOfResponse, long responseTime){
      thisTrial.stimulusOnset = timeImageOnset;
      thisTrial.timeOfResponse = timeOfResponse;
      thisTrial.responseTime = responseTime;

    }

  }

  public class Trial{
    int trialNum;
    double ratio;
    long stimulusOnset;
    long timeOfResponse;
    long responseTime;
    String im1;
    String im2;

    public Trial(double ratio, int trialNum, String im1, String im2){
      this.im1 = im1;
      this.im2 = im2;
      this.trialNum = trialNum;
      this.ratio = ratio;
    }


    public String toString(){
      return "Trial:" + trialNum + ",ImageRatio:" + ratio +
        ",StimOnset:" + stimulusOnset + ",TimeOfResponse:" + timeOfResponse +
        ",ResponseTime:" + responseTime;
    }

  }

}
