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

  static String eegOutputFileName = "eegdata.csv";
  // see what megan does here...
  static boolean DEBUG = true;
  static int TRAINING_EPOCHS = 60;
  static int FEEDBACK_EPOCHS = 0;
  static int TRIALS_PER_EPOCH = 50;
  static int NUM_EPOCHS = TRAINING_EPOCHS + FEEDBACK_EPOCHS;
  static int NUM_IMAGES_PER_CATEGORY = 70;
  static int WEB_SOCKETS_PORT = 8885;
  /* in millis, how long is each trial? i.e. how long do they get to respond? */
  static int RESPONSE_TIME = 100*10;
  static double TEASE_RATIO = .1;
  /* image directories. please name them 1,2,3,...n.jpg */
  static String MALE_FACES ="./imagesRenamed/male_neut/";
  static String FEMALE_FACES ="./imagesRenamed/female_neut/";
  static String OUTDOOR_PLACES ="./imagesRenamed/outdoor/";
  static String INDOOR_PLACES ="./imagesRenamed/indoor/";
  static boolean FACES = true;
  static boolean PLACES = false;
  volatile boolean done = false;
  boolean feedback;
  boolean withEEG;
  boolean tcpPublish;
  Timer timer;
  EEGJournal journal;
  EEGLog eeglog;
  EEGLoggingThread logger;

  int participantNum;
  boolean[] epochArray;
  String[][] epochImageFiles;
  boolean[][] answers;
  boolean[] epochType;
  ExperimentServer thisServer;
  private boolean hasStarted;


  int currentEpoch;
  int currentTrial;
  Long timeOfResponse;
  Long responseTime;
  Long stimOnset;
  double thisRatio;

  Dfa dfa;

  public AttentionExperiment (int participantNum, String outputDir, boolean realFeedback, boolean withEEG, boolean tcpPublish) throws Exception{
    super( new InetSocketAddress( WEB_SOCKETS_PORT ) );

    System.out.println("Starting new GUI web socket on port " + WEB_SOCKETS_PORT);
    this.participantNum = participantNum;
    this.feedback = realFeedback;
    this.withEEG = withEEG;
    this.tcpPublish = tcpPublish;
    hasStarted = false;
    done = false;
    /* If we're using the emotiv, star the emotiv logging */
    if(withEEG){
      eeglog = new EEGLog();
      eeglog.tryConnect();
      System.out.println("Successfully connected to emotiv");
      eeglog.addUser();
      System.out.println("Successfully added user. Starting logging thread");
      logger = new EEGLoggingThread(eeglog, outputDir + "/eeg", participantNum, tcpPublish);
    }

    // Start timer to schedule recurring trials
    timer = new Timer();
    // Generate a unique epoch array (random) for this participant
    // If epochArray[0] is true, click yes for male faces. If epochArray[1]
    // is true, click yes for outdoor places.
    epochArray = getEpochArray(participantNum);
    // generate the list of image files corresponding to this epoch array
    epochImageFiles = getEpochImages(TEASE_RATIO);

    // Create journal, struct for storing participant responses, times, etc
    journal = new EEGJournal(outputDir + "/log", participantNum, getExperimentHeader(), RESPONSE_TIME);

    currentEpoch = 0;
    currentTrial = 0;

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
      done = true;
      System.out.println("Quitting ... goodbye!");
      if(journal != null) journal.close();
      System.out.println("Closed journal");
      if(logger != null)  logger.close();
      System.out.println("Closed logger");
      System.exit(0);
    }
}

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
  done = true;
  journal.close();
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

  /* Returns a vector, [clickFemale, clickIndoor],
  where clickFemale is true if array[0] = true */
  private boolean[] getEpochArray(int participantNum){

    // two tasks array determines which stimulu (male, outdoors, ex)
    // this participant should click for

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

    // trial order is chosen such that the average time of each task is the
    // same: AABBAABBA
    epochType = new boolean[NUM_EPOCHS];
    boolean startCategory = Math.random() > 0.5 ? FACES : PLACES;
    int i = 0;
    while(i < NUM_EPOCHS){
      epochType[i++] = startCategory;
      if(NUM_EPOCHS > i)  epochType[i++] = !startCategory;
      if(NUM_EPOCHS > i)  epochType[i++] = !startCategory;
      if(NUM_EPOCHS > i)  epochType[i++] = startCategory;
    }
    System.out.println("Start category is " + (startCategory == FACES? "faces" : "places" ));
    return twoTasks;
  }


  /* Returns an array filenames specifying images to be shown in each trial.
     Tease ratio is the ratio of images that come from the lure category */

  private String[][] getEpochImages(double tease_ratio){
    int num_lures = (int) Math.ceil(tease_ratio * TRIALS_PER_EPOCH);
    boolean trial_type;
    String[][] epochImages = new String[NUM_EPOCHS][];
    answers = new boolean[NUM_EPOCHS][TRIALS_PER_EPOCH];
    int rand_idx;

    // Create lures
    for(int epoch = 0; epoch < NUM_EPOCHS; epoch++){

      String[] thisEpochFaces = new String[TRIALS_PER_EPOCH];
      String[] thisEpochPlaces = new String[TRIALS_PER_EPOCH];

      for(int i = 0; i < num_lures; i++){

        // faces
        rand_idx = (int) Math.floor(Math.random() * NUM_IMAGES_PER_CATEGORY);
        if(epochArray[0]){
          // Get a random index for this image
          thisEpochFaces[i] = MALE_FACES + rand_idx + ".jpg";
        }
        else{
          thisEpochFaces[i] = FEMALE_FACES + rand_idx + ".jpg";
        }

        // places
        rand_idx = (int) Math.floor(Math.random() * NUM_IMAGES_PER_CATEGORY);
        if(epochArray[1]){
          // Get a random index for this image
          thisEpochPlaces[i] = OUTDOOR_PLACES + rand_idx + ".jpg";
        }
        else{
          thisEpochPlaces[i] = INDOOR_PLACES + rand_idx + ".jpg";
        }
      }
      // Create non-lures
      for(int i = num_lures; i < TRIALS_PER_EPOCH; i++){

        rand_idx = (int) Math.floor(Math.random() * NUM_IMAGES_PER_CATEGORY);
        if(epochArray[0]){
          // Get a random index for this image
          thisEpochFaces[i] = FEMALE_FACES + rand_idx + ".jpg";
        }
        else{
          thisEpochFaces[i] = MALE_FACES + rand_idx + ".jpg";
        }

        rand_idx = (int) Math.floor(Math.random() * NUM_IMAGES_PER_CATEGORY);
        if(epochArray[1]){
          // Get a random index for this image
          thisEpochPlaces[i] = INDOOR_PLACES + rand_idx + ".jpg";
        }
        else{
          thisEpochPlaces[i] = OUTDOOR_PLACES + rand_idx + ".jpg";
        }
      }

      // Create a new array and merge the lures with the non-lures,
      // first shuffling them
      Collections.shuffle(Arrays.asList(thisEpochFaces));
      Collections.shuffle(Arrays.asList(thisEpochPlaces));

      epochImages[epoch] = new String[TRIALS_PER_EPOCH * 2];
      for(int j = 0; j < TRIALS_PER_EPOCH; j++){
        epochImages[epoch][j*2] = thisEpochFaces[j];
        epochImages[epoch][j*2+1] = thisEpochPlaces[j];

        if(epochType[epoch] == FACES){
          // trial has a face, the lure is a male face
          if(thisEpochFaces[j].startsWith(FEMALE_FACES) && epochArray[0]){
            // Answer is correct when the participant should click
            answers[epoch][j] = true;
          }
          else if(thisEpochFaces[j].startsWith(MALE_FACES) && !epochArray[0]){
            answers[epoch][j] = true;
          }
          else{
            answers[epoch][j] = false;
          }
        }
        else{
          // trial has an indoor place, the lure is an outdoor place
          if(thisEpochPlaces[j].startsWith(INDOOR_PLACES) && epochArray[1]){
            // Answer is correct when the participant should click
            answers[epoch][j] = true;
          }
          else if(thisEpochPlaces[j].startsWith(OUTDOOR_PLACES) && !epochArray[1]){
            answers[epoch][j] = true;
          }
          else{
            answers[epoch][j] = false;
          }
        }
      }

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
      System.out.println("Entering experiment start");

    }

    public synchronized void doNext(boolean fromTimer){
      if(!hasStarted){
        epochNum = 0;
        trialNum = 0;
        sendToAll(getInstructionCommand(epochNum));
        hasStarted = true;
        System.out.println("Adding first epoch to journal");
        journal.addEpoch(epochType(epochNum));
        inInstructs = true;
        thisRatio = 0.5;
        // initializes logger, does not start acquisition
        if(withEEG){
          System.out.println("Beginning to log EEG data!");
          logger.start();
        }
        return;
      }
      if(done){
        System.out.println("done");
        return;
      }
      // can only get here not from timer if we're in instructs
      if(!fromTimer && !inInstructs) return;
      if(!inInstructs){

        /* Log this response */
        boolean shouldClick = answers[epochNum][trialNum];
        if(timeOfResponse != null){
            journal.endTrial(stimOnset, timeOfResponse, responseTime, shouldClick);
        }
        else{
          journal.endTrial(stimOnset, !shouldClick);
        }

        /* Increment the number of this trial */
        trialNum++;

        /* Check to see if we're in the feedback phase, if so, edit ratio */
        if(epochNum >= TRAINING_EPOCHS){
          if(epochNum == TRAINING_EPOCHS &&  FEEDBACK_EPOCHS > 0){


          }
          //thisRatio = logger.getRecentClassify() ? 1 : 0;
        }

        if(trialNum < TRIALS_PER_EPOCH){
          journal.addTrial(thisRatio, epochImageFiles[epochNum][trialNum%2],
            epochImageFiles[epochNum][trialNum%2+1]);
          timeOfResponse = null;
          responseTime = null;
          // Use 50% ratio
          thisRatio = 0.5;
          sendToAll(getTrialImagesCommand(epochNum, trialNum, thisRatio));
          stimOnset = System.currentTimeMillis();
          timer.schedule(new doNextLater(), RESPONSE_TIME);
        }
        // Otherwise, go to next epoch
        else{

          epochNum++;
          // Show the next instruction
          inInstructs = true;
          if(epochNum >= NUM_EPOCHS){
            sendToAll("Done! Good Job!");
            sendToAll("I, All Done, nice work!");
            done = true;
            System.out.println("trying to close journal at " + System.currentTimeMillis());
            if(journal != null) journal.close();
            System.out.println("Trying to close logger at " + System.currentTimeMillis());
            if(logger != null)  logger.close();
            System.out.println("exiting at " + System.currentTimeMillis());
            System.exit(0);
          }
          else{
            sendToAll(getInstructionCommand(epochNum));
            sendToAll("T," + (epochType[epochNum] == FACES ? "Faces" : "Places"));
            journal.addEpoch(epochType(epochNum));
            trialNum = 0;
          }
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
        thisRatio = 0.5;
        sendToAll(getTrialImagesCommand(epochNum, trialNum, thisRatio));
        sendToAll("T," + (epochType[epochNum] == FACES ? "Faces" : "Places"));
        timer.schedule(new doNextLater(), RESPONSE_TIME);
      }
    }

    private class doNextLater extends TimerTask{

      public void run(){
        doNext(true);
      }

    }

    public String epochType(int epochNum){
      return epochType[epochNum] == PLACES ? "places" : "faces";
    }

    public String getInstructionCommand(int epochNum){
      String instruct;
      if(!hasStarted){
        return "I, Welcome! In each trial, you will be given a task: either to recognize the gender of faces or the location of places. " +
        "When you are asked to identify faces, click any key when you see a " + (epochArray[0] ? "female" : "male") + " face, but do not" +
        " click at all when a " + (!epochArray[0] ? "female" : "male") + " face is shown.  Similarly, click any key when a shown location is " +
        ((epochArray[1]) ? "indoors" : "outdoors") + ", but do not click when the location is " + ((!epochArray[1]) ? "indoors" : "outdoors") +
        ". When the word 'faces' is shown on the screen before a task, you should complete the face identification task as described above, and ignore the " +
        "shown places. Similary, when the word 'places' is shown, you should complete the places task and ignore any faces.  Good luck! Press any key to continue";
      }

      if(epochNum % 10 == 0){
        instruct = "I, You are on block " + epochNum + " of 40. Take a longer break, if you want! Next block is ";
        if(epochType[epochNum] == FACES){
          instruct += "FACES";
        }
        else{
          instruct += "PLACES";
        }

        return instruct;
      }
      if(epochType[epochNum] == FACES){
        instruct = "I, FACES";
      }
      else{
        instruct = "I, PLACES";
      }
      return instruct;
    }

    public String getTrialImagesCommand(int epochNum, int trialNum, double ratio){
      return "S," + epochImageFiles[epochNum][trialNum*2] + "," + epochImageFiles[epochNum][trialNum*2+1] +
      "," + ratio;
    }

  }

  public static void main(String[] args){
    if(args.length < 4){
      System.out.println("Usage: <ParticipantNum> <outputDir> <realFeedback (1,0)> <tcpPublish (1,0)>");
      return;
    }

    int participantNum = Integer.parseInt(args[0]);
    String outputDir = args[1];
    boolean feedback = (Integer.parseInt(args[2]) == 1) ? true : false;
    boolean tcpPublish = (Integer.parseInt(args[3]) == 1) ? true : false;

    try{
      AttentionExperiment thisExperiment =
        new AttentionExperiment(participantNum, outputDir, feedback, true, false);
      thisExperiment.start();
    }
    catch(Exception e){
      System.out.println("Couldn't start experiment " + e);
      e.printStackTrace();
      return;
    }
  }

}
