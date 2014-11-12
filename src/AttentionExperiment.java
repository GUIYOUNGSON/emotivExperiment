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
  static boolean DEBUG = true;
  static int NUM_EPOCHS = 5;
  static int TRAINING_EPOCHS = 3;
  static int FEEDBACK_EPOCHS = 2;
  static int TRIALS_PER_EPOCH = 4;
  static int NUM_IMAGES_PER_CATEGORY = 70;
  static int WEB_SOCKETS_PORT = 8885;
  static int RESPONSE_TIME = 100*10;
  static String MALE_FACES ="./imagesRenamed/male_neut/";
  static String FEMALE_FACES ="./imagesRenamed/female_neut/";
  static String OUTDOOR_PLACES ="./imagesRenamed/outdoor/";
  static String INDOOR_PLACES ="./imagesRenamed/indoor/";

  boolean feedback;
  boolean withEEG;
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
  Long timeOfResponse;
  Long responseTime;
  Long stimOnset;
  double thisRatio;

  Dfa dfa;

  // starts experiment with real eeg data by default
  public AttentionExperiment (int participantNum, String outputDir, boolean realFeedback) throws Exception{
    this(participantNum, outputDir, realFeedback, true);
  }

  public AttentionExperiment (int participantNum, String outputDir, boolean realFeedback, boolean withEEG) throws Exception{
    super( new InetSocketAddress( WEB_SOCKETS_PORT ) );

    this.participantNum = participantNum;
    this.feedback = realFeedback;
    this.withEEG = withEEG;
    journal = new EEGJournal(outputDir, participantNum);
    if(withEEG){
      eeglog = new EEGLog();
      eeglog.tryConnect();
      System.out.println("Successfully connected to emotiv");
      eeglog.addUser();
      System.out.println("Successfully added user");
      logger = new EEGLoggingThread(eeglog, outputDir + "eegdata.csv");
    }

    timer = new Timer();
    epochArray = getEpochArray(participantNum);
    epochImageFiles = getEpochImages();
    // save the header
    journal.addMetaData(getExperimentHeader());

  }


@Override
public void onMessage( WebSocket conn, String message ) {
    // when we get a message
    if(DEBUG) System.out.println("Message from client " + message);
    // Got a response (it's elapsed time)
    if(message.charAt(0) == 'R' && (responseTime == null ) ){
      timeOfResponse = System.currentTimeMillis();
      responseTime = Long.parseLong(message.substring(2));
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



/*---- Websocket Server Stuff ----*/


@Override
public void onError(WebSocket conn, Exception e){
  System.out.println("Error with websocket connection: " +e);
  e.printStackTrace();
  return;
}

@Override
public void onClose( WebSocket conn, int code, String reason, boolean remote ){
  System.out.println(conn + " has disconnected");
  if(logger != null)  logger.close();
  return;
}

@Override
	public void onOpen( WebSocket conn, ClientHandshake handshake ) {
    System.out.println("Connected to " + conn);
    startExperiment();
  }

@Override
	public void onFragment( WebSocket conn, Framedata fragment ) {
		System.out.println( "received fragment: " + fragment );
}

/*--------*/





public void sendToAll( String text ) {
  if(DEBUG) System.out.println("Sending message " + text);
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
    dfa = new Dfa();
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
    LinkedList<Integer> list = new LinkedList<Integer>();
    for(int i = 0; i < length; i++){
      list.add(i);
    }
    Collections.shuffle(list);
    Queue<Integer> queue = list;
    return queue;
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
      // two images for every trial. This is hardcoded. need to fix
      String[] thisEpochImages = new String[TRIALS_PER_EPOCH*2];
      for(int j = 0; j < TRIALS_PER_EPOCH; j++){
        if(male.isEmpty()){
          male = newRandomInts(NUM_IMAGES_PER_CATEGORY);
        }
        if(female.isEmpty()){
          female = newRandomInts(NUM_IMAGES_PER_CATEGORY);
        }
        if(outdoor.isEmpty()){
          outdoor = newRandomInts(NUM_IMAGES_PER_CATEGORY);
        }
        if(indoor.isEmpty()){
          indoor = newRandomInts(NUM_IMAGES_PER_CATEGORY);
        }
        if(Math.random() > 0.5){
          thisEpochImages[j*2] = MALE_FACES + male.remove() + ".jpg";
        }
        else{
          thisEpochImages[j*2] = FEMALE_FACES + female.remove() + ".jpg";
        }
        if(Math.random() > 0.5){
          thisEpochImages[j*2+1] = OUTDOOR_PLACES + outdoor.remove() + ".jpg";
        }
        else{
          thisEpochImages[j*2+1] = INDOOR_PLACES + indoor.remove() + ".jpg";
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
      if(withEEG) logger.init();
    }

    public void doNext(boolean fromTimer){
      // can only get here not from timer if we're in instructs
      if(!fromTimer && !inInstructs) return;
      if(!inInstructs){
        if(timeOfResponse != null){
            journal.endTrial(stimOnset, timeOfResponse, responseTime);
        }
        else{
          journal.endTrial(stimOnset);
        }

        trialNum++;

        // random feedback data
        if(epochNum > TRAINING_EPOCHS)  thisRatio = Math.random();

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
          if(withEEG) logger.resume();
          sendToAll(getInstructionCommand(epochNum));
          inInstructs = true;
          if(timeOfResponse != null){
              journal.endTrial(stimOnset, timeOfResponse, responseTime);
          }
          else{
            journal.endTrial(stimOnset);
          }
          // Show the next instruction
          epochNum++;
          if(epochNum >= NUM_EPOCHS){
            sendToAll("I,Done! Good Job!");
            return;
          }
          journal.addEpoch(epochType(epochNum));
          trialNum = 0;
        }
      }
      // advance to start of trial
      else{
        inInstructs = false;
        journal.addTrial(thisRatio, epochImageFiles[epochNum][trialNum%2],
          epochImageFiles[epochNum][trialNum%2+1]);
        timeOfResponse = null;
        responseTime = null;
        stimOnset = System.currentTimeMillis();
        if(withEEG) logger.resume();
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
      if(epochNum%2 == 0){
        instruct += "face you see is ";
        instruct += (epochArray[0]) ? "female" : "male";
      }
      else{
        instruct += "place you see is ";
        instruct += (epochArray[0]) ? "indoors" : "outdoors";
      }

      return instruct;
    }

    public String getTrialImagesCommand(int epochNum, int trialNum, double ratio){
      if(epochImageFiles[epochNum][trialNum*2] == null){
        System.out.println("Got null image at epoch " + epochNum + " trial " + trialNum);
        return null;
      }
      return "S," + epochImageFiles[epochNum][trialNum*2] + "," + epochImageFiles[epochNum][trialNum*2+1] +
      "," + ratio;
    }

  }

  public static void main(String[] args){
    if(args.length != 3){
      System.out.println("Usage: <ParticipantNum> <outputDir> <realFeedback (1,0)>");
      return;
    }

    int participantNum = Integer.parseInt(args[0]);
    String outputDir = args[1];
    boolean feedback = (Integer.parseInt(args[2]) == 1) ? true : false;

    try{
      // don't use a real eeg
      AttentionExperiment thisExperiment =
        new AttentionExperiment(participantNum, outputDir, feedback, false);
      thisExperiment.start();
    }
    catch(Exception e){
      System.out.println("Couldn't start experiment " + e);
      e.printStackTrace();
      return;
    }
  }

}
