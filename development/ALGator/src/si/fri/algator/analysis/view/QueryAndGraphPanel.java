/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.fri.algator.analysis.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.TreeSet;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.json.JSONArray;
import org.math.plot.Plot2DPanel;
import si.fri.algator.analysis.DataAnalyser;
import si.fri.algator.analysis.TableData;
import si.fri.algator.entities.EPresenter;
import si.fri.algator.entities.EQuery;
import si.fri.algator.entities.GraphType;
import si.fri.algator.entities.NameAndAbrev;
import si.fri.algator.entities.Project;

/**
 *
 * @author tomaz
 */
public class QueryAndGraphPanel extends javax.swing.JPanel {

    SeriesSelectNew seriesSelect1;
    private Project project = null;
    private String computerID;

    Plot2DPanel plotPanel = null;

    boolean izPrograma = true;

    /**
     * Creates new form QueryAndGraphPanel
     */
    public QueryAndGraphPanel(String computerID) {
        initComponents();

        this.computerID = computerID;

        ActionListener onSeriesSelectButtonChange = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // ActionEvent has to be null (and not e) - see jButton4ActionPerformed for details
                jButton4ActionPerformed(null);
            }
        };

        seriesSelect1 = new SeriesSelectNew(onSeriesSelectButtonChange);

        JPanel xypanel = new JPanel(new BorderLayout());
        graphPanel.add(xypanel, BorderLayout.PAGE_END);

        JScrollPane scp = new JScrollPane(seriesSelect1);
        scp.setPreferredSize(new Dimension(100, 90));
        xypanel.add(scp);

        Toolkit tk = Toolkit.getDefaultToolkit();
        setSize(tk.getScreenSize().width, tk.getScreenSize().width);

        queryComposer1.setOuterChangeListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jButton4ActionPerformed(e);
            }
        });

        izPrograma = false;
    }

    public void setProject(Project project) {
        if (project == null) {
            return;
        }

        this.project = project;
        getCurrentComposer().setProject(project, computerID);
    }

    public EQuery getQuery() {
        return getCurrentComposer().getQuery();
    }

    public void setQuery(EQuery query) {
        getCurrentComposer().setQuery(query);
    }
    
    
    public EPresenter getPresenter() {
      EPresenter presenter = new EPresenter();
      presenter.setQuery(getQuery());
      presenter.set(EPresenter.ID_HasGraph, "1");
      presenter.set(EPresenter.ID_Xaxis,seriesSelect1.getXField());
      presenter.set(EPresenter.ID_Yaxes,new JSONArray(seriesSelect1.getYFieldsID()));
      presenter.setGraphTypes(seriesSelect1.getGraphType());      
      return presenter;
    }

    public void setPresenter(EPresenter presenter) {
      EQuery query = presenter.getQuery();
      getCurrentComposer().setQuery(query);
      run(new ActionEvent(new JCheckBox(), 0, "Re-run graph")); // to run the query (and thus create fields in XCOmbo)
      seriesSelect1.setXField(presenter.getField(EPresenter.ID_Xaxis));
      
      String [] yFIelds = presenter.getStringArray(EPresenter.ID_Yaxes);
      seriesSelect1.setYFieldsID(yFIelds);
            
      seriesSelect1.setGraphType(presenter.getGraphTypes());
    }
    

    public void run(ActionEvent evt) {
        
        EQuery query = getCurrentComposer().getQuery();

        System.out.println(query.toJSONString(true));

        TableData td = getCurrentComposer().runQuery();
        if (td == null) {
            return;
        }

        // this action is triggered by many events; to prevent changing the contenet 
        // if seriesSelect panel, addFields is called only  when CheckBox is the trigger 
        // (one of the Fields is changed)
        if (evt != null && evt.getSource() instanceof JCheckBox) {
            ArrayList<String> fields = new ArrayList<>();

            ArrayList<String> outParams = new ArrayList<>();
            for (String op : query.getStringArray(EQuery.ID_Indicators)) {
                outParams.add(new NameAndAbrev(op).getAbrev());
            }
            outParams.add("COUNT");

            // two possibilities: in the table are fields or in tha table are COUNTs
            int start = td.header.size() > 0 && td.header.get(0).equals("#") ? 0 : 3;

            for (int i = start; i < td.header.size(); i++) {
                String field = td.header.get(i);  // field = N or Java7.Tavg, .... or *.EM, *.CNT, *.JVM

                if (field.startsWith("*")) {
                    continue;
                }

                int pikaPos = field.lastIndexOf('.');
                String lastPart = pikaPos != -1 ? field.substring(pikaPos + 1) : field;   // lastPart = only last part (N or Tavg, ...)

                String genField = "*." + lastPart;
                if (/*outParams.contains(lastPart) &&*/!fields.contains(genField)) {
                    fields.add(genField);
                }

                fields.add(field);
            }
            seriesSelect1.addFields(fields);
        }
        DefaultTableModel dtm = new DefaultTableModel(td.getDataAsArray(), td.header.toArray());
        dataTable.setModel(dtm);

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setBackground(new Color(239, 198, 46));

        for (int i = 0; i < dataTable.getModel().getColumnCount(); i++) {
            if (i < td.numberOfInputParameters) {
                dataTable.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
            }
        }

        if (td.data != null && td.data.size() > 0 && td.data.get(0).size() >= 2) {
            try {
                int xFieldID = getFieldID(td, seriesSelect1.getXField());
                int yFieldIDs[] = getFieldsID(td, seriesSelect1.getYFieldsID());

                TreeSet<GraphType> graphType = seriesSelect1.getGraphType();

                if (xFieldID >= 0 && yFieldIDs.length > 0) {
                    drawGraph(td, graphPanel, xFieldID, yFieldIDs, graphType);
                }
            } catch (Exception e) {
            }
        }
    }

    /**
     * Draw y.length graphs with X-axis be the x-th column and y-axis the y[i]
     * column of td.
     */
    private void drawGraph(TableData td, JPanel outPanel, int xAxis, int[] yAxes, TreeSet<GraphType> graphType) {
        if (td.data.size() == 0 || td.data.get(0).size() < 2) {
            return;
        }

        if (plotPanel != null) {
            outPanel.remove(plotPanel);
        }

        boolean drawLine  = graphType.contains(GraphType.LINE);
        boolean drawStair = graphType.contains(GraphType.STAIR);;
        boolean drawBar   = graphType.contains(GraphType.BAR);;
        boolean drawBox   = graphType.contains(GraphType.BOX);;
//        boolean drawCloud = (graphType & 16) != 0;
//        boolean drawHist = (graphType & 32) != 0;

        plotPanel = new Plot2DPanel();
        double[] x = getDoubleArray(td.data, xAxis);

        for (int i = 0; i < yAxes.length; i++) {
            double[] y = getDoubleArray(td.data, yAxes[i]);
            if (drawLine) {
                plotPanel.addLinePlot((String) td.header.get(yAxes[i]), x, y);
            }

            if (drawBar) {
                plotPanel.addBarPlot((String) td.header.get(yAxes[i]), x, y);
            }

            if (drawStair) {
                plotPanel.addStaircasePlot((String) td.header.get(yAxes[i]), x, y);
            }
        }

        if (drawBox) {
            int W = 30;

            int numberOfColumns = yAxes.length;
            int numberOfRows = td.data.size();

            double maxData = 0;

            for (int col = 0; col < numberOfColumns; col++) {
                double podatki[][] = new double[numberOfRows][4];

                for (int i = 0; i < numberOfRows; i++) {
                    double data = 0;
                    try {
                        data = Double.parseDouble(String.valueOf(td.data.get(i).get(yAxes[col])));
                    } catch (Exception e) {
                    }
                    if (data > maxData) {
                        maxData = data;
                    }

                    podatki[i][0] = (i * (numberOfColumns + 1) + col + 1) * W;
                    podatki[i][1] = data / 2;
                    podatki[i][2] = W;
                    podatki[i][3] = data;
                }

                plotPanel.addBoxPlot(td.header.get(yAxes[col]), podatki);
            }

            plotPanel.setFixedBounds(0, 0, (numberOfColumns + 1) * numberOfRows * W);
            plotPanel.setFixedBounds(1, 0, maxData);
        }

        plotPanel.addLegend("SOUTH");
        outPanel.add(plotPanel);
        jSplitPane2.revalidate();
    }

    private double[] getDoubleArray(ArrayList<ArrayList<Object>> list, int col) {
        if (list == null || list.get(0).size() < col) {
            return new double[0];
        }

        double[] result = new double[list.size()];
        for (int i = 0; i < result.length; i++) {
            double value = 0;
            try {
                String sv = String.valueOf(list.get(i).get(col));
                value = Double.parseDouble(sv);
            } catch (Exception e) {
                value = 0;
            }
            result[i] = value;
        }
        return result;
    }

    private int getFieldID(TableData td, String fieldName) {
        for (int i = 0; i < td.header.size(); i++) {
            if (td.header.get(i).equals(fieldName)) {
                return i;
            }
        }
        return -1;
    }

    private int[] getFieldsID(TableData td, String[] fieldsName) {
        ArrayList<Integer> ids = new ArrayList<Integer>();
        for (String fieldName : fieldsName) {
            if (fieldName.startsWith("*")) {
                String suffix = fieldName.substring(1);
                for (int i = 0; i < td.header.size(); i++) {
                    if (td.header.get(i).endsWith(suffix)) {
                        ids.add(i);
                    }
                }
            } else {
                int pos = getFieldID(td, fieldName);
                if (pos != -1) {
                    ids.add(pos);
                }
            }
        }
        int[] result = new int[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            result[i] = ids.get(i);
        }
        return result;
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

        jSplitPane2 = new javax.swing.JSplitPane();
        jSplitPane1 = new javax.swing.JSplitPane();
        qPanel = new javax.swing.JPanel();
        queryComposer1 = new si.fri.algator.analysis.view.QueryComposer();
        jButton4 = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        dataTable = new javax.swing.JTable();
        jPanel13 = new javax.swing.JPanel();
        graphPanel = new javax.swing.JPanel();

        setLayout(new java.awt.GridBagLayout());

        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        qPanel.setMinimumSize(new java.awt.Dimension(650, 400));
        qPanel.setPreferredSize(new java.awt.Dimension(500, 400));
        qPanel.setLayout(new java.awt.GridBagLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 338;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        qPanel.add(queryComposer1, gridBagConstraints);

        jButton4.setText("Run!");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        qPanel.add(jButton4, gridBagConstraints);

        jButton1.setText("A-SQL");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                linqButtonClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        qPanel.add(jButton1, gridBagConstraints);

        jSplitPane1.setLeftComponent(qPanel);

        dataTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        jScrollPane3.setViewportView(dataTable);

        jSplitPane1.setRightComponent(jScrollPane3);

        jSplitPane2.setLeftComponent(jSplitPane1);

        jPanel13.setLayout(new java.awt.BorderLayout());

        graphPanel.setLayout(new java.awt.BorderLayout());
        jPanel13.add(graphPanel, java.awt.BorderLayout.CENTER);

        jSplitPane2.setRightComponent(jPanel13);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(jSplitPane2, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

  private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
      if (!izPrograma) {
          run(evt);
      }
  }//GEN-LAST:event_jButton4ActionPerformed

    private IQueryComposer currentComposer;

    private IQueryComposer getCurrentComposer() {
        if (currentComposer == null) {
            currentComposer = queryComposer1;
        }
        return currentComposer;
    }

    private void linqButtonClicked(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_linqButtonClicked
        IQueryComposer newComposer;
        JButton button = ((JButton) evt.getSource());
        String compid = getCurrentComposer().getComputerID();
        Component c = (Component) getCurrentComposer();
        Container parent = c.getParent();
        parent.remove(c);
        if (button.getText().equals("JSON")) {
            newComposer = new QueryComposer();
            newComposer.setOuterChangeListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    jButton4ActionPerformed(e);
                }
            });
            button.setText("A-SQL");
        } else {
            newComposer = new ASQLComposer();
            button.setText("JSON");
        }
        newComposer.setProject(project, compid);
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 338;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        parent.add((Component) newComposer, gridBagConstraints);
        currentComposer = newComposer;
        revalidate();
    }//GEN-LAST:event_linqButtonClicked

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTable dataTable;
    private javax.swing.JPanel graphPanel;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton4;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JPanel qPanel;
    private si.fri.algator.analysis.view.QueryComposer queryComposer1;
    // End of variables declaration//GEN-END:variables
}
