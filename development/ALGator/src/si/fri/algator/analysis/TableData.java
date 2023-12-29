package si.fri.algator.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import si.fri.algator.entities.VariableType;
import si.fri.algator.entities.StatFunction;
import si.fri.algator.global.ATGlobal;

/**
 *
 * @author tomaz
 */
public class TableData {

  public ArrayList<String> header;
  public ArrayList<ArrayList<Object>> data;
  
  public int numberOfInputParameters = 0;

  public TableData() {
    header = new ArrayList<>();
    data = new ArrayList<>();
  }

  private String add(String prefix, String sufix, String delim) {
    if (prefix.isEmpty()) {
      return sufix;
    } else {
      return prefix + delim + sufix;
    }
  }

  private Comparator<Object> getComparator(final int fieldNo, String type) {
    final int predznak = (type.equals("-") || type.equals("<")) ? -1 : 1;
    
    switch (type) {
      case ">": case "<":
        return new Comparator<Object>() {
          @Override
          public int compare(Object o1, Object o2) {
            try {
              ArrayList<Object> ao1 = (ArrayList<Object>) o1;
              ArrayList<Object> ao2 = (ArrayList<Object>) o2;
              return predznak * ((String) ao1.get(fieldNo)).compareTo((String) ao2.get(fieldNo));
            } catch (Exception e) {
              return 0;
            }
          }
        };
        
      default: // "+", "-", or anything else
        return new Comparator<Object>() {
          @Override
          public int compare(Object o1, Object o2) {
            try {
              ArrayList<Object> ao1 = (ArrayList<Object>) o1;
              ArrayList<Object> ao2 = (ArrayList<Object>) o2;
              Double left = ((Number) ao1.get(fieldNo)).doubleValue();
              return predznak * left.compareTo(((Number) ao2.get(fieldNo)).doubleValue());
            } catch (Exception e) {
              return 0;
            }
          }
        };
    }
  }

  public Object [][] getDataAsArray() {
    if (data==null) return new Object[0][0];
    Object[][] dataArray = new Object[data.size()][];
    for (int i = 0; i < data.size(); i++) {
      Object[] row = new Object[data.get(i).size()];
      for (int j = 0; j < data.get(i).size(); j++) {
        row[j] = data.get(i).get(j);
      }
      dataArray[i] = row;
    }
    return dataArray;
  }
  
  /**
   * Get the i-th column out of two-dimencional araylist
   */
  private <E> ArrayList<E> getColumn(ArrayList<ArrayList<Object>> group, int col, E type) {
    ArrayList<E> result = new ArrayList<>();
    if (!group.isEmpty() && group.get(0).size() > col) {
      for (ArrayList<Object> line : group) {
        result.add((E)line.get(col));
      }
    }
    return result;
  }
  
  /**
   * Find a statistical function for a given field in a given groupBy string. If 
   * a function for given field is not prescribed, the default function is returned. 
   * If default is not given, FIRST is returned.  
   */
  private static StatFunction getFunctionForField(String groupBy, String field) {
    StatFunction result = StatFunction.FIRST;
    
    StatFunction defaultFunc = StatFunction.UNKNOWN;
    
    String [] fields = groupBy.split(";");
    // fields[0] = groupBy field name; fields[1, 2, ...] = stat. funcs. for fields
    for (int i = 1; i < fields.length; i++) {
      if (fields[i].contains(":")){
        String [] stat = fields[i].trim().split(":");
        if (field.endsWith("."+stat[0])) { 
          defaultFunc = StatFunction.getStatFunction(stat[1]);
          break;
        }
      } else {
        defaultFunc = StatFunction.getStatFunction(fields[i].trim());
      }
    }
    if (!defaultFunc.equals(StatFunction.UNKNOWN))
      result = defaultFunc;
    
    return result;
  }
   
  /**
   * Sqeezes group  - from array of lines it returns one line that contains 
   * a summary of all lines; a function that is used to summarize each column
   * can be given in groupBy parameter; default function: FIRST.
   */
  private ArrayList<Object> squeezeGroup(ArrayList<ArrayList<Object>> group, String groupBy) {
    ArrayList<Object> result = new ArrayList<>();
    for (int i = 0; i < header.size(); i++) {
      String colName = header.get(i);
      ArrayList<Integer> values = getColumn(group,i, 1);
      StatFunction function = getFunctionForField(groupBy, colName);
      Object rezultat = StatFunction.getFunctionValue(function,values);
      result.add(rezultat);
    }
    
    return result;
  }
  
  /**
   * REturns the position of the field in the header table; if field does not exist, method returns -1
   * @param fieldName
   * @return 
   */
  private int getFieldPos(String fieldName) {
    for (int i = 0; i < header.size(); i++) {
      if (header.get(i).equals(fieldName))
        return i;
    }
    return -1;
  }
  
