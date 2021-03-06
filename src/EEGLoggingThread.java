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

  DataOutputStream outToServer;
  DataInputStream inFromServer;
  private PrintWriter writer;
  private String fileName;


  private final Semaphore resumed = new Semaphore(1);
  private final Semaphore readyQuit = new Semaphore(1);

  private int iters = 0;

  public EEGLoggingThread(EEGLog log, String outputDir, int participantNum, boolean withTcp) throws Exception{
    resumed.acquire();
    SimpleDateFormat ft = new SimpleDateFormat("yyyy.MM.dd.hh.mm");
    fileName = outputDir + "/eeg_" + participantNum + "_" + ft.format(new Date());
    System.out.println("In logging thread, beginning data acquisition to file " + fileName);
    this.log = log;
    this.withTcp = withTcp;
    if(withTcp){
      System.out.println("Opened EEG data server on port " + PUBLISH_PORT);
      Socket clientSocket = new Socket("localhost", PUBLISH_PORT);
      outToServer = new DataOutputStream(clientSocket.getOutputStream());
    }
    File file = new File(fileName);
    writer = new PrintWriter(file);

    // Start thread (but do not start collection. Waiting on resume semaphore)
    t = new Thread (this, "logger");
    t.start();
  }

  public String getFilename(){
      return fileName;
  }

  public void run() {
    System.out.println("In logging thread " + Thread.currentThread().getId());
    try{
      resumed.acquire();
      if(!readyQuit.tryAcquire()){
        System.out.println("Could not begin logging... is device connected?");
        return;
      }
      System.out.println("Acquisition beginning");
    }
    catch(InterruptedException e){
      System.out.println("Interrupted in run");
    }

    while(true){
      if(doQuit){
        System.out.println("Releasing readyquit semaphore");
        readyQuit.release();
        return;
      }

      try {
        //thisData[channel][datapoints in time]
        double[][] thisData = log.getEEG();
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
    iters++;
    if(iters % 100 == 0){
      writer.flush();
    }
    long now;
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
          for(int channel = 0; channel < data.length; channel++){
            outToServer.writeChars(data[channel][datum] + ",");
          }
          outToServer.writeChar('\n');
        }
        catch(Exception e){
          System.out.println("Couldn't send data to server: " + e);
        }
      }

    }

  }


  public void close(){
    writer.flush();
    if(doQuit){
      System.out.println("Two functions called doQuit on eeglogging thread");
    }
    doQuit = true;
    try{
      System.out.println("Acquisition thread is alive? " + t.isAlive());
      //System.out.println("Waiting to acquire ready quit, with que len:" + readyQuit.getQueueLength());
      readyQuit.tryAcquire();
      if(readyQuit.tryAcquire()){
        System.out.println("Acquired semaphore");
      }
      else{
        System.out.println("Could not acquire semaphore, trying again. Doquit is " + doQuit);
        readyQuit.acquire();
        System.out.println("Acquired");
      }

    }
    catch(InterruptedException e){
      System.out.println("Got interrupted " + e);
    }
    System.out.println("In eeg logging closing writer");
    writer.close();
    readyQuit.release();
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
      logger = new EEGLoggingThread(emotiv, outdir, 10, false);
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
