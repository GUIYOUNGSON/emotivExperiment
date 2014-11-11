import java.util.*;

public class AttentionExperiment implements Experiment{

  // see what megan does here...
  static int NUM_EPOCHS = 10;
  static int TRAINING_EPOCHS = 5;
  static int FEEDBACK_EPOCHS = 3;
  static int TRIALS_PER_EPOCH = 20;
  static int NUM_IMAGES_PER_CATEGORY = 70;
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


  public AttentionExperiment(int participantNum, String outputDir, boolean realFeedback) throws Exception{
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
    epochImagesFiles = getEpochImages();
    // save the header
    journal.addMetaData(getExperimentHeader());
  }


  /* Get the port of the browser websocket */
  public int getWebsocketsPort(){
    return 8887;
  }

  /* When message is recieved from the websocket... */
  public void onMessage(String message){

    // Got a response (it's elapsed time)
    if(message.charAt(0) == 'R'){
      long thisTime = System.currentTimeMillis();

      long elapsedTime = Long.parseLong(message.substring(2));

    }

    if(message.equals("init")){
      // Do initialization routine
    }
  }

  /* Initialization method to be sent to browser.  We don't need this because
  the client is going to do the initializing. */
  public String initMessage(){
    //do nothing
    return null;
  };

  /* Set up any scheduled processes */
  public void startExperiment(WebSocket conn){

    return;
  };


  private String getExperimentHeader(){
      String headerString = "/*********************************************/\n";
      headerString += "Participant Number: " + participantNum + "\n";
      headerString += "------Epochs-----\n\n";
      for(int i = 0; i < epochImageFiles; i++){
        headerString += "Epoch " + i + ": \n";
        for(String imageFile : epochImageFiles[i]){
          headerString += imageFile + "\n";
        }
        headerString+= "\n\n";
      }

      return headerString
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
    Queue<Integer> list = new ArrayList<Integer>();
    for(int i = 0; i < length; i++){
      list.add(i);
    }
    Collections.shuffle(list);
    return list;
  }

  private String[][] getEpochImages(){
    String[][] epochImages = new String[NUM_EPOCHS];
    // randomly choose faces and places
    Queue<Integer> male = new RandomInts(NUM_IMAGES_PER_CATEGORY);
    Queue<Integer> female = new RandomInts(NUM_IMAGES_PER_CATEGORY);
    Queue<Integer> outdoor = new RandomInts(NUM_IMAGES_PER_CATEGORY);
    Queue<Integer> indoor = new RandomInts(NUM_IMAGES_PER_CATEGORY);

    while(int i = 0; i < NUM_EPOCHS; i++){
      // two images for every trial
      thisEpochImages = new String[TRIALS_PER_EPOCH*2];

      // Do a faces epoch
      if(i%2){
        for(int j = 0; j < TRIALS_PER_EPOCH; j+= 2){
          if(male.isEmpty()){
            male = new RandomInts(NUM_IMAGES_PER_CATEGORY);
          }
          thisEpochImages[j] = MALE_FACES + String.parseString(male.remove()) + ".jpg";

          if(female.isEmpty()){
            female = new RandomInts(NUM_IMAGES_PER_CATEGORY);
          }
          thisEpochImages[j+1] = FEMALE_FACES + String.parseString(female.remove()) + ".jpg";
        }
      }
      else{
        for(int j = 0; j < TRIALS_PER_EPOCH; j+= 2){
          if(outdoor.isEmpty()){
            outdoor = new RandomInts(NUM_IMAGES_PER_CATEGORY);
          }
          thisEpochImages[j] = OUTDOOR_PLACES + String.parseString(outdoor.remove()) + ".jpg";

          if(indoor.isEmpty()){
            indoor = new RandomInts(NUM_IMAGES_PER_CATEGORY);
          }
          thisEpochImages[j+1] = INDOOR_PLACES + String.parseString(indoor.remove()) + ".jpg";
        }
      }

      epochImages[i] = thisEpochImages;

    }

    return epochImages;

  }

  public void sendMessage(String mess){
    conn.send(mess);
  }

  private class dfa(){
    private int epochNum;
    private int trialNum;
    private boolean inInstructs;

    public dfa(){
      epochNum = -1;
      // initially set this this way so that scheduleNext
      // schedules us to a new epoch
      trialNum = TRIALS_PER_EPOCH;
      inInstructs = true;
      scheduleNext();
    }

    public String nextEpoch(){
      String epochType;
      if(epochNum%2)  epochType = "faces,";
      else epochType = "places";

      if(epochNum >= TRAINING_EPOCHS){
        epochType += "feedback";
      }
      return epochType;
    }

    public String instruction(){
      String instruct = "Click when the ";
      if(epochNum%2){
        instruct += "face you see is ";
        instruct += (Outer.epochArray[0]) ? "female" : "male"
      }
      else{
        instruct += "place you see is ";
        instruct += (Outer.epochArray[0]) ? "indoors" : "outdoors"
      }
    }

    private void scheduleNext(){

      if(inInstructs){
        inInstructs = false;
        // schedule new epoch
      }
      else{
        logger.pause();
        journal.endTrial();
        if(trialNum == TRIALS_PER_EPOCH){
          // schedule a new epoch
          epochNum++;
          journal.addEpoch(nextEpoch());
          inInstructs = true;
          Outer.sendMessage("I:" + instruction());
        }
        else{
          // run next trial
          trialNum++;
          // get the right picture, ratio
          journal.addTrial()
        }
      }
      else{

        logger.pause();
        // record response times
        epochNum++;
        trialNum = 0;
        inInstructs = true;
        // schedule instructions
      }
    }

    private class SendMessageLater extends TimerTask{

      String message;

      public SendMessageLater(String message){
        this.message = message;
      }

      public void run(){
        Outer.Outer.this.sendMessage(mess);
        scheduleNext();
      }

    }
  }



}
