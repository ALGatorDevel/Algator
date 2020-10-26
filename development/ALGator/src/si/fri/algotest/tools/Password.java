package si.fri.algotest.tools;

import org.apache.commons.codec.digest.Crypt;
import org.apache.commons.lang3.RandomStringUtils;

/**
 *
 * @author tomaz
 */
public class Password {
  public static String encript(String password) {
    return Crypt.crypt(password, RandomStringUtils.random(15, true, true));
  }
  
  // checks if the passwords match
  public static boolean checkPassword(String encripted, String password) {
    return encripted.equals(Crypt.crypt(password, encripted));
  }
}
