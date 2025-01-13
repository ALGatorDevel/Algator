package si.fri.algator.server;

import java.io.File;
import si.fri.algator.ausers.CanUtil;
import static si.fri.algator.ausers.CanUtil.accessDeniedString;
import si.fri.algator.entities.EProject;
import si.fri.algator.global.ATGlobal;
import static si.fri.algator.server.ASTools.OK_STATUS;
import static si.fri.algator.server.ASTools.sAnswer;
import java.io.IOException;
import java.util.Base64;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.json.JSONObject;

/**
 *
 * @author tomaz
 */
public class ASJARTools {
    public static String getJarFileContent(String uid, String projectName, String fileName) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName)) {
      return sAnswer(2, String.format("Project '%s' does not exist.", projectName), "");
    }

    String eid = EProject.getProject(projectName).getEID();
    if (!CanUtil.can(uid, eid, "can_read")) {
      return sAnswer(99, "getJarFileContent: " + accessDeniedString, accessDeniedString);
    }

    File jarFolder = new File(ATGlobal.getPROJECTlibPath(projectName));
    File jarFile   = new File(jarFolder, fileName);
    if (!jarFile.exists())
      return sAnswer(4, String.format("File '%s' does not exist.", fileName), "");

    String result = "unknown content";
    try {result = getJarTreeAsHtml(jarFile.getPath());} catch (Exception e) {}
    String encoded = Base64.getEncoder().encodeToString(result.getBytes());
    return ASTools.sAnswer(OK_STATUS, String.format("JAR file '%s' content.", fileName), encoded);
  }

  
   
    public static String getJarTreeAsHtml(String jarFilePath) throws IOException {
        // HTML structure initialization
        StringBuilder htmlBuilder = new StringBuilder();
        htmlBuilder.append("<ul>");

        // Open the JAR file
        try (JarFile jarFile = new JarFile(jarFilePath)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            
            // Tree map to organize entries
            Map<String, Object> tree = new HashMap<>();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                addToTree(tree, entry.getName().split("/"));
            }
            
            // Render HTML from the tree structure
            renderTree(tree, htmlBuilder);
        }

        htmlBuilder.append("</ul>");
        return htmlBuilder.toString();
    }

    // Helper method to add a path into the tree structure
    private static void addToTree(Map<String, Object> tree, String[] pathParts) {
        Map<String, Object> current = tree;
        for (String part : pathParts) {
            current.putIfAbsent(part, new HashMap<>());
            current = (Map<String, Object>) current.get(part);
        }
    }

    // Helper method to render HTML from the tree structure
    private static void renderTree(Map<String, Object> tree, StringBuilder htmlBuilder) {
        for (String key : tree.keySet()) {
            htmlBuilder.append("<li>").append(key);
            Map<String, Object> children = (Map<String, Object>) tree.get(key);
            if (!children.isEmpty()) {
                htmlBuilder.append("<ul>");
                renderTree(children, htmlBuilder);
                htmlBuilder.append("</ul>");
            }
            htmlBuilder.append("</li>");
        }
    }
}

