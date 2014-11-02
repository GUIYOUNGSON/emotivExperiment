import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import java.io.*;
import java.util.Arrays;

public class EEGLog {

	public Pointer eEvent;
	public Pointer eState;
	public IntByReference userID;
	public IntByReference nSamplesTaken;
	public Pointer hData;
	public short composerPort					= 1726;
	public int option 								= 2;
	public int state  								= 0;
	public float secs 								= 1;
	public boolean readytocollect 		= false;
	private boolean connected 				= false;
	private int user 									= -1;
	private int TIMEOUT 							= 100000;
	private boolean DEBUG 				= false;


	private static String[] channels = {"ED_COUNTER",
	                                     "ED_INTERPOLATED",
	                                     "ED_RAW_CQ",
	                                     "ED_AF",
	                                     "ED_F7",
	                                    "ED_F3",
	                                     "ED_FC5",
	                                     "ED_T7",
	                                     "ED_P7",
	                                     "ED_O1",
	                                     "ED_O2",
	                                     "ED_P8",
	                                     "ED_T8",
	                                     "ED_FC6",
	                                     "ED_F4",
	                                     "ED_F8",
	                                     "ED_AF4",
	                                     "ED_GYROX",
	                                     "ED_GYROY",
	                                     "ED_TIMESTAMP",
	                                     "ED_ES_TIMESTAMP",
	                                     "ED_FUNC_ID",
	                                     "ED_FUNC_VALUE",
	                                     "ED_MARKER",
	                                     "ED_SYNC_SIGNAL"};

	private static int DATA_OFFSET = 3;
	private static int NUM_CHANNELS = 14;
	public EEGLog(){
		eEvent = Edk.INSTANCE.EE_EmoEngineEventCreate();
		eState = Edk.INSTANCE.EE_EmoStateCreate();
		userID 			= new IntByReference(0);
		nSamplesTaken	= new IntByReference(0);
	}

	public static void main(String[] args){
		System.out.println(channels.length);
		int numSamples = 1;//Integer.parseInt(args[0]);

		EEGLog log = new EEGLog();
		try{
			if(args.length > 0 && args[0] == "--remote")	log.tryRemoteConnect(args[1], (short)Integer.parseInt(args[2]));
			else	log.tryConnect();
			log.addUser();
		}
		catch (Exception e)
		{
			System.out.println("An error occured: " + e);
			return;
		}

		try{
			double[][] theData = log.getEEG();
			int i = 0;
			for(double[] dataArray : theData){
				System.out.println("Field " + channels[i]);
				for(double dataPoint : dataArray){
					System.out.print(dataPoint);
				}
			}

			//theData = sliceChannelData(theData);
		//System.out.println("Number of columns (channels) is " + theData.length);
		//System.out.println("Number of data lines is " + theData[0].length);
			log.prettyPrint(theData, true);
		}
		catch(Exception e)
		{
			System.out.println("An error " + e);
		}
		/*
		PrintWriter writer;
		try{
			writer = new PrintWriter("/Users/Dale/Dropbox/code/EEGLog/scratch/test1", "UTF-8");
			System.out.println("Opened file for writing");
		}
		catch(Exception e){
			System.out.println("Error: " + e);
			return;
		}

		for(int i = 0; i < numSamples; i++){
			try{
				String strs = log.getEEGAsString();
				writer.println(strs);
				System.out.println(strs);
			}
			catch(Exception e){
				System.out.println("Could not write file, sorry: " + e);
			}
				writer.close();

		}
		*/



	}

	public double[][] getChannelData() throws Exception{
		return sliceChannelData(getEEG());
	}

	public static double[][] sliceChannelData(double[][] allData){
		return Arrays.copyOfRange(allData, DATA_OFFSET, NUM_CHANNELS + DATA_OFFSET - 1);
	}

	public int tryConnect() throws Exception{
		if (Edk.INSTANCE.EE_EngineConnect("Emotiv Systems-5") != EdkErrorCode.EDK_OK.ToInt()) {
			throw new Exception("Emotiv Engine start up failed.");
		}
		connected = true;
		hData = Edk.INSTANCE.EE_DataCreate();
		Edk.INSTANCE.EE_DataSetBufferSizeInSec(secs);
		System.out.println("Ready to collect data");
		return 1;
	}

	public int tryRemoteConnect(String ipAddr, short port) throws Exception{
		System.out.println("Target IP of EmoComposer: [" + ipAddr + "] ");

		if (Edk.INSTANCE.EE_EngineRemoteConnect(ipAddr, port, "Emotiv Systems-5") != EdkErrorCode.EDK_OK.ToInt()) {
			System.out.println("Cannot connect to EmoComposer on " + ipAddr + ":" + port);
			throw new Exception("Remote Emotiv Engine start up failed");
		}
		System.out.println("Connected to EmoComposer on [" + ipAddr + "]");
		return 1;
	}