  /**
   * Returns positions of the fields in the header table with suffix 
   * (last part of the name - everything that follows .) equal to fieldSuffix
   */
  private ArrayList<Integer> getFieldsPos(String fieldSuffix) {
    int pos; 
    
    ArrayList<Integer> result = new ArrayList<>();
    for (int i = 0; i < header.size(); i++) {
      String headerName = header.get(i);
      if ((pos = headerName.indexOf(".")) != -1) {
        headerName = headerName.substring(pos + 1);
      }
      if (headerName.equals(fieldSuffix))
        result.add(i);
    }    
    return result;
  }
  
  /**
   * Filter out rows that do not satisfy the filter condition
   * @param groupby 
   */
  public void filter(String filter) {
    if (data == null || data.size() == 0 || filter.isEmpty()) return;
    
    String operators = "<=|<|>=|>|==|!=";
    String[] flt = filter.split(operators);
    
    // if filter is correct, the result should contain 2 values (field name and value)
    if (flt.length!=2) 
      return; // cant make this filer
    
    int opStart = flt[0].length();
    int opEnd   = filter.indexOf(flt[1]);
    if (opStart < 0 || opStart >=filter.length() || opEnd < opStart || opEnd >= filter.length())
      return;
    
    String filedName = flt[0].trim();
    String operator  = filter.substring(opStart, opEnd).trim();
    String value  = flt[1].trim();
    
    // empty "field_name" value
    if (filedName.length()==0)
      return;
    
    ArrayList<Integer> fieldPositions = getFieldsPos(filedName);
    if (fieldPositions.isEmpty()) return;
    
    VariableType pTypes [] = new VariableType[fieldPositions.size()];
    for (int i = 0; i < fieldPositions.size(); i++) {
      // detect the type of data in corresponding column
      VariableType  type = VariableType.UNKNOWN;
      if (data.get(0).get(fieldPositions.get(i)) instanceof String)
        type = VariableType.STRING;
      if (data.get(0).get(fieldPositions.get(i)) instanceof Integer)
        type = VariableType.INT;
      if (data.get(0).get(fieldPositions.get(i)) instanceof Double)
        type = VariableType.DOUBLE;
    
      if (type.equals(VariableType.UNKNOWN))
        return;
      
      pTypes[i] = type;
    }
    
    
    Iterator<ArrayList<Object>> iterator = data.iterator();
    
    // iterate all lines of the data
    while (iterator.hasNext()) {
      ArrayList<Object> line = iterator.next();
      
      // check for each field to comply the condition
      for (int i = 0; i < fieldPositions.size(); i++) {
        
        boolean keepLine = true;
        int cmp;
        try {
          Comparable curValue = (Comparable) line.get(fieldPositions.get(i));
          Comparable refValue = "";
          switch (pTypes[i]) {
            case STRING:
              refValue = value;
              break;
            case INT: 
              refValue = Integer.parseInt(value);
              break;
            case DOUBLE: 
              refValue = Double.parseDouble(value);
              break;
          }

          cmp = refValue.compareTo(curValue);
        } catch (Exception e) {
          // if comparison can not be done, item is removed
          // this happens, for example, if one of the values is undefined or if the comparison condition is incorrectly formed
          iterator.remove();
          break;
        }
      
        switch(operator) {
          case "==":
            keepLine &= cmp == 0;
            break;
          case "!=":
            keepLine &= cmp != 0;
            break;
          case "<":
            keepLine &= cmp  > 0;
            break;
          case "<=":
            keepLine &= cmp  >= 0;
            break;
          case ">":
            keepLine &= cmp  < 0;
            break;
          case ">=":
            keepLine &= cmp  <= 0;
            break;
        }
      
        if (!keepLine) {
          iterator.remove();
          break;
        }
      }
    }  
  }
  
