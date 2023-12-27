package si.fri.algator.entities;

import java.io.File;
import org.json.JSONArray;

/**
 *
 * @author tomaz
 */
public class EQuery extends Entity {
  // Entity identifier
  
  public static final String ID_Query = "Query";

  //Fields
  public static final String ID_Description    = "Description";	     // String
  public static final String ID_Algorithms     = "Algorithms";       // NameAndAbrev []
  public static final String ID_TestSets       = "TestSets";         // NameAndAbrev []
  public static final String ID_Parameters     = "Parameters";       // String []
  public static final String ID_Indicators     = "Indicators";       // String []
  public static final String ID_GroupBy        = "GroupBy";          // String []
  public static final String ID_Filter         = "Filter";           // String []
  public static final String ID_SortBy         = "SortBy";           // String []
  public static final String ID_Count          = "Count";            // String (1-true, other-false)
  public static final String ID_ComputerID     = "ComputerID";       // ID of computer that provides the results file; if null or "", the most suitable result file is selected
  
  public EQuery() {
   super(ID_Query, 
	 new String [] {ID_Description, ID_Algorithms, ID_TestSets, ID_Parameters, ID_Indicators, 
                        ID_GroupBy, ID_Filter, ID_SortBy, ID_Count, ID_ComputerID});
   setRepresentatives(ID_Algorithms, ID_TestSets);
  }
  
  public EQuery(String [] algs, String [] tsts, String [] inParams, String [] outParams,
                String [] groupby, String [] filter, String [] sortby, String count, 
                String computerID) {
    this();
    
    set(ID_Algorithms,    new JSONArray(algs));
    set(ID_TestSets,      new JSONArray(tsts));
    set(ID_Parameters,  new JSONArray(inParams));
    set(ID_Indicators, new JSONArray(outParams));
    set(ID_GroupBy,       new JSONArray(groupby));
    set(ID_Filter,        new JSONArray(filter));
    set(ID_SortBy,        new JSONArray(sortby));
    set(ID_Count,         count);
    set(ID_ComputerID,    computerID);
  }
  
  
  public EQuery(File fileName) {
    this();
    initFromFile(fileName);
  }
  
  public EQuery(File fileName, String [] params) {
    this();
    
    entityParams = params;    
    initFromFile(fileName);
  }
  
  public EQuery(String json, String [] params) {
    this();
    
    entityParams = params;    
    initFromJSON(json);
  }
  
  /**
   * Return an aray of NameAndAbrev for parameter of type String [] with vaules 
   * of form "name as abrev".
   * Note: algorithms and testsets are given in json file as array of string 
   * values of form "name as abrev". 
   * @param id ID of parameter (i.e. ID_Algorithms)
   * @return 
   */
  public NameAndAbrev [] getNATabFromJSONArray(String [] entities) {    
    NameAndAbrev [] result = new NameAndAbrev[entities.length];
    for (int i = 0; i < entities.length; i++) {
      result[i] = new NameAndAbrev(entities[i]);
    }
    
    return result;
  }

    public NameAndAbrev[] getNATabFromJSONArray(String id) {
        String[] entities = getStringArray(id);
    return getNATabFromJSONArray(entities);
  }
  
  /**
   * Method produces an json array of string of form "name as abrev" from a given 
   * array of NameAndAbrev entities.
   */
  public void setJSONArrayFromNATab(NameAndAbrev [] entities, String id) {
    String [] strEntities = new String[entities.length];
    for (int i = 0; i < entities.length; i++) {
      strEntities[i] = entities[i].toString();
    }
    JSONArray jTab = new JSONArray(strEntities);
    set(id, jTab);
  }
  
  public boolean isCount() {
    Object count = get(ID_Count);
    return (count != null && count.equals("1"));
  }
  
  public void applyParameters(String [] parameters) {
    
  }
  
  public String getCacheKey() {
    Object algs = get(ID_Algorithms);
    Object ts = get(ID_TestSets);
    Object params = get(ID_Parameters);
    Object indicators = get(ID_Indicators);
    Object compId = get(ID_ComputerID);
    return ((algs == null ? "" : algs.toString())
            + (ts == null ? "" : ts.toString())
            + (params == null ? "" : params.toString())
            + (indicators == null ? "" : indicators.toString())
            + ((get(ID_Count) == null) ? "" : get(ID_Count).toString())
            + (compId == null ? "" : compId.toString()));
  }
  
}
