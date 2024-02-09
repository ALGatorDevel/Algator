package si.fri.algator.entities;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import si.fri.algator.global.ATGlobal;
import si.fri.algator.global.ATLog;
import si.fri.algator.global.ErrorStatus;

/**
 * A set of variables.
 * @author tomaz
 */
public class Variables implements Serializable, Iterable<EVariable> {
  
  private HashMap<String, EVariable> variables;

  public Variables() {
        variables = new HashMap<>();
  }
  
  public Variables(Variables variables) {
    this();
    for (EVariable var : variables.variables.values()) {
      try {
        addVariable((EVariable) var.clone(), true);
      } catch (CloneNotSupportedException ex) {
	ATLog.log("Can't clone (EVariable)", 2);
      }
    } 
  }
  
  public void addVariable(EVariable variable) {
    addVariable(variable, true);
  }
  
  /**
   * Če spremenljivke ni v množici, jo dodam; če pa  že obstaja, potem zamenjam 
   * njeno vrednost (replaceValue==true) oziroma ne naredim ničesar (replaceValue==false)
   */
  public void addVariable(EVariable variable, boolean replaceValue) {
    EVariable oldVar = variables.get(variable.getName());
    if (oldVar == null) {
      variables.put(variable.getName(), variable);
    } else if (replaceValue) {
      oldVar.set(EVariable.ID_Value, variable.get(EVariable.ID_Value));
    }
  }
  
  public void addVariables(Variables vSet) {
    addVariables(vSet, true);
  }
  
  public void addVariables(Variables vSet, boolean replaceExisting) {
    for (EVariable variable : vSet) {
      addVariable(variable, replaceExisting);
    }
  }
  
  public EVariable getVariable(int i) {
    if (i < variables.size()) {
      return variables.values().toArray(new EVariable[0])[i];
    } else {
      return null;
    }
  }
  
  public EVariable setVariable(String name, Object value) {
    EVariable var = getVariable(name);
    if (var == null) {
      var = new EVariable(name, value);
      addVariable(var, true);
    } else {
      var.setValue(value);
    }
    return var;    
  }
  
  public int size() {
    return variables.size();
  }
  
  public Variables copy() {
    Variables copy = new Variables();
    for (Map.Entry<String, EVariable> entry : variables.entrySet()) {
      try {
        copy.variables.put(entry.getKey(), (EVariable) entry.getValue().clone());
      } catch (CloneNotSupportedException ex) {
        ATLog.log("Can't clone (EVariable)", 3);
      }
    }
    return copy;
  }

  public EVariable getVariable(String name) {
    return variables.get(name);
  }

  // get a variable; if variable does not exist, method creates and returns 
  // a variable with a given default value
  public EVariable getVariable(String name, Object defaultValue) {
    EVariable variable = getVariable(name);
    if (variable == null || variable.getValue() == null)
      variable = new EVariable(name, defaultValue);
    
    return variable;
  }
  
  public void clearProperties(String propVariableName) {
    EVariable propVar = getVariable(propVariableName);
    if (propVar == null) {
      propVar = new EVariable(propVariableName, "");
      addVariable(propVar);
    }
    propVar.setValue("");     
  }
  
  public void addProperty(String propVariableName, String propName, Object value) {
    EVariable propVar = getVariable(propVariableName);
    if (propVar == null) {
      propVar = new EVariable(propVariableName, VariableType.JSONSTRING, "");
      addVariable(propVar);
    }
    
    // add a value of a prop ...
    String val = String.format("\"%s\":\"%s\"", propName, value.toString());
    // ... and remove previous prop with the same name (if exists)    
    String[] props = ((String)propVar.getValue()).replaceAll("\"", "").split(",");
    for (String propN : props) {
      String[] propParts = propN.split(":");
      if (propParts.length>1 && !propName.equals(propParts[0]))
        val += (val.isEmpty() ? "":",") + String.format("\"%s\":\"%s\"", propParts[0], propParts[1]); 
    }
    
    propVar.setValue(val); 
  }

  
  @Override
  public String toString() {
    String result="";
    for (String varName : variables.keySet()) {
      if (!result.isEmpty()) result+="; ";
      EVariable p = variables.get(varName);
      if (p!=null)
        result += p.getName() + "=" + p.getField(EVariable.ID_Value);
    }
    return result;
  }
  
