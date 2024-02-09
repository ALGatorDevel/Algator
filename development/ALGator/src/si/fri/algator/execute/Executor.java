package si.fri.algator.execute;

import java.io.File;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import org.apache.commons.io.FileUtils;
import si.fri.algator.server.ASTask;
import si.fri.algator.server.ASTools;
import si.fri.algator.server.ASTaskStatus;
import si.fri.algator.entities.AlgorithmLanguage;
import si.fri.algator.entities.EAlgorithm;
import si.fri.algator.entities.EGenerator;
import si.fri.algator.entities.EProject;
import si.fri.algator.entities.EResult;
import si.fri.algator.entities.ETestSet;
import si.fri.algator.entities.EVariable;
import si.fri.algator.entities.MeasurementType;
import si.fri.algator.entities.Project;
import si.fri.algator.entities.VariableType;
import si.fri.algator.entities.Variables;
import si.fri.algator.global.ATGlobal;
import si.fri.algator.global.ATLog;
import si.fri.algator.tools.ATTools;
import si.fri.algator.global.ErrorStatus;
import static si.fri.algator.tools.ATTools.getTaskResultFileName;

/**
 *
 * @author tomaz
 */
public class Executor {

  public static ErrorStatus projectMakeCompile(String data_root, String projectName, boolean alwaysCompile) {
    EProject eProject = new EProject(new File(ATGlobal.getPROJECTfilename(data_root, projectName)));    
    return projectMakeCompile(eProject, alwaysCompile);
  }  
  /**
   * Compares the creation date of the files at bin directory and compiles the
   * sources if required (bin files are older than src files or bin files are
   * missing). If alwaysCompile==true, the verification of the date is omitted
   * (the compilations takes place although the classes already exist and are up
   * to date).
   */
  public static ErrorStatus projectMakeCompile(EProject projekt, boolean alwaysCompile) {
    String projRoot = projekt.getProjectRootDir();

    // the classes to be compiled
    String algorithm  = projekt.getAbstractAlgorithmClassname();
    String testCase   = projekt.getTestCaseClassname();
    String input      = projekt.getInputClassname();
    String output     = projekt.getOutputClassname();   
    ArrayList<String> baseSources  = new ArrayList(Arrays.asList(new String[]{algorithm, testCase, input, output}));
    
    // add all IndicatorTest_* ...
    String projectName = projekt.getName();
    Project project = new Project(ATGlobal.getALGatorDataRoot(), projectName);
    Variables indicators = project.getResultDescriptions().get(MeasurementType.EM).getIndicators();
    for (EVariable indicator : indicators) {
      if (!indicator.getType().equals(VariableType.TIMER))
        baseSources.add(ATGlobal.INDICATOR_TEST_OFFSET + indicator.getName());
    }
    // ... and TestCaseGenerator_* classes
    HashMap<String, EGenerator> generators = project.getTestCaseDescription().getGenerators();
    for (String generator : generators.keySet()) {
      baseSources.add("TestCaseGenerator_" + generator);
    }
    
    String[] sources = baseSources.toArray(new String[]{});

    // java src and bin dir
    String projSrc = ATGlobal.getPROJECTsrc(projRoot);
    String projBin = ATGlobal.getPROJECTbin(projekt.getName());

    String missingSource = ATTools.sourcesExists(projSrc, sources);
    if (missingSource != null) {
      return ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR_SOURCES_DONT_EXIST, missingSource);
    }

    // compare the creation date of the files
    if (!alwaysCompile && !ATTools.isSourceNewer(projSrc, projBin, sources)) {
      return ErrorStatus.setLastErrorMessage(ErrorStatus.STATUS_OK,
              String.format("Compile project '%s' - nothing to be done.", projekt.getName()));
    }

    String projJARs = ATTools.buildJARList(projekt.getStringArray(EProject.ID_ProjectJARs), ATGlobal.getPROJECTlib(projRoot));
    for (int i = 0; i < sources.length; i++) sources[i] = sources[i] + ".java";
    ErrorStatus err = ATTools.compile(projSrc, sources,
            projBin, new String[]{}, projJARs, String.format("project '%s'", projekt.getName()));

