import java.util.*;
import java.util.concurrent.Semaphore;
import java.io.*;
import java.net.*;
import java.text.*;

class EEGLoggingThread implements Runnable {

  private Thread t;
  // EEG log controls the emotiv headset
  private EEGLog log;
  // Send data to tcp server?
  private boolean withTcp;
  private volatile boolean doAcquire = false;
  private volatile boolean doQuit = false;

  private int NUM_CHANNELS = 25;//14;
  private int PUBLISH_PORT = 6789;
  private static int pointPeriod = 60*1000/128;

  // last channel is timestamp
  ArrayList<ArrayList<Double>> data;
  DataOutputStream outToServer;
  DataInputStream inFromServer;
  private PrintWriter writer;
  private String fileName;

  PythonCommander python;
  private boolean classify;
  private boolean recentClassify = false;
  private int BUFFER_SIZE = 10;
  private int currentBuff = 0;
  private String finalData;

  private final Semaphore resumed = new Semaphore(1);
  private final Semaphore readyQuit = new Semaphore(1);

  public EEGLoggingThread(EEGLog log, String outputDir, int participantNum, boolean withTcp) throws Exception{
    resumed.acquire();
    SimpleDateFormat ft = new SimpleDateFormat("yyyy.MM.dd.hh.mm");
    fileName = outputDir + "/eeg_" + participantNum + "_" + ft.format(new Date());
    System.out.println("In logging thread, beginning data acquisition to file " + fileName);
    this.log = log;
    this.withTcp = withTcp;
    data = new ArrayList<ArrayList<Double>> ();
    for(int i = 0; i < NUM_CHANNELS; i++){
      data.add(i, new ArrayList<Double>());
    }
    if(withTcp){
      System.out.println("Opened EEG data server on port " + PUBLISH_PORT);
      Socket clientSocket = new Socket("localhost", PUBLISH_PORT);
      outToServer = new DataOutputStream(clientSocket.getOutputStream());
    }
    File file = new File(fileName);
    writer = new PrintWriter(file);
    this.classify = false;

    // Start thread (but do not start collection. Waiting on resume semaphore)
    t = new Thread (this, "logger");
    t.start();
  }

  public String getFilename(){
      return fileName;
  }

  public void run() {
    System.out.println("In logging thread");
    try{
      readyQuit.acquire();
      resumed.acquire();
    }
    catch(InterruptedException e){
      // do nothing
    }

    while(true){
      if(doQuit){
        readyQuit.release();
        return;
      }
      try {
        //thisData[channel][datapoints in time]
        double[][] thisData = log.getEEG();
        for(int i = 0; i < thisData.length; i++){
          for(Double datum : thisData[i]){
            data.get(i).add(datum);
          }
        }
        onData(thisData);
      }
      catch (Exception e) {
        System.out.println("Exception in EEGLogging thread: " + e);
        e.printStackTrace(System.out);
        return;
      }
    }

  }

  /* When we get data, do this. Currently, save data to file and write to server
  if TCP is enabled */

  public void onData(double[][] data) throws Exception{
    long timeStamp = System.currentTimeMillis();
    long now;
    String outString = "dale";
    int num_points = data[0].length;
    for(int datum = 0; datum < num_points; datum++){
      try{
        if(writer != null){
          for(int channel = 0; channel < data.length; channel++){
            writer.print(data[channel][datum] + ",");
          }
            // space the points out given the priod (128 samples per second)
            writer.println(timeStamp + (num_points - 1 - datum) * pointPeriod);
        }

      }
      catch(Exception e){
        System.out.println("Exception with writing to file: " + e);
      }
      if(withTcp){
        try{
          outToServer.writeChars(outString);
        }
        catch(Exception e){
          System.out.println("Couldn't send data to server: " + e);
        }
      }

    }

    if(classify){
      currentBuff++;
      if(currentBuff != BUFFER_SIZE) return;

      try{
        recentClassify = python.classify(finalData);
        currentBuff = 0;
        finalData = "";
      }
      catch(Exception e){
        throw e;
      }
    }
  }

  /* Begin classification with python */
  public void beginClassify(PythonCommander python){
    this.python = python;
    this.classify = true;
  }

  public boolean getRecentClassify(){
    return recentClassify;
  }


  public void close(){
    doQuit = true;
    try{
      System.out.println("Waiting to acquire ready quit");
      readyQuit.acquire();
    }
    catch(InterruptedException e){
      System.out.println("Got interrupted " + e);
    }
    writer.close();
    return;
  }

  public void start(){
    resumed.release();
  }

  /* Main thread for testing */
  public static void main(String[] args){

    final EEGLoggingThread logger;
    EEGLog emotiv;
    try{
      emotiv = new EEGLog();
      emotiv.tryConnect();
      System.out.println("Successfully connected to emotiv");
      emotiv.addUser();
      System.out.println("Successfully added user. Starting logging thread");
    }
    catch(Exception e){
      e.printStackTrace();
      return;
    }


    String outdir = "performance_tests";
    if(args.length < 1){
      System.out.println("Usage: num_seconds");
      return;
    }
    for(String in : args){
      System.out.println("Arg is " + in);
    }
    int num_secs = Integer.parseInt(args[0]);

    try{
      logger = new EEGLoggingThread(emotiv, outdir, 0, false);
    }
    catch(Exception e){
      e.printStackTrace();
      return;
    }

    logger.start();

    Timer t = new Timer();
    System.out.println("Beginning collection at time " + System.currentTimeMillis());
    t.schedule(new TimerTask(){
      public void run(){
        System.out.println("Closing logger");
        logger.close();
        System.out.println("Done at time " + System.currentTimeMillis());
        System.exit(0);
      }
    }, num_secs);
  }


}
