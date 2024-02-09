package si.fri.algator.entities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.TreeSet;
import org.json.JSONArray;
import org.json.JSONObject;
import si.fri.algator.tools.ATTools;
import si.fri.algator.global.ErrorStatus;

/**
 * Entity represents a low-level class needed to implement an entity like
 * ATProject, ATAlgorithm, ATParameter, ... Entity loads an entity from a file
 * or from a JSON string. The fields of an entity are set through the
 * constructor's fieldName parameter. The method initFromJSON reads all these
 * fields from JSON description of an entity and fills the map with correspnding
 * values. To use the vaules of the fileds in a subclass of Entity the user can
 * either use the get(set)String, get(set)Integer, get(set)Double,
 * get(set)StringArray methods or write his own getters and setters.
 *
 * @author tomaz
 */
public class Entity implements Cloneable, Serializable {

  public static final String unknown_value = "?";

  // The name of the entity (property Name)
  public static final String ID_NAME = "Name";

  /**
   * The ID of an entity in an JSON file
   */
  protected String entity_id;

  /**
   * The folder of the entity file (if entity was read from a file), null
   * otherwise
   */
  public String entity_rootdir;

  /**
   * The value of ID_NAME property. If this property does not exist, the prefix
   * of the name of the file from which the entity was read (if entity was read
   * from a file), unknown_value otherwise
   */
  private String entity_name;

  // if this is set to true, JSON representation of this entity includes the 'name' property
  protected boolean export_name = true;

  /**
   * The of the file from which the entity was read, or null if it was
   * initialized by json string
   */
  public String entity_file_name;

  protected String[] fieldNames;
  protected HashMap<String, Object> fields;

  /**
   * An array of parameters values. This array has to be set before initFromJSON
   * is called. If array is not empty, each parameter $x in json representation
   * of entity is replaced by x-th string in array.
   */
  protected String[] entityParams = null;

  // a list of representative fields (fields that represent this entity)
  // this list is used to construct toString message
  protected ArrayList<String> representatives;

  public Entity() {
    fieldNames = new String[0];    
    fields = new HashMap();
  }

  public Entity(String entityID, String[] fieldNames) {
    this(entityID, fieldNames, new Object[0]);
  }
  
  /**
   * 
   * @param entityID
   * 
   * @param fieldNames  imena lastnosti v JSON datoteki. Vrstni red lastnosti je pomemben zaradi toString
   * zapisa (ker sem json objekt popravil tako, da uporablja LinkedList, se lastnosti pri toString berejo v
   * vrstnem redu, ki je podan tule). 
   * 
   * @param defaultValues privzete vrednosti za posamezno lastnost. Če uporabnik želi, da ALGator sam ustvari 
   * JSON objekt (z new <X>Entity()) z vsemi lastnostmi, mora določiti privzete vrednosti; na podlagi teh 
   * vrednosti ALGator tudi ve, kakšnega tipa je lastnost (npr. String, int, JSONObject, JSONArray, ...). 
   * Primer: glej entiteto ELOcalConfig
   */
  public Entity(String entityID, String[] fieldNames, Object[] defaultValues) {
    this();

    entity_id = entityID;
    entity_name = unknown_value;
    this.fieldNames = fieldNames;
    
    // add default values, if they are provided
    if (defaultValues.length == fieldNames.length)
      for (int i = 0; i < fieldNames.length; i++) {
        fields.put(fieldNames[i], defaultValues[i]);
      }

    representatives = new ArrayList<>();
  }
  
  public void addFieldName(String fieldName) {
    String[] newNames = new String[fieldNames.length + 1];
    for (int i = 0; i < fieldNames.length; i++) {
      newNames[i] = fieldNames[i];
    }
    newNames[fieldNames.length] = fieldName;
    fieldNames = newNames;
  }

  public String getName() {
    // first try property Name ...
    String name = getField(ID_NAME);
    // ... if property doesn't exist, try entity_name (filename without extension) ...
    if (name == null || name.isEmpty()) {
      name = entity_name;
    }
    // ... it even this doesn't exist, return unknown_value
    if (name == null || name.isEmpty()) {
      name = unknown_value;
    }

    return name;
  }

