package si.fri.algator.tools;

import java.io.BufferedInputStream;
import si.fri.algator.global.ErrorStatus;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import si.fri.algator.entities.EComputer;
import si.fri.algator.entities.EComputerFamily;
import si.fri.algator.entities.EAlgatorConfig;
import si.fri.algator.entities.EAlgorithm;
import si.fri.algator.entities.EProject;
import si.fri.algator.entities.EQuery;
import si.fri.algator.entities.ETestSet;
import si.fri.algator.entities.MeasurementType;
import si.fri.algator.entities.NameAndAbrev;
import si.fri.algator.entities.Project;
import si.fri.algator.global.ATGlobal;
import si.fri.algator.global.Answer;

/**
 *
 * @author tomaz
 */
public class ATTools {

  /**
   * Creates a temporary directory.
   *
   * @param baseDir
   * @return
   */
  public static File createTempDir(String baseDir) {
    String baseName = "tmp" + System.currentTimeMillis();

    File tempDir = new File(baseDir, baseName);
    if (tempDir.mkdir()) {
      ErrorStatus.setLastErrorMessage(ErrorStatus.STATUS_OK, "");
      return tempDir;
    }

    ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR_CANT_COPY, "Failed to create directory " + tempDir.getAbsolutePath());
    return null;
  }


  private static String getCompileInfo(List<String> options, String[] sources, boolean result) {
    StringBuilder info = new StringBuilder();

    info.append("Source files:\n\n");
    for (String file : sources) {
        info.append(" - ").append(file).append("\n");
    }
    //info.append("\n Options: \n\n").append(String.join(" ", options)).append("\n");

    info.append("\nCompilation ").append(result ? "succeeded." : "failed: \n\n");
    
    return info.toString();
  }
  
  /**
   *
   * @param sourcePath path to source files
   * @param sources a list of sources (with .java extension)
   * @param destPath a path for compiled files
   * @param classpaths a list of paths to bo included in classpath
   * @param msg message substring to be displayed in log file
   * @return
   */
  public static Answer compile(String srcPath, String[] sources, String destPath,
          String[] classpaths, String jars, String msg) {
    // a path has to be at least 10 charaters long to prevent errors (i.e deleting root folder)
    if (destPath.length() < 10) {
      return new Answer(ErrorStatus.ERROR_INVALID_DESTPATH);
    }
    File destPathF = new File(destPath);
    // if folder exists, delete it ...
    if (destPathF.exists()) {
      destPathF.delete();
    }
    // ... and create a new one
    destPathF.mkdirs();

    if (!destPathF.exists()) {
      return new Answer(ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR_CANT_CREATEDIR, destPath));
    }

    // build a classpath
    StringBuilder sb = new StringBuilder();
    
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    Writer wos = new OutputStreamWriter(os);

    String pathSeparator = System.getProperty("path.separator"); 
    String[] classPathEntries = System.getProperty("java.class.path") .split(pathSeparator);  
    for (String classPathEntry : classPathEntries) {
      sb.append(classPathEntry).append(File.pathSeparator);
    }
    
    for (String cp : classpaths) {
      sb.append(cp).append(File.pathSeparator);
    }

    // add a project/algorithm specific jars
    sb.append(File.pathSeparator).append(jars);

    ArrayList<File> srcFiles = new ArrayList();
    int i = 0;
    for (String src : sources) {
      srcFiles.add(new File(srcPath + File.separator + src));
    }

    List<String> options = new ArrayList<String>();

    options.add("-sourcepath");
    options.add(srcPath);

    options.add("-classpath");  // -classpath <path>      Specify where to find user class files
    options.add(sb.toString());
    options.add("-d");          // -d <directory>         Specify where to place generated class files
    options.add(destPath);

    options.add("-g");          // debug option

    JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
    StandardJavaFileManager fileManager = javac.getStandardFileManager(null /* diagnosticlistener */, null, null);
    JavaCompiler.CompilationTask task = javac.getTask(wos, fileManager, null /* diagnosticlistener */, options, null, fileManager.getJavaFileObjectsFromFiles(srcFiles));

    Boolean compileOK = task.call();

    String compileMsg = getCompileInfo(options, sources, compileOK);
    
    if (!compileOK) {
      String error = os.toString();
      //Ziga Zorman:  fixed BUG - on windows escape the File.separator chars in regex otherwise an exception for invalid pattern will be thrown
      // error = error.replaceAll(srcPath + File.separator, "");
      String escapedSrcPath = (srcPath + File.separator).replaceAll("\\\\", "\\\\\\\\");
      error = error.replaceAll(escapedSrcPath, "");

      return new Answer(ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR_CANT_COMPILE, error), compileMsg + error);
    } 
    try {compileMsg += "\n" + os.toString("UTF-8");} catch (Exception e) {}
    return new Answer(ErrorStatus.setLastErrorMessage(ErrorStatus.STATUS_OK, String.format("Compiling %s  - done.", msg)), compileMsg);
  }

  public static boolean isSourceNewer(String srcDir, String binDir, String[] srcNames) {
    for (String srcName : srcNames) {
      File f1 = new File(srcDir + File.separator + srcName + ".java");
      File f2 = new File(binDir + File.separator + srcName + ".class");
      try {
        if (FileUtils.isFileNewer(f1, f2)) {
          return true;
        }
      } catch (Exception e) {
        // this happens, for example, if bin folder does not exist
        return true;
      }
    }
    return false;
  }


  /**
   * Compare the date of last change of project, algorithm and testset with 
   * the date of change of the file with results
   */
  public static boolean resultsAreUpToDate(Project project, String algorithmName, String testsetName, String mtype) {
    String resultFileName = getTaskResultFileName(project, algorithmName, testsetName, mtype);
    return resultsAreUpToDate(project, algorithmName, testsetName, mtype, resultFileName);
  }

  public static boolean resultsAreUpToDate(Project project, String algorithmName, String testsetName, String mtype, String resultFileName) {
    return resultsAreUpToDate(project.getEProject(), 
      EAlgorithm.getAlgorithm(project.getName(), algorithmName), ETestSet.getTestset(project.getName(), testsetName), resultFileName);
  }
  public static boolean resultsAreUpToDate(EProject eProj, EAlgorithm eAlg, ETestSet eTst, String resultFileName) {    
    // when the project-alg-tst was last changed
    long lastChanged = Arrays.stream(new long[]{eProj.lastModified(eProj.getName(), ""), eAlg.lastModified(eProj.getName(), eAlg.getName()), eTst.lastModified(eProj.getName(), eTst.getName())}).max().getAsLong();
    
    return new File(resultFileName).lastModified()/1000 > lastChanged;
  }

  /**
   * Result file is considered to be complete if it contains exactly
   * expectedNumberOfLines lines
   */
  public static boolean resultsAreComplete(String resultFileName, int expectedNumberOfInstances) {
    return expectedNumberOfInstances == getNumberOfTests(resultFileName);
  }

  
  /**
   * Metoda vrne VSE rezultate izvajanja z različnimi računalniki, torej vse obstoječe 
   * datoteke z imenom algoritem-testset.mtype. Če je družina (family) podana, vrne samo 
   * rezultate te družine sicer rezultate vseh družin
   */
  public static List<Path> getAllResultFiles(Project project, String algorithmName, String testsetName, String mType, String family) {
      String parentDir    = ATGlobal.getRESULTSrootroot(project.getProjectRoot());
      String filename     = ATGlobal.getResultFilenameName(algorithmName, testsetName, mType);
            
      // to ensure completness of familyName
      String familyName   = family.isEmpty() ? "" : family+".";
      
      List<Path> files = new ArrayList();
      try {
        files = Files.walk(Paths.get(parentDir), 2)
          .filter(p -> p.getParent() != null && p.getParent().getFileName().toString().startsWith(familyName))
          .filter(p -> p.getFileName().toString().equals(filename))
          .collect(Collectors.toList());
        } catch (IOException e) {}
    return files;
  }  
  
  /**
   * This is a simplification of the oldest getTaskResultFileName method. Here the 
   * resultFileName is the name of the most recent file with results for this family 
   * (or for any family if family is not set in Project)
   * 
   */
  public static String getTaskResultFileName(Project project, String algorithmName, String testsetName, String mType) {
    String family = project.getEProject().getProjectFamily(mType);
    List<Path> files = getAllResultFiles(project, algorithmName, testsetName, mType, family);
    Path mostRecentFile = files.stream()
      .max(Comparator.comparingLong(p -> {
        try {
          return Files.getLastModifiedTime(p).toMillis();
        } catch (IOException e) {
          return Long.MIN_VALUE; // Treat errors as oldest
        }
      })).orElse(null);
    if (mostRecentFile != null)
      return mostRecentFile.toString();
    else return "";
  }

  /**
   * Returns the number of different tests in result file. Each test result is represented by a json 
   * line in result file. Test identifier is "InstanceID".  Method returns the number of different identifiers in 
   * result file. 
   */
  public static int getNumberOfTests(String filename) {
    TreeSet<String> testIDs = new TreeSet<>();
    try (Scanner sc = new Scanner(new File(filename));) {      
      while (sc.hasNextLine()) {
        String testResult = sc.nextLine();
        try {
          JSONObject jResult = new JSONObject(testResult);
          testIDs.add(jResult.getString("InstanceID"));
        } catch (Exception e) {}
      }
    } catch (Exception e) {}
    return testIDs.size();
  }

  public static int getNumberOfLines(File file) {
    try (InputStream is = new BufferedInputStream(new FileInputStream(file));) {
      byte[] c = new byte[1024];
      int count = 0;
      int readChars = 0;
      boolean empty = true;
      while ((readChars = is.read(c)) != -1) {
        empty = false;
        for (int i = 0; i < readChars; ++i) {
          if (c[i] == '\n') {
            ++count;
          }
        }
      }
      return (count == 0 && !empty) ? 1 : count;
    } catch (Exception e) {
      return 0;
    }
  }

  /**
   * Returns null if all the sources exists or the name of the missing source
   */
  public static String sourcesExists(String srcDir, String[] srcs) {
    for (String srcName : srcs) {
      File f = new File(srcDir + File.separator + srcName + ".java");
      if (!f.exists()) {
        return f.getAbsolutePath();
      }
    }
    return null;
  }

  /**
   * Extracts the path (without the filename) from the file
   */
  public static String extractFilePath(File file) {
    String fileName = file.getAbsolutePath();
    int pos = fileName.lastIndexOf(File.separator);
    return pos != -1 ? fileName.substring(0, pos) : fileName;
  }

  /**
   * Extracts the prefix of the filename (the name of the file without the path
   * and file extension)
   */
  public static String extractFileNamePrefix(File file) {
    String fileName = file.getAbsolutePath();

    // get the name of the file (without the path)
    int pos = fileName.lastIndexOf(File.separator);
    fileName = pos != -1 ? fileName.substring(pos + 1) : fileName;

    pos = fileName.lastIndexOf(".");
    return (pos != -1) ? fileName.substring(0, pos) : fileName;
  }
  
  
  /**
   * Extract last folder name from path. Examples:
   * /home/user/documents/project/file.txt → project
   * /home/user/documents/project/ → project
   * C:\Users\User\Documents\Project\File.txt → Project
   */
  public static String getLastFolderName(String path) {
    // Normalize separators to handle both / and \
    String normalizedPath = path.replace('\\', '/');

    // Find the last separator
    int lastSeparatorIndex = normalizedPath.lastIndexOf('/');
    if (lastSeparatorIndex == -1) return ""; // No separators in the path

    // Find the second-to-last separator
    int secondLastSeparatorIndex = normalizedPath.lastIndexOf('/', lastSeparatorIndex - 1);
    if (secondLastSeparatorIndex == -1) return ""; // No parent folder

    // Extract and return the last folder name
    return normalizedPath.substring(secondLastSeparatorIndex + 1, lastSeparatorIndex);
  }


  /**
   * Strips the file extension
   */
  public static String stripFilenameExtension(String fileName) {
    int pos = fileName.lastIndexOf(".");
    return (pos != -1) ? fileName.substring(0, pos) : fileName;
  }

  /**
   * Returns the file extension
   */
  public static String getFilenameExtension(String fileName) {
    int pos = fileName.lastIndexOf(".");
    return (pos != -1) ? fileName.substring(pos + 1, fileName.length()) : fileName;
  }
  
  
  /**
   * Method creates path of a file (if this path does not exist yet).
   */
  public static void createFilePath(String fileName) {
    File resPath = new File(ATTools.extractFilePath(new File(fileName)));        
    if (!resPath.exists()) resPath.mkdirs();
  }


  /**
   * Builds a list of JARs in the following format: jar_0:jar_1:..., where jar_i
   * is i-th element of jars prepended with tha path
   *
   */
  public static String buildJARList(String[] jars, String path) {
    String result = "";
    for (String jar : jars) {
      result += (result.isEmpty() ? "" : File.pathSeparator) + path + File.separator + jar;
    }
    return result;
  }

  public static URL[] getURLsFromJARs(String[] jars, String path) {
    try {
      URL[] urls = new URL[jars.length];
      int i = 0;
      for (String jar : jars) {
        urls[i++] = new File(path + File.separator + jar).toURI().toURL();
      }
      return urls;
    } catch (Exception e) {
      return new URL[0];
    }
  }

  public static String stringJoin(String delim, Iterable<String> parts) {
    String result = "";
    for (String part : parts) {
      result += (result.isEmpty() ? "" : delim) + part;
    }
    return result;
  }

  public static String getResourceFile(String fileName) {
    StringBuilder result = new StringBuilder("");

    //Get file from resources folder
    ClassLoader classLoader = ATTools.class.getClassLoader();

    try (Scanner scanner = new Scanner(classLoader.getResourceAsStream(fileName))) {

      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        result.append(line).append("\n");
      }

      scanner.close();

    } catch (Exception e) {
    }
    return result.toString();
  }

  /**
   * Creates a HashMap from JSON and adds missing default keys with empty values
   */
  public static HashMap<String, Object> jSONObjectToMap(JSONObject jObj, String... defaultKeys) {
    HashMap<String, Object> result;
    try {
      result  = ATTools.jSONObjectToMap(jObj);
    } catch (Exception e) {
      result = new HashMap<>();
    }    
    
    for (String defaultKey : defaultKeys) {
      if (!result.containsKey(defaultKey))
        result.put(defaultKey, "");
    }
    return result;    
  }
  
  /**********************+ JSON tools *************************/
  public static HashMap<String, Object> jSONObjectToMap(JSONObject object) throws JSONException {
    HashMap<String, Object> map = new HashMap<>();

    Iterator<String> keysItr = object.keys();
    while (keysItr.hasNext()) {
      String key = keysItr.next();
      Object value = object.get(key);

      if (value instanceof JSONArray) {
        value = jSONObjectToList((JSONArray) value);
      } else if (value instanceof JSONObject) {
        value = ATTools.jSONObjectToMap((JSONObject) value);
      }
      map.put(key, value);
    }
    return map;
  }
  public static List<Object> jSONObjectToList(JSONArray array) throws JSONException {
    List<Object> list = new ArrayList<Object>();
    for (int i = 0; i < array.length(); i++) {
      Object value = array.get(i);
      if (value instanceof JSONArray) {
        value = jSONObjectToList((JSONArray) value);
      } else if (value instanceof JSONObject) {
        value = ATTools.jSONObjectToMap((JSONObject) value);
      }
      list.add(value);
    }
    return list;
  }
 
  
  
  
    /**
   * Method replaces all occurencies of "...Date":dateL,  with "...Date":"dateS", where
   * dateS is a string representation of a dateL.
   * 
   * Example: if dateFormat = "YY/MM/dd hh:mm:ss", "CreationDate":1673360936132 -> "CreationDate":"2023/01/10 15:28:56"
   */  
  public static String replaceDateL2S(String line, String dateFormat) {
    Pattern datePat = Pattern.compile("Date\":([0-9]+),");
    DateFormat dateFormater = new SimpleDateFormat(dateFormat);
    
    Matcher action = datePat.matcher(line);
    
    while (action.find()) {
      String dateL = action.group(1);
      if (dateL == null || dateL.length() < 6) continue;
      
      Date   dateD = new Date(Long.parseLong(dateL)); 
      String dateS = dateFormater.format(dateD);

      line = line.replaceAll(dateL, "\""+ dateS +"\"");
    }
    return line;
  }
  
  public static void main(String[] args) {
// /home/user/documents/project/file.txt → project
// /home/user/documents/project/ → project
// C:\Users\User\Documents\Project\File.txt → Project    
//    System.out.println(getLastFolderName("C:\\ALGATOR_ROOT\\data_root\\projects\\PROJ-Sort\\algs\\ALG-QuickSort\\algorithm.json"));

// d:\Users\tomaz\OneDrive\ULFRI\ALGATOR_ROOT\data_root\projects\PROJ-MnozenjeStevil\tests\TestSet5.json

    // System.out.println(extractFileNamePrefix(new File("d:\\Users\\tomaz\\OneDrive\\ULFRI\\ALGATOR_ROOT\\data_root\\projects\\PROJ-MnozenjeStevil\\tests\\TestSet5.json")));
    
    Project p = new Project(ATGlobal.getALGatorDataRoot(), "BasicSort");
    System.out.println(
      getTaskResultFileName(p, "BubbleSort", "TestSet110", "em") 
    );
 
  }
  
  

}
