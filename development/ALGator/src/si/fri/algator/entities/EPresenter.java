package si.fri.algator.entities;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.TreeSet;
import org.json.JSONArray;
import org.json.JSONObject;
import si.fri.algator.global.ErrorStatus;

/**
 *
 * @author tomaz
 */
public class EPresenter extends Entity implements Serializable {

  // Entity identifier
  public static final String ID_PresenterParameter = "Presenter";

  // Fields
  public static final String ID_Title      = "Title";           // String
  public static final String ID_ShortDesc  = "ShortTitle";      // String
  public static final String ID_Desc       = "Description";     // String
  public static final String ID_Query      = "Query";           // String
  public static final String ID_HasGraph   = "HasGraph";        // String (1-true, other-false)
  public static final String ID_Xaxis      = "xAxis";           // String
  public static final String ID_Yaxes      = "yAxes";           // String []
  public static final String ID_GraphTypes = "GraphTypes";      // GraphType [] (coma separated string)
  public static final String ID_XaxisTitle = "xAxisTitle";      // String
  public static final String ID_YaxisTitle = "yAxisTitle";      // String (1-true, other-false)
  public static final String ID_HasTable   = "HasTable";        // String
  public static final String ID_Columns    = "Columns";         // String []

  public EPresenter() {
    super(ID_PresenterParameter,
            new String[]{ID_Title, ID_ShortDesc, ID_Desc, ID_Query, ID_HasGraph, ID_Xaxis, ID_Yaxes,
              ID_GraphTypes, ID_XaxisTitle, ID_YaxisTitle, ID_HasTable, ID_Columns});

    setRepresentatives(ID_Title);
  }

  public EPresenter(String json) {
    this();
    initFromJSON(json);
  }
  
  public EPresenter(File fileName) {
    this();
    initFromFile(fileName);
  }   

  public TreeSet<GraphType> getGraphTypes() {
    TreeSet<GraphType> result = new TreeSet();
    String[] gtype = getStringArray(ID_GraphTypes);
    for (int i = 0; i < gtype.length; i++) {
      try {
        GraphType gt = GraphType.getType(gtype[i]);
        if (gt != null) {
          result.add(gt);
        }
      } catch (Exception e) {
      }
    }
    return result;
  }

  public void setGraphTypes(TreeSet<GraphType> gTypes) {
    ArrayList<String> types = new ArrayList();
    for (GraphType gType : gTypes) {
      types.add(gType.toString());
    }
    set(ID_GraphTypes, types.toArray());
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
