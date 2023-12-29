package si.fri.algator.analysis.view;

import java.awt.BorderLayout;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;
import si.fri.algator.entities.EPresenter;
import si.fri.algator.entities.EQuery;
import si.fri.algator.entities.Entity;
import si.fri.algator.entities.Project;
import si.fri.algator.global.ATGlobal;
import si.fri.algator.global.ErrorStatus;

/**
 *
 * @author tomaz
 */
public class ChartPanels extends javax.swing.JFrame {

  private Project project = null;

  int lastQueryNumber = 1;

  boolean izPrograma = true;

  ArrayList<QueryAndGraphPanel> queryAndGraphPanels;

  String computerID; // the ID of computer; the results are in computerID folder
  
  // the presenter given in the arguments; the presenter to be shown just after program starts
  EPresenter presenter;
  

  /**
   * Creates new form ChartPanel
   */
  public ChartPanels(java.awt.Frame parent, boolean modal, String computerID) {

    initComponents();
    queryAndGraphPanels = new ArrayList<QueryAndGraphPanel>();

    QueryAndGraphPanel queryAndGraphpanel = new QueryAndGraphPanel(computerID);
    jPanel9.add(queryAndGraphpanel);
    queryAndGraphPanels.add(queryAndGraphpanel);

    //Ziga Zorman: maximizes and shows this frame on the largest screen
    //setSize(Toolkit.getDefaultToolkit().getScreenSize());
    GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice[] monitors = environment.getScreenDevices();
    if (monitors.length > 0) {
      GraphicsDevice largestScreen = getLargestScreen(monitors);
      this.setLocation(largestScreen.getDefaultConfiguration().getBounds().x, largestScreen.getDefaultConfiguration().getBounds().y);
      this.setExtendedState(MAXIMIZED_BOTH);
    } else {
      throw new RuntimeException("No Screens Found");
    }

    izPrograma = false;
  }

  /**
   * @author: Ziga Zorman
   */
  private static GraphicsDevice getLargestScreen(GraphicsDevice[] screens) {
    GraphicsDevice largestScreen = screens[0];
    int largestWidth = screens[0].getDisplayMode().getWidth();
    int largestHeight = screens[0].getDisplayMode().getHeight();
    for (GraphicsDevice screen : screens) {
      DisplayMode displayMode = screen.getDisplayMode();
      int width = displayMode.getWidth();
      int height = displayMode.getHeight();
      if (width * height >= largestWidth * largestHeight) {
        largestHeight = height;
        largestWidth = width;
        largestScreen = screen;
      }
    }
    return largestScreen;
  }

