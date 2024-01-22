package si.fri.algator.entities;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;
import org.json.JSONArray;
import org.json.JSONObject;
import si.fri.algator.global.ErrorStatus;

/**
 *
 * @author tomaz
 */
public class EPresenterN extends Entity implements Serializable {

  // Entity identifier
  public static final String ID_PresenterParameter = "Presenter";

  // Fields
  public static final String ID_Title      = "Title";           // String
  public static final String ID_ShortDesc  = "ShortTitle";      // String
  public static final String ID_Desc       = "Description";     // String
  
  public static final String ID_Query      = "Query";           // String
  
  public static final String ID_Layout     = "Layout";          // String [][]
 

  public EPresenterN() {
    super(ID_PresenterParameter,
            new String[]{ID_Title, ID_ShortDesc, ID_Desc, ID_Query, ID_Layout});
    setRepresentatives(ID_Title);
  }
  
  public EPresenterN(File fileName) {
    this();
    initFromFile(fileName);
  }   

  @Override
  public ErrorStatus initFromJSON(String json) {
    super.initFromJSON(json); 
    
    try {
      JSONObject jsonO = new JSONObject(json);
      
      JSONArray layout = (JSONArray) get(ID_Layout);
      for (int i = 0; i < layout.length(); i++) {
        JSONArray line = (JSONArray) layout.get(i);
        for (int j = 0; j < line.length(); j++) {
          String viewerName = (String)line.get(j);
          String viewerS = jsonO.get(viewerName).toString();
          Entity viewer = null;
          if (viewerName.startsWith("Graph")) {
            viewer = new EGraphN(viewerS);
          } else if (viewerName.startsWith("Table")) {
            viewer = new ETableN(viewerS);
          }
          if (viewer != null) {
            set(viewerName, new JSONObject(viewer.toJSONString()));
            addFieldName(viewerName);
          }
        }
      }
    } catch (Exception e) {}
    
    return ErrorStatus.STATUS_OK;
  }

  

  public EQuery getQuery() {
    Object queryObject  = getField(ID_Query);
    EQuery result = new EQuery();

    if (queryObject != null) try {
      if (queryObject instanceof JSONObject){
        result.initFromJSON(queryObject.toString());
      } else if (queryObject instanceof String) {
          String qString = entity_rootdir + File.separator + queryObject;
          result.initFromFile(new File(qString));        
      }
     } catch (Exception e) {
        ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR_CANT_INIT_FROM_JSON,
                String.format("Query: %s, Msg: %s", queryObject.toString(), e.toString()));
     }
    return result;
  }
  
  public void setQuery(EQuery query) {
    set(ID_Query, new JSONObject(query.toJSONString()));
  }
}