  // izpiše rezultate v dveh oblikah:
  // - če asJSON==false ... izpis v obliki ene vrstice CVS datoteke; ločilo je ; (oziroma delim)
  // - če asJSON==true  ... izpis v obliki JSON objekta
  public String toString(String [] order, boolean asJSON, String delim) {
    if (asJSON) delim = ",";
    
    String result = "";
    
    String localOrder [] = order.clone();

//    prej:   če je prišlo do napake, se je kot 5. rezultat (za KILLED ali FAILED) izpisalo
//            sporočilo o napaki; 
//    sedaj:  sporočilo o napaki izpišem povsem na koncu, prej pa izpišem vrednosi vseh parametrov
//            (indikatorji bodo napačni, parametri pa bodo pravilni)
//    int numVar = localOrder.length;
//    if (numVar > EResult.FIXNUM && variables.values().contains(EResult.getErrorIndicator(""))) {
//      numVar = EResult.FIXNUM;
//      localOrder[numVar++] = EResult.errorParName;
//    }
    int numVar = localOrder.length;
    for (int i = 0; i < numVar; i++) {
      EVariable v = null;
    
      // find a variable with name==localOrder[i]
      for (String varName : variables.keySet()) {        
        EVariable curV = variables.get(varName);
        if (curV != null && curV.getName() != null && curV.getName().equals(localOrder[i])) {
          v = curV;
	  break;
        }
      }
      
      if (v!=null) {      
       if (asJSON)
         result += '"' + v.getName() + '"' + ":";
       
       
        Object value = v.getValue();
        if (v.getType().equals(VariableType.STRING) || v.getType().equals(VariableType.ENUM)) {          
          value = ((String)value).replaceAll(delim, " ").replaceAll("\n", " ");
          if (asJSON)
            value = '"' + value.toString() + '"';
        }       
        if (v.getType().equals(VariableType.JSONSTRING) && asJSON) {         
          value = '{' + value.toString() + '}';
        }
        if (v.getType().equals(VariableType.TIMER) && "ALL".equals(v.getMeta("STAT", "")) && asJSON) {         
          value = '[' + value.toString() + ']';
        }        
        result += value + delim;
      } else
        if (!asJSON) result += delim;
    }

    // strip 'delim' at the end of line
    while (result.endsWith(delim))
      result = result.substring(0,result.length()-delim.length());
        
    if (!asJSON) {
      if (variables.values().contains(EResult.getErrorIndicator(""))) {
        result += delim + variables.get(EResult.errorParName).getValue();
      }
    } else {
      result = '{' + result + '}';
    }
    return result;
  }
  
  public void printToFile(File resultFile, String [] variablesOrder, boolean asJSON) {
    try {
      // tole bi uporabil namesto spodnje kode, če bi želel, da je izhodna datoteka 
      // zapisana v UTF-8  (namesto v default) kodirnem sistemu
      //FileOutputStream fis = new FileOutputStream(resultFile, true);
      //OutputStreamWriter osw = new OutputStreamWriter(fis, "UTF-8");
      //PrintWriter pw_with_encoding = new PrintWriter(osw);
      //  printToFile(pw_with_encoding, variablesOrder, false, asJSON);        
      //pw_with_encoding.close();
      
      PrintWriter pw = new PrintWriter( new FileWriter(resultFile, true));
        printToFile(pw, variablesOrder, false, asJSON);        
      pw.close();
    } catch (Exception e) {
      ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR_CANT_WRITEFILE, e.toString());
    } 
  }
  public void printToFile(PrintWriter pw, String [] variablesOrder, boolean isStdout, boolean asJSON) {
    try {   
      // pri izpisu na zaslon (torej ne v datoteko z rezultati) vrstice, ki predstavljajo "results" označim
      // z nevidnim belim znakom; to je signal za programe, ki procesirajo izhod, da gre za "pravi podatek";
      // ti programi (na primer panel.html) lahko ostale vrstice zavržejo! 
      if (isStdout) 
        pw.print('~'); // Zero-Width Space       &#8203;
      
      pw.println(this.toString(variablesOrder, asJSON, ATGlobal.DEFAULT_CSV_DELIMITER));
      pw.flush();
    } catch (Exception e) {
    }
  }
  
  @Override
  public Iterator<EVariable> iterator() {
      return variables.values().iterator();
  }
  



  public static Variables join(Variables v1, Variables v2) {
    Variables result = new Variables();
    result.addVariables(v1, true);    
    result.addVariables(v2, false);
    
    return result;
  }

}
