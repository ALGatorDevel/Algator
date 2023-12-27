package si.fri.algator.entities;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.json.JSONObject;
import si.fri.algator.tools.ATTools;

/**
 *
 * @author tomaz
 */
public class EVariable extends Entity  implements Serializable {
  // Entity identifier
  public static final String ID_Variable   = "Variable";
  
  // Fields
  public static final String ID_Desc     = "Description";
  public static final String ID_Type     = "Type";
  public static final String ID_Meta     = "Meta";
  public static final String ID_Value    = "Value";
  public static final String ID_Default  = "Default";
  
  
  private VariableType              type;
  private HashMap<String, Object>   metadata; 
  
  public EVariable() {
   super(ID_Variable, 
	 new String [] {ID_Desc, ID_Type, ID_Meta, ID_Value});
  
   setRepresentatives(ID_Value);
  }
  
  public EVariable(File fileName) {
    this();
    initFromFile(fileName);
    
    setTypeAndMetaData();
  }
  
  public EVariable(String json) {
    this();
    initFromJSON(json);

    setTypeAndMetaData();
  }

  public EVariable(String name, VariableType type, Object value) {
    this(name, "", type, value);
  }
  
  public EVariable(String name, Object value) {
    this(name, "", VariableType.UNKNOWN, value);
       
    if      (value instanceof String)  type = VariableType.STRING;
    else if (value instanceof Integer) type = VariableType.INT;
    else if (value instanceof Double)  type = VariableType.DOUBLE;
  }
      
  public EVariable(String name, String desc, VariableType type, Object value) {
    this();
    
    setName(name);
    set(ID_Desc, desc);
    
    this.type = type;
    if (type != null)
      set(ID_Type, type.toString());     
    
    setValue(value);
  }
  
  @Override
  public void set(String fieldKey, Object object) {
    if (!fieldKey.equals(ID_Value))
      super.set(fieldKey, object); 
    else {
      try {
        switch (this.type) { 
	  case INT: case TIMER: case COUNTER:
            if (object instanceof Integer || object instanceof Long)
              fields.put(fieldKey, object);
            else
	      try {
                fields.put(fieldKey, Integer.parseInt((String) object));
              } catch (Exception e) {
                // to se bo zgodilo, na primer, ko bom skušal v counter (ki je kot tipa int) stlačiti 3,4,5,6
                fields.put(fieldKey, object);
              }            
	    break; 
	  case DOUBLE:
            if (object instanceof Double)
              fields.put(fieldKey, object);
            else 
              fields.put(fieldKey, Double.parseDouble((String) object));
	    break;
	  default:
	    fields.put(fieldKey, object);
	}
      } catch (Exception e) {        
	fields.put(fieldKey, get(ID_Default));
      }
    }
  }
  
  public void setMeta(String meta) {
    try {
      fields.put(ID_Meta, new JSONObject(meta));
      setTypeAndMetaData();
    } catch (Exception e) {}
  }
  
  /**
   * Method detects a type of a parameter and sets the values of the {@code type} 
   * field and its {@code meta} field.
   */
  private void setTypeAndMetaData() {    
    String typeDesc = getField(ID_Type);
    this.type = VariableType.getType(typeDesc);   
    
    try {        
      metadata = ATTools.jSONObjectToMap(getField(ID_Meta));
    } catch (Exception e) {
      metadata = new HashMap<>();
    }       
    
    Object defaultValue = metadata.get("Default");
    set(ID_Default, defaultValue);
    set(ID_Value,   defaultValue);
  }

  public VariableType getType() {
    return type;
  }
  
  public void setType(VariableType type) {
    this.type = type;
  }

  public HashMap<String,Object> getMetaData() {
    return metadata;
  }
  public void setMetaData(HashMap<String,Object> metadata) {
    this.metadata = metadata;
    Object defaultValue = metadata.get("Default");
    set(ID_Default, defaultValue);    
  }
  
  public Object getDefaultValue() {
    HashMap meta = getMetaData();
    if (meta!=null)
      return meta.get("Default");
    return null;
  }
    
  public Object getValue() {
    Object value = get(ID_Value);
    
    // getValue() method for parameters of type DOUBLE returns a value with
    // limited number of decimals (given in meta data)
    switch (type) {
      case DOUBLE:
        int decimals = getMeta("Decimals", 2);
        try {
          Double d = null;
          if (value instanceof String)
            d = Double.parseDouble((String) value);
          else if (value instanceof Double)
            d = (Double) value;
          else return value;
          
          double potenca = Math.pow(10, decimals);
          value = Math.round(d * potenca)/potenca;
        } catch (NumberFormatException e) {} 
        break;
      case STRING:
      case JSONSTRING:
      case ENUM:
        if (value == null) value = "?";
        break;
      case INT:
        if (value == null) value = 0;
        break;
    }
  
    return value;
  }
  
  public int getIntValue() {
    return getIntValue(0);       
  }
  public int getIntValue(int defaultValue) {
    int result = defaultValue;
    try {
      result = (Integer) getValue();
    } catch (Exception e) {
      try {result = Integer.parseInt((String) getValue());} catch (Exception e1) {}
    }
    return result;
  }
  public long getLongValue() {
    return getLongValue(0);       
  }
  public long getLongValue(long defaultValue) {
    long result = defaultValue;
    try {
      result = (Long) getValue();
    } catch (Exception e) {
      try {result = Long.parseLong((String) getValue());} catch (Exception e1) {}
    }
    return result;
  }
  
  
  public double getDoubleValue() {
    return getDoubleValue(0);       
  }
  public double getDoubleValue(double defaultValue) {
    double result = defaultValue;
    try {
      result = (Double) getValue();
    } catch (Exception e) {
      try {result = Double.parseDouble((String) getValue());} catch (Exception e1) {}
    }
    return result;
  }

  public String getStringValue() {
    return getStringValue("");       
  }
  public String getStringValue(String defaultValue) {
    String result = defaultValue;
    try {
      result = getValue().toString();
    } catch (Exception e) {}
    return result;
  }

  public void setValue(Object value) {
    if (type != null && type.equals(VariableType.ENUM)) {
      if (getMetaStringArray("Values").contains(value))
        set(ID_Value, value);
      else
        set(ID_Value, get(ID_Default));
    } else
      set(ID_Value, value);
  }
    
  @Override
  public boolean equals(Object obj) {
    return (obj instanceof EVariable && ((EVariable)obj).getName().equals(this.getName()));
  }  
  
  
  /*********** Tools to handle meta data ***************/
  /**
   * Call method like this: int a = getMeta("Min", 100); or like this 
   * String a = getMeta("Value", "RND");
   */
  public <T extends Object> T getMeta(String name, T defaultValue) {
    try { 
      Object result = metadata.get(name);
      if (result != null)
        return (T) result;
      else
        return defaultValue;
    } catch (Exception e) {
      return defaultValue;
    }
  }
  
  public ArrayList<String> getMetaStringArray(String name) {
    ArrayList<String> result = new ArrayList<>();
    try {
      List<Object> list = (List<Object>) metadata.get(name);
      if (list != null) {
        for (Object object : list) {
          result.add((String) object);
        }
      } 
    } catch (Exception e) {}
    return result;
  }

  @Override
  public String toString() {
    return String.format("[%s = %s, %s]", getName(), getValue(), getMetaData());
  }  
  
}