  public void setName(String name) {
    set(ID_NAME, name);
  }

  /**
   * Reads a JSON file. If entity_id=="", then whole file represents an JSON
   * object to be read (i.e. the file contains only this object); else, the file
   * contains a JSON object in which the field with key=entity_id is read.
   *
   * @param entityFile
   * @return
   */
  public ErrorStatus initFromFile(File entityFile) {
    entity_rootdir = ATTools.extractFilePath(entityFile);
    entity_name = ATTools.extractFileNamePrefix(entityFile);
    entity_file_name = entityFile.getPath();

    try {
      String vsebina = getFileText(entityFile);
      if (!ErrorStatus.getLastErrorStatus().equals(ErrorStatus.STATUS_OK))
        return ErrorStatus.getLastErrorStatus();
      
      JSONObject queryObject = new JSONObject(vsebina);
      if (entity_id == null || entity_id.isEmpty()) {
        return initFromJSON(queryObject.get("Query").toString());
      }

      String entity = queryObject.optString(entity_id);
      if (entity.isEmpty()) {
        throw new Exception("Token '" + entity_id + "' does not exist.");
      }

      return initFromJSON(entity);
    } catch (Exception e) {
      return ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR_CANT_READFILE,
              String.format("File: %s, Msg: %s", entityFile.getAbsolutePath(), e.toString()));
    }
  }

  public String getFileText(File entityFile) {
    ErrorStatus.resetErrorStatus();
    
    String vsebina = "";
    try (Scanner sc = new Scanner(entityFile, "UTF-8");) {      
      while (sc.hasNextLine()) {
        vsebina += sc.nextLine();
      }
    } catch (FileNotFoundException ex) {
      ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR_CANT_READFILE,
              String.format("File: %s, Msg: %s", entityFile.getAbsolutePath(), ex.toString()));
    }
    return vsebina;
  }

  /**
   * Method reads a JSON object and fills the map with correspnding values.
   *
   * @param json
   * @return
   */
  public ErrorStatus initFromJSON(String json) {

    if (entityParams != null) {
      for (int i = 0; i < entityParams.length; i++) {
        String ithParam = "[$]" + i;
        json = json.replaceAll(ithParam, entityParams[i]);
      }
    }

    try {
      JSONObject jsonObj = new JSONObject(json);

      // every entity should have Name
      if (!fields.containsKey(ID_NAME))
         fields.put(ID_NAME, jsonObj.opt(ID_NAME));

      for (String sp : fieldNames) {
        Object f = jsonObj.opt(sp);
        if (f!=null) fields.put(sp, f);
      }
      return ErrorStatus.setLastErrorMessage(ErrorStatus.STATUS_OK, "");
    } catch (Exception e) {
      return ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR_CANT_INIT_FROM_JSON, "");
    }
  }

  public void saveEntity() {
    if (entity_file_name == null) {
      return; // can not save 
    }
    File f = new File(entity_file_name);
    try (PrintWriter pw = new PrintWriter(f)) {
      pw.println(toJSONString(true));
    } catch (Exception e) {
    }
  }

  public String toJSONString() {
    return toJSONString(false, 2);
  }
  public String toJSONString(int indent) {
    return toJSONString(false, indent);
  }
  public String toJSONString(boolean wrapWithEntity) {
    return toJSONString(wrapWithEntity, 2);
  }
  public String toJSONString(boolean wrapWithEntity, int indent) {
    return toJSON(wrapWithEntity).toString(indent);
  }  
  public JSONObject toJSON(boolean wrapWithEntity) {
    JSONObject result = new JSONObject();

    if (export_name)
      result.put(ID_NAME, getName());
    
    for (String sp : fieldNames) {
      Object o = fields.get(sp);

      if (o instanceof Object[]) {
        o = new JSONArray(o);
      }

      result.put(sp, o);
    }
    if (wrapWithEntity) {
      JSONObject wrapped = new JSONObject();
      wrapped.put(entity_id, result);
      return wrapped;
    } else {
      return result;
    }
  }

  
  public Object get(String fieldKey) {
    if (fields.containsKey(fieldKey) && fields.get(fieldKey)!=null) {
      return fields.get(fieldKey);
    } else {
      return unknown_value;
    }
  }
  
  public String getString(String fieldKey) {
    Object value = fields.get(fieldKey);
    return value != null && value instanceof String ? (String) value : unknown_value;
  }
  
  public void clear(String fieldKey) {
    if (fields.containsKey(fieldKey)) fields.remove(fieldKey);
  }
  

  public void set(String fieldKey, Object object) {
    fields.put(fieldKey, object);
  }

  /**
   * Method is used to get a field in its type. For example, a field of type
   * Integer is read by Integer i = getField("key"), a field of type Double is
   * read by Double i = getField("key").
   */
  public <E> E getField(String fieldKey) {
    E result = null;
    if (fields.containsKey(fieldKey)) {
      result = (E) fields.get(fieldKey);
    }
    return result;
  }

  public int getFieldAsInt(String fieldKey, int default_value) {
    int result = default_value;
    if (new TreeSet(Arrays.asList(fieldNames)).contains(fieldKey)) {
      try {
        result = (Integer) fields.get(fieldKey);
      } catch (Exception e1) {
        try {
          result = Integer.parseInt((String) fields.get(fieldKey));
        } catch (Exception e2) {
        }
      }
    }
    return result;
  }

  public long getFieldAsLong(String fieldKey, long default_value) {
    long result = default_value;
    if (new TreeSet(Arrays.asList(fieldNames)).contains(fieldKey)) {
      try {
        result = (Long) fields.get(fieldKey);
      } catch (Exception e1) {
        try {
          result = Long.parseLong((String) fields.get(fieldKey));
        } catch (Exception e2) {}
      }
    }
    return result;
  }

  
  public int getFieldAsInt(String fieldKey) {
    return getFieldAsInt(fieldKey, 0);
  }
  
  public long getFieldAsLong(String fieldKey) {
    return getFieldAsLong(fieldKey, 0);
  }

  public boolean getFieldAsBoolean(String fieldKey, boolean defaultValue) {
    boolean result = defaultValue;
    if (new TreeSet(Arrays.asList(fieldNames)).contains(fieldKey)) {
      try {
        result = (Boolean) fields.get(fieldKey);
      } catch (Exception e) {}
    }
    return result;
  }

  /**
   * Method return an String array obtained from corresponding field. If
   * fieldKey does not exist or if jason field is not an string array, an array
   * with length==0 is returned.
   */
  public String[] getStringArray(String fieldKey) {
    try {
      JSONArray ja = getField(fieldKey);
      String[] result = new String[ja.length()];
      for (int i = 0; i < ja.length(); i++) {
        result[i] = ja.getString(i);
      }
      return result;
    } catch (Exception e) {
      // ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR_NOT_A_STRING_ARRAY, 
      //String.format("[%s.%s, %s]", entity_name, entity_file_ext, fieldKey));

      return new String[0];
    }
  }

  public void setRepresentatives(String... fields) {
    representatives = new ArrayList<>();
    for (String string : fields) {
      representatives.add(string);
    }
  }

  @Override
  public String toString() {
    String desc = getName().length() > 1 ? getName() : "";
    for (String rep : representatives) {
      desc += (desc.isEmpty() ? "" : ", ") + rep + "=" + fields.get(rep);
    }

    return entity_id + "[" + desc + "]";
  }

  @Override
  protected Object clone() throws CloneNotSupportedException {
    Entity myClone = (Entity) super.clone();
    myClone.fields = (HashMap) this.fields.clone();
    myClone.fieldNames = (String[]) this.fieldNames.clone();
    myClone.representatives = (ArrayList) this.representatives.clone();
    return myClone;
  }
}
