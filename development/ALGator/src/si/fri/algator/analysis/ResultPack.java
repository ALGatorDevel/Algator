package si.fri.algator.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import si.fri.algator.entities.EResult;
import si.fri.algator.entities.Variables;

/**
 * Objects of this class contain a map of results (key=something describing test 
 * (i.e. alg-testset-test or testset-test or just test)  value=ParameterSet for 
 * this test) and a resultDescrition to describe the parameters in ResultsSets)
 * @author tomaz
 */
public class ResultPack {
  private HashMap<String, Variables> results;
  EResult resultDescription;
  
  ArrayList<String> keyOrder;

  public ResultPack() {
    results = new HashMap<>();
    resultDescription = new EResult();
    keyOrder = new ArrayList<>();
  }
  
  public void putResult(String key, Variables value) {
    results.put(key, value);
    keyOrder.add(key);
  }

  public Variables getResult(String key) {
    return results.get(key);
  }
  
  @Override
  public String toString() {
    String res = "";
    for (String key : results.keySet()) {
      res += key+" : "+results.get(key) + "\n";
    }
    return res;
  }
  
  
}
