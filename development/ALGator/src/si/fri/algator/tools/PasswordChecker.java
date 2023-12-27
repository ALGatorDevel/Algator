package si.fri.algator.tools;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Used to check if the password written in Django format matches a given password
 * @author tomaz
 */
public class PasswordChecker {
      
  /**
   *  Computes the PBKDF2 hash of a password.
   *
   * @param   password    the password to hash.
   * @param   salt        the salt
   * @param   iterations  the iteration count (slowness factor)
   * @param   bytes       the length of the hash to compute in bytes
   * @return              the PBDKF2 hash of the password
   */
  private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int bytes)
      throws NoSuchAlgorithmException, InvalidKeySpecException
  {
      PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, bytes * 8);
      SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
      return skf.generateSecret(spec).getEncoded();
  }  
  
  /**
   * Checks if "password" matches the password encripted in "hashedPassword".
   * @param password  plain-text password
   * @param hashedPassword hashed password in form "<algorithm>$<iterations>$<salt>$<hash>"
   * @return true if password matches.
   */
    
  static boolean check(String password, String hashedPassword) {    
    String[] params = hashedPassword.split("[$]");
    if (params.length != 4) return false;
    
    int iterations = Integer.parseInt(params[1]);
    byte[] salt = params[2].getBytes();
    byte[] hash = Base64.getDecoder().decode(params[3]);
    
    try {
      byte[] testHash = pbkdf2(password.toCharArray(), salt, iterations, hash.length);
          
      return Arrays.equals(hash, testHash);
    } catch (Exception e) {
      return false;
    }
  }

  public static void main(String[] args) {
    String goodHash = "pbkdf2_sha256$260000$WEPWAhL8Xh3lUSmcIeuHPn$kSn5SGPLrLXUS0Nbrd6xwTCyqi7QFW3lds939+ejgzw=";
    
    System.out.println(check("polde", goodHash));
  }
  
}
