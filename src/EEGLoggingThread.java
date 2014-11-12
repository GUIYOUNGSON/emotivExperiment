import java.util.*;

class EEGLoggingThread implements Runnable {

  private Thread t;
  private EEGJournal journal;
  private EEGLog log;
  private volatile boolean doAcquire = false;
  private volatile boolean endAcquire = false;
  private int NUM_CHANNELS = 14;

  public EEGLoggingThread(EEGJournal journal, EEGLog log){
    this.journal = journal;
    this.log = log;
  }

  public void run() {
    try {
      if(endAcquire){
        System.out.println("Thread exiting.");
        return;
      }
      while(!doAcquire)	wait();

      System.out.println("Acquiring data continually");
      double[][] thisData = log.getEEG();

      // If data was collected, add it to the channelData array.
      if(thisData != null){
        // should replace "3" with a timestamp
        journal.addData(thisData, 3);
      }
    } catch (Exception e) {
      System.out.println("Exception in EEGLogging thread: " + e);
      e.printStackTrace(System.out);
      return;

    }

  }

  public void start ()
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
    ifthis.doAcquire = true;
  }

}
