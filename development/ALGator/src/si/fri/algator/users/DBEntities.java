package si.fri.algator.users;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author tomaz
 */
public class DBEntities {
    public ArrayList<DBEntity>             projects;
    public ArrayList<DBEntity>             algorthms;
    public ArrayList<DBEntity>             tests;
    public HashMap<String, List<DBEntity>> alg_map;
    public HashMap<String, List<DBEntity>> test_map;


  public DBEntities() {
    projects  = new ArrayList<>();
    algorthms = new ArrayList<>();
    tests     = new ArrayList<>();
    alg_map   = new HashMap<>();
    test_map  = new HashMap<>();     
  }        
}