  public ChartPanels(final Project project, final String computerID, EPresenter presenter) {
    java.awt.EventQueue.invokeLater(new Runnable() {
      public void run() {
        ChartPanels dialog = new ChartPanels(new javax.swing.JFrame(), true, computerID);
        dialog.setProject(project, computerID);        
        
        try{
          dialog.getCurrentQAG().setPresenter(presenter);
        }catch (Exception e) {}
        
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
          @Override
          public void windowClosing(java.awt.event.WindowEvent e) {
            System.exit(0);
          }
        });
        dialog.setVisible(true);
      }
    });
  }

  public void setProject(Project project, String computerID) {
    if (project == null) {
      return;
    }

    this.project = project;
    this.computerID = computerID;

    setTitle(String.format("ALGator analyzer - [%s] ", project.getEProject().getName()));

    getCurrentQAG().setProject(project);
  }

  private QueryAndGraphPanel getCurrentQAG() {
    int index = jTabbedPane1.getSelectedIndex();
    return queryAndGraphPanels.get(index);
  }

  private void addNewQueryTab() {
    if (jTabbedPane1.getSelectedIndex() == jTabbedPane1.getTabCount() - 1) {
      String queryName = "Query" + (++lastQueryNumber);
      int tabCount = jTabbedPane1.getTabCount();

      QueryAndGraphPanel qagp = new QueryAndGraphPanel(computerID);
      queryAndGraphPanels.add(qagp);
      qagp.setProject(project);

      JPanel nov = new JPanel(new BorderLayout());
      nov.add(qagp);

      jTabbedPane1.insertTab(queryName, null, nov, queryName, tabCount - 1);
      jTabbedPane1.setSelectedIndex(tabCount - 1);
    }
  }

  // save query (type=0) or presenter (type=1)
  void saveQueryOrPresentation(int type) {
    if (project == null) {
      return;
    }
    JFileChooser jfc = new JFileChooser();


    
    String qpRoot = "";
    String ident = "";
    String qpExtension = ".";
    if (type==0) {
      qpRoot=ATGlobal.getQUERIESroot(project.getEProject().getProjectRootDir());
      qpExtension = ATGlobal.AT_FILEEXT_query;
      ident = "query";
    } else if (type==1){ 
      qpRoot=ATGlobal.getPRESENTERSroot(project.getEProject().getProjectRootDir());
      qpExtension = ATGlobal.AT_FILEEXT_presenter;
      ident = "presenter";
    }

    jfc.setCurrentDirectory(new File(qpRoot));
    
    final String fqpExtension = qpExtension;
    final String fident = ident;
    jfc.setFileFilter(new FileFilter() {
      @Override
      public boolean accept(File f) {
        return f.getAbsolutePath().endsWith(fqpExtension);
      }

      @Override
      public String getDescription() {
        return "ALGator " + fident + " (*." + fqpExtension + ")";
      }
    });

    int ans = jfc.showSaveDialog(this);
    if (ans == JFileChooser.APPROVE_OPTION) {
      File fileToSave = jfc.getSelectedFile();
      if (!fileToSave.getPath().endsWith("." + fqpExtension)) {
        fileToSave = new File(fileToSave.getPath() + "." + fqpExtension);
      }

      String fileMsg = String.format("File %s exists. Overwrite?", fileToSave.getPath());
      boolean save = !fileToSave.exists()
              || (JOptionPane.showConfirmDialog(this, fileMsg, "Save "+ fident +" warning", JOptionPane.YES_NO_CANCEL_OPTION) == JOptionPane.YES_OPTION);

      if (save) {

        String dataToSave="";
        Entity entityToSave = null;
        if (type==0)
          entityToSave = getCurrentQAG().getQuery();
        else if (type==1)
          entityToSave = getCurrentQAG().getPresenter();
        if (entityToSave != null) {
          entityToSave.setName(fileToSave.getName().replace("."+fqpExtension, ""));
          dataToSave = entityToSave.toJSONString(true);
        }
        
        try (PrintWriter pw = new PrintWriter(fileToSave);) {
          pw.println(dataToSave);
        } catch (FileNotFoundException ex) {
          JOptionPane.showMessageDialog(this, ex.toString(), "Save " + fident +" error", JOptionPane.OK_OPTION);
        }
      }
    }
  }

