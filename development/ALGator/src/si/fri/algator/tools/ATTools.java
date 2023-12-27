package si.fri.algator.tools;

import java.io.BufferedInputStream;
import si.fri.algator.global.ErrorStatus;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import static si.fri.algator.entities.EAlgatorConfig.ID_DBServer;
import si.fri.algator.entities.EProject;
import si.fri.algator.entities.EQuery;
import si.fri.algator.entities.ETestSet;
import si.fri.algator.entities.MeasurementType;
import si.fri.algator.entities.NameAndAbrev;
import si.fri.algator.entities.Project;
import si.fri.algator.global.ATGlobal;

/**
 *
 * @author tomaz
 */
public class ATTools {

  // nuber of attempt to create dir
  private static final int TEMP_DIR_ATTEMPTS = 10;

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

  /**
   * Copies the source .java files to the working directory and compiles them
   * into .class files
   *
   * @param sources the list of source files
   * @param workingDir the working directory to copy and compile to
   * @return {@code ERROR_CANT_COPY} if files can't be copied to the working
   * directory, {@code ERROR_CANT_COMPILE} if files can't be compiled, otherwise
   * {@code STATUS_OK}.
   */
  //DELA, a brez class path-a
  public static ErrorStatus compileOld(String sourcePath, String[] sources, File workingDir) {
    int i = 0;
    String[] filesToCompile = new String[sources.length];
    for (String filename : sources) {
      File inFile = new File(sourcePath + File.separator + filename);
      File outFile = new File(workingDir + File.separator + filename);
      try {
        FileUtils.copyFile(inFile, outFile);
        filesToCompile[i++] = outFile.getAbsolutePath();
      } catch (Exception e) {
        return ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR_CANT_COPY, e.toString());
      }
    }

    String errors = "";

    OutputStream os = new ByteArrayOutputStream();
    JavaCompiler javac = ToolProvider.getSystemJavaCompiler();

    int error_status = javac.run(null, os, os, filesToCompile);