  /**
   * Group data by a given field
   * Example of a complex groupby value: "N:>,T; Tmax:SUM; MIN"
   */
  public void groupBy(String groupby) {
    if (groupby==null) return; // avoid null error
    
    groupby = groupby.replaceAll(" ", ""); // remove spaces
    
    if (data.isEmpty() || groupby.isEmpty()) return;
    
    // the first column in groupby is the "field", which is of form: "field:sortType,field1,field2,..." 
    String field = groupby.split(";")[0];    
    
    // fields are separated by ","; the first field is the main groupby field
    String [] fields = field.split("[,]");
    field            = fields[0];    
    
    String sortType = "+"; // default: numerical ascending sorting
    if (field.contains(":")) {
      String [] ops = field.split("[:]");
      field         = ops[0];
      sortType      = ops[1];
    }
    
    fields[0] = field; // write only fieldname (without possible ":>) back to array
    
    // get the fields indeces in the header array
    int [] fieldNo = new int[fields.length];
    for (int i = 0; i < fieldNo.length; i++) {
      fieldNo[i] = header.indexOf(fields[i]);
      if (fieldNo[i] == -1) {
        return;
      }      
    }
            
        
    // detect if the values of the main filed are Strings -> adjust sorting
    try {
      // check the first entry in this column
      if (data.get(0).get(fieldNo[0]) instanceof String)
        sortType = ">";
    } catch (Exception e) {}
    
    // soring is performed over the main groupby field in the given (or default) order
    sort(field+":"+sortType);
    
    ArrayList<ArrayList<Object>> newData = new ArrayList<>();

    ArrayList<ArrayList<Object>> group;
    
    int kjeVData=0;
    while(kjeVData < data.size()) {
      group = new ArrayList<>();
      group.add(data.get(kjeVData++));
      
      Object [] refFields = new Object[fieldNo.length];
      for (int i = 0; i < refFields.length; i++) refFields[i] = group.get(0).get(fieldNo[i]);      
      
      // put into a group all elements that are equal (according to the given fields) to the first group element
      boolean inGroup = true;
      if (kjeVData < data.size()) do {        
        for (int i = 0; i < fieldNo.length; i++) {
          Object curField = data.get(kjeVData).get(fieldNo[i]);
          if (curField == null || !curField.equals(refFields[i])) {
            inGroup =false;
          }
        }
        if (inGroup) 
          group.add(data.get(kjeVData++));
      } while (kjeVData < data.size() && inGroup);
      
      newData.add(squeezeGroup(group, groupby));                        
    }
    data = newData;
  }
  
  /**
   * Sort data array
   *
   * @param criteria sorting criteria (e.g. N:+)
   */
  public void sort(String criteria) {
    if (criteria == null || criteria.isEmpty()) {
      return;
    }

    String[] sC = criteria.split(":");
    String field = sC[0];
    String type = (sC.length > 1 ? sC[1] : "+");

    int fieldNo = header.indexOf(field);
    if (fieldNo == -1) {
      return;
    }

    // TODO - uredi to tabelo! V javi 1.8 je šlo, v 1.7 pa ne gre tako preprosto!
    //Arrays.sort(data, getComparator(fieldNo, type));
    
    Object [] dataTable = data.toArray();
    Arrays.sort(dataTable, getComparator(fieldNo, type));
    
    data = new ArrayList<>();
    for (Object object : dataTable) {
      data.add((ArrayList<Object>)object);
    }
  }

  @Override
  public String toString() {
    if (data==null || data.size()==0) return "";
      
    // iz rezultata izloci vse stolpce, v katerih se pojavljajo same ničle (0), vprašaji (?) ali null    ArrayList<ArrayList<Object>> clearedData   = (ArrayList) data  .clone();;
    HashSet<Integer> emptyRows = new HashSet();    
    for (int j = 0; j < data.get(0).size(); j++) {      
      boolean allUndefined = true;
      for (int i = 0; i < data.size(); i++) {
        try {
          Object object = data.get(i).get(j);
          if (object != null && !object.toString().isEmpty() && /*!"0".equals(object.toString()) &&*/ !"?".equals(object.toString())) {
            // found defined value - column will not be deleted
            allUndefined=false;
            break;
          }
        } catch (Exception e) {};
      }
      if (allUndefined) {
        emptyRows.add(j);
      }
    }
    
    
    String result = "";
    for (int i = 0; i < header.size(); i++) {
      if (!emptyRows.contains(i))
        result = add(result, header.get(i), ATGlobal.DEFAULT_CSV_DELIMITER);
    }

    for (int i = 0; i < data.size(); i++) {
      String vrstica = "";
      for (int j = 0; j < data.get(i).size(); j++) {
        if (emptyRows.contains(j)) continue;
        
        Object object = data.get(i).get(j);
        String value  = object != null ? object.toString() : "null";        
        vrstica = add(vrstica, value, ATGlobal.DEFAULT_CSV_DELIMITER);
      }
      result = add(result, vrstica, "\n");
    }
    return result;
  }
  
  
  public static void main(String[] args) {
    String operators = "<|<=|>|>=|==|!=";
    String izraz="a <= b";
    String [] f = izraz.split(operators);
    String operator = izraz.substring(f[0].length(),izraz.indexOf(f[1]));
    System.out.printf("'%s'\n", operator);
    
    System.out.println("abc".substring(01,5));
  }
  
  
}
