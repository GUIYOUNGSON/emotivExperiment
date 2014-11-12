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

  public void run() {
    while(true){
      try {
        if(endAcquire){
          System.out.println("Thread exiting.");
          return;
        }
        while(!doAcquire)	wait();

        System.out.println("Acquiring data continually");
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
        outString += data[channel][datum] + ",";
      }
      outString = outString.substring(0, (outString.length() - 1)) + '\n';
      try{
        System.out.println(outString);
        outToServer.writeBytes(outString);//(outString);
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
      thisLog = new EEGLoggingThread(log);
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
