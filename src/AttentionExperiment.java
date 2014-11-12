import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.util.*;

public class AttentionExperiment extends WebSocketServer{

  // see what megan does here...
  static int NUM_EPOCHS = 10;
  static int TRAINING_EPOCHS = 5;
  static int FEEDBACK_EPOCHS = 3;
  static int TRIALS_PER_EPOCH = 20;
  static int NUM_IMAGES_PER_CATEGORY = 70;
  static int WEB_SOCKETS_PORT = 8887;
  static int RESPONES_TIME = 100;
  static String MALE_FACES ="./frontend/male_neut/";
  static String FEMALE_FACES ="./frontend/female_neut/";
  static String OUTDOOR_PLACES ="./frontend/outdoor/";
  static String INDOOR_PLACES ="./frontend/indoor/";

  boolean feedback;
  Timer timer;
  EEGJournal journal;
  EEGLog eeglog;
  EEGLoggingThread logger;

  int participantNum;
  boolean[] epochArray;
  String[][] epochImageFiles;
  ExperimentServer thisServer;

  int currentEpoch = 0;
  int currenTrial = 0;
  long timeOfResponse;
  long responseTime;
  long stimOnset;
  double thisRatio;

  Dfa dfa;

  public AttentionExperiment (int participantNum, String outputDir, boolean realFeedback) throws Exception{
    this.participantNum = participantNum;
    this.feedback = realFeedback;
    journal = new EEGJournal(outputDir, participantNum);
    eeglog = new EEGLog();
    eeglog.tryConnect();
    System.out.println("Successfully connected to emotiv");
    eeglog.addUser();
    System.out.println("Successfully added user");
    logger = new EEGLoggingThread(journal, eeglog);
    timer = new Timer();
    epochArray = getEpochArray(participantNum);
    epochImageFiles = getEpochImages();
    // save the header
    journal.addMetaData(getExperimentHeader());

    dfa = new Dfa();
  }

