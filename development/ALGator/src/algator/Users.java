package algator;

import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import jline.console.ConsoleReader;
import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.math.NumberUtils;
import si.fri.algator.database.Database;
import si.fri.algator.entities.ELocalConfig;
import si.fri.algator.global.ATGlobal;
import si.fri.algator.global.ATLog;
import si.fri.algator.users.DBEntities;
import si.fri.algator.users.UsersDatabase;
import si.fri.algator.users.DBEntity;
import si.fri.algator.users.UsersTools;


/**
 *
 * @author Gregor, Tomaž
 */
public class Users {
  
  private static String currentUser=""; // the name of the current user 
  
  private static String help_msg =
              "init \n"
            + "   Inits database and creates tables.\n\n"
          
            + "adduser username password\n"
            + "   Creates new user; requires username and password.\n\n"

            + "passwd username new_password\n"
            + "   Changes users password; requires username and password.\n\n"          
          
            + "addgroup groupname\n"
            + "   Creates new group, requires name.\n\n"
          
            + "moduser groupname username\n"
            + "   Adds/removes user to group, requires groupname and username\n\n"

            + "userperm username [entity]\n"
            + "   Returns premissions for username on specified enitity, where entity is:\n"
            + "     -all         ... return permissions for all entities\n"
            + "     <empty>      ... return permissions for all projects\n"
            + "     name | id    ... return permissions for the given entity and all subentites\n\n"

            + "canuser username permission enitity\n"           
            + "     Return true or false if username has specific permission on entity. \n\n"

            + "chmod perm who entity\n"
            + "      Changes permission for user (or group)\n"
            + "        perm:   +/- permission (e.g. +can_write or -can_read)\n"
            + "        who:    user or group (e.g. algator or :algator)\n"
            + "        entity: project (proj_name) or \n"
            + "                algorithm (proj_name/alg_name) or\n"
            + "                testset (proj_name//testset_name)\n"
            + "        Example: chmod +can_write joe Sorting/QuickSort\n\n"
          
            + "getentityid entity\n"
            + "       Prints the id of a given entity, -1 if not found.\n\n"
          
            + "setowner user entity\n"
            + "     Set  the ownership of the entity to the user.\n\n"
          
            + "insert_project project_name\n"
            + "     Inserts the project and all its enclosing entities (algorithms, testsets)\n"
            + "     into database and sets the owner (current user).\n\n"                    
          
            + "insert_projects\n"
            + "     Inserts the projects and all enclosing entities (algorithms, testsets)\n"
            + "     into database and sets the owner (current user).\n\n"          
          
            + "showowner entity  \n"
            + "     Shows the owner owner of the entity.\n\n"
          
            + "showusers [username]\n"
            + "     Show all active users in the system. If arg <username> is passed to \n"
            + "     command, it shows only that specific user.\n\n"

            + "showgroups\n"
            + "     Show all active groups in the system.\n\n"

            + "showpermissions\n"
            + "     Show all permissions in the system.\n\n"
          
            + "listentities [-all] | [project_name] | [project_id]\n"
            + "     List the entities of the system. Parameter can be:\n"
            + "     -all                      ... list all entities\n"
            + "     <no parameter>            ... list all projects\n"
            + "     project_name | project_id ... list all entities of the project\n\n"          
          
            + "changeuserstatus username status\n"
            + "     Changes status of user active/inactive. Status must be int (0/1)\n\n"
          
            + "changegroupstatus groupname status\n"
            + "     Changes staus of group active/inactive. Status must be int (0/1)\n\n" 
          
            + "setformat json | string\n"
            + "     Sets the output print format";

  
  public static void help() {
    System.out.println(help_msg);
  }
  

  private static void help_for_command(String command) {
    help_for_command("Missing arguments", command);
  }  
  private static void help_for_command(String eMsg, String command) {
    if (!eMsg.isEmpty())
      System.out.println(">>> Error: " + eMsg + "\n");
    
    String [] msgs = help_msg.split("\n\n");
    for (String msg : msgs) {
      if (msg.trim().toUpperCase().startsWith(command.toUpperCase() + " "))
        System.out.println(msg);
    }
    System.out.println("");
  }
  
  public static String do_users(String ... sinput) {    
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(baos);
    PrintStream old = System.out;
    System.setOut(ps);

    main(sinput);

    System.out.flush();
    System.setOut(old);    
    return baos.toString();
  }
  
