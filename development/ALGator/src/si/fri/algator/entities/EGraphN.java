package si.fri.algator.entities;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.TreeSet;

/**
 *
 * @author tomaz
 */
public class EGraphN extends Entity implements Serializable {

  // Entity identifier
  public static final String ID_GraphParameter = "Graph";

  // Fields
  public static final String ID_ShortDesc    = "ShortTitle";     // String
  public static final String ID_Desc         = "Description";    // String  
  public static final String ID_Xaxis        = "xAxis";          // String
  public static final String ID_Yaxes        = "yAxes";          // String []
  public static final String ID_GraphTypes   = "graphTypes";     // GraphType [] (coma separated string)
  public static final String ID_XaxisTitle   = "xAxisTitle";     // String
  public static final String ID_YaxisTitle   = "yAxisTitle";     // String (1-true, other-false)
  public static final String ID_categoryList = "categoryLabels"; // boolean
  public static final String ID_gridX        = "gridX";          // boolean
  public static final String ID_gridY        = "gridY";          // boolean
  public static final String ID_logscale     = "logScale";       // boolean 
  public static final String ID_manData      = "manData";        // json
  public static final String ID_subchart     = "subchart";       // boolean 
  public static final String ID_zoom         = "zoom";           // boolean

  public EGraphN() {
    super(ID_GraphParameter,
            new String[]{ID_ShortDesc, ID_Desc, ID_Xaxis, ID_Yaxes, ID_GraphTypes, ID_XaxisTitle, ID_YaxisTitle,
            ID_categoryList, ID_gridX, ID_gridY, ID_logscale, ID_manData, ID_subchart, ID_zoom});
    setRepresentatives(ID_ShortDesc);
    export_name = false;
  }

  public EGraphN(String json) {
    this();
    initFromJSON(json);
  }
  
  public EGraphN(File fileName) {
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
  
}