	public void addUser() throws Exception{

		if(!connected)	throw new Exception("Not connected!");

		// Poll until a user-added event occurs
		for(int i = 0; i < TIMEOUT; i++){

			state = Edk.INSTANCE.EE_EngineGetNextEvent(eEvent);
			System.out.println("Got event code " + eEvent);
			if(state == EdkErrorCode.EDK_OK.ToInt()){
				int eventType = Edk.INSTANCE.EE_EmoEngineEventGetType(eEvent);
				Edk.INSTANCE.EE_EmoEngineEventGetUserId(eEvent, userID);
				if (eventType == Edk.EE_Event_t.EE_UserAdded.ToInt())
				{
					Edk.INSTANCE.EE_DataAcquisitionEnable(userID.getValue(),true);
					user = userID.getValue();
					System.out.println("Got a user");
					return;
				}
			}
			else if(state != EdkErrorCode.EDK_NO_EVENT.ToInt()) {
				throw new Exception("Internal error in Emotiv Engine: " + state);
			}
		}

		throw new Exception("Timed out waiting to add user");
	}


public String getEEGAsString() throws Exception{
	double[][] res = getEEG();
	String resultString = "";
	for(int trial = 0; trial < res[0].length; trial++){
		for(int j = 0; j < res.length; j++){
			resultString += res[j][trial];
			resultString += (j == res.length - 1) ? "" : ",";
		}
		resultString += (trial == res[0].length - 1) ? "" : "\n";
	}
	return resultString;
}

public void prettyPrint(double[][] data, boolean onlyData){
	int i = 0;
	int offset = onlyData ? DATA_OFFSET : 0;
	for(double[] channel : data){
		System.out.println("Data from channel " + channels[offset + i++] + " is: ");
		for(double trial : channel){
			System.out.print(trial + "  ");
		}
		System.out.println();
	}
}

public double[][] getEEG() throws Exception{
	if(!connected)		throw new Exception("Not connected!");

	if(user == -1)		throw new Exception("No user added!");

	for(int i = 0; i < TIMEOUT; i++){
		Edk.INSTANCE.EE_DataUpdateHandle(0, hData);
		Edk.INSTANCE.EE_DataGetNumberOfSample(hData, nSamplesTaken);

		if(nSamplesTaken != null && nSamplesTaken.getValue() != 0){

			double[][] data = new double[channels.length][nSamplesTaken.getValue()];
			int sampleIdx = nSamplesTaken.getValue();
			for (int j = 0 ; j < channels.length ; j++) {
				Edk.INSTANCE.EE_DataGet(hData, j, data[j], nSamplesTaken.getValue());
			}

			if(DEBUG){
				if(nSamplesTaken == null){
					System.out.println("No samples to collect");
				}
				else{
					System.out.println("Collected data");

				}
			}

			return data;
		}
	}
	// a timeout occured...
	throw new Exception("Timed out on acquiring EEG data");

}



    public String readData() //double[] getSomeData()
    {

		//double[] noData = {};
		String noData = ":(";

    	switch (option) {
		case 1:
		{
			if (Edk.INSTANCE.EE_EngineConnect("Emotiv Systems-5") != EdkErrorCode.EDK_OK.ToInt()) {
				System.out.println("Emotiv Engine start up failed.");
				return noData;
			}
			break;
		}
		case 2:
		{
			System.out.println("Target IP of EmoComposer: [127.0.0.1] ");

			if (Edk.INSTANCE.EE_EngineRemoteConnect("127.0.0.1", composerPort, "Emotiv Systems-5") != EdkErrorCode.EDK_OK.ToInt()) {
				System.out.println("Cannot connect to EmoComposer on [127.0.0.1]");
				return noData;
			}
			System.out.println("Connected to EmoComposer on [127.0.0.1]");
			break;
		}
		default:
			System.out.println("Invalid option...");
			return noData;
    	}
    	Edk.INSTANCE.EE_DataSetBufferSizeInSec(secs);
		System.out.print("Buffer size in secs: ");
		System.out.println(this.secs);

    	System.out.println("Start receiving EEG Data!");

		while (true)
		{
			state = Edk.INSTANCE.EE_EngineGetNextEvent(eEvent);

			// New event needs to be handled
			if (state == EdkErrorCode.EDK_OK.ToInt())
			{
				int eventType = Edk.INSTANCE.EE_EmoEngineEventGetType(eEvent);
				Edk.INSTANCE.EE_EmoEngineEventGetUserId(eEvent, userID);

				// Log the EmoState if it has been updated
				if (eventType == Edk.EE_Event_t.EE_UserAdded.ToInt())
				if (userID != null)
					{
						System.out.println("User added");
						Edk.INSTANCE.EE_DataAcquisitionEnable(userID.getValue(),true);
						readytocollect = true;
					}
			}
			else if (state != EdkErrorCode.EDK_NO_EVENT.ToInt()) {
				System.out.println("Internal error in Emotiv Engine!");
				break;
			}

			if (readytocollect)
			{
				Edk.INSTANCE.EE_DataUpdateHandle(0, hData);

				Edk.INSTANCE.EE_DataGetNumberOfSample(hData, nSamplesTaken);

				if (nSamplesTaken != null)
				{
					if (nSamplesTaken.getValue() != 0) {

						System.out.print("Updated: ");
						System.out.println(nSamplesTaken.getValue());

						String result = "";

						double[] data = new double[nSamplesTaken.getValue()];
						for (int sampleIdx=0 ; sampleIdx<nSamplesTaken.getValue() ; ++ sampleIdx) {
							for (int i = 0 ; i < channels.length ; i++) {

								Edk.INSTANCE.EE_DataGet(hData, i, data, nSamplesTaken.getValue());
								System.out.print(data[sampleIdx]);
								System.out.print(",");
								result += data[sampleIdx] + ",";
							}
							System.out.println();
						}
						//return result;
						//return data;

					}
				}
			}
		}

    	Edk.INSTANCE.EE_EngineDisconnect();
    	Edk.INSTANCE.EE_EmoStateFree(eState);
    	Edk.INSTANCE.EE_EmoEngineEventFree(eEvent);
    	System.out.println("Disconnected!");
		return noData;

    }

