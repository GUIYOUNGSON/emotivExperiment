import java.lang.Runtime;
import java.io.*;

public class Command{

  public static void main(String[] args) throws Exception{
    final Process p = Runtime.getRuntime().exec("python pythontest.py");

    new Thread(new Runnable() {
      public void run(){
        BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
        BufferedWriter output = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
        String line = null;

        try {

          output.write("build_model('test.txt')");
          output.close();
          int i = 0;
          while(!input.ready()){
            i++;
          }
          System.out.println("Doing other stuff for " + i);
          while ((line = input.readLine()) != null) {
            System.out.println(line);
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }).start();

    p.waitFor();
  }


}
