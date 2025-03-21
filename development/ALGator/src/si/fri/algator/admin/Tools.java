package si.fri.algator.admin;

import algator.Admin;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import si.fri.algator.ausers.AUsersTools;

/**
 *
 * @author tomaz
 */
public class Tools {

  public static void deleteDirectory(File folder) throws IOException {
     File[] files = folder.listFiles();
        if (files != null) { // Checks for null in case folder is empty
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file); // Recursively delete subdirectories
                } else {
                    file.delete(); // Delete file
                }
            }
        }
        folder.delete(); // Finally delete the empty folder
  }

  public static HashMap<String, String> getSubstitutions(String proj_name) {
    StringBuffer lc = new StringBuffer(proj_name);
    lc.setCharAt(0, Character.toLowerCase(proj_name.charAt(0)));
    String projNameCamelCase = lc.toString();

    SimpleDateFormat sdf = new SimpleDateFormat("MM, YYYY");

    HashMap<String, String> substitutions = new HashMap();
    substitutions.put("<PPP>", proj_name);
    substitutions.put("<pPP>", projNameCamelCase);
    substitutions.put("<today>", sdf.format(new Date()));
    substitutions.put("<eid>", AUsersTools.getUniqueDBid("e_"));

    // substitutions.put("\r", "\n");   
    return substitutions;
  }

  public static String readFile(String fileName) {
    try {
      ClassLoader classLoader = Admin.class.getClassLoader();
      InputStream fis = classLoader.getResourceAsStream(fileName);
      return new Scanner(fis).useDelimiter("\\Z").next();
    } catch (Exception e) {
      System.out.println(e.toString());
    }
    return "";
  }

  public static void writeFile(String fileName, String content) {
    try {
      // first creates a folder ...
      String filePath = FilenameUtils.getFullPath(fileName);
      File filePathFile = new File(filePath);
      if (!filePathFile.exists())
        filePathFile.mkdirs();

      // ... then writes a content
      PrintWriter pw = new PrintWriter(fileName);
      pw.print(content);
      pw.close();
    } catch (Exception e) {
      System.out.println(e.toString());
    }
  }

  public static String replace(String source, String what, String with) {
    return source.replaceAll(what, with);
  }

  /**
   * Copies a template to destination folder + makes substitutions.
   */
  public static void copyFile(String tplName, String outputFolder, String outputFileName, HashMap<String, String> substitutions) {
    String absAlg = readFile(tplName);
    for (String key : substitutions.keySet()) {
      absAlg = replace(absAlg, key, substitutions.get(key));
    }
    writeFile(new File(outputFolder, outputFileName).getAbsolutePath(), absAlg);
  }

  public static String encodeFileToBase64Binary(String fileName) {
    try {
      File file = new File(fileName);
      byte[] encoded = Base64.encodeBase64(FileUtils.readFileToByteArray(file));
      return new String(encoded, StandardCharsets.US_ASCII);
    } catch (Exception e) {
      return "Error: " + e.toString();
    }
  }

}
