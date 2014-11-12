import java.util.*;
import java.io.*;
import java.net.*;
import java.util.*;

class EEGLoggingThread implements Runnable {

  private Thread t;
  private EEGLog log;
  private volatile boolean doAcquire = false;
  private volatile boolean endAcquire = false;
  private int NUM_CHANNELS = 25;//14;
  private int PUBLISH_PORT = 6789;
  // last channel is timestamp
  ArrayList<ArrayList<Double>> data;
  ArrayList<Long> timestamps;
  DataOutputStream outToServer;
  private PrintWriter writer;

  public EEGLoggingThread(EEGLog log) throws Exception{
    this.log = log;
    data = new ArrayList<ArrayList<Double>> ();
    for(int i = 0; i < NUM_CHANNELS; i++){
      data.add(i, new ArrayList<Double>());
    }
    timestamps = new ArrayList<Long>();
    Socket clientSocket = new Socket("localhost", PUBLISH_PORT);
    outToServer = new DataOutputStream(clientSocket.getOutputStream());
  }

  public EEGLoggingThread(EEGLog log, String fileName) throws Exception{
    this.log = log;
    data = new ArrayList<ArrayList<Double>> ();
    for(int i = 0; i < NUM_CHANNELS; i++){
      data.add(i, new ArrayList<Double>());
    }
    timestamps = new ArrayList<Long>();
    Socket clientSocket = new Socket("localhost", PUBLISH_PORT);
    outToServer = new DataOutputStream(clientSocket.getOutputStream());
    writer = new PrintWriter(fileName);
  }



  public void run() {
    while(true){
      try {
        if(endAcquire){
          System.out.println("Thread exiting.");
          return;
        }
        while(!doAcquire)	wait();

        double[][] thisData = log.getEEG();
        long timestamp = System.currentTimeMillis();
        String outString = "";
        for(int i = 0; i < thisData.length; i++){
          for(Double datum : thisData[i]){
            data.get(i).add(datum);
          }
        }
        for(int j = 0; j < thisData[0].length; j++){
          timestamps.add(timestamp);
        }


        onData(thisData);

      } catch (Exception e) {
        System.out.println("Exception in EEGLogging thread: " + e);
        e.printStackTrace(System.out);
        return;

      }
    }

  }

  public void onData(double[][] data){
    System.out.println("Sending data to server");
    for(int datum = 0; datum < data[0].length; datum++){
      String outString = "";
      for(int channel = 0; channel < data.length; channel++){
        try{
          outToServer.writeDouble(data[channel][datum]);
        }
        catch(Exception e){
          System.out.println("Couldn't send data to server: " + e);
        }

        outString += data[channel][datum] + ",";
      }
      outString = outString.substring(0, (outString.length() - 1)) + '\n';
      try{
        if(writer != null){
            writer.println(outString);
        }
      }
      catch(Exception e){
        System.out.println("Exception with writing to server: " + e);
      }

    }

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
  public void pause(){
    this.doAcquire = false;
  }

  public void close(){
    this.endAcquire = true;
  }

  public void resume(){
    this.doAcquire = true;
  }

  public static void main(String[] args){
    EEGLoggingThread thisLog;
    try{
      EEGLog log = new EEGLog();
      log.tryConnect();
      log.addUser();
      thisLog = new EEGLoggingThread(log,
        "/Users/Dale/Dropbox/code/neuromancer/testdata/eegdata.csv");
    }
    catch(Exception e){
      System.out.println(e);
      e.printStackTrace();
      return;
    }


    thisLog.init();
    thisLog.resume();
  }

}
