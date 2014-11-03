import java.util.*;

class EEGLoggingThread implements Runnable {

  private Thread t;
  private List<Double>[] channelData;
  private EEGLog log;
  private volatile boolean doAcquire = true;
  private volatile boolean endAcquire = false;
  private int NUM_CHANNELS = 14;

  public EEGLoggingThread(List<Double>[] channelData, EEGLog log){
    this.channelData = channelData;
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
        for(int i = 0; i < NUM_CHANNELS; i++){
          double[] thisChannelData = thisData[i];
          for(double dataPoint : thisChannelData){
            channelData[i].add(dataPoint);
          }
        }

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
    this.doAcquire = true;
  }

}
