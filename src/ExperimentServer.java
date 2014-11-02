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
	EEGLogging runningLogger;
  AttentionExperiment thisExperiment;
	Timer timer;
	String outputDir;
	int participant;
	int numPic = 1;
	Queue<double[][]> dataQueue;
	int trialsInQueue = 0;
	String[] states = {"indoor", "outdoor", "male_neut", "female_neut"};
	int TRIAL_TIME = 20*1000; //20 seconds.


	class EEGLogging implements Runnable {

		private Thread t;
		private volatile boolean doAcquire = true;
		private volatile boolean endAcquire = false;

		public void run() {
			try {
				if(endAcquire){
					System.out.println("Thread exiting.");
					return;
				}
				while(!doAcquire)	wait();

				System.out.println("Acquiring data continually");
				double[][] thisData =log.getEEG();
				if(thisData != null){
					dataQueue.add(thisData);
					if(thisData[0] != null)	trialsInQueue += thisData[0].length;
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
				t.start ();
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

	public ExperimentServer( int port , String dirName, int participant) throws UnknownHostException {
		super( new InetSocketAddress( port ) );
		log = new EEGLog();
		dataQueue = new LinkedList<double[][]>();
		timer = new Timer();
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

	@Override
	public void onMessage( WebSocket conn, String message ) {
		if(message.equals("initExperiment")){
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
				return;
			}

			this.sendToAll("connSuccess");
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
				runningLogger = new EEGLogging();
				runningLogger.start();
			}
			else{
				// pause logger, save data, reset queue and count...
				runningLogger.pause();
				thisExperiment.addTrials(dataQueue, trialsInQueue);
				dataQueue.clear();
				trialsInQueue = 0;
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
			System.out.println("Message did not messed known messages");
		}
		System.out.println( conn + ": " + message );
	}

	@Override
	public void onFragment( WebSocket conn, Framedata fragment ) {
		System.out.println( "received fragment: " + fragment );
	}

	public static void main( String[] args ) throws InterruptedException , IOException {
		if(args.length != 2){
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