    if (error_status != 0) {
      String error = os.toString().replaceAll(workingDir.getAbsolutePath(), "File: ");
      return ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR_CANT_COMPILE, error);
    }
    return ErrorStatus.setLastErrorMessage(ErrorStatus.STATUS_OK, "");
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
  public static ErrorStatus compile(String srcPath, String[] sources, String destPath,
          String[] classpaths, String jars, String msg) {
    // a path has to be at least 10 charaters long to prevent errors (i.e deleting root folder)
    if (destPath.length() < 10) {
      return ErrorStatus.ERROR_INVALID_DESTPATH;
    }
    File destPathF = new File(destPath);
    // if folder exists, delete it ...
    if (destPathF.exists()) {
      destPathF.delete();
    }
    // ... and create a new one
    destPathF.mkdirs();

    if (!destPathF.exists()) {
      return ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR_CANT_CREATEDIR, destPath);
    }

    // build a classpath
    StringBuilder sb = new StringBuilder();
    
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

    ByteArrayOutputStream os = new ByteArrayOutputStream();
    Writer wos = new OutputStreamWriter(os);

    JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
    StandardJavaFileManager fileManager = javac.getStandardFileManager(null /* diagnosticlistener */, null, null);
    JavaCompiler.CompilationTask task = javac.getTask(wos, fileManager, null /* diagnosticlistener */, options, null, fileManager.getJavaFileObjectsFromFiles(srcFiles));

    Boolean compileOK = task.call();

    if (!compileOK) {
      String error = os.toString();
      //Ziga Zorman:  fixed BUG - on windows escape the File.separator chars in regex otherwise an exception for invalid pattern will be thrown
      // error = error.replaceAll(srcPath + File.separator, "");
      String escapedSrcPath = (srcPath + File.separator).replaceAll("\\\\", "\\\\\\\\");
      error = error.replaceAll(escapedSrcPath, "");

      return ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR_CANT_COMPILE, error);
    }
    return ErrorStatus.setLastErrorMessage(ErrorStatus.STATUS_OK, String.format("Compiling %s  - done.", msg));
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

  /*  
  public static ETestSet getFirstTestSetFromProject(String dataroot, String projName) {
    String projRoot     = ATGlobal.getPROJECTroot    (dataroot, projName);
    String projFilename = ATGlobal.getPROJECTfilename(dataroot, projName);
    
    EProject   eProject = new EProject(new File(projFilename)); 
    if (ErrorStatus.getLastErrorStatus() != ErrorStatus.STATUS_OK) 
      return null;

    String [] eTestSetNames = eProject.getStringArray(EProject.ID_TestSets);
    if (eTestSetNames.length < 1) {
      ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR, "No testsets defined in a project");
      return null;
    }
    
    String testSetFilename = ATGlobal.getTESTSETfilename(dataroot, projName, eTestSetNames[0]);
    ETestSet testSet = new ETestSet(new File(testSetFilename));
    if (ErrorStatus.getLastErrorStatus() != ErrorStatus.STATUS_OK)
      return null;
    else
      return testSet;
  }
   */
  /**
   * Returns all the files that this query depends on. If any of these files
   * chenges, the query has to be run again.
   */
  public static HashSet<String> getFilesForQuery(String projectname, String queryname, String[] params) {
    HashSet<String> result = new HashSet<>();

    try {
      Project project = new Project(ATGlobal.getALGatorDataRoot(), projectname);
      EQuery query = new EQuery(new File(ATGlobal.getQUERYfilename(project.getEProject().getProjectRootDir(), queryname)), params);

      result.add(query.entity_file_name);

      // Project
      result.addAll(getFilesForProject(project));
      // Algorithms
      NameAndAbrev[] algs = query.getNATabFromJSONArray(EQuery.ID_Algorithms);
      for (NameAndAbrev alg : algs) {
        result.addAll(getFilesForAlgorithm(project, alg.getName()));
      }
      // Algorithms
      NameAndAbrev[] tss = query.getNATabFromJSONArray(EQuery.ID_TestSets);
      for (NameAndAbrev ts : tss) {
        result.addAll(getFilesForTestSet(project, ts.getName()));
      }

    } catch (Exception e) {
    }

    return result;
  }

  private static HashSet<String> getFilesForProject(Project project) {
    HashSet<String> result = new HashSet<>();
    if (project == null || project.getEProject() == null) {
      return result;
    }

    try {
      // Project.atp
      result.add(ATGlobal.getPROJECTfilename(project.getDataRoot(), project.getName()));

      // project src files
      String algorithm = project.getEProject().getAbstractAlgorithmClassname();
      String testCase = project.getEProject().getTestCaseClassname();
      String input = project.getEProject().getInputClassname();
      String output = project.getEProject().getOutputClassname();

      String projSrc = ATGlobal.getPROJECTsrc(project.getEProject().getProjectRootDir());
      result.add(projSrc + File.separator + algorithm + ".java");
      result.add(projSrc + File.separator + testCase + ".java");
      result.add(projSrc + File.separator + input + ".java");
      result.add(projSrc + File.separator + output + ".java");

      URL[] proJARs = ATTools.getURLsFromJARs(project.getEProject().getStringArray(EProject.ID_ProjectJARs), ATGlobal.getPROJECTlib(project.getEProject().getProjectRootDir()));
      for (URL url : proJARs) {
        result.add(url.toString());
      }

      URL[] algJARs = ATTools.getURLsFromJARs(project.getEProject().getStringArray(EProject.ID_AlgorithmJARs), ATGlobal.getPROJECTlib(project.getEProject().getProjectRootDir()));
      for (URL url : algJARs) {
        result.add(url.getFile());
      }
    } catch (Exception e) {
    }
    return result;
  }

  private static HashSet<String> getFilesForAlgorithm(Project project, String algName) {
    HashSet<String> result = new HashSet<>();
    try {
      // <algorithm>.atal
      result.add(ATGlobal.getALGORITHMfilename(project.getEProject().getProjectRootDir(), algName));

      // MainClass.java
      String mainClass = project.getAlgorithms().get(algName).getAlgorithmClassname();
      String srcDir = ATGlobal.getALGORITHMsrc(project.getEProject().getProjectRootDir(), algName);
      result.add(srcDir + File.separator + mainClass + ".java");
    } catch (Exception e) {
    }
    return result;
  }

  // Returns all files describing testsets of a given project. The files are taken from data_root (and not data_local) folder 
  private static HashSet<String> getFilesForTestSet(Project project, String testsetName) {
    HashSet<String> result = new HashSet<>();
    try {
      // TestSetX.atts
      result.add(ATGlobal.getTESTSETfilename(project.getDataRoot(), project.getName(), testsetName));

      // testsetX.txt
      String descFile = project.getTestSets().get(testsetName).getTestSetDescriptionFile();
      result.add(ATGlobal.getTESTSroot(project.getDataRoot(), project.getName()) + File.separator + descFile);
    } catch (Exception e) {
    }
    return result;
  }

  private static HashSet<String> getFilesForMeasurementType(Project project, String mType) {
    HashSet<String> result = new HashSet<>();
    try {
      // Project-[mtype].atrd
      result.add(ATGlobal.getRESULTDESCfilename(project.getEProject().getProjectRootDir(), project.getName(), MeasurementType.valueOf(mType)));
    } catch (Exception e) {
    }
    return result;
  }

  /**
   * Returns filenames of all files composing the
   * project-algorithm-testset-mtype (apt, atts, atrd, ..., jars, *.java, ...)
   */
  private static HashSet<String> getFilesForProjectAlgorithmTestSetMType(Project project, String algName, String testsetName, String mType) {
    HashSet<String> result = new HashSet<>();

    result.addAll(getFilesForProject(project));
    result.addAll(getFilesForAlgorithm(project, algName));
    result.addAll(getFilesForTestSet(project, testsetName));
    result.addAll(getFilesForMeasurementType(project, mType));

    return result;
  }

  /**
   * Compare the date of last change of the file with results with the date of
   * last change of all files of the project-algoritgm-testset-mtype and returns
   * true if results file is the youngest file and false otherwise.
   */
  public static boolean resultsAreUpToDate(Project project, String algorithmName, String testsetName, String mtype) {
    String resultFileName = getTaskResultFileName(project, algorithmName, testsetName, mtype);
    return resultsAreUpToDate(project, algorithmName, testsetName, mtype, resultFileName);
  }

  public static boolean resultsAreUpToDate(Project project, String algorithmName, String testsetName,
          String mtype, String resultFileName) {

    HashSet<String> depFiles = getFilesForProjectAlgorithmTestSetMType(project, algorithmName, testsetName, mtype);
    return resultsAreUpToDate(depFiles, resultFileName);
  }

  /**
   * Compare the date of last change of resultFile with the date of last change
   * of all files in a given set (depFiles). Method returns true if resultFile
   * is the youngest.
   */
  public static boolean resultsAreUpToDate(HashSet<String> depFiles, String resultFileName) {
    try {
      File curFile = new File(resultFileName);
      if (!curFile.exists()) {
        return false;
      }

      for (String file : depFiles) {
        File f = new File(file);
        if (!f.exists()) {
          continue;
        }
        if (FileUtils.isFileNewer(f, curFile)) {
          return false;
        }
      }
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Result file is considered to be complete if it contains exactly
   * expectedNumberOfLines lines
   */
  public static boolean resultsAreComplete(String resultFileName, int expectedNumberOfInstances) {
    return expectedNumberOfInstances == getNumberOfTests(resultFileName);
  }

  /**
   * Za vse aktualne pare [familyID].[computerID] v folderju
   * PROJ-[project]/results/[familyID].[computerID] pogleda za obstoj datoteke
   * [algorithm]-[testset].[mtype]. Če med temi datotekami najde up-to-date
   * datoteko (ima pravilno število vrstic (zapisno v testset.N) in je mlajša od
   * konfiguracijksih datotek), vrne njemo ime (če obstaja več takih datotek,
   * vrne prvo, na katero naleti). Če nobena datoteka ni up-to-date, vrne prvo,
   * ki ima pravilno število vrstic. Sicer vrne prvo neprazno datoteko. Če pa
   * tudi nobena neprazna datoteka ne obstaja, vrne ime datoteke na
   * thisComputerID() ("C0").<br><br>
   *
   * Namen: metoda vrne datoteko z rezultati za dan trojček
   * algoritem-testset-mtype. Ker lahko obstaja več datotek, ki vsebujejo te
   * rezultate (če so, na primer, različni računalniki izvedli isti task), moram
   * izbrati eno izmed njih. Izbiram po naslednjem postopku: najprej določim
   * aktualne družine računalnikov. To je project.family, če obstaja, sicer so
   * to vse obstoječe družine. Potem za vse računalnike izbranih družin po vrsti
   * pregledujem datoteke in vrnem PRVO up-todate ali complete ali non-empty
   * datoteko.
   */
  public static String getTaskResultFileName(Project project, String algorithmName, String testsetName, String mType) {
    MeasurementType mt = MeasurementType.UNKNOWN;
    try {
      mt = MeasurementType.valueOf(mType.toUpperCase());
    } catch (Exception e) {
    }

    String familyName = project.getEProject().getProjectFamily(mt);
    EAlgatorConfig config = EAlgatorConfig.getConfig();

    // The families that are to be checked
    TreeSet<String> families = new TreeSet<>();
    // If family is known, check only results for this family ...
    if (familyName != null && !familyName.isEmpty()) {
      families.add(familyName);
    } else { // ... else check all families        
      for (EComputerFamily family : config.getFamilies()) {
        String familyID = family.getField(EComputerFamily.ID_FamilyID);
        if (familyID != null && !familyID.isEmpty()) {
          families.add(familyID);
        }
      }
    }

    int expectedNumberOfLines = 0;
    ETestSet testset = project.getTestSets().get(testsetName);
    if (testset != null) {
      try {
        expectedNumberOfLines = testset.getFieldAsInt(ETestSet.ID_N, 0);
      } catch (Exception e) {
      }
    }

    String firstNonEmptyFile = "", firstCompleteFile = "";
    // loop all families ...
    for (EComputerFamily compFamily : config.getFamilies()) {
      String thisFamilyID = compFamily.getField(EComputerFamily.ID_FamilyID);
      // ... and check all that are in the set "families"
      if (families.contains(thisFamilyID)) {
        String threeFiles[]
                = getTaskResultFilesNameForFamily(project, algorithmName, testsetName, mt, compFamily, expectedNumberOfLines);
        if (!threeFiles[2].isEmpty()) {
          // uptodate file was found!
          return threeFiles[2];
        }
        if (firstNonEmptyFile.isEmpty() && !threeFiles[1].isEmpty()) {
          firstNonEmptyFile = threeFiles[1];
        }
        if (firstCompleteFile.isEmpty() && !threeFiles[0].isEmpty()) {
          firstCompleteFile = threeFiles[0];
        }
      }
    }
    if (!firstCompleteFile.isEmpty()) {
      return firstCompleteFile;
    } else {
      return firstNonEmptyFile;
    }
  }

  /**
   * Po vrsti za vsak računalnik [comp] iz družine [family] pregleda datoteke
   * results/[family].[comp]/algorithm-testset.mtype in tri vrne imena datotek:
   * prvo neprazno datoteko, prvo popolno datoteko in prvi ažurno datoteko, na
   * katero je naletel med pregledom.
   */
  private static String[] getTaskResultFilesNameForFamily(Project project, String algorithmName, String testsetName,
          MeasurementType mType, EComputerFamily compFamily, int expectedNumberOfTests) {

    String firstNonEmptyFile = "", firstCompletFile = "", firstUptodateFile = "";

    String thisFamilyID = compFamily.getField(EComputerFamily.ID_FamilyID);

    // for all computer in given family ...
    for (EComputer computer : EAlgatorConfig.getConfig().getComputers()) {
      // vse računalnike, ki niso v 
      if (!computer.getField(EComputer.ID_FamilyID).equals(compFamily.get(EComputerFamily.ID_FamilyID))) continue;
      
      String computerID = thisFamilyID + "." + computer.getField(EComputer.ID_ComputerID);
      String resultFileName = ATGlobal.getRESULTfilename(project.getEProject().getProjectRootDir(),
              algorithmName, testsetName, mType, computerID);

      // check the corresponding [algorithm]-[testset].[mtype] file 
      File resultFile = new File(resultFileName);
      if (!resultFile.exists()) {
        continue;
      }
      if ((resultFile.length() > 0) && firstNonEmptyFile.isEmpty()) {
        firstNonEmptyFile = resultFileName;
      }

      int numberOfTests = getNumberOfTests(resultFileName);
      if (numberOfTests == expectedNumberOfTests) {
        if (firstCompletFile.isEmpty()) {
          firstCompletFile = resultFileName;
        }

        if (resultsAreUpToDate(project, algorithmName, testsetName, mType.getExtension(), resultFileName)) {
          firstUptodateFile = resultFileName;
          break; // all results are known, method returns
        }
      }

    }

    return new String[]{firstNonEmptyFile, firstCompletFile, firstUptodateFile};
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
   * Returns true is an aray is sorted
   *
   * @param order 1 ... increasing order, -1 ... decreasing order
   * @return
   */
  public static boolean isArraySorted(int tab[], int order) {
    for (int i = 0; i < tab.length - 1; i++) {
      if (order * tab[i] > order * tab[i + 1]) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns a string representation of an array (first 10 elements)
   */
  public static <E> String arrarToString(E[] array) {
    if (array == null) {
      return "null";
    }

    int i;
    String result = "";
    for (i = 0; i < Math.min(10, array.length); i++) {
      if (i > 0) {
        result += ",";
      }
      result += array[i].toString();
    }
    if (i < array.length) {
      result += ", ... (" + array.length + " elements)";
    }
    return "[" + result + "]";
  }

  public static String intArrayToString(int[] array) {
    if (array == null) {
      return "null";
    }

    Integer[] tab = new Integer[array.length];
    for (int i = 0; i < tab.length; i++) {
      tab[i] = array[i];
    }
    return arrarToString(tab);
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
    
    System.out.println(line);
    while (action.find()) {
      String dateL = action.group(1);
      if (dateL == null || dateL.length() < 6) continue;
      
      Date   dateD = new Date(Long.parseLong(dateL)); 
      String dateS = dateFormater.format(dateD);

      line = line.replaceAll(dateL, "\""+ dateS +"\"");
    }
    return line;
  }

}
