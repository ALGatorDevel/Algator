package si.fri.algator.execute;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import si.fri.algator.entities.EResult;
import si.fri.algator.entities.ETestSet;
import si.fri.algator.entities.EVariable;
import si.fri.algator.entities.Project;
import si.fri.algator.global.ATLog;
import si.fri.algator.global.ErrorStatus;
import si.fri.algator.tools.UniqueIDGenerator;

/**
 * DefaultTestSetIterator provides the methods to iterate throught  testsets in which 
 * each line of description file describes exactly one testcase. 
 * The project-dependant testsetiterator that extends this 
 * class has only to implement the getCurrent() method, which parses the current 
 * input line (currentInputLine) and produces corresponding testcase.    
 * @author tomaz
 */
public class DefaultTestSetIterator  extends AbstractTestSetIterator {

  private   Scanner inputSource;       // scanner used to iterate throught the Description file
  protected String  testFileName;      // the name of the file this iterator reads from
  protected String  filePath;          // path of a description file
  
  protected int     lineNumber;        // the number of the current line
  protected String  currentInputLine;  // the current input line 


  AbstractTestCase  testCaseInstance;
  
  String currentJobID; // ID of a current job (used to obtain the classloader for jobs classes)
  
  public DefaultTestSetIterator(Project project, ETestSet testSet, String currentJobID) {
    super(project, testSet);
    
    this.currentJobID = currentJobID;
  }

      
  
  // This method is used to report an error in input file
  protected void reportInvalidDataFormat() {
    // to sem odstranil, ker se sicer napaka kopičijo (nihče jih ne postavi na "", 
    // spodnji spstem pa jih samo dodaja, po nekaj klicih dobiš " sdfd (sdfsd (sdas (....))))"
    //String oldMsg = ErrorStatus.getLastErrorMessage();
    String msg = String.format("Invalid input data in file %s in line %d.", testFileName, lineNumber);
    //if (ErrorStatus.getLastErrorStatus()!=ErrorStatus.STATUS_OK && !oldMsg.isEmpty())
    //  msg += " ("+oldMsg+")";
    
    ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR,msg);
  }

  @Override
  /**
   * The constructor of this class was changed and initIterator() has to be called manually!
   */
  public void initIterator() {
    if (testSet != null) {
      String fileName = testSet.getTestSetDescriptionFile();

      try {
        if (fileName == null) 
	  throw new Exception("Testset descritpion file does not exist.");
      
        filePath = testSet.entity_rootdir;      
        testFileName = filePath + File.separator + fileName;
      
        inputSource = new Scanner(new File(testFileName));
	lineNumber=0;        
      } catch (Exception e) {
	inputSource = null;
        ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR_INVALID_TESTSET, e.toString());
      } 
    }
  }

  @Override
  public boolean hasNext() {
    return (inputSource != null && inputSource.hasNextLine());
  }
  
  @Override
  public void readNext() {
    try {
      if (inputSource == null){
        ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR, "No more input to read!");
        return;
      }
      
      currentInputLine = null;             
      while (inputSource.hasNextLine()) {
        currentInputLine = inputSource.nextLine(); lineNumber++;
        
        if (!currentInputLine.startsWith("#")) break;        
      }
    } catch (Exception e) {
       ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR, "Can not read next line: " + e.toString());      
       currentInputLine="";
    }
  }

  @Override
  public boolean readTest(int testNumber) {
    if (lineNumber > testNumber) {
      try {
        inputSource.close();
        inputSource = new Scanner(new File(testFileName));
	lineNumber=0;
      } catch (Exception e) {
	inputSource = null;	
	ATLog.log("Error reading test with number " + testNumber+ "! Error: " + e.toString(), 2);
        return false;
      } 
    }
    
    while (testNumber > lineNumber && hasNext())
      readNext();
    
    return testNumber == lineNumber;
  }

  @Override
  public AbstractTestCase getCurrent() {
    AbstractTestCase testCase = null;
    try {
      String testCaseClassName = project.getEProject().getTestCaseClassname();
      
      if (testCaseInstance == null)
        testCaseInstance = New.testCaseInstance(currentJobID, testCaseClassName);
      
      testCase = testCaseInstance.getTestCase(project, currentInputLine, filePath);
      if (testCase == null) {
        reportInvalidDataFormat();
        return null;
      }  
      
      // iterator labels each test with an unique label (which will probably be overriden by callers)      
      EVariable iID = EResult.getInstanceIDParameter(UniqueIDGenerator.getNextID());
      testCase.getInput().getParameters().addVariable(iID, true);                              
    } catch (Exception e) {
      ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR, "can not create testCase instance");
      reportInvalidDataFormat();  
    }
    return testCase;   
  }
 
  
    
 @Override
  public void close() throws IOException {
    inputSource.close();
  }
  

}