    public static void main2(String[] args)
    {

    	Pointer eEvent				= Edk.INSTANCE.EE_EmoEngineEventCreate();
    	Pointer eState				= Edk.INSTANCE.EE_EmoStateCreate();
    	IntByReference userID 		= null;
		IntByReference nSamplesTaken= null;
    	short composerPort			= 1726;
    	int option 					= 1;
    	int state  					= 0;
    	float secs 					= 1;
    	boolean readytocollect 		= false;

    	userID 			= new IntByReference(0);
		nSamplesTaken	= new IntByReference(0);

    	switch (option) {
		case 1:
		{
			if (Edk.INSTANCE.EE_EngineConnect("Emotiv Systems-5") != EdkErrorCode.EDK_OK.ToInt()) {
				System.out.println("Emotiv Engine start up failed.");
				return;
			}
			break;
		}
		case 2:
		{
			System.out.println("Target IP of EmoComposer: [127.0.0.1] ");

			if (Edk.INSTANCE.EE_EngineRemoteConnect("127.0.0.1", composerPort, "Emotiv Systems-5") != EdkErrorCode.EDK_OK.ToInt()) {
				System.out.println("Cannot connect to EmoComposer on [127.0.0.1]");
				return;
			}
			System.out.println("Connected to EmoComposer on [127.0.0.1]");
			break;
		}
		default:
			System.out.println("Invalid option...");
			return;
    	}

		Pointer hData = Edk.INSTANCE.EE_DataCreate();
		Edk.INSTANCE.EE_DataSetBufferSizeInSec(secs);
		System.out.print("Buffer size in secs: ");
		System.out.println(secs);

    	System.out.println("Start receiving EEG Data!");
		while (true)
		{
			state = Edk.INSTANCE.EE_EngineGetNextEvent(eEvent);

			// New event needs to be handled
			if (state == EdkErrorCode.EDK_OK.ToInt())
			{
				int eventType = Edk.INSTANCE.EE_EmoEngineEventGetType(eEvent);
				Edk.INSTANCE.EE_EmoEngineEventGetUserId(eEvent, userID);

				// Log the EmoState if it has been updated
				if (eventType == Edk.EE_Event_t.EE_UserAdded.ToInt())
				if (userID != null)
					{
						System.out.println("User added");
						Edk.INSTANCE.EE_DataAcquisitionEnable(userID.getValue(),true);
						readytocollect = true;
					}
			}
			else if (state != EdkErrorCode.EDK_NO_EVENT.ToInt()) {
				System.out.println("Internal error in Emotiv Engine!");
				break;
			}

			if (readytocollect)
			{
				Edk.INSTANCE.EE_DataUpdateHandle(0, hData);

				Edk.INSTANCE.EE_DataGetNumberOfSample(hData, nSamplesTaken);

				if (nSamplesTaken != null)
				{
					if (nSamplesTaken.getValue() != 0) {

						System.out.print("Updated: ");
						System.out.println(nSamplesTaken.getValue());

						double[] data = new double[nSamplesTaken.getValue()];
						for (int sampleIdx=0 ; sampleIdx<1; ++ sampleIdx){ //sampleIdx<nSamplesTaken.getValue() ; ++ sampleIdx) {
							for (int i = 0 ; i < channels.length ; i++) {

								Edk.INSTANCE.EE_DataGet(hData, i, data, nSamplesTaken.getValue());
								System.out.print(data[sampleIdx]);
								System.out.print(",");
							}
							System.out.println();
						}
					}
				}
			}
		}

    	Edk.INSTANCE.EE_EngineDisconnect();
    	Edk.INSTANCE.EE_EmoStateFree(eState);
    	Edk.INSTANCE.EE_EmoEngineEventFree(eEvent);
    	System.out.println("Disconnected!");
    }

}
