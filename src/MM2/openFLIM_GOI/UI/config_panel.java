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

import static MM2.openFLIM_GOI.UI.OpenFLIM_GOI_hostframe.gui_;
import MM2.openFLIM_GOI.Utilities.OpenFLIM_GOI_MM2_Utils;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.DefaultTableModel;
import mmcorej.StrVector;
import org.apache.commons.lang3.ArrayUtils;

public class config_panel extends javax.swing.JPanel {
    DefaultTableModel devadapt_search_tbl_model;
    DefaultTableModel prop_search_tbl_model;
    JPopupMenu da_srch_tbl_pop = new JPopupMenu();
    JPopupMenu prop_srch_tbl_pop = new JPopupMenu();
    JMenuItem dast_deleteItem = new JMenuItem("Delete");
    JMenuItem dast_insertItem = new JMenuItem("Insert");
    JMenuItem pst_deleteItem = new JMenuItem("Delete");
    JMenuItem pst_insertItem = new JMenuItem("Insert");        
    OpenFLIM_GOI_hostframe parent_ = null;
    StrVector da_search_strings = null;
    StrVector prop_search_strings = null;
    String DEV_ID = "";
    String DEL_PROP_ID = "Current delay (ps)";    
    String DEL_LIST_ID = "Delay sequence";
    boolean initialised = false;
    boolean searching = false;
    boolean valid_devname = false;
    boolean valid_delprop = false;
    boolean valid_settings = false;
    OpenFLIM_GOI_MM2_Utils utils_ = null;
            
    /**
     * Creates new form config_panel
     */
    public config_panel() {
        initComponents();
        utils_ = new OpenFLIM_GOI_MM2_Utils();
    }
    
    public void set_parent(OpenFLIM_GOI_hostframe parent_frame){
        parent_ = parent_frame;
        //Following bits are here so that they run a bit later than normal?
        da_search_strings = new StrVector();
        prop_search_strings = new StrVector();
        devadapt_search_tbl_model = get_model(devadapt_search_table,devadapt_search_tbl_model,da_search_strings);
        prop_search_tbl_model = get_model(prop_search_table,prop_search_tbl_model,prop_search_strings);
        setup_for_this_box();
        make_popup_for_srch_tbl(devadapt_search_table,devadapt_search_tbl_model,da_srch_tbl_pop,dast_deleteItem,dast_insertItem);        
        make_popup_for_srch_tbl(prop_search_table,prop_search_tbl_model,prop_srch_tbl_pop,pst_deleteItem,pst_insertItem);
        initialised = true;
    }
    
    public void check_validity(){
        if (valid_devname && valid_delprop){
            valid_settings = true;
        } else {
            valid_settings = false;
        }
    }
    
    public boolean force_GUI_sync(){
        return force_GUI_sync.isSelected();
    }

