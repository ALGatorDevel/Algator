package si.fri.algator.entities;

import java.io.File;
import java.io.Serializable;
import org.json.JSONObject;
import si.fri.algator.global.ATGlobal;
import si.fri.algator.global.ErrorStatus;

/**
 *
 * @author tomaz
 */
public class EPresenter extends Entity implements Serializable {

  // Entity identifier
  public static final String ID_PresenterParameter = "Presenter";

  // Fields
  public static final String ID_Author    = "Author";	        // String
  public static final String ID_Date      = "Date";		// String
  
  public static final String ID_Title      = "Title";           // String
  public static final String ID_ShortDesc  = "ShortTitle";      // String
  public static final String ID_Desc       = "Description";     // String
  
  public static final String ID_Query      = "Query";           // String
  
  public static final String ID_Layout     = "Layout";          // String [][]
 
  // returns an EPresenterN with given name in current data_root folder
  public static EPresenter getPresenter(String projectName, String presenterName) {
    String data_root = ATGlobal.getALGatorDataRoot();
    EPresenter prs = new EPresenter(new File(ATGlobal.getPRESENTERFilename(data_root, projectName, presenterName)));
    prs.set(ID_LAST_MODIFIED, prs.getLastModified(projectName, presenterName));
    return prs;
  } 

  public EPresenter() {
    super(ID_PresenterParameter,
            new String[]{ID_Author, ID_Date, ID_Title, ID_ShortDesc, ID_Desc, ID_Query, ID_Layout});
    setRepresentatives(ID_Title, ID_Author);
    export_name = false;
  }
  
  public EPresenter(File fileName) {
    this();
    initFromFile(fileName);
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
  
  @Override
  public long getLastModified(String projectName, String entityName) {
    String fileName = ATGlobal.getPRESENTERFilename(ATGlobal.getALGatorDataRoot(), projectName, entityName);
    File   prsFile  = new File(fileName);
    return prsFile.lastModified()/1000;
  }  
}