    return ErrorStatus.setLastErrorMessage(err, "");
  }

  public static ErrorStatus algorithmMakeCompile(String data_root, String projName, String algName, MeasurementType mType, boolean alwaysCompile) {
    String projectRoot    = ATGlobal.getPROJECTroot(data_root, projName);
    EProject eProject     = new EProject  (new File(ATGlobal.getPROJECTfilename(data_root, projName)));    
    EAlgorithm eAlgorithm = new EAlgorithm(new File(ATGlobal.getALGORITHMfilename(projectRoot, algName)));    
    
    return algorithmMakeCompile(eProject, eAlgorithm, mType, alwaysCompile);
  }

  /**
   * Compares the creation date of the files at src and bin folder and compiles
   * the sources if required (bin files are older than src files or bin files
   * are missing). If alwaysCompile==true, the verification of the date is
   * omitted (the compilations takes place although the classes already exist
   * and are up to date).
   */
  public static ErrorStatus algorithmMakeCompile(EProject eProjekt, EAlgorithm eAlgorithm, MeasurementType mType, boolean alwaysCompile) {
    String projRoot = eProjekt.getProjectRootDir();

    String projBin = ATGlobal.getPROJECTbin(eProjekt.getName());

    String algName = eAlgorithm.getName();

    String algSrc   = ATGlobal.getALGORITHMsrc(projRoot, algName);
    String algBin   = ATGlobal.getALGORITHMbin(eProjekt.getName(), algName);
    String algClass = eAlgorithm.getAlgorithmClassname();

    if (mType.equals(MeasurementType.CNT)) {
      testAndCreateCOUNTClass(algSrc, algClass);
      algClass += ATGlobal.COUNTER_CLASS_EXTENSION;
    }

    // test for sources
    String missingSource = ATTools.sourcesExists(algSrc, new String[]{algClass});
    if (missingSource != null) {
      return ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR_SOURCES_DONT_EXIST, missingSource);
    }

    // compare the creation date of the files
    if (!alwaysCompile && !ATTools.isSourceNewer(algSrc, algBin, new String[]{algClass})) {
      return ErrorStatus.setLastErrorMessage(ErrorStatus.STATUS_OK,
              String.format("Compiling algorithm  '%s' - nothing to be done.", algName));
    }

    String algJARs = ATTools.buildJARList(eProjekt.getStringArray(EProject.ID_AlgorithmJARs), ATGlobal.getPROJECTlib(projRoot));
    ErrorStatus err = ATTools.compile(algSrc, new String[]{algClass + ".java"},
            algBin, new String[]{projBin}, algJARs, String.format("algorithm '%s'", algName));

    return err;
  }

  /**
   * Create a new java class in which all //@COUNT{cnt_name, value} are replaced
   * with Counters.addToCounter(cnt_name, value) commands. The new class is
   * placed to the same src folder where the original class resides.
   */
  static void testAndCreateCOUNTClass(String classRoot, String className) {
    try {
      String newClassName = className + ATGlobal.COUNTER_CLASS_EXTENSION;
      File classFile = new File(classRoot + File.separator + className + ".java");
      File newClassFile = new File(classRoot + File.separator + newClassName + ".java");

      if (newClassFile.exists() && FileUtils.isFileNewer(newClassFile, classFile)) {
        return;
      }

      String classContent = "";
      Scanner sc = new Scanner(classFile);
      while (sc.hasNextLine()) {
        // skip all the lines that contain @REMOVE_LINE tag
        String line = sc.nextLine();

        if (line.contains("//@REMOVE_LINE")) {
          continue;
        }

        classContent += line + "\n";
      }
      sc.close();

      classContent = "import si.fri.algator.execute.Counters;\n" + classContent;
      classContent = classContent.replaceAll("class +"+className, "class "+newClassName);
      classContent = classContent.replaceAll("//\\@COUNT\\{(.*),(.*)\\}", "Counters.addToCounter(\"$1\", $2);");

      PrintWriter pw = new PrintWriter(newClassFile);
      pw.println(classContent);
      pw.close();
    } catch (Exception e) {
    }
  }

  /**
   * Runs the algorithm over all the tests from the given test set. The method
   * first checks if the if the project and the algorithm has to be compiled. If
   * source files are newer than the classes it compiles the project and/or the
   * algorithm.
   *
   * @param projectsRoot path to the projects folder
   * @param projName the name of the project
   * @param algName the name of the algorithm
   * @param testSetName the name of the test set
   * @param notificator a notificator
   * @param alwaysCompile if true the sources are compiled although the classes
   * are up to date.
   * @param alwaysRun run the algorithm although the results already exist; if
   * algorithm runs sucessfully, the existing results are overwriten.
   * @return
   */
  public static ErrorStatus algorithmRun(Project project, String algName, String testSetName,
          MeasurementType mType, Notificator notificator, boolean alwaysCompile, 
          boolean alwaysRun, int whereToPrint, boolean asJSON, ASTask task) {

    if (project == null) {
      return ErrorStatus.ERROR;
    }
    
    String runningMsg = String.format("Running [%s, %s, %s]", mType.getExtension(), testSetName, algName);
    ErrorStatus.setLastErrorMessage(ErrorStatus.STATUS_OK, "");
    ATLog.log(runningMsg, 3);

    String projRoot = project .getProjectRoot();

    EResult resDesc = project.getResultDescriptions().get(mType);
    if (resDesc == null) {
      return ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR_INVALID_RESULTDESCRIPTION, "");
    }

    EAlgorithm eAlgorithm = project.getAlgorithms().get(algName);
    if (eAlgorithm == null) {
      return ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR_INVALID_ALGORITHM, "");
    }

    ETestSet eTestSet = project.getTestSets().get(testSetName);
    if (eTestSet == null) {
      return ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR_INVALID_TESTSET, "");
    }

    int numberOfInstances = eTestSet.getFieldAsInt(ETestSet.ID_N);
    String mt = mType.getExtension().toUpperCase();
    String resultFileName = getTaskResultFileName(project, algName, testSetName, mt);

    boolean resultsAreUptodate = ATTools.resultsAreUpToDate(project, algName, testSetName, mt, resultFileName);
    boolean resultsAreComplete = ATTools.resultsAreComplete(resultFileName, numberOfInstances);
    if (!(alwaysRun || alwaysCompile) && resultsAreUptodate && resultsAreComplete) {
      return ErrorStatus.setLastErrorMessage(ErrorStatus.STATUS_OK, runningMsg + " - nothing to be done.");
    }
    

    if (eAlgorithm.getLanguage().equals(AlgorithmLanguage.C)) { // C algorithm
      if (!mType.equals(MeasurementType.EM)) {
        return ErrorStatus.setLastErrorMessage(ErrorStatus.STATUS_OK,  "   ... can not run C algorith in " + mType.getExtension() + " mode.");
      }
      
      int testRepeat = eTestSet.getFieldAsInt(ETestSet.ID_TestRepeat, 1);
      
      int timeLimit  = eTestSet.getFieldAsInt(ETestSet.ID_TimeLimit, 10);
      if (timeLimit <=0) timeLimit = 10; // default timeLimit: 10 sec per test
                
      CExecutor.runWithLimitedTime(project.getName(), algName, testSetName, mType, testRepeat*timeLimit, numberOfInstances);
        
    } else {    //java
    
      if (projectMakeCompile(project.getEProject(), alwaysCompile) != ErrorStatus.STATUS_OK) {
        return ErrorStatus.getLastErrorStatus();
      }

      if (algorithmMakeCompile(project.getEProject(), eAlgorithm, mType, alwaysCompile) != ErrorStatus.STATUS_OK) {
        return ErrorStatus.getLastErrorStatus();
      }

      URL[] urls = New.getClassPathsForProjectAlgorithm(project, algName);
      String currentJobID = New.generateClassloaderAndJobID(urls);
      
      AbstractTestSetIterator tsIt = new DefaultTestSetIterator(project, eTestSet, currentJobID);
      tsIt.initIterator();

      notificator.setNumberOfInstances(numberOfInstances);

      // prepare file for results
      File resFile;
      try {
        String resFilename = ATGlobal.getRESULTfilename(projRoot, algName, testSetName, mType, ATGlobal.getThisComputerID());

        ATTools.createFilePath(resFilename);        
        resFile = new File(resFilename);
        
        // Če bom želel, da datoteke  ne brišem (da nove rezultate (ponovne rezultate testov) dodajam na konec;
        // ko berem datoteko, se odločim, katere rezultate bom uporabil (prve, zadnje, najboljše, vse, ...))
        // potem moram zakomentirati spodnjo vrstico
        if (resFile.exists()) resFile.delete();
        
      } catch (Exception e) {
        New.removeClassLoader(currentJobID);
        return ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR_CANT_RUN, e.toString());
      }


      try {
        if (mType.equals(MeasurementType.EM) || mType.equals(MeasurementType.CNT)) 
          ExternalExecutor.iterateTestSetAndRunAlgorithm(project, algName, currentJobID, tsIt, notificator, mType, resFile, whereToPrint, asJSON, task);
        else
          VMEPExecutor.iterateTestSetAndRunAlgorithm(project, algName, currentJobID, testSetName, resDesc, tsIt, notificator, resFile, asJSON);
      } catch (Exception e) {
        ASTools.logTaskStatus(task,  ASTaskStatus.FAILED, ErrorStatus.ERROR_CANT_RUN.toString(), ATGlobal.getThisComputerID());
        return ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR_CANT_RUN, e.toString());
      } finally {
        New.removeClassLoader(currentJobID);
      }
    } // end java execution
      

    ErrorStatus lastError = ErrorStatus.getLastErrorStatus();
    if (lastError.isOK()) {  
      if (task != null) ASTools.logTaskStatus(task,  ASTaskStatus.COMPLETED, null, ATGlobal.getThisComputerID());
      return ErrorStatus.setLastErrorMessage(ErrorStatus.STATUS_OK, runningMsg + " - done.");
    } else  {// execution failed or was paused
      if (task != null)  ASTools.logTaskStatus(task,  lastError.taskWasQueued() ? ASTaskStatus.QUEUED : ASTaskStatus.FAILED, lastError.toString(), ATGlobal.getThisComputerID());
      return lastError;
    } 
  }
}