  public static boolean main_switch(String[] sinput) {
    boolean result = true;
    
    if (sinput.length > 1 && sinput[1].equals("?")) {
      System.out.println("");
      help_for_command("", sinput[0]);
      return true;
    }
    
    switch (sinput[0]) {
      case "exit":
        break;
      case "help":
        help();
        break;
      case "init":
        UsersDatabase.init();
        break;
      case "adduser":
        try {
          UsersDatabase.addNewUser(sinput[1], sinput[2]);
        } catch (ArrayIndexOutOfBoundsException e) {
          help_for_command("adduser");
        }
        break;
      case "passwd":
        try {
          UsersDatabase.passwd(sinput[1], sinput[2]);
        } catch (ArrayIndexOutOfBoundsException e) {
          help_for_command("passwd");
        }
        break;        
      case "addgroup":
        try {
          UsersTools.addgroup(sinput[1]);
        } catch (ArrayIndexOutOfBoundsException e) {
          help_for_command("addgroup");
        }
        break;
      case "moduser":
        try {
          UsersTools.moduser(sinput[1], sinput[2]);
        } catch (ArrayIndexOutOfBoundsException e) {          
          help_for_command("moduser");
        }
        break;
      case "showusers":
        if (sinput.length > 1) {
          UsersTools.showUsers(sinput[1]);
        } else {
          UsersTools.showUsers("");
        }
        break;
      case "showgroups":
        UsersTools.showGroups();
        break;
      case "showpermissions":
        UsersTools.showPermissions();
        break;
      case "listentities":
        DBEntities dbEntities = UsersTools.load_entites(sinput.length > 1 ? sinput[1] : "");
        
        if (UsersTools.format.equals("json")) {
          Gson gson = new Gson();
          List<DBEntity> combined = new ArrayList<DBEntity>();
          combined.addAll(dbEntities.projects);
          combined.addAll(dbEntities.algorthms);
          combined.addAll(dbEntities.tests);
          System.out.println(gson.toJson(combined));
        } else {
          UsersTools.listEntities();
        }
        break;
      case "chmod":
        try {
          String errorMsg = UsersTools.chmod(sinput[1], sinput[2], sinput[3]);
          if (!errorMsg.isEmpty())
            help_for_command(errorMsg, "chmod");          
        } catch (ArrayIndexOutOfBoundsException e) {          
          help_for_command("chmod");
        }        
        break;
      case "getentityid":
        try {
          System.out.println(UsersTools.findEntityId(sinput[1]));
        } catch (ArrayIndexOutOfBoundsException e) {          
          help_for_command("chmod");
        }        
        break;                
      case "changeuserstatus":
        try {
          UsersTools.changeUserStatus(sinput[1], sinput[2]);
        } catch (ArrayIndexOutOfBoundsException e) {          
          help_for_command("changeuserstatus");
        }
        break;
      case "changegroupstatus":
        try {
          UsersTools.changeGroupStatus(sinput[1], sinput[2]);
        } catch (ArrayIndexOutOfBoundsException e) {          
          help_for_command("changegroupstatus");
        }
        break;
      case "userperm":
        try {          
          int entity_id=UsersTools.ALL_PROJECTS_ID;   // default
          
          if (sinput.length > 2) { 
            if (sinput[2].equals("-all"))
              entity_id = UsersTools.ALL_ENTITIES_ID; // all entities
            else if (NumberUtils.isNumber(sinput[2])) 
              entity_id = Integer.parseInt(sinput[2]);
            else
              entity_id = UsersTools.findEntityId(sinput[2]);
          }                    
          UsersTools.userperm(sinput[1], entity_id);
          
        } catch (ArrayIndexOutOfBoundsException e) {
          help_for_command("userperm");
        }
        break;
      case "canuser":
        try {
          if (UsersTools.format.equals("json")) 
            System.out.println(new Gson().toJson(UsersTools.can_user(sinput[1], sinput[2], sinput[3])));
          else
            System.out.println(UsersTools.can_user(sinput[1], sinput[2], sinput[3]));
        } catch (ArrayIndexOutOfBoundsException e) {
          help_for_command("canuser");
          System.out.println(e);
        }
        break;
      case "setowner":
        try {
          UsersTools.setowner(sinput[1], sinput[2]);
        } catch (ArrayIndexOutOfBoundsException e) {
          System.out.println(e.toString());
          help_for_command("setowner");
        }
        break;

      case "insert_project":
        try {
          UsersTools.insertProjectToDB(sinput[1], currentUser);
        } catch (ArrayIndexOutOfBoundsException e) {
          System.out.println(e.toString());
          help_for_command("insert_project");
        }
        break;        

      case "insert_projects":
        try {
          UsersTools.insertAllProjectsToDB(currentUser);
        } catch (ArrayIndexOutOfBoundsException e) {
          System.out.println(e.toString());
          help_for_command("insert_projects");
        }
        break;
      case "showowner":
        try {
          String [] parts = sinput[1].split("/");
          if (parts.length == 1)
            UsersTools.showOwnerProj(parts[0]);
          else if (parts.length == 2)
            UsersTools.showOwnerAlg(parts[0], parts[1]);
          else if (parts.length == 3)
            UsersTools.showOwnerTest(parts[0], parts[2]);
        } catch (ArrayIndexOutOfBoundsException e) {
          help_for_command("showowner");
        }
        break;
        
      case "setformat":
        if (sinput[1].equals("json")) UsersTools.format="json"; else UsersTools.format="string";
        break;
        
      default:
        result = false;
        break;
    }
    return result;
  }

  private static void doConsoleInput() {
    while (true) {
      Scanner reader = new Scanner(System.in);
      System.out.print(">>> ");
      String input = reader.nextLine();

      if (input.equals("")) {
        continue;
      }
      String[] sinput = input.split(" ");

      main_switch(sinput);

      if (input.equals("exit")) {
        break;
      }
    }
  }
  
