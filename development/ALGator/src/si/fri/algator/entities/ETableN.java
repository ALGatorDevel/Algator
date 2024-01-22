package si.fri.algator.entities;

import java.io.File;
import java.io.Serializable;

/**
 *
 * @author tomaz
 */
public class ETableN extends Entity implements Serializable {

  // Entity identifier
  public static final String ID_TableParameter = "Table";

  // Fields
  public static final String ID_ShortDesc       = "ShortTitle";      // String
  public static final String ID_Desc            = "Description";     // String  
  public static final String ID_Columns         = "Columns";         // String []
  public static final String ID_sortableColumns = "sortableColumns"; // boolean
  public static final String ID_sortBy          = "sortBy";          // String

  
  public ETableN() {
    super(ID_TableParameter,
            new String[]{ID_ShortDesc, ID_Desc, ID_Columns, ID_sortableColumns, ID_sortBy});
    setRepresentatives(ID_ShortDesc);
    export_name = false;
  }

  public ETableN(String json) {
    this();
    initFromJSON(json);
  }
  
  public ETableN(File fileName) {
    this();
    initFromFile(fileName);
  }   
}
