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
    String outString = "";
    for(int i = 0; i < 10; i++){
      outString += Math.random()*10 + ",";
    }
    while(true){
      //outString = outString.subString(0,outString.length-1) + '\n';
      outToServer.writeDouble(10.001);
      if(inFromServer.ready()){
        System.out.println("server has a message ");
        modifiedSentence = inFromServer.readLine();
        System.out.println("FROM SERVER: " + modifiedSentence);
      }else{
        System.out.println("server not ready");
      }
    }
  }
  clientSocket.close();
 }
}
