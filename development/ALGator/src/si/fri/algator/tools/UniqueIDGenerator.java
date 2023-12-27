package si.fri.algator.tools;

import java.security.MessageDigest;
import java.util.Random;

/**
 *
 * @author tomaz
 */
public class UniqueIDGenerator {
  static String rndSeed = String.valueOf(new Random().nextInt());
  static int    counter = 0;
  
  private static String printHexBinary(byte[] val) {
    String result = "";
    for (byte b : val) {
      result += String.format("%02X", b);
    }
    return result;
  }
  
  public static String sha1(String input, int len) {
    String sha1 = null;
    try {
      MessageDigest msdDigest = MessageDigest.getInstance("SHA-1");
      msdDigest.update(input.getBytes("UTF-8"), 0, input.length());
      sha1 = printHexBinary(msdDigest.digest());
    } catch (Exception e) {}
    return sha1.substring(0, len);
  }
  
  public static String getNextID() {
    return sha1((++counter) + rndSeed, 12);
  }
  
  public static String formatNumber(int number, int digits) {
    String result = Integer.toString(number);
    while (result.length() < digits) result = "0" + result;
    return result;
  }
  
  public static void main(String[] args) {
    for (int i = 0; i < 10; i++) {
      System.out.println(getNextID());
    }
  }

}
