package si.fri.algator.ausers;

import spark.Request;

/**
 *
 * @author Matej Bertoncelj
 */
public class AUsersHelper {

  static boolean isEmptyOrNull(String s) {
    return s == null || s.trim().isEmpty();
  }

  public static String getUIDFromHeaders(Request request) {
    String uid = "_unknown_";
    try {
      if (request.headers().contains("burden")) 
        uid = request.headers("burden");    
    } catch (Exception e) {}
    return uid;
  }

}