  private static void doConsoleInputWithJLIne() {
    try {
      ConsoleReader reader = new ConsoleReader();

      reader.setPrompt(">>> ");

      List<Completer> completors = new LinkedList<Completer>();
      completors.add(new StringsCompleter("cls", "quit", "exit", "chmod", "getentityid",
              "help","init","adduser","passwd","addgroup","moduser","showusers",
              "showgroups","showpermissions","listentities","changeuserstatus",
              "changegroupstatus", "userperm","canuser","insert_project",
              "insert_projects", "setowner", "showowner", "setformat"
      ));

      for (Completer c : completors) {
        reader.addCompleter(c);
      }

      String line;
      PrintWriter out = new PrintWriter(reader.getOutput());

      while ((line = reader.readLine()) != null) {
        if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
          return;
        }
        if (line.equalsIgnoreCase("cls")) {
          reader.clearScreen();
          continue;
        }
        
        String[] sinput = line.split(" ");
        boolean result = main_switch(sinput);

        if (!result) {
          out.println("Unknown command \"" + line + "\"");        
          out.flush();
        }
        
      }

    } catch (Exception e) {
      System.out.println("Error: " + e.toString());
    }
    
  }

  private static Options getOptions() {
    Options options = new Options();

    Option data_root = OptionBuilder.withArgName("folder")
	    .withLongOpt("data_root")
	    .hasArg(true)
	    .withDescription("use this folder as data_root; default value in $ALGATOR_DATA_ROOT (if defined) or $ALGATOR_ROOT/data_root")
	    .create("dr");
    
    Option algator_root = OptionBuilder.withArgName("folder")
            .withLongOpt("algator_root")
            .hasArg(true)
            .withDescription("use this folder as algator_root; default value in $ALGATOR_ROOT")
            .create("r");
    
    Option verbose = OptionBuilder.withArgName("verbose_level")
            .withLongOpt("verbose")
            .hasArg(true)
            .withDescription("print additional information (0 = OFF (default), 1 = some, 2 = all")
            .create("v");
    
    Option usernameO = OptionBuilder.withArgName("username")	    
	    .hasArg(true)
	    .withDescription("the name of the current user")
	    .create("u");

    Option passwordO = OptionBuilder.withArgName("password")	    
	    .hasArg(true)
	    .withDescription("the password of the current user")
	    .create("p");     

    options.addOption(data_root);
    options.addOption(algator_root);
        
    options.addOption(verbose);
    options.addOption(usernameO);
    options.addOption(passwordO);
            
    options.addOption("use", "usage", false, "print usage guide");
    
    return options;
  }

  
  public static void main(String[] args) {

    Options options = getOptions();

    CommandLineParser parser = new BasicParser();
    try {
      CommandLine line = parser.parse(options, args);

      String[] curArgs = line.getArgs();
            
      String algatorRoot = ATGlobal.getALGatorRoot();
      if (line.hasOption("algator_root")) {
        algatorRoot = line.getOptionValue("algator_root");        
      }
      ATGlobal.setALGatorRoot(algatorRoot);

      String dataRoot = ATGlobal.getALGatorDataRoot();
      if (line.hasOption("data_root")) {
	dataRoot = line.getOptionValue("data_root");
      }
      ATGlobal.setALGatorDataRoot(dataRoot);

      ATGlobal.logTarget = ATLog.TARGET_STDOUT;
      ATLog.setLogTarget(ATGlobal.logTarget);
      
      ELocalConfig localConfig = ELocalConfig.getConfig();
      String username=localConfig.getUsername();
      if (line.hasOption("u")) {
	username = line.getOptionValue("u");
      }      
      String password=localConfig.getPassword();
      if (line.hasOption("p")) {
	password = line.getOptionValue("p");
      }                              
      
      ATGlobal.verboseLevel = 3; // verbose  all messages in the shell
      
      if (!Database.databaseAccessGranted(username, password)) return;

      Connection conn  = Database.getConnectionToDatabase();
      if (conn == null) {
        String err = Database.isDatabaseMode() ?
           "Please, check your settings in the algator.acfg file." :     
           "File $ALGATOR_ROOT/anonymous is preventing connection.";        
        System.out.println("Can not connect to database. " + err);
        return;
      }
      
      currentUser = username;

      // execute action defined with arguments or ...
      if (curArgs.length > 0) {
        UsersTools.format = "json";
        main_switch(curArgs);
        return;
      }

      // ... open a console
      UsersTools.format = "string";
      System.out.println(
              "    ___     __    ______        __              \n"
            + "   /   |   / /   / ____/____ _ / /_ ____   _____\n"
            + "  / /| |  / /   / / __ / __ `// __// __ \\ / ___/\n"
            + " / ___ | / /___/ /_/ // /_/ // /_ / /_/ // /    \n"
            + "/_/  |_|/_____/\\____/ \\__,_/ \\__/ \\____//_/     \n"
            + "                                                ");
      System.out.println(">>> Welcome to Algator Users panel!");
      doConsoleInputWithJLIne();
      System.out.println(">>> Goodbye!");
    } catch (Exception e) {}

  }
}
