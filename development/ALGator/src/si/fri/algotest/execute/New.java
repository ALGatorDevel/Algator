package si.fri.algotest.execute;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.HashSet;
import si.fri.algotest.entities.EProject;
import si.fri.algotest.entities.MeasurementType;
import si.fri.algotest.entities.Project;
import si.fri.algotest.global.ATGlobal;
import si.fri.algotest.global.ATLog;
import si.fri.algotest.tools.ATTools;

/**
 *
 * @author tomaz
 */
public class New {

  // For a given pair (project, algorithm) we always use the same loader 
  private static final HashMap<String, URLClassLoader> classloaders = new HashMap<>();

  public static URL[] getClassPathsForProject(Project project) {
    return getClassPathsForAlgorithm(project, "");
  }
      
  
    public static URL[] getClassPathsForAlgorithm(Project project, String algName) {
      String projBin = ATGlobal.getPROJECTbin(project.getName());
      
      String algBin  = algName.isEmpty() ? "" : ATGlobal.getALGORITHMbin(project.getName(), algName);

      URL[] proJARs  = ATTools.getURLsFromJARs(project.getEProject().getStringArray(EProject.ID_ProjectJARs), ATGlobal.getPROJECTlib(project.getEProject().getProjectRootDir()));
      URL[] algJARs  = ATTools.getURLsFromJARs(project.getEProject().getStringArray(EProject.ID_AlgorithmJARs), ATGlobal.getPROJECTlib(project.getEProject().getProjectRootDir()));
      
      URLClassLoader parentclassLoader = (URLClassLoader) Thread.currentThread().getContextClassLoader();
      URL[] parentURLs = parentclassLoader.getURLs();

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
        if (!algBin.isEmpty()) urls[stevec++] = new File(algBin).toURI().toURL();
      } catch (Exception e) {
      }
      
      return urls;
  }

  
  
  /*
  Opomba: Prvotna verzija programa je uporabljala metodo getCLassLoader(projekt, algoritm),
    ki je za vsak par projekt-algoritm ustvarila NOV classloader. To se ni obneslo, saj je 
    vmep pri tem javljal napako: ClassCastException (kot da bi bila AbstractTestsetIterator
    in npr. SortTestsetIterator naložena z drugim nalagalnikom). Javanska verzija programa 
    (če se algator požene z običajno javo) je delala brez problemov. 
    Da sem odpravil to težavo, ves čas uporabljam ISTI nalagalnik, le classpath mu po potrebi
    dopolnjujem. V množici pathsAdded si zapomnim, katere poti sem že dodal in jih ob nadaljnjih 
    izvajanjih ne dodajam ponovno (brez tega je program delal bistveno bolj počasi).
  */
  
  private static HashSet<String> pathsAdded = new HashSet<String>();
  public static ClassLoader getClassloader(URL [] urls) throws Exception {    
    Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
    method.setAccessible(true);
    for (int i = 0; i < urls.length; i++) {
      if (!pathsAdded.contains(urls[i].toString())) {
        method.invoke(ClassLoader.getSystemClassLoader(), new Object[]{urls[i]});
        pathsAdded.add(urls[i].toString());
      }
    }
    return ClassLoader.getSystemClassLoader();
  }
  
  /**
   * Metoda se trenutno ne uporablja - glej komentar pri metodi getClassLoader(URL [] url)
   */
  private static URLClassLoader getClassloader(Project project, String algName) {
    String key = project.getName() + "+" + algName;
    URLClassLoader result = classloaders.get(key);

    if (result == null) {
      try {
        URL[] urls = getClassPathsForAlgorithm(project, algName);
        classloaders.put(key, (result = URLClassLoader.newInstance(urls)));
      } catch (Exception e) {
        ATLog.log("Error creating class loader: " + e.toString(), 2);
      }
    }
    return result;
  }

  public static AbstractAlgorithm algorithmInstance(Project project, String algName, MeasurementType mType) {
    AbstractAlgorithm result = null;
    try {
      // ... glej opombo zgoraj pri getClassLoader
      // ClassLoader classLoader = getClassloader(project, algName);
      ClassLoader classLoader = getClassloader(getClassPathsForAlgorithm(project, algName));
      
      String algClassName = project.getAlgorithms().get(algName).getAlgorithmClassname();

      if (mType.equals(MeasurementType.CNT)) {
        algClassName += ATGlobal.COUNTER_CLASS_EXTENSION;
      }

      Class algClass = Class.forName(algClassName, true, classLoader);
      result = (AbstractAlgorithm) algClass.newInstance();
      
      result.setmType(mType);
    } catch (Exception e) {
      ATLog.log("Can't make an instance of algorithm " + algName + ": " + e, 2);
    }
    return result;
  }

  
  public static AbstractTestCase testCaseInstance(Project project) {
    AbstractTestCase result = null;
    try {
      ClassLoader classLoader = getClassloader(getClassPathsForProject(project));
      
      String testCaseClassName
              = project.getEProject().getTestCaseClassname();
      Class tcClass = Class.forName(testCaseClassName, true, classLoader);
      result = (AbstractTestCase) tcClass.newInstance();
    } catch (Exception e) {
      ATLog.log("Can't make an instance of TestCase for project " + project.getName() + ": " + e, 2);
    }
    return result;
  }
  

}
