package si.fri.algator.execute;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import si.fri.algator.entities.EProject;
import si.fri.algator.entities.MeasurementType;
import si.fri.algator.entities.Project;
import si.fri.algator.global.ATGlobal;
import si.fri.algator.global.ATLog;
import si.fri.algator.tools.ATTools;
import si.fri.algator.tools.UniqueIDGenerator;

/**
 *
 * @author tomaz
 */
public class New {

  // classPath that includes ALGator paths (ALGator.jar + *.jar) + 
  // project paths (proj/bin/* + *.jar) + algorithm paths (ALG/bin/* + *.jar) 
  public static URL[] getClassPathsForProjectAlgorithm(Project project, String algName) {
    String projBin = ATGlobal.getPROJECTbin(project.getName());

    String algBin = algName.isEmpty() ? "" : ATGlobal.getALGORITHMbin(project.getName(), algName);

    URL[] proJARs = ATTools.getURLsFromJARs(project.getEProject().getStringArray(EProject.ID_ProjectJARs), ATGlobal.getPROJECTlib(project.getEProject().getProjectRootDir()));
    URL[] algJARs = ATTools.getURLsFromJARs(project.getEProject().getStringArray(EProject.ID_AlgorithmJARs), ATGlobal.getPROJECTlib(project.getEProject().getProjectRootDir()));

    String pathSeparator = System.getProperty("path.separator");
    String[] classPathEntries = System.getProperty("java.class.path").split(pathSeparator);
    URL[] parentURLs = new URL[classPathEntries.length];
    try {
      for (int i = 0; i < classPathEntries.length; i++) {
        parentURLs[i] = new URL("file", "", classPathEntries[i]);
      }
    } catch (Exception e) {
    }

    URL[] urls = new URL[parentURLs.length + 1 + (algBin.isEmpty() ? 0 : 1) + proJARs.length + algJARs.length];
    int stevec = 0;

    for (int j = 0; j < parentURLs.length; j++) {
      urls[stevec++] = parentURLs[j];
    }
    for (int j = 0; j < proJARs.length; j++) {
      urls[stevec++] = proJARs[j];
    }
    for (int j = 0; j < algJARs.length; j++) {
      urls[stevec++] = algJARs[j];
    }
    try {
      urls[stevec++] = new File(projBin).toURI().toURL();
      if (!algBin.isEmpty()) {
        urls[stevec++] = new File(algBin).toURI().toURL();
      }
    } catch (Exception e) {
    }

    return urls;
  }

  // Slovar vseh trenutno aktivnih classloaderjev. 
  // Za vsako "job" (job = kakrsnokoli izvajanje algoritma - za en test, za več testov, za timelimit, ...)
  // se ustvari nov classloader; dokler poteka ta job, classloader nalaga razrede, ki jih potrebujemo;
  // ko se job zaključi, se uniči tudi classloader (na ta način poskrbim, da se ob daljši uporabi programa
  // (recimo znotraj ALGatorServerja) število classloaderjev ne namnoži preveč); da ima vsak job svoj classloader
  // pa zagotovi, da se bodo ob morebitni spremembi konfiguracije projekta razredi prav nalagali (če bi imel, na 
  // primer, en class loader za par (algoritm, projekt), bi se ob spremembi algoritma  nov razred 
  // ne osvežil, saj bi classloader rekel, da je razred že naložen); 
  private static final HashMap<String, ClassLoader> classLoaders = new HashMap();

  /**
   * Ob začetku joba ustvarim classloader in unique ID, s katerim se kasneje sklicujem
   * na ta class loader.
   * 
   * @param urls vsi URLji, ki jih potrebujem za izvajanje joba (urlja dobim z metodo getClassPathsForProjectAlgorithm())
   * @return vrne ID, preko katerega pridobim classloader za nalaganje razredov tega joba
   */
  public static String generateClassloaderAndJobID(URL[] urls) {
    String id = UniqueIDGenerator.getNextID();
    try {
      classLoaders.put(id, new URLClassLoader(urls));
    } catch (Exception e) {}
    return id;
  }
  
  public static ClassLoader getClassloader(String currentJobID) throws Exception {
    if (classLoaders.get(currentJobID) == null) {
      classLoaders.put(currentJobID, new URLClassLoader(new URL[]{}));
    }
    return classLoaders.get(currentJobID);
  }
  
  public static void removeClassLoader(String currentJobID) {
    classLoaders.remove(currentJobID);
  }
  

  public static AbstractAlgorithm algorithmInstance(String currentJobID, String algClassName, MeasurementType mType) {
    AbstractAlgorithm result = null;
    try {
      ClassLoader classLoader = getClassloader(currentJobID);

      if (mType.equals(MeasurementType.CNT)) {
        algClassName += ATGlobal.COUNTER_CLASS_EXTENSION;
      }

      Class algClass = Class.forName(algClassName, true, classLoader);
      result = (AbstractAlgorithm) algClass.newInstance();

      result.setmType(mType);
    } catch (Exception e) {
      ATLog.log("Can't make an instance of algorithm " + algClassName + ": " + e, 2);
    }
    return result;
  }

  public static AbstractTestCase testCaseInstance(String currentJobID, String testCaseClassName) {
    AbstractTestCase result = null;
    try {
      ClassLoader classLoader = getClassloader(currentJobID);     
      Class tcClass = Class.forName(testCaseClassName, true, classLoader);
      result = (AbstractTestCase) tcClass.newInstance();
    } catch (Exception e) {
      ATLog.log("Can't make an instance of TestCase " + testCaseClassName + ": " + e, 2);
    }
    return result;
  }
}