  public static void main(String[] args){
    if(args.length != 3){
      System.out.println("Usage: <ParticipantNum> <outputDir> <realFeedback (1,0)>");
      return;
    }

    int participantNum = Integer.parseInt(args[0]);
    String outputDir = args[1];
    boolean feedback = (Integer.parseInt(args[2]) == 1) ? true : false;

    AttentionExperiment thisExperiment =
      new AttentionExperiment(participantNum, outputDir, feedback);

    thisExperiment.startExperiment();
  }




@Override
public void onMessage( WebSocket conn, String message ) {
    // when we get a message

    // Got a response (it's elapsed time)
    if(message.charAt(0) == 'R' && (responseTime == null ) ){
      long timeOfResponse = System.`currentTimeMillis();
      long responseTime = Long.parseLong(message.substring(2));
    }
    else if(message.charAt(0) == 'C'){
      // doNext from user input
      dfa.doNext(false);
    }
    else if(message.charAt(0) == 'Q' && message.charAt(1) == 'q'){
      System.out.println("Quitting ... goodbye!");
      if(logger != null)  logger.close();
      return;
    }
}

sdfads 
public void sendToAll( String text ) {
  Collection<WebSocket> con = connections();
  synchronized ( con ) {
    for( WebSocket c : con ) {
      c.send( text );
    }
  }
}



  /* Set up any scheduled processes */
  public void startExperiment(){
    // shows the first instruction.
    dfa.start();
  };


  private String getExperimentHeader(){
      String headerString = "/*********************************************/\n";
      headerString += "Participant Number: " + participantNum + "\n";
      headerString += "------Epochs-----\n\n";
      for(int i = 0; i < epochImageFiles.length; i++){
        headerString += "Epoch " + i + ": \n";
        for(String imageFile : epochImageFiles[i]){
          headerString += imageFile + "\n";
        }
        headerString+= "\n\n";
      }

      return headerString;
  }

  /* Returns a vector, [clickFemale, clickIndoor],
  where clickFemale is true if array[0] = true */
  private boolean[] getEpochArray(int participantNum){
    boolean[] twoTasks = new boolean[2];
    switch(participantNum % 4){
      case(1):
        twoTasks[0] = false;
        twoTasks[1] = false;
      case(2):
        twoTasks[0] = false;
        twoTasks[1] = true;
      case(3):
        twoTasks[0] = true;
        twoTasks[1] = false;
      case(4):
        twoTasks[0] = true;
        twoTasks[1] = true;
    }

    return twoTasks;
  }

  private Queue<Integer> newRandomInts(int length){
    Queue<Integer> list = new LinkedList<Integer>();
    for(int i = 0; i < length; i++){
      list.add(i);
    }
    Collections.shuffle(list);
    return list;
  }

  /* Returns an array filenames specifying images to be shown in each trial*/
  private String[][] getEpochImages(){
    String[][] epochImages = new String[NUM_EPOCHS][];
    // randomly choose faces and places
    Queue<Integer> male = newRandomInts(NUM_IMAGES_PER_CATEGORY);
    Queue<Integer> female = newRandomInts(NUM_IMAGES_PER_CATEGORY);
    Queue<Integer> outdoor = newRandomInts(NUM_IMAGES_PER_CATEGORY);
    Queue<Integer> indoor = newRandomInts(NUM_IMAGES_PER_CATEGORY);

    for(int i = 0; i < NUM_EPOCHS; i++){
      // two images for every trial
      String[] thisEpochImages = new String[TRIALS_PER_EPOCH*2];

      // Do a faces epoch
      if(i%2 == 0){
        for(int j = 0; j < TRIALS_PER_EPOCH; j+= 2){
          if(male.isEmpty()){
            male = newRandomInts(NUM_IMAGES_PER_CATEGORY);
          }
          thisEpochImages[j] = MALE_FACES + Integer.toString(male.remove()) + ".jpg";

          if(female.isEmpty()){
            female = newRandomInts(NUM_IMAGES_PER_CATEGORY);
          }
          thisEpochImages[j+1] = FEMALE_FACES + Integer.toString(female.remove()) + ".jpg";
        }
      }
      else{
        for(int j = 0; j < TRIALS_PER_EPOCH; j+= 2){
          if(outdoor.isEmpty()){
            outdoor = newRandomInts(NUM_IMAGES_PER_CATEGORY);
          }
          thisEpochImages[j] = OUTDOOR_PLACES + Integer.toString(outdoor.remove()) + ".jpg";

          if(indoor.isEmpty()){
            indoor = newRandomInts(NUM_IMAGES_PER_CATEGORY);
          }
          thisEpochImages[j+1] = INDOOR_PLACES + Integer.toString(indoor.remove()) + ".jpg";
        }
      }

      epochImages[i] = thisEpochImages;

    }

    return epochImages;

  }


  private class Dfa{
    private int epochNum;
    private int trialNum;
    private boolean inInstructs;
    private boolean inTraining;

    /* Begin the experiment by showing the first instruction */
    public void start(){
      epochNum = 0;
      trialNum = 0;
      sendToAll(getInstructionCommand(epochNum));
      journal.addEpoch(epochType(epochNum));
      inInstructs = true;
      thisRatio = 0.5;
      // initializes logger, does not start acquisition
      logger.init();
    }

    public void doNext(boolean fromTimer){
      // can only get here not from timer if we're in instructs
      if(!fromTimer && !inInstructs) return;
      if(!inInstructs){
        journal.endTrial(stimOnset, timeOfResponse, responseTime);
        trialNum++;
        if(trialNum < TRIALS_PER_EPOCH){
          journal.addTrial(thisRatio, epochImageFiles[epochNum][trialNum%2],
            epochImageFiles[epochNum][trialNum%2+1]);
          timeOfResponse = null;
          responseTime = null;
          // Use 50% ratio
          sendToAll(getTrialImagesCommand(epochNum, trialNum, thisRatio));
          stimOnset = System.currentTimeMillis();
          timer.schedule(new doNextLater(), RESPONSE_TIME);
        }
        // Otherwise, go to next epoch
        else{
          logger.resume();
          sendToAll(getInstructionCommand(epochNum));
          inInstructs = true;
          journal.endTrial(stimOnset, timeOfResponse, responseTime);
          // Show the next instruction
          epochNum++;
          journal.addEpoch(epochType(epochNum));
          trialNum = 0;
        }
      }
      // advance to start of trial
      else{
        inInstructs = false;
        journal.addTrial(thisRatio);
        timeOfResponse = null;
        responseTime = null;
        stimOnset = System.currentTimeMillis();
        logger.resume();
        sendToAll(getTrialImagesCommand(epochNum, trialNum, thisRatio));
        timer.schedule(new doNextLater(), RESPONSE_TIME);
      }
    }

    private class doNextLater extends TimerTask{

      public void run(){
        doNext(true);
      }

    }

    public String epochType(int epochNum){
      String epochType;
      if(epochNum%2 == 0)  epochType = "faces,";
      else epochType = "places";

      if(epochNum >= TRAINING_EPOCHS){
        epochType += "feedback";
      }
      return epochType;
    }

    public String getInstructionCommand(int epochNum){
      String instruct = "I,Click when the ";
      if(epochNum%2){
        instruct += "face you see is ";
        instruct += (AttentionExperiment.epochArray[0]) ? "female" : "male";
      }
      else{
        instruct += "place you see is ";
        instruct += (AttentionExperiment.epochArray[0]) ? "indoors" : "outdoors";
      }
    }

    public String getTrialImagesCommand(int epochNum, int trialNum, double ratio){
      return "S," + epochImageFiles[epochNum][trialNum*2] + "," + epochImageFiles[epochNum][trialNum*2+1] +
      "," + ratio;
    }

  }



}
