package si.fri.algator.tools;

import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.apache.commons.codec.digest.Crypt;
import org.apache.commons.lang3.RandomStringUtils;

/**
 *
 * @author tomaz
 */
public class Password {
  // encripts password with random salt
  public static String encriptOld(String password) {
    return Crypt.crypt(password, RandomStringUtils.random(15, true, true));
  }
  // checks if the passwords match
  public static boolean checkPasswordOld(String encripted, String password) {
    return encripted.equals(Crypt.crypt(password, encripted));
  }

  
  
  /**
   * Computes the PBKDF2 hash of a password.
   *
   * @param password the password to hash.
   * @param salt the salt
   * @param iterations the iteration count (slowness factor)
   * @param bytes the length of the hash to compute in bytes
   * @return the PBDKF2 hash of the password
   */
  private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int bytes) {
    try {
      PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, bytes * 8);
      SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
      return skf.generateSecret(spec).getEncoded();
    } catch (Exception e) {
      return new byte[0];
    }
  }

  public static String encript(String password) {
    String algorithm  = "pbkdf2_sha256";
    int    iterations = 26000;
    String salt       = RandomStringUtils.random(22, true, true);
    String hashedPwd  = Base64.getEncoder().encodeToString(pbkdf2(password.toCharArray(), salt.getBytes(), iterations, 32));
    return String.format("%s$%d$%s$%s", algorithm, iterations, salt, hashedPwd);
  }


  /**
   * Checks if "password" matches the password encripted in "encripted"
   * password.
   *
   * @param password plain-text password
   * @param hashedPassword hashed password in form
   * "<algorithm>$<iterations>$<salt>$<hash>"
   * @return true if password matches.
   */
  public static boolean checkPassword(String encripted, String password) {
    String[] params = encripted.split("[$]");
    if (params.length != 4) {
      return false;
    }

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
    String goodHash = "pbkdf2_sha256$26000$OiCBrBLvLnOOBZQPMSkIBc$5C81EFiFuaLwdm2gB6J36NCNsWXRgs40QFRiP61U9wc=";
                    //"pbkdf2_sha256$260000$WEPWAhL8Xh3lUSmcIeuHPn$kSn5SGPLrLXUS0Nbrd6xwTCyqi7QFW3lds939+ejgzw=";

    System.out.println(checkPassword(goodHash, "pojedina"));
    
    System.out.println(goodHash);
    System.out.println(encript("pojedina"));
    
  }

}
