import java.util.*;
import java.util.concurrent.Semaphore;
import java.io.*;
import java.net.*;
import java.text.*;

class EEGLoggingThread implements Runnable {

  private Thread t;
  private EEGLog log;
  private boolean withTcp;
  private volatile boolean doAcquire = false;
  private volatile boolean doQuit = false;
  private int NUM_CHANNELS = 25;//14;
  private int PUBLISH_PORT = 6789;
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
    if(withTcp){
      //System.out.println("TCP is not yet supported");
      withTcp = true;
    }
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
  }

  public String getFilename(){
      return fileName;
  }

  public void run() {
    try{
      readyQuit.acquire();
      resumed.acquire();
    }
    catch(InterruptedException e){
      // do nothing
    }

    while(true){
      if(doQuit){
        System.out.println("Signaling finished collection");
        readyQuit.release();
        System.out.println("About to return from infinite loop");
        return;
      }
      System.out.println("Acquired lock");
      try {
        //thisData[channel][datapoints in time]
        double[][] thisData = log.getEEG();
        long timestamp = System.currentTimeMillis();
        String outString = "";
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

  public void onData(double[][] data) throws Exception{
    for(int datum = 0; datum < data[0].length; datum++){
      String outString = "";
      for(int channel = 0; channel < data.length; channel++){

        outString += data[channel][datum] + ",";
      }
      outString = outString + System.currentTimeMillis() + '\n';
      finalData += outString;
      try{
        if(writer != null){
            writer.println(outString);
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

  public void beginClassify(PythonCommander python){
    this.python = python;
    this.classify = true;
  }

  public boolean getRecentClassify(){
    return recentClassify;
  }

  public void init()
  {
    System.out.println("Beginning data acquisition in thread start");
    if (t == null)
    {
      t = new Thread (this, "logger");
      t.start();
    }
  }

  public void close(){
    doQuit = true;
    try{
      System.out.println("Awaiting signal in close");
      readyQuit.acquire();
    }
    catch(InterruptedException e){
      System.out.println("Got interrupted " + e);
    }
    writer.close();
    System.out.println("Returning from close");
    return;
  }

  public void start(){
    resumed.release();
  }



}
