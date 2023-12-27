package si.fri.algator.analysis.view;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JComponent;
import org.asql.ASqlObject;
import org.json.JSONObject;
import si.fri.algator.analysis.AlgInterpreter;
import si.fri.algator.analysis.TableData;
import si.fri.algator.entities.EProject;
import si.fri.algator.entities.EQuery;
import si.fri.algator.entities.Project;
import si.fri.algator.global.ATGlobal;
import si.fri.algator.tools.ATTools;

/**
 *
 * @author ernest, judita
 */
public class ASQLComposer extends javax.swing.JPanel implements IQueryComposer {

    private ArrayList<String> algorithms = new ArrayList<>();
    private ArrayList<String> testsets = new ArrayList<>();
    private ArrayList<String> inputParameters = new ArrayList<>();
    private ArrayList<String> outputParameters = new ArrayList<>();
    private Project project;

    ActionListener ipButtonListener, opButtonListener, algButtonListener, tsButtonListener, calcButtonListener;

    /**
     * Creates new form ASQLComposer
     */
    public ASQLComposer() {
        initComponents();

        ipButtonListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String ip = ((JButton) e.getSource()).getText();
                setASQLText(ip, "SELECT ", ", ");
                asqlText.requestFocus();
            }

        };

        opButtonListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String ip = ((JButton) e.getSource()).getText();
                setASQLText(ip, "SELECT ", ", ");
                asqlText.requestFocus();
            }
        };

        algButtonListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String ip = ((JButton) e.getSource()).getText();
                setASQLText("algorithm=" + ip, "WHERE (", " OR ");
                asqlText.requestFocus();
            }
        };

        tsButtonListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String ip = ((JButton) e.getSource()).getText();
                setASQLText(ip, "FROM ", ", ");
                asqlText.requestFocus();
            }
        };

        calcButtonListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                String op = ((JButton) ae.getSource()).getText();
                asqlText.insert(op, asqlText.getCaretPosition());
                if (op.endsWith(")")) {
                    asqlText.setCaretPosition(asqlText.getCaretPosition() - 1);
                }
                asqlText.requestFocus();
            }
        };

        computerIDTF.setText(ATGlobal.getThisComputerID());

        asqlText.setText("FROM \n"
                + "WHERE ( \n"
                + ") \n"
                + "AND \n"
                + "AND ComputerID=" + computerIDTF.getText() + "\n"
                + "SELECT \n"
                + "GROUPBY \n"
                + "ORDERBY \n");

    }

    private String getASQLString() {
        return asqlText.getText() + "\n";
    }

    private void setASQLText(String p, String rowStart, String delimeter) {
        String[] rows = asqlText.getText().split("\n");
        ArrayList<String> newRows = new ArrayList<>();
        boolean done = false;
        int caretPosition = 0;
        for (String row : rows) {
            if (!done && row.startsWith(rowStart)) {
                done = true;
                String[] params = row.replace(rowStart, "").split(delimeter);
                boolean exists = false;
                for (String param : params) {
                    if (param.trim().equals(p)) {
                        exists = true;
                        if (row.length() == row.indexOf(param) + param.length()) {
                            row = row.replace(param.trim(), "");
                        } else {
                            row = row.replace(param + delimeter, "");
                        }
                        for (String del : new String[]{", ,", ",, ", " AND  AND ", " OR  OR", " AND AND ", " OR OR"}) {
                            row = row.trim().replace(del, del.substring(0, del.indexOf(" ", 1)));
                        }

                        if (row.endsWith(delimeter.trim())) {
                            row = row.substring(0, row.length() - delimeter.trim().length());
                        }
                        row = row.replace(rowStart + delimeter, rowStart);
                        if (row.trim().equals(rowStart.trim())) {
                            row = rowStart;
                        }
                    }
                }
                if (!exists) {
                    if (!row.trim().equals(rowStart.trim())) {
                        row = row + delimeter + p;
                    } else {
                        row = row + p;
                    }
                }
                while (row.contains("  ")) {
                    row = row.replace("  ", " ");
                }
                newRows.add(row);
                caretPosition += row.length();
            } else {
                newRows.add(row);
                if (!done) {
                    caretPosition += row.length() + 1;
                }
            }
        }
        asqlText.setText(ATTools.stringJoin("\n", newRows));
        if (asqlText.getText().length() >= caretPosition && caretPosition >= 0) {
            asqlText.setCaretPosition(caretPosition);
        }
    }

    private JButton createButton(String text, ActionListener listener, JComponent cmp) {
        JButton btn = new JButton(text);
        btn.setSize(100, 30);
        btn.addActionListener(listener);
        cmp.add(btn);
        if (listener == calcButtonListener) {
            btn.addKeyListener(new KeyListener() {
                @Override
                public void keyTyped(KeyEvent ke) {
                    calculatedPanelKeyTyped(ke);
                }

                @Override
                public void keyPressed(KeyEvent ke) {
                }

                @Override
                public void keyReleased(KeyEvent ke) {
                }
            }
            );
        }
        return btn;
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

    jSplitPane1 = new javax.swing.JSplitPane();
    jTabbedPane1 = new javax.swing.JTabbedPane();
    algsPanel = new javax.swing.JPanel();
    testsetsPanel = new javax.swing.JPanel();
    inputsPanel = new javax.swing.JPanel();
    jScrollPane3 = new javax.swing.JScrollPane();
    outputsPanel = new javax.swing.JPanel();
    filterPanel = new javax.swing.JPanel();
    countCB = new javax.swing.JCheckBox();
    jLabel7 = new javax.swing.JLabel();
    filterTF = new javax.swing.JTextField();
    filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 30), new java.awt.Dimension(0, 30), new java.awt.Dimension(0, 32767));
    jLabel6 = new javax.swing.JLabel();
    groupbyTF = new javax.swing.JTextField();
    filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 32767));
    jLabel8 = new javax.swing.JLabel();
    sortbyTF = new javax.swing.JTextField();
    computerIDTF = new javax.swing.JTextField();
    jLabel1 = new javax.swing.JLabel();
    jScrollPane2 = new javax.swing.JScrollPane();
    calculatedPanel = new javax.swing.JPanel();
    jScrollPane1 = new javax.swing.JScrollPane();
    asqlText = new javax.swing.JTextArea();

    setLayout(new java.awt.GridLayout(1, 0));

    jSplitPane1.setDividerLocation(180);
    jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

    jTabbedPane1.addChangeListener(new javax.swing.event.ChangeListener() {
      public void stateChanged(javax.swing.event.ChangeEvent evt) {
        tabStateChanged(evt);
      }
    });

    algsPanel.setAutoscrolls(true);
    jTabbedPane1.addTab("Algorithms", algsPanel);

    testsetsPanel.setAutoscrolls(true);
    jTabbedPane1.addTab("Testsets", testsetsPanel);

    inputsPanel.setAutoscrolls(true);
    jTabbedPane1.addTab("Input fields", inputsPanel);

    outputsPanel.setLayout(new java.awt.GridLayout(5, 0));
    jScrollPane3.setViewportView(outputsPanel);

    jTabbedPane1.addTab("Output fields", jScrollPane3);

    filterPanel.setAutoscrolls(true);
    java.awt.GridBagLayout filterPanelLayout = new java.awt.GridBagLayout();
    filterPanelLayout.columnWidths = new int[] {0, 3, 0, 3, 0, 3, 0, 3, 0, 3, 0};
    filterPanelLayout.rowHeights = new int[] {0, 3, 0, 3, 0, 3, 0};
    filterPanel.setLayout(filterPanelLayout);

    countCB.setText("COUNT");
    countCB.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    countCB.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
    countCB.addItemListener(new java.awt.event.ItemListener() {
      public void itemStateChanged(java.awt.event.ItemEvent evt) {
        countCBItemStateChanged(evt);
      }
    });
    countCB.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        countCBActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 0;
    filterPanel.add(countCB, gridBagConstraints);

    jLabel7.setText("Filter");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
    filterPanel.add(jLabel7, gridBagConstraints);

    filterTF.setColumns(20);
    filterTF.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        filterTFActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    filterPanel.add(filterTF, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 6;
    gridBagConstraints.gridy = 0;
    filterPanel.add(filler1, gridBagConstraints);

    jLabel6.setText("GroupBy");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
    filterPanel.add(jLabel6, gridBagConstraints);

    groupbyTF.setColumns(20);
    groupbyTF.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        groupbyTFActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 4;
    filterPanel.add(groupbyTF, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 8;
    gridBagConstraints.gridy = 0;
    filterPanel.add(filler2, gridBagConstraints);

    jLabel8.setText("SortBy");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
    filterPanel.add(jLabel8, gridBagConstraints);

    sortbyTF.setColumns(20);
    sortbyTF.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        sortbyTFActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 6;
    filterPanel.add(sortbyTF, gridBagConstraints);

    computerIDTF.setColumns(10);
    computerIDTF.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        computerIDTFActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 4;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
    filterPanel.add(computerIDTF, gridBagConstraints);

    jLabel1.setText("Computer ID");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
    filterPanel.add(jLabel1, gridBagConstraints);

    jTabbedPane1.addTab("Filter, Grouping, Sorting", filterPanel);

    calculatedPanel.setAutoscrolls(true);
    calculatedPanel.addKeyListener(new java.awt.event.KeyAdapter() {
      public void keyTyped(java.awt.event.KeyEvent evt) {
        calculatedPanelKeyTyped(evt);
      }
    });
    calculatedPanel.setLayout(new java.awt.GridLayout(5, 0));
    jScrollPane2.setViewportView(calculatedPanel);

    jTabbedPane1.addTab("Calculated fields", jScrollPane2);

    jSplitPane1.setLeftComponent(jTabbedPane1);

    asqlText.setColumns(20);
    asqlText.setRows(5);
    jScrollPane1.setViewportView(asqlText);

    jSplitPane1.setRightComponent(jScrollPane1);

    add(jSplitPane1);
  }// </editor-fold>//GEN-END:initComponents

    private void countCBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_countCBActionPerformed
        // innerChangeListner.actionPerformed(new ActionEvent(countCB, 0, "Re-run"));
    }//GEN-LAST:event_countCBActionPerformed

    private void countCBItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_countCBItemStateChanged
        setASQLText("COUNT(*)", "SELECT ", ", ");
    }//GEN-LAST:event_countCBItemStateChanged

    private void sortbyTFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sortbyTFActionPerformed
        setASQLText(sortbyTF.getText(), "ORDERBY ", ", ");
    }//GEN-LAST:event_sortbyTFActionPerformed

    private void groupbyTFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_groupbyTFActionPerformed
        setASQLText(groupbyTF.getText(), "GROUPBY ", ", ");
    }//GEN-LAST:event_groupbyTFActionPerformed

    private void filterTFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterTFActionPerformed
        setASQLText(filterTF.getText(), "AND", ", ");
    }//GEN-LAST:event_filterTFActionPerformed

    private void tabStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_tabStateChanged
        try {
            if (jTabbedPane1.getSelectedIndex() == 5) {
                int index = asqlText.getText().indexOf("\n", asqlText.getText().indexOf("SELECT "));
                asqlText.setCaretPosition(index);
            }
        } catch (Exception ex) {
        }
    }//GEN-LAST:event_tabStateChanged

    private void calculatedPanelKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_calculatedPanelKeyTyped
    }//GEN-LAST:event_calculatedPanelKeyTyped

    private void computerIDTFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_computerIDTFActionPerformed
        String[] rows = asqlText.getText().split("\n");
        ArrayList<String> newRows = new ArrayList<>();
        boolean done = false;
        for (String row : rows) {
            if (!done && row.startsWith("AND ComputerID=")) {
                row = "AND ComputerID=" + computerIDTF.getText();
            }
            newRows.add(row);
        }
        asqlText.setText(ATTools.stringJoin("\n", newRows));
    }//GEN-LAST:event_computerIDTFActionPerformed


  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JPanel algsPanel;
  private javax.swing.JTextArea asqlText;
  private javax.swing.JPanel calculatedPanel;
  private javax.swing.JTextField computerIDTF;
  private javax.swing.JCheckBox countCB;
  private javax.swing.Box.Filler filler1;
  private javax.swing.Box.Filler filler2;
  private javax.swing.JPanel filterPanel;
  private javax.swing.JTextField filterTF;
  private javax.swing.JTextField groupbyTF;
  private javax.swing.JPanel inputsPanel;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JLabel jLabel6;
  private javax.swing.JLabel jLabel7;
  private javax.swing.JLabel jLabel8;
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JScrollPane jScrollPane2;
  private javax.swing.JScrollPane jScrollPane3;
  private javax.swing.JSplitPane jSplitPane1;
  private javax.swing.JTabbedPane jTabbedPane1;
  private javax.swing.JPanel outputsPanel;
  private javax.swing.JTextField sortbyTF;
  private javax.swing.JPanel testsetsPanel;
  // End of variables declaration//GEN-END:variables

    @Override
    public EQuery getQuery() {
        EQuery query = new EQuery();
        ASqlObject lo = new ASqlObject(getASQLString());
        query.initFromJSON(lo.getJSONObject(project).get("Query").toString());
        return query;
    }

    @Override
    public void setOuterChangeListener(ActionListener action) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setProject(Project project, String computerID) {
        if (project == null) {
            return;
        }

        this.project = project;
        EProject eProject = project.getEProject();
        if (eProject == null) {
            return;
        }

        String[] algs = eProject.getStringArray(EProject.ID_Algorithms);
        String[] tsts = eProject.getStringArray(EProject.ID_TestSets);
        String[] inPars = Project.getTestParameters(project.getTestCaseDescription());
        String[] outPars = Project.getIndicators(project.getResultDescriptions());

        createButton("*", algButtonListener, algsPanel);
        createButton("*", tsButtonListener, testsetsPanel);
        createButton("*", ipButtonListener, inputsPanel);
        createButton("*", opButtonListener, outputsPanel);

        for (String alg : algs) {
            createButton(alg, algButtonListener, algsPanel);
        }

        for (String ts : tsts) {
            createButton(ts, tsButtonListener, testsetsPanel);
        }

        for (String ip : inPars) {
            createButton(ip, ipButtonListener, inputsPanel);
        }

        for (String op : new String[]{"*EM", "*CNT", "*JVM"}) {
            createButton(op, opButtonListener, outputsPanel);
        }

        for (String op : new String[]{", ", "+", "-", "*", "/"}) {
            createButton(op, calcButtonListener, calculatedPanel);
        }

        for (String op : AlgInterpreter.mathMembers) {
            createButton(op + "()", calcButtonListener, calculatedPanel);
        }

        for (String op : outPars) {
            createButton(op, opButtonListener, outputsPanel);
            createButton(op, calcButtonListener, calculatedPanel);
        }

        algsPanel.revalidate();
        testsetsPanel.revalidate();
        inputsPanel.revalidate();
        outputsPanel.revalidate();
        algsPanel.repaint();
        testsetsPanel.repaint();
        inputsPanel.repaint();
        outputsPanel.repaint();
    }

    @Override
    public void setQuery(EQuery query) {
        asqlText.setText(ASqlObject.initFromJSON(new JSONObject(query.toJSONString(true))).getASQLString());
    }

    @Override
    public String getComputerID() {
        return computerIDTF.getText();
    }

    @Override
    public TableData runQuery() {
        ASqlObject lo = new ASqlObject(asqlText.getText());
        return lo.runQuery(project);
    }

}