    public DefaultTableModel get_model(JTable this_table, DefaultTableModel this_model, StrVector search_strings){
        search_strings.clear();
        this_model = (DefaultTableModel)this_table.getModel();
        for (int i=0;i<this_table.getRowCount();i++){
            //System.out.println((this_model.getValueAt(i, 0).toString()));
            search_strings.add(this_model.getValueAt(i, 0).toString());
        }
        return this_model;
    }
    public void setup_for_this_box(){
        StrVector devadaptnames = new StrVector();
        StrVector sel_devadaptnames = new StrVector();
        StrVector devnames = new StrVector();
        try {
            //Don't get confused by ◆ getDeviceDelayMs() if it shows up
            devadaptnames = gui_.core().getDeviceAdapterNames();
        } catch (Exception ex) {
            Logger.getLogger(OpenFLIM_GOI_hostframe.class.getName()).log(Level.SEVERE, null, ex);
        }
        boolean found_delbox = false;
        del_box_sel_name.removeAllItems();
        //Make a list of device adapters that contain the search strings in the table
        for(String devadaptname : devadaptnames){
            for (String srch : da_search_strings){
                if(devadaptname.toLowerCase().contains(srch.toLowerCase())){
                    //System.out.println(devadaptname);
                    sel_devadaptnames.add(devadaptname);
                }
            }
        }
        for (String sel_adapt: sel_devadaptnames){
            try {
                //Find devices using this device adapter
                devnames = gui_.core().getAvailableDevices(sel_adapt);
            } catch (Exception ex) {
                Logger.getLogger(config_panel.class.getName()).log(Level.SEVERE, null, ex);
            }
            for (String dev : devnames){
                del_box_sel_name.addItem(dev);
                //System.out.println(dev);
            }
        }
        //If there's only one device in the list, surely we've got the right one...
        boolean go_ahead = false;
        if(del_box_sel_name.getItemCount()==1){
            String dev_name = del_box_sel_name.getItemAt(0);
            //System.out.println(dev_name);
            DEV_ID = dev_name;
            valid_devname = true;
            go_ahead = true;
            try {
                expose_device_properties();
            } catch (Exception ex) {
                Logger.getLogger(config_panel.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            for (int i=0; i<del_box_sel_name.getItemCount(); i++){
                System.out.println(del_box_sel_name.getItemAt(i));
            }
            String[] opts = new String[]{"It is","It isn't", "I'm not sure"};
            String mult_devs = "Multiple potential delay devices were found with these search strings - please ensure the correct one is selected!";
            int option_popup = utils_.option_popup((Object)this, "Warning!",(Object)mult_devs, opts);
            if(option_popup == 0){
                go_ahead = true;
                valid_devname = true;
            } else {
                go_ahead = false;
                valid_devname = false;
            }
        }
        check_validity();
    }
    
    public void expose_device_properties() throws Exception{
        StrVector device_properties = new StrVector();
        StrVector sel_propnames = new StrVector();
        dev_props.removeAllItems();
        if(parent_.gui_.core().getDevicePropertyNames(DEV_ID)!=null){
            device_properties = parent_.gui_.core().getDevicePropertyNames(DEV_ID);
            List<String> big_list = new ArrayList<>();
            for(String prop : device_properties){
                for (String srch : prop_search_strings){
                    if(prop.toLowerCase().contains(srch.toLowerCase())){
                        big_list.add(prop);
                    }
                }
            }
            //https://www.geeksforgeeks.org/how-to-remove-duplicates-from-arraylist-in-java/
            List<String> dedup_list = big_list.stream().distinct().collect(Collectors.toList()); 
            for(String prop : dedup_list){
                dev_props.addItem(prop);
                sel_propnames.add(prop);
            }
            if(del_box_sel_name.getItemCount()==1 && dev_props.getItemCount()==1){
                DEL_PROP_ID = dev_props.getItemAt(0);
                valid_delprop = true;
            } else {
                valid_delprop = false;            
            }
            check_validity();
        } else {
            valid_delprop = false;
            valid_devname = false;
            valid_settings = false;
            String[] options = new String[]{"OK"};
            utils_.option_popup(this, "Warning!", "No delay box-like device found?", options);
        }
    }
    
    public void make_popup_for_srch_tbl(JTable this_jtable, DefaultTableModel this_tablemodel, JPopupMenu popupMenu, JMenuItem deleteItem, JMenuItem insertItem){
        deleteItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] selectedRows = this_jtable.getSelectedRows();
                //https://stackoverflow.com/questions/2137755/how-do-i-reverse-an-int-array-in-java
                Arrays.sort(selectedRows);
                //Delete the highest indices first...
                ArrayUtils.reverse(selectedRows);
                for (int row : selectedRows){
                    this_tablemodel.removeRow(row);
                }
                if(this_tablemodel.getRowCount()<1){
                    //prevent having no rows
                    Vector<String> insertval = new Vector<String>();
                    insertval.add("0");
                    this_tablemodel.addRow(insertval);
                }
            }
        });        
        insertItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //JOptionPane.showMessageDialog(parent_, "Right-click performed on table and choose DELETE");
                Vector<String> insertval = new Vector<String>();
                insertval.add("0");
                this_tablemodel.insertRow(this_jtable.getSelectedRow(),insertval);
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
                        int rowAtPoint = this_jtable.rowAtPoint(SwingUtilities.convertPoint(popupMenu, new Point(0, 0), this_jtable));
                        if (rowAtPoint > -1 && this_jtable.getSelectedRowCount()==0) {
                            this_jtable.setRowSelectionInterval(rowAtPoint, rowAtPoint);
                            this_jtable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                            //System.out.println(this_jtable.getSelectionBackground());
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
        this_jtable.setComponentPopupMenu(popupMenu);
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        del_box_sel_name = new javax.swing.JComboBox<>();
        jLabel1 = new javax.swing.JLabel();
        dev_props = new javax.swing.JComboBox<>();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        devadapt_search_table = new javax.swing.JTable();
        jScrollPane2 = new javax.swing.JScrollPane();
        prop_search_table = new javax.swing.JTable();
        redo_search_but = new javax.swing.JButton();
        apply_config = new javax.swing.JButton();
        force_GUI_sync = new javax.swing.JCheckBox();
        jLabel3 = new javax.swing.JLabel();
        min_dt = new javax.swing.JTextField();

        del_box_sel_name.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        del_box_sel_name.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                del_box_sel_nameActionPerformed(evt);
            }
        });

        jLabel1.setText("Delay box name in config");

        dev_props.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        dev_props.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dev_propsActionPerformed(evt);
            }
        });

        jLabel2.setText("Delay property [ps] name");

        devadapt_search_table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"HDG"},
                {"Kentech"},
                {"Delay"}
            },
            new String [] {
                "Search term in device adapter"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        devadapt_search_table.setCellSelectionEnabled(true);
        jScrollPane1.setViewportView(devadapt_search_table);
        devadapt_search_table.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_INTERVAL_SELECTION);

        prop_search_table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"delay"},
                {"(ps)"}
            },
            new String [] {
                "Search term in properties"
            }
        ));
        jScrollPane2.setViewportView(prop_search_table);

        redo_search_but.setText("Search again");
        redo_search_but.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                redo_search_butActionPerformed(evt);
            }
        });

        apply_config.setText("Apply config");
        apply_config.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                apply_configActionPerformed(evt);
            }
        });

        force_GUI_sync.setText("Force GUI sync");
        force_GUI_sync.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                force_GUI_syncActionPerformed(evt);
            }
        });

        jLabel3.setText("Min time step [ps]");

        min_dt.setText("25");
        min_dt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                min_dtActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(dev_props, 0, 178, Short.MAX_VALUE)
                    .addComponent(del_box_sel_name, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(jLabel1)
                        .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(redo_search_but, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(apply_config, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(force_GUI_sync)
                    .addComponent(jLabel3)
                    .addComponent(min_dt, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(del_box_sel_name, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dev_props, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 117, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(redo_search_but)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(apply_config)
                        .addGap(18, 18, 18)
                        .addComponent(force_GUI_sync)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(min_dt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void del_box_sel_nameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_del_box_sel_nameActionPerformed
        if (initialised && !searching){
            DEV_ID = del_box_sel_name.getSelectedItem().toString();
            try {
                expose_device_properties();
            } catch (Exception ex) {
                Logger.getLogger(config_panel.class.getName()).log(Level.SEVERE, null, ex);
            }
            valid_devname = true;       
            check_validity();
        }
    }//GEN-LAST:event_del_box_sel_nameActionPerformed

    private void dev_propsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dev_propsActionPerformed
        valid_delprop = true;       
        check_validity();
    }//GEN-LAST:event_dev_propsActionPerformed

    private void redo_search_butActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_redo_search_butActionPerformed
        devadapt_search_tbl_model = get_model(devadapt_search_table,devadapt_search_tbl_model,da_search_strings);
        prop_search_tbl_model = get_model(prop_search_table,prop_search_tbl_model,prop_search_strings);
        searching = true;
        setup_for_this_box();
        searching = false;
    }//GEN-LAST:event_redo_search_butActionPerformed

    private void apply_configActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_apply_configActionPerformed
        if(valid_settings){
            parent_.configure_for_this_delay_box(false, DEV_ID, DEL_PROP_ID,DEL_LIST_ID);
        } else {
            String[] opts = new String[]{"OK"};
            String err_msg = "Configuration was not applied because we're not 100% sure that the correct device and property are selected. Please re-select on the dropdowns to confirm.";
            utils_.option_popup((Object)this, "Warning!",(Object)err_msg, opts);
        }
    }//GEN-LAST:event_apply_configActionPerformed

    private void force_GUI_syncActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_force_GUI_syncActionPerformed
        
    }//GEN-LAST:event_force_GUI_syncActionPerformed

    private void min_dtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_min_dtActionPerformed
        min_dt.setText(utils_.strip_non_numeric(min_dt.getText()));
    }//GEN-LAST:event_min_dtActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton apply_config;
    private javax.swing.JComboBox<String> del_box_sel_name;
    private javax.swing.JComboBox<String> dev_props;
    private javax.swing.JTable devadapt_search_table;
    private javax.swing.JCheckBox force_GUI_sync;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextField min_dt;
    private javax.swing.JTable prop_search_table;
    private javax.swing.JButton redo_search_but;
    // End of variables declaration//GEN-END:variables
}
