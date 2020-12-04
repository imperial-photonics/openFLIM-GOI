/* 
 * Copyright (C) 2020 Imperial College London.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 * 
 * @author <sunil.kumar@imperial.ac.uk>
 */
package MM2.openFLIM_GOI.UI;

import MM2.openFLIM_GOI.Utilities.TableCellListener;
import java.util.ArrayList;
import java.util.Vector;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import MM2.openFLIM_GOI.Utilities.OpenFLIM_GOI_MM2_Utils;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.commons.lang3.ArrayUtils;

/**
 *
 * @author Fogim
 */
public class del_ind_panel extends javax.swing.JPanel {
    DefaultTableModel del_tbl_model;
    String curr_del_ind_startstr;
    int stored_maxpoint_ps = 3500;
    int stored_num_dels = 7;
    int stored_gatewidth_ps = 3000;
    int stored_tau_pred_ps = 3500;
    int stored_t_max_ps = 12500;
    double stored_reprate_mhz = 80.0;
    OpenFLIM_GOI_MM2_Utils utils = null;
    OpenFLIM_GOI_hostframe parent_ = null;
    ArrayList<Integer> old_list = null;
    
    /**
     * Creates new form del_ind_panel
     */
    public del_ind_panel() {
        initComponents();
        del_tbl_model = (DefaultTableModel)del_list.getModel();
        del_tbl_model.setRowCount(1);        
        del_tbl_model.setValueAt("0", 0, 0);
        curr_del_ind_startstr = curr_del_indicator.getText();
        utils = new MM2.openFLIM_GOI.Utilities.OpenFLIM_GOI_MM2_Utils();
        old_list = get_del_list();
        setup_del_list();
    }
    
    public void set_parent(OpenFLIM_GOI_hostframe parent_frame){
        parent_ = parent_frame;
    }
      
