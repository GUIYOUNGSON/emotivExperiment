import java.io.*;
import java.net.*;

class TCPClient
{
  int i = 0;

 public static void main(String argv[]) throws Exception
 {
  String sentence;
  String modifiedSentence;
  BufferedReader inFromUser = new BufferedReader( new InputStreamReader(System.in));
  Socket clientSocket = new Socket("localhost", 6789);
  DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
  BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
  for(int j = 0; j < 10; j++){
    sentence = inFromUser.readLine();
    String outString;
    for(int i = 0; i < 10; i++){
      outString += Math.random()*10 + ",";
    }
    outString = outString.subString(0,outString.length-1) + '\n';
    outToServer.writeBytes(outString);
    modifiedSentence = inFromServer.readLine();
    System.out.println("FROM SERVER: " + modifiedSentence);
  }
  clientSocket.close();
 }
}