// open query (type=0) or presenter (type=1)
  void openQueryOrPresentation(int type) {
    if (project == null) {
      return;
    }
    JFileChooser jfc = new JFileChooser();

    String qpRoot="";
    String qpExtension = ".";
    String ident="";
    if (type==0) {
      qpRoot = ATGlobal.getQUERIESroot(project.getEProject().getProjectRootDir());
      qpExtension = ATGlobal.AT_FILEEXT_query;
      ident="query";
    } else {
      qpRoot = ATGlobal.getPRESENTERSroot(project.getEProject().getProjectRootDir());
      qpExtension = ATGlobal.AT_FILEEXT_presenter;
      ident="presenter";
    }
    
    
    jfc.setCurrentDirectory(new File(qpRoot));
    
    String fExtension = qpExtension;
    String fident = ident;
    jfc.setFileFilter(new FileFilter() {
      @Override
      public boolean accept(File f) {
        return f.getAbsolutePath().endsWith(fExtension);
      }

      @Override
      public String getDescription() {
        return "ALGator " + fident + " (*." + fExtension + ")";
      }
    });

    int ans = jfc.showOpenDialog(this);
    if (ans == JFileChooser.APPROVE_OPTION) {
      File fileToOpen = jfc.getSelectedFile();
      
      Entity openEntity = new Entity();
      if (type==0)
        openEntity = new EQuery();
      else if (type==1)
        openEntity = new EPresenter();
      openEntity.initFromFile(fileToOpen);
      
      if (ErrorStatus.getLastErrorStatus() == ErrorStatus.STATUS_OK) {
        if (type==0)
          getCurrentQAG().setQuery((EQuery)openEntity);
        else if (type==1)
          getCurrentQAG().setPresenter((EPresenter)openEntity);

        getCurrentQAG().run(new ActionEvent(new JCheckBox(), 0, "Re-run graph"));
      }
    }
  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {
    java.awt.GridBagConstraints gridBagConstraints;

    jTabbedPane1 = new javax.swing.JTabbedPane();
    jPanel9 = new javax.swing.JPanel();
    jPanel1 = new javax.swing.JPanel();
    jMenuBar1 = new javax.swing.JMenuBar();
    jMenu1 = new javax.swing.JMenu();
    jMenuItem1 = new javax.swing.JMenuItem();
    jMenuItem4 = new javax.swing.JMenuItem();
    jSeparator2 = new javax.swing.JPopupMenu.Separator();
    jMenuItem2 = new javax.swing.JMenuItem();
    jMenuItem5 = new javax.swing.JMenuItem();
    jSeparator1 = new javax.swing.JPopupMenu.Separator();
    jMenuItem3 = new javax.swing.JMenuItem();

    setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
    getContentPane().setLayout(new java.awt.GridBagLayout());

    jTabbedPane1.addChangeListener(new javax.swing.event.ChangeListener() {
      public void stateChanged(javax.swing.event.ChangeEvent evt) {
        jTabbedPane1StateChanged(evt);
      }
    });

    jPanel9.setLayout(new java.awt.BorderLayout());
    jTabbedPane1.addTab("Query1", jPanel9);
    jTabbedPane1.addTab("+", jPanel1);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    getContentPane().add(jTabbedPane1, gridBagConstraints);

    jMenu1.setText("File");

    jMenuItem1.setText("Open query ...");
    jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jMenuItem1ActionPerformed(evt);
      }
    });
    jMenu1.add(jMenuItem1);

    jMenuItem4.setText("Open presentation ...");
    jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jMenuItem4ActionPerformed(evt);
      }
    });
    jMenu1.add(jMenuItem4);
    jMenu1.add(jSeparator2);

    jMenuItem2.setText("Save query ...");
    jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jMenuItem2ActionPerformed(evt);
      }
    });
    jMenu1.add(jMenuItem2);

    jMenuItem5.setText("Save presentation ...");
    jMenuItem5.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jMenuItem5ActionPerformed(evt);
      }
    });
    jMenu1.add(jMenuItem5);
    jMenu1.add(jSeparator1);

    jMenuItem3.setText("Quit");
    jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jMenuItem3ActionPerformed(evt);
      }
    });
    jMenu1.add(jMenuItem3);

    jMenuBar1.add(jMenu1);

    setJMenuBar(jMenuBar1);

    pack();
  }// </editor-fold>//GEN-END:initComponents

  private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
    System.exit(0);
  }//GEN-LAST:event_jMenuItem3ActionPerformed

  private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
    saveQueryOrPresentation(0);
  }//GEN-LAST:event_jMenuItem2ActionPerformed

  private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
    openQueryOrPresentation(0);
  }//GEN-LAST:event_jMenuItem1ActionPerformed

  private void jTabbedPane1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jTabbedPane1StateChanged
    if (!izPrograma) {
      izPrograma = true;
      addNewQueryTab();
      izPrograma = false;
    }
  }//GEN-LAST:event_jTabbedPane1StateChanged

  private void jMenuItem5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem5ActionPerformed
    saveQueryOrPresentation(1);
  }//GEN-LAST:event_jMenuItem5ActionPerformed

  private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem4ActionPerformed
    openQueryOrPresentation(1);
  }//GEN-LAST:event_jMenuItem4ActionPerformed


  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JMenu jMenu1;
  private javax.swing.JMenuBar jMenuBar1;
  private javax.swing.JMenuItem jMenuItem1;
  private javax.swing.JMenuItem jMenuItem2;
  private javax.swing.JMenuItem jMenuItem3;
  private javax.swing.JMenuItem jMenuItem4;
  private javax.swing.JMenuItem jMenuItem5;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JPanel jPanel9;
  private javax.swing.JPopupMenu.Separator jSeparator1;
  private javax.swing.JPopupMenu.Separator jSeparator2;
  private javax.swing.JTabbedPane jTabbedPane1;
  // End of variables declaration//GEN-END:variables
}