    public void setup_del_list(){
        //https://stackoverflow.com/questions/16743427/jtable-right-click-popup-menu
        final JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem deleteItem = new JMenuItem("Delete");
        JMenuItem insertItem = new JMenuItem("Insert");
        deleteItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] selectedRows = del_list.getSelectedRows();
                System.out.println (selectedRows);
                //https://stackoverflow.com/questions/2137755/how-do-i-reverse-an-int-array-in-java
                Arrays.sort(selectedRows);
                //Delete the highest indices first...
                ArrayUtils.reverse(selectedRows);
                for (int row : selectedRows){
                    del_tbl_model.removeRow(row);
                }
                if(del_tbl_model.getRowCount()<1){
                    //prevent having no rows
                    Vector<String> insertval = new Vector<String>();
                    insertval.add("0");
                    del_tbl_model.addRow(insertval);
                }
                set_del_list(get_del_list());
            }
        });        
        insertItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //JOptionPane.showMessageDialog(parent_, "Right-click performed on table and choose DELETE");
                Vector<String> insertval = new Vector<String>();
                insertval.add("0");
                del_tbl_model.insertRow(del_list.getSelectedRow(),insertval);
                set_del_list(get_del_list());
            }
        });      
        popupMenu.add(deleteItem);
        popupMenu.add(insertItem);
        popupMenu.addPopupMenuListener(new PopupMenuListener() {

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        int rowAtPoint = del_list.rowAtPoint(SwingUtilities.convertPoint(popupMenu, new Point(0, 0), del_list));
                        if (rowAtPoint > -1 && del_list.getSelectedRowCount()==0) {
                            del_list.setRowSelectionInterval(rowAtPoint, rowAtPoint);
                            del_list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                            System.out.println(del_list.getSelectionBackground());
                        }
                    }
                });
            }
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });        
        del_list.setComponentPopupMenu(popupMenu);
        //NEW BITS
        Action action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TableCellListener tcl = (TableCellListener)e.getSource();
                //System.out.println("Row   : " + tcl.getRow());
                //System.out.println("Column: " + tcl.getColumn());
                //System.out.println("Old   : " + tcl.getOldValue());
                //System.out.println("New   : " + tcl.getNewValue());
                parent_.set_del_seq_in_MM_GUI(get_del_list());
            }
        };
        TableCellListener tcl = new TableCellListener(del_list, action);
    }
    
    public void sanitise(JTextField this_field, boolean force_int, boolean pos_only, int num_dp){
        this_field.setText(utils.read_num_sensible(this_field.getText(), force_int, pos_only, num_dp));
    }        
    
    public void constrain(JTextField this_field,double min,double max){
        this_field.setText(utils.constrain_val(this_field.getText(), min, max));
    }
    
    public void set_del_val(int new_del_val) {
        curr_del_indicator.setText(Integer.toString(new_del_val));
    }    
    
    public void set_del_val(String new_del_txt) {
        curr_del_indicator.setText(new_del_txt);
    }  
    
    public int get_del_val() {
        return Integer.parseInt(curr_del_indicator.getText());
    }      
    
    public void set_del_list(ArrayList<Integer> dels_in){
        if(dels_in!=null && dels_in.size()>0){
            del_tbl_model.setNumRows(dels_in.size());
            for(int i=0;i<dels_in.size();i++){
                del_tbl_model.setValueAt(Integer.parseInt(dels_in.toArray()[i].toString()), i, 0);        
            }
            parent_.set_del_seq_in_MM_GUI(dels_in);
        }
    }
    
    public ArrayList<Integer> get_del_list(){
        ArrayList<Integer> del_list = new ArrayList<Integer>();
        for(int i=0;i<del_tbl_model.getRowCount();i++){
            int delval = Integer.parseInt(del_tbl_model.getValueAt(i, 0).toString());
            del_list.add(delval); 
        }
        return del_list;
    }
       
    public ArrayList<Integer> gen_log_list(int maxpoint_ps, int num_dels, int gatewidth_ps, int tau_pred_ps, int t_max_ps){
        ArrayList<Integer> new_dels = new ArrayList<Integer>();
        new_dels.add(maxpoint_ps-(gatewidth_ps+100));//just before rising edge
        new_dels.add(maxpoint_ps-(gatewidth_ps/2));//~halfway through rising edge
        for (int i=0;i<num_dels-2;i++){
            new_dels.add(maxpoint_ps+(i*num_dels/(t_max_ps-maxpoint_ps)));
        }
        return new_dels;
    }     
    
    public void set_calibrated(boolean iscalib){
        if (iscalib){
            Calib_indic.setForeground(Color.green);
            curr_del_indicator.setText("0");
        } else {
            Calib_indic.setForeground(Color.red);
            curr_del_indicator.setText("UNCALIBRATED");
        }
    }
    
    void set_maxpoint_value(int newval) {
        maxpoint_value.setText(Integer.toString(newval));
    }    
    
    public void set_triggering(int howgood){
        if (Math.abs(howgood)<10){
            TFB_indic.setForeground(Color.green);
        } else if (Math.abs(howgood)<30){
            TFB_indic.setForeground(Color.orange);
        } else {
            TFB_indic.setForeground(Color.red);
        }
        parent_.set_TFBdisplayed(howgood);
    }    
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        curr_del_indicator = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        del_list = new javax.swing.JTable();
        clear_delays = new javax.swing.JButton();
        num_gates = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        estimated_tau = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        maxpoint_value = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        load_del_list = new javax.swing.JButton();
        find_maxpoint = new javax.swing.JButton();
        generate_del_list = new javax.swing.JButton();
        TFB_indic = new javax.swing.JLabel();
        TFB_ok = new javax.swing.JLabel();
        laser_reprate_mhz = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        Calib_indic = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        maxpoint_in_list_only = new javax.swing.JCheckBox();

        jLabel1.setText("Current delay [ps]:");

        curr_del_indicator.setText("UNCALIBRATED");
        curr_del_indicator.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                curr_del_indicatorActionPerformed(evt);
            }
        });

        del_list.setAutoCreateRowSorter(true);
        del_list.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null},
                {null},
                {null},
                {null},
                {null},
                {null},
                {null},
                {null},
                {null},
                {null}
            },
            new String [] {
                "Delays [ps]"
            }
        ));
        del_list.setCellSelectionEnabled(true);
        jScrollPane1.setViewportView(del_list);

        clear_delays.setText("Clear list");
        clear_delays.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clear_delaysActionPerformed(evt);
            }
        });

        num_gates.setText("5");
        num_gates.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                num_gatesActionPerformed(evt);
            }
        });

        jLabel2.setText("#Gates to generate");

        estimated_tau.setText("2500");
        estimated_tau.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                estimated_tauActionPerformed(evt);
            }
        });

        jLabel3.setText("Estimated lifetime [ps]");

        maxpoint_value.setText("UNKNOWN");
        maxpoint_value.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                maxpoint_valueActionPerformed(evt);
            }
        });

        jLabel4.setText("Maximum point [ps]");

        load_del_list.setText("Load CSV list");
        load_del_list.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                load_del_listActionPerformed(evt);
            }
        });

        find_maxpoint.setText("Find maxpoint");
        find_maxpoint.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                find_maxpointActionPerformed(evt);
            }
        });

        generate_del_list.setText("Generate test list");
        generate_del_list.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                generate_del_listActionPerformed(evt);
            }
        });

        TFB_indic.setFont(new java.awt.Font("Tahoma", 0, 24)); // NOI18N
        TFB_indic.setForeground(new java.awt.Color(255, 0, 51));
        TFB_indic.setText("•");
        TFB_indic.setToolTipText("");

        TFB_ok.setText("Triggering?");

        laser_reprate_mhz.setText("80.0");
        laser_reprate_mhz.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                laser_reprate_mhzActionPerformed(evt);
            }
        });

        jLabel5.setText("Laser rep rate [MHz]");

        Calib_indic.setFont(new java.awt.Font("Tahoma", 0, 24)); // NOI18N
        Calib_indic.setForeground(new java.awt.Color(255, 0, 51));
        Calib_indic.setText("•");
        Calib_indic.setToolTipText("");

        jLabel6.setText("Calibrated?");

        maxpoint_in_list_only.setText("Only check these dels");
        maxpoint_in_list_only.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                maxpoint_in_list_onlyActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                        .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(TFB_indic)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(TFB_ok)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(num_gates)
                    .addComponent(clear_delays, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(estimated_tau)
                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(curr_del_indicator)
                    .addComponent(maxpoint_value)
                    .addComponent(load_del_list, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(find_maxpoint, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(generate_del_list, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(laser_reprate_mhz)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(Calib_indic)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel6))
                            .addComponent(jLabel2)
                            .addComponent(jLabel4)
                            .addComponent(jLabel5)
                            .addComponent(maxpoint_in_list_only))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(curr_del_indicator, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(clear_delays)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(load_del_list)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(generate_del_list)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(find_maxpoint)
                        .addGap(5, 5, 5)
                        .addComponent(maxpoint_in_list_only)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel5)
                        .addGap(5, 5, 5)
                        .addComponent(laser_reprate_mhz, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(maxpoint_value, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(num_gates, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel3)
                        .addGap(4, 4, 4)
                        .addComponent(estimated_tau, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(TFB_indic, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(TFB_ok, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(Calib_indic, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void estimated_tauActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_estimated_tauActionPerformed
        sanitise(estimated_tau,true,true,0);
        stored_tau_pred_ps = Integer.parseInt(estimated_tau.getText());
    }//GEN-LAST:event_estimated_tauActionPerformed

    private void num_gatesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_num_gatesActionPerformed
        sanitise(num_gates,true,true,0);
        stored_num_dels = Integer.parseInt(num_gates.getText());
        stored_reprate_mhz = Double.parseDouble(laser_reprate_mhz.getText());
    }//GEN-LAST:event_num_gatesActionPerformed

    private void generate_del_listActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_generate_del_listActionPerformed
        //ArrayList<Integer> newvals = gen_log_list(stored_maxpoint_ps, stored_num_dels, stored_gatewidth_ps, stored_tau_pred_ps, stored_t_max_ps);
        ArrayList<Integer> newvals = new ArrayList<Integer>();
        int maxpt = Integer.parseInt(maxpoint_value.getText());
        int max_del = parent_.get_slider_max_delay();
        //maxpt will always be >=0
        ArrayList<Integer> rel_dels = new ArrayList<Integer>(Arrays.asList(-3600,-1800,0,250,500,1000,2000,4000,7000));
        for(int rel_del : rel_dels){
            newvals.add(Math.min(max_del,Math.max(0,maxpt+rel_del)));
        }
        set_del_list(newvals);
        
        if(Collections.min(rel_dels)+maxpt<0 || Collections.max(rel_dels)+maxpt>max_del){
            String[] options = new String[]{"Whatever", "Oh no!"};
            utils.option_popup(this,"Warning!","Delays falling outside of the range 0 to "+Integer.toString(max_del)+" have been generated, and coerced to fall within this range. Beware duplicates!",options);
        }
    }//GEN-LAST:event_generate_del_listActionPerformed

    private void clear_delaysActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clear_delaysActionPerformed
        del_tbl_model.setNumRows(1);
        int setval = 0;
        if(curr_del_ind_startstr.equals(curr_del_indicator.getText())){
            //leave as zero if uncalibrated
        } else {
           setval = Integer.parseInt(curr_del_indicator.getText());
        }
        del_tbl_model.setValueAt(Integer.toString(setval), 0, 0);
    }//GEN-LAST:event_clear_delaysActionPerformed

    private void maxpoint_valueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_maxpoint_valueActionPerformed
        sanitise(maxpoint_value,true,true,0);
        constrain(maxpoint_value,0,stored_t_max_ps);
        sanitise(maxpoint_value,true,true,0);
    }//GEN-LAST:event_maxpoint_valueActionPerformed

    private void laser_reprate_mhzActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_laser_reprate_mhzActionPerformed
        sanitise(laser_reprate_mhz,false,true,2);
        stored_reprate_mhz = Double.parseDouble(laser_reprate_mhz.getText());
        //MHz to ps
        stored_t_max_ps = (int)(1000000/Double.parseDouble(utils.read_num_sensible(laser_reprate_mhz.getText(), true, true)));
        //Sanity check
        if(stored_t_max_ps>utils.max_allowd_del()){
            stored_t_max_ps = utils.max_allowd_del();
        }
        System.out.println(stored_t_max_ps);
        parent_.set_slider_max(stored_t_max_ps);
    }//GEN-LAST:event_laser_reprate_mhzActionPerformed

    private void curr_del_indicatorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_curr_del_indicatorActionPerformed
        sanitise(curr_del_indicator, true, true, 0);
        if(parent_.calib_loaded){
            parent_.set_slider_val(Integer.parseInt(curr_del_indicator.getText()));
        } else {
            curr_del_indicator.setText("0");
        }
    }//GEN-LAST:event_curr_del_indicatorActionPerformed

    private void find_maxpointActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_find_maxpointActionPerformed
        parent_.use_dels = maxpoint_in_list_only.isSelected();
        parent_.threaded_find_maxpoint();
    }//GEN-LAST:event_find_maxpointActionPerformed

    private void maxpoint_in_list_onlyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_maxpoint_in_list_onlyActionPerformed
        
    }//GEN-LAST:event_maxpoint_in_list_onlyActionPerformed

    private void load_del_listActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_load_del_listActionPerformed
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
            "Comma-separated value files", "csv", "xxx");
        chooser.setFileFilter(filter);
        int returnVal = chooser.showOpenDialog(this);
        List<List<String>> records = new ArrayList<>();
        ArrayList<Integer> loaded_del_list = new ArrayList<>();
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            try (Scanner scanner = new Scanner(chooser.getSelectedFile());) {
                while (scanner.hasNextLine()) {
                    records.add(getRecordFromLine(scanner.nextLine()));
                }
                del_list.removeAll();
                for(List<String>record : records){
                    String val = utils.read_num_sensible(record.get(0),true,true);
                    if(val==""){
                        //Was probably a header, so just ignore it
                    } else {
                        loaded_del_list.add(Integer.parseInt(val));
                    }
                }
                set_del_list(loaded_del_list);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(del_ind_panel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }    
    }//GEN-LAST:event_load_del_listActionPerformed

    private List<String> getRecordFromLine(String line) {
        List<String> values = new ArrayList<String>();
        try (Scanner rowScanner = new Scanner(line)) {
            Pattern COMMA_DELIMITER = Pattern.compile(",");
            rowScanner.useDelimiter(COMMA_DELIMITER);
            while (rowScanner.hasNext()) {
                values.add(rowScanner.next());
            }
        }
        return values;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel Calib_indic;
    private javax.swing.JLabel TFB_indic;
    private javax.swing.JLabel TFB_ok;
    private javax.swing.JButton clear_delays;
    private javax.swing.JTextField curr_del_indicator;
    private javax.swing.JTable del_list;
    private javax.swing.JTextField estimated_tau;
    private javax.swing.JButton find_maxpoint;
    private javax.swing.JButton generate_del_list;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField laser_reprate_mhz;
    private javax.swing.JButton load_del_list;
    private javax.swing.JCheckBox maxpoint_in_list_only;
    private javax.swing.JTextField maxpoint_value;
    private javax.swing.JTextField num_gates;
    // End of variables declaration//GEN-END:variables

}