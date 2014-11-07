

public AttentionExperiment implements Experiment{

  // see what megan does here...
  static int NUM_EPOCHS = 10;
  static int TRAINING_TRIALS = 5;
  // How to do this ...
  static HashSet<String, String> commands = new HashSet{
    ""
  }

  Timer timer;
  EEGJournal journal;
  EEGLog eeglog;
  EEGLoggingThread logger;

  int participantNum;
  String[] epochArray;


  public AttentionExperiment throws Exception(int participantNum, String outputDir){
    this.participantNum = participantNum;
    journal = new EEGJournal(outputDir, participantNum);
    eeglog = new EEGLog();
    eeglog.tryConnect();
    System.out.println("Successfully connected to emotiv");
    eeglog.addUser();
    System.out.println("Successfully added user");
    logger = new EEGLoggingThread(journal, eeglog);
    timer = new Timer();
    epochArray = getEpochArray(participantNum);
  }


  /* Get the port of the browser websocket */
  public int getWebsocketsPort(){
    return 8887;
  }

  /* When message is recieved from the websocket... */
  public void onMessage(String message){

    // Got a response (it's elapsed time)
    if(message[0] == 'R'){
      long thisTime = GET CURRENT TIME
      // whatever time javascript uses
      elapsedTime = the last part of the message array

    }

    if(message.equals("init")){
      // Do initialization routine
    }
  }

  /* Initialization method to be sent to browser */
  public String initMessage();

  /* Set up any scheduled processes */
  public void startExperiment();


  /* Returns one of four possible arrangements of clickFemale, clickIndoor,
  etc determined by participant Num */
  private String[] getEpochArray(int participantNum){
    String[] twoTasks;
    switch(participantNum % 4){
      case(1):
        return ["clickFemale", "clickIndoor"];
      case(2):
        return ["clickFemale", "clickOutdoor"];
      case(3):
        return ["clickMale", "clickIndoor"];
      case(4):
        return ["clickMale", "clickOutdoor"];
    }


  }
}
