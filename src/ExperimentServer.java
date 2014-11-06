import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.util.*;


/**
* A simple WebSocketServer implementation. Keeps track of a "chatroom".
*/
public class ExperimentServer extends WebSocketServer {
	EEGLog log;
	EEGLoggingThread runningLogger;
  AttentionExperiment thisExperiment;
	String outputDir;
	int participant;
	int numPic = 1;
	String[] states = {"indoor", "outdoor", "male_neut", "female_neut"};
	int TRIAL_TIME = 20*1000; //20 seconds.
	int NUM_CHANNELS = 14;

	public ExperimentServer( int port , String dirName, int participant) throws UnknownHostException {
		super( new InetSocketAddress( port ) );
		log = new EEGLog();
		doTest(true);
	}

	public ExperimentServer( InetSocketAddress address ) {
		super( address );
	}

	@Override
	public void onOpen( WebSocket conn, ClientHandshake handshake ) {
		this.sendToAll( "new connection: " + handshake.getResourceDescriptor() );
		System.out.println( conn.getRemoteSocketAddress().getAddress().getHostAddress() + " entered the room!" );
	}

	@Override
	public void onClose( WebSocket conn, int code, String reason, boolean remote ) {
		this.sendToAll( conn + " has left the room!" );
		System.out.println( conn + " has left the room!" );
	}


	public void doTest(boolean remote){
		if(thisExperiment != null){
			System.out.println("Experiment already in progress! Cannot test");
			return;
		}
		try{
			thisExperiment = new AttentionExperiment("./testdir", 1);
			log.tryConnect();
			System.out.println("Successfully connected");

			log.addUser();
			System.out.println("Successfully added user");
		}
		catch(Exception e){
			System.out.println("Connection failed: " + e);
			e.printStackTrace(System.out);
			return;
		}

		System.out.println("Adding test epoch for faces");
		thisExperiment.addEpoch("femaleFaces");
		System.out.println("Running synchronous version of data acquisition" +
		"on 3 measurements per 3 trials, 1 epoch");
		try{
			for(int i = 0; i < 3; i++){
				thisExperiment.addTrial("face1", "place1", (float)0.5);
				for(int j = 0; j < 3; j++){
					thisExperiment.addData(log.getEEG(), System.currentTimeMillis());
				}
				thisExperiment.endTrial();
			}
			thisExperiment.addEpoch("maleFaces");
			for(int i = 0; i < 3; i++){
				thisExperiment.addTrial("face1", "place1", (float)0.5);
				for(int j = 0; j < 3; j++){
					thisExperiment.addData(log.getEEG(), System.currentTimeMillis());
				}
				thisExperiment.endTrial();
			}
		}
		catch(Exception e){
			System.out.println("An exception occured " + e);
			e.printStackTrace(System.out);
		}
		System.out.println("Done!: ");
		System.out.println(thisExperiment.dumpAllData());

		return;
	}

	public void initExperiment(){
		try{
			thisExperiment = new AttentionExperiment(outputDir, participant);
			log.tryConnect();
			System.out.println("Successfully connected to emotiv");
			log.addUser();
			System.out.println("Successfully added user");
		}
		catch(Exception e){
			System.out.println("Connection failed");
			this.sendToAll("connFail");
			e.printStackTrace(System.out);
			return;
		}

		this.sendToAll("connSuccess");
	}


	@Override
	public void onMessage( WebSocket conn, String message ) {
		if(message.equals("initExperiment")){
			initExperiment();
		}
		else if(message.equals("newEpoch")){
			try{
				thisExperiment.addEpoch("indoorClick");
			}
			catch(Exception e){
				System.out.println("Could not add epoch: " + e);
				e.printStackTrace(System.out);
				return;
			}
		}
		else if(message.equals("newTrial")){

			if(runningLogger == null){
				runningLogger = new EEGLoggingThread(thisExperiment, log);
				runningLogger.start();
			}
			else{
				// pause logger, save data, reset queue and count...
				runningLogger.pause();
				//thisExperiment.addTrialAndClear(channelData);
				runningLogger.resume();
				this.sendToAll("newTrial");
			}
		}
		else if(message.equals("pauseAcquire")){
			if(runningLogger != null)	runningLogger.pause();
		}
		else if(message.equals("endAcquire")){
			if(runningLogger != null)	runningLogger.close();
		}
		else{
			System.out.println("Message did not match known messages");
		}
		System.out.println( conn + ": " + message );
	}

	public static void main( String[] args ) throws InterruptedException , IOException {
		if(args.length < 2){
			System.out.println("Usage: <outputDir> <participantInt>");
			return;
		}
		int participant = Integer.parseInt(args[1]);
		WebSocketImpl.DEBUG = true;
		int port = 8887; // 843 flash policy port
		try {
			port = Integer.parseInt( args[ 0 ] );
		} catch ( Exception ex ) {
		}
		ExperimentServer s = new ExperimentServer( port , args[0], participant);
		s.start();
		System.out.println( "ExperimentServer started on port: " + s.getPort() );

		BufferedReader sysin = new BufferedReader( new InputStreamReader( System.in ) );
		while ( true ) {
			String in = sysin.readLine();
			s.sendToAll( in );
			if( in.equals( "exit" ) ) {
				s.stop();
				break;
			} else if( in.equals( "restart" ) ) {
				s.stop();
				s.start();
				break;
			}
		}
	}
	@Override
	public void onError( WebSocket conn, Exception ex ) {
		ex.printStackTrace();
		if( conn != null ) {
			// some errors like port binding failed may not be assignable to a specific websocket
		}
	}

	/**
	* Sends <var>text</var> to all currently connected WebSocket clients.
	*
	* @param text
	*            The String to send across the network.
	* @throws InterruptedException
	*             When socket related I/O errors occur.
	*/
	public void sendToAll( String text ) {
		Collection<WebSocket> con = connections();
		synchronized ( con ) {
			for( WebSocket c : con ) {
				c.send( text );
			}
		}
	}
}
