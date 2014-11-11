/*
  A class that implements the experiment interfaces can plug into the
  EEG Logging/HTML-stimulus-display-and-response-collection, controlling
  what is displayed as well as how the data is processed
*/

public interface Experiment{

  /* When message is recieved from the websocket... */
  public void onMessage(String message);

  /* Get the port of the browser websocket */
  public int getWebsocketsPort();

  /* Initialization method to be sent to browser */
  public String initMessage();

  /* Set up any scheduled processes */
  public void startExperiment(WebSocket conn);
}

/* Browser must implement a websockets interface */
