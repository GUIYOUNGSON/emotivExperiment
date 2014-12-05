import java.util.*;
import java.util.concurrent.locks.*;
import java.io.*;
import java.net.*;
import java.text.*;

class EEGLoggingThread implements Runnable {

  private Thread t;
  private EEGLog log;
  private boolean withTcp;
  private volatile boolean doAcquire = false;
  private volatile boolean endAcquire = false;
  private int NUM_CHANNELS = 25;//14;
  private int PUBLISH_PORT = 6789;
  // last channel is timestamp
  ArrayList<ArrayList<Double>> data;
  DataOutputStream outToServer;
  DataInputStream inFromServer;
  private PrintWriter writer;


  final Lock lock = new ReentrantLock();
  final Condition resumed = lock.newCondition();

  public EEGLoggingThread(EEGLog log, String outputDir, int participantNum, boolean withTcp) throws Exception{
    if(withTcp){
      System.out.println("TCP is not yet supported");
      withTcp = false;
    }
    SimpleDateFormat ft = new SimpleDateFormat("yyyy.MM.dd.hh.mm");
    String fileName = outputDir + "/eeg_" + participantNum + "_" + ft.format(new Date());
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
  }



  public void run() {
    while(true){
      lock.lock();
      try {
        if(endAcquire){
          System.out.println("Thread exiting.");
          return;
        }
        if(!doAcquire){
          resumed.wait();
        }
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

      } catch (Exception e) {
        System.out.println("Exception in EEGLogging thread: " + e);
        e.printStackTrace(System.out);
        return;
      }
      finally{
        lock.unlock();
      }

    }

  }

  public void onData(double[][] data){
    for(int datum = 0; datum < data[0].length; datum++){
      String outString = "";
      for(int channel = 0; channel < data.length; channel++){
        if(withTcp){
          try{
            outToServer.writeDouble(data[channel][datum]);
          }
          catch(Exception e){
            System.out.println("Couldn't send data to server: " + e);
          }
        }

        outString += data[channel][datum] + ",";
      }
      outString = outString + System.currentTimeMillis() + '\n';
      try{
        if(writer != null){
            writer.println(outString);
        }
      }
      catch(Exception e){
        System.out.println("Exception with writing to file: " + e);
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
    lock.lock();
    this.doAcquire = false;
    lock.unlock();
  }

  public void close(){
    writer.close();
    lock.lock();
    this.endAcquire = true;
    lock.unlock();
  }

  public void resume(){
    lock.lock();
    this.doAcquire = true;
    resumed.signal();
    lock.unlock();
  }


/* for testing */
  public static void main(String[] args){
    /*
    EEGLoggingThread thisLog;
    try{
      EEGLog log = new EEGLog();
      log.tryConnect();
      log.addUser();
      thisLog = new EEGLoggingThread(log,
        "/Users/Dale/Dropbox/code/neuromancer/testdata/eegdata.csv", true);
    }
    catch(Exception e){
      System.out.println(e);
      e.printStackTrace();
      return;
    }


    thisLog.init();
    thisLog.resume();
    */
    return;
  }

}
