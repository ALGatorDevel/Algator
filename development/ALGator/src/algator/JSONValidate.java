package algator;

import java.io.File;
import java.util.Scanner;
import org.json.JSONObject;

/**
 *
 * @author tomaz
 */
public class JSONValidate {
  
  public static void main(String[] args) {   
    if (args.length != 1) {
      System.out.println("Uporaba: java algator.JSONValidate filename");
      System.exit(0);
    }
    
    try {
      Scanner sc = new Scanner(new File(args[0]));
      sc.useDelimiter("\\Z");
      String vsebina = sc.next();
      JSONObject jObject = new JSONObject(vsebina);
      System.out.println(jObject.keySet() + " - OK");
    } catch (Exception e) {
      System.out.println("Napaka: " + e.toString());
    }
            
  }

}
