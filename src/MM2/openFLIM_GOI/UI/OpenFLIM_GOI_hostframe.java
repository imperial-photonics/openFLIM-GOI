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

import MM2.openFLIM_GOI.Threads.FLIM_snap_thread;
import MM2.openFLIM_GOI.Threads.Findmaxpoint_thread;
import MM2.openFLIM_GOI.Threads.Graph_progbar_thread;
import MM2.openFLIM_GOI.Threads.Optimise_trigger_thread;
import MM2.openFLIM_GOI.Threads.Timer_display_thread;
import MM2.openFLIM_GOI.Utilities.FLIM_OME_TIFF_writer;
import MM2.openFLIM_GOI.Utilities.OpenFLIM_GOI_MM2_Utils;
import com.google.common.eventbus.Subscribe;
import ij.process.ImageProcessor;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import loci.formats.FormatException;

import mmcorej.CMMCore;
import org.micromanager.MenuPlugin;
import org.micromanager.PropertyMap;
import org.micromanager.Studio;
import org.micromanager.acquisition.SequenceSettings;
import org.micromanager.data.Coords;
import org.micromanager.data.Datastore;
import org.micromanager.data.Datastore.SaveMode;
import org.micromanager.data.Image;
import org.micromanager.data.Metadata;
import org.micromanager.data.SummaryMetadata;
import org.micromanager.events.PropertyChangedEvent;

//http://www.java2s.com/Code/Java/Chart/JFreeChartDualAxisDemo2.htm

public class OpenFLIM_GOI_hostframe extends javax.swing.JFrame {
    static OpenFLIM_GOI_hostframe frame_;
    public static Studio gui_ = null;
    private CMMCore core_ = null;
    int tracker = 0;
    int prev_slider_val = 0;
    int prev_box_val = 0;
    int min_del_step = 25;    
    String DEL_DEV_ID = null;
    String DEL_PROP_ID = null;
    String DEL_LIST_ID = null;
    boolean calib_loaded = false;
    int [][] del_graph = null;
    OpenFLIM_GOI_main uberparent_;
    boolean use_dels = false;
    boolean kill_all_threads = false;
    public boolean is_progbar_active;
    Thread gp_thread = new Thread();
    Thread ot_thread = new Thread();
    Thread fm_thread = new Thread();
    Thread snap_thread = new Thread();
    FLIM_OME_TIFF_writer FLIM_writer = new FLIM_OME_TIFF_writer();
    OpenFLIM_GOI_MM2_Utils utils2 = new MM2.openFLIM_GOI.Utilities.OpenFLIM_GOI_MM2_Utils();
    int setting_was_sel = 0;
    ArrayList<String> infobox_messages = new ArrayList<String>();
    HashMap<String,String> metadata = new HashMap<String,String>();

    MenuPlugin omnistop_instance = null;
    Method get_os_ans = null;
    boolean omnistop_available = false;
    
    /**
     * Creates new form del_con_hostframe
     */
    public OpenFLIM_GOI_hostframe(Studio gui) {
        try {
            frame_ = this;
            gui_ = gui;
            core_ = gui_.getCMMCore();
            gui_.events().registerForEvents(this);
            initComponents();
            del_ind_panel1.set_parent(this);
            config_panel2.set_parent(this);
            del_graph_panel2.set_parent(this);
            FLIM_writer.set_parent(this);
            //testevent(new PropertyChangedEvent());
            configure_for_this_delay_box(true,"","","");
            Graph_progbar.setVisible(false);
            //http://www.java2s.com/Tutorial/Java/0240__Swing/ListeningforValueChangesinaJSliderComponent.htm
            //Will cause an issue on loading calibration with current device adapter - force calib load first.
            del_slider.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent evt) {
                    JSlider slider = (JSlider) evt.getSource();
                    if (!slider.getValueIsAdjusting() && calib_loaded == true) {
                        int value = slider.getValue();
                        set_box_val(value);
                        //slider.setValue(get_box_val());
                        //System.out.println(value);
                    }
                    if (calib_loaded == false){
                        slider.setValue(0);
                    }
                }
            });
            //##Prevent closing of Micromanager on plugin closing
            frame_.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            frame_.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent we) {
                    if (true == confirmQuit()){
                        kill_all_threads = true; // MAY NEED TO RECONSIDER THIS?
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(OpenFLIM_GOI_hostframe.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        frame_.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
                    } else {
                        frame_.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                    }
                }
            });
            gp_thread = new Thread(new Graph_progbar_thread(this));
            gp_thread.start();
            Settings.setSelectedIndex(0);
            setting_was_sel = Settings.getSelectedIndex();
            determine_metadata();
            setup_abort();
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(OpenFLIM_GOI_hostframe.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(OpenFLIM_GOI_hostframe.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(OpenFLIM_GOI_hostframe.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(OpenFLIM_GOI_hostframe.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void setparent(OpenFLIM_GOI_main delcon){
        uberparent_ = delcon;
        frame_.setTitle(uberparent_.getName());
        add_infopanel_message("Load calibration, optimise triggering, find maxpoint, set delays, snap! Files will be saved in the path specified in the MDA panel (make sure it's ticked)");
    }

    public boolean use_dels(){
        return use_dels;
    }
    
    private boolean confirmQuit() {
        int n = JOptionPane.showConfirmDialog(frame_,
                "Quit: are you sure? (This window can be reopened)", "Quit", JOptionPane.YES_NO_OPTION);
        if (n == JOptionPane.YES_OPTION) {
            return true;
        }
            return false;
    }      
    
    public void configure_for_this_delay_box(boolean init, String DEVID, String DELPROPID, String DELLISTID){
        if(init){//~default case
            DEL_DEV_ID = "Kentech HDG";
            DEL_PROP_ID = "Current delay (ps)";
            DEL_LIST_ID = "Delay sequence";
        } else {
            DEL_DEV_ID = DEVID;
            DEL_PROP_ID = DELPROPID;
            DEL_LIST_ID = DELLISTID;
        }
    }
        
    @Subscribe
    public void prop_change(PropertyChangedEvent event){
        if(event.getProperty().equalsIgnoreCase("Recalib")){
            System.out.println("Delay box calibration loaded");
            if(event.getValue().equalsIgnoreCase("Calibrated")){
                calib_loaded = true;
                del_ind_panel1.set_calibrated(true);
            } else {
                calib_loaded = false;
                del_ind_panel1.set_calibrated(false);                
            }
        } else if (event.getProperty().equalsIgnoreCase("Delay")) {
            del_ind_panel1.set_del_val(event.getValue());
            del_slider.setValue(Integer.parseInt(event.getValue()));            
            //System.out.println("Del change event fired");
        } else if (event.getProperty().equalsIgnoreCase("Optimised")) {
            del_ind_panel1.set_triggering(Integer.parseInt(event.getValue()));
        } else if (event.getProperty().equalsIgnoreCase("Message")) {
            JOptionPane.showInputDialog(null, "Message data!", event.getValue()+" from "+event.getDevice());
        } else {
            System.out.println("Property changed: ".equals(event.getProperty()));
        }
    }
    
    public void set_slider_max(int slidemax){
        maxdelval_txt.setText(Integer.toString(slidemax));
        del_slider.setMaximum(slidemax);
        set_graph_xmax(slidemax);
    }
    
    public int get_slider_max_delay(){
        return del_slider.getMaximum();
    }    
    
    public void set_graph_xmax(int xmax){    
        del_graph_panel2.set_def_xrange_max(xmax);
    }
    
    public void set_slider_val(int slideval){
        del_slider.setValue(slideval);
        prev_slider_val = slideval;
        update_actual_value(slideval);
    }    
    
    public void set_box_val(int boxval){
        del_ind_panel1.set_del_val(boxval);
        prev_box_val = boxval;
        update_actual_value(boxval);
    }        
    
    public boolean should_i_kill_all_threads(){
        return kill_all_threads;
    }
    
    public int get_box_val(){
        return del_ind_panel1.get_del_val();
    }
    
    public ArrayList<Integer> get_dels(){
        return del_ind_panel1.get_del_list();
    }
    
    public void update_actual_value(int newval){
        if(prev_box_val != prev_slider_val){
            //System.out.println("Setting physical delay box value");
            try {
                //Potential place to check if calibration loaded?
                gui_.core().setProperty(DEL_DEV_ID, DEL_PROP_ID,newval);
            } catch (Exception ex) {
                Logger.getLogger(OpenFLIM_GOI_hostframe.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        updateGUIifneeded();
    }    

    public void updateGUIifneeded(){
        if(config_panel2.force_GUI_sync()){
            gui_.app().refreshGUIFromCache();
        }
    }
    
    public void set_progbar_going(){
        is_progbar_active = true;
    }
    
    public void stop_progbar_going(){
        is_progbar_active = false;
    }    
    
    public void show_progbar(boolean show){
        Graph_progbar.setVisible(show);
        Graph_progbar.setIndeterminate(show);    
    }
    
    void threaded_find_maxpoint(){
        is_running(true); // kill if running
        set_busy_condition(true);
        use_dels = false;
        fm_thread = new Thread(new Findmaxpoint_thread(this));
        fm_thread.start();
    }
    
    public void disable_snap_button(){
        FLIM_snap_button.setEnabled(false);
    }
    
    public void enable_snap_button(){
        FLIM_snap_button.setEnabled(true);
    }    
    
    public void find_maxpoint_sub() {
        //Might want to add a check that the FLIM camera is the one that is enabled!
        int delval = 0;
        int num_points = 0;
        int max_del = get_slider_max_delay();//Should be set via calculation from reprate
        Coords.CoordsBuilder builder = gui_.data().getCoordsBuilder();
        Settings.setSelectedIndex(1);//Should be the graphs panel
        try {
            if(use_dels()){
                String n_dels = core_.getProperty(DEL_DEV_ID, "Number of delays in sequence");
                num_points = Integer.parseInt(n_dels);
                //Get the array of delays
                String del_seq = core_.getProperty(DEL_DEV_ID, "Delay sequence");
                //Now make an array of them
                String[] dels = del_seq.split("#");
                del_graph = new int[2][num_points];
                System.out.println("# of delays: "+n_dels);        
                Datastore store = gui_.data().createRAMDatastore();
                builder.z(num_points);
                Coords coords = builder.build();    
                core_.setProperty(DEL_DEV_ID, DEL_PROP_ID, delval);
                core_.waitForDevice(DEL_DEV_ID);
                core_.snapImage();
                Image curr_img = gui_.data().convertTaggedImage(core_.getTaggedImage(),coords,null);            
                ImageProcessor calc_proc = gui_.data().getImageJConverter().createProcessor(curr_img);
                //Don't care about memory use for one image? Accuraccy loss from float is kind of worth the convenience of not worying about overflow?           
                for (int i=0;i<dels.length;i++){
                    coords = coords.copy().z(i).build();
                    delval = Integer.parseInt(dels[i]);
                    coords = coords.copy().z(i).build();
                    core_.snapImage();
                    curr_img = gui_.data().convertTaggedImage(core_.getTaggedImage(),coords,null);
                    calc_proc = gui_.data().getImageJConverter().createProcessor(curr_img);
                    store.putImage(curr_img);
                    double mean = calc_proc.getStatistics().mean;
                    int avg = (int) mean;
                    del_graph[0][i] = delval;
                    del_graph[1][i] = avg;
                    //del_graph_panel2.update_graph();
                }
            } else {
                //Do a search, stepping down by factors of 10x, overscanning to try and combat noise
                //Finish of with a highest possible res scan
                int n_scales = 0;
                while(max_del/Math.pow(10,n_scales)>min_del_step){
                    n_scales = n_scales+1;
                }
                int[] scale_sizes = new int[n_scales+1];
                //System.out.println("Scan separations of...");
                for (int i=0;i<n_scales-1;i++){
                    scale_sizes[i] = (int)Math.pow(10,n_scales-i);
                    //System.out.println(scale_sizes[i]);
                }
                scale_sizes[n_scales-1] = min_del_step;
                //System.out.println(min_del_step);
                ArrayList<Integer> del_values = new ArrayList<Integer>();
                ArrayList<Double> measured_values = new ArrayList<Double>();
                int start_del = 0;
                builder.z(1);
                for (int i=0;i<n_scales;i++){
                    double loc_max = 0;
                    int running_maxpoint = 0;
                    int step_size = scale_sizes[i];
                    int max_del_in_range = Math.min(start_del+(step_size*20), max_del);
                    if(i==0){
                        max_del_in_range = max_del;
                    }
                    if(i==n_scales-1){//smallest step range
                        max_del_in_range = Math.min(start_del+(step_size*8), max_del);//assumption on step size for now
                    }                    
                    for (int j=start_del;j<=max_del_in_range;j=j+step_size){
                        del_values.add(j);
                        //System.out.println(j);
                        Coords coords = builder.build();    
                        core_.setProperty(DEL_DEV_ID, DEL_PROP_ID, j);
                        core_.waitForDevice(DEL_DEV_ID);
                        core_.snapImage();
                        Image curr_img = gui_.data().convertTaggedImage(core_.getTaggedImage(),coords,null);            
                        ImageProcessor calc_proc = gui_.data().getImageJConverter().createProcessor(curr_img);
                        double mean = calc_proc.getStatistics().mean;
                        if(mean>loc_max){
                            loc_max = mean;
                            running_maxpoint = j;
                        }
                        measured_values.add(mean);
                    }
                    start_del = (int)((Math.floor(running_maxpoint/step_size)-1)*step_size);//without overscan, drop -1
                    if(start_del<0){
                        start_del = 0;
                    }
                    //System.out.println("Running maxpoint: "+running_maxpoint);
                }
                //System.out.println("============================");
                num_points = measured_values.size();
                del_graph = new int[2][num_points];
                for (int i=0;i<num_points;i++){
                    del_graph[0][i] = del_values.get(i);
                    del_graph[1][i] = (int)Math.round(measured_values.get(i));
                    //System.out.println(del_values.get(i) + " - " + measured_values.get(i));
                }
                del_graph_panel2.set_graph(del_graph);
                del_graph_panel2.update_graph();
                double maxval = Collections.max(measured_values);
                int max_ind = measured_values.indexOf(maxval);
                update_maxpoint_value(del_values.get(max_ind));
            }
        } catch (Exception ex) {
            Logger.getLogger(OpenFLIM_GOI_hostframe.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void update_maxpoint_value(int newval){
        del_ind_panel1.set_maxpoint_value(newval);
    }
    
    public void set_delstep(int min_dt){
        min_del_step = min_dt;
    }
    
    public void run_timer(int num_s, String explanation){
        Thread rt_thread = new Thread(new Timer_display_thread(this, num_s, explanation));
        rt_thread.start();
    }
    
    public void threaded_FLIM_snap(){
        snap_thread = new Thread(new FLIM_snap_thread(this));
        snap_thread.start();
    }
    
    public void do_FLIM_snap(){
        set_busy_condition(true);
        String acq_start_time = "X#X@X~X#X"; // Should be overwritten by the value in the MDA panel...
        SequenceSettings tmp_ss = gui_.acquisitions().getAcquisitionSettings();
        try {
            File newdir = new File(tmp_ss.root);
            newdir.mkdirs();
            System.out.println("Making: "+newdir.toString());
            boolean FLIM_snapped = FLIM_writer.FLIM_snap(tmp_ss.root, tmp_ss.prefix, DEL_DEV_ID, DEL_PROP_ID);
            System.out.println("Flim snapped? "+FLIM_snapped);
//            //=======>DELETE BELOW
//            Datastore new_ds = gui_.data().createRAMDatastore();
//            Image snapped_img = gui_.acquisitions().snap().get(0);
//            Metadata md = snapped_img.getMetadata();
//            PropertyMap mud = md.getUserData();
//            PropertyMap mpm = mud.copyBuilder().putString("Sunil", "Hello in metadata").build();
//            Metadata nmd = md.copyBuilderPreservingUUID().userData(mpm).build();
//            Image sic = snapped_img.copyWithMetadata(nmd);
//            SummaryMetadata sm = new_ds.getSummaryMetadata();
//            PropertyMap UD = sm.getUserData();
//            PropertyMap.Builder UDB = UD.copyBuilder().putString("Jonny", "Hello");
//            PropertyMap built_UD = UDB.build();
//            SummaryMetadata.Builder smb = sm.copyBuilder();
//            SummaryMetadata built_sm = smb.userData(built_UD).build();
//            new_ds.setSummaryMetadata(built_sm);
//            new_ds.putImage(sic);
//            String uniqueSaveDirectory = gui_.data().getUniqueSaveDirectory(tmp_ss.root);
//            new_ds.setSavePath(uniqueSaveDirectory);
//            new_ds.freeze();
//            String wtf = uniqueSaveDirectory;
//            new_ds.save(Datastore.SaveMode.MULTIPAGE_TIFF, wtf);
//            new_ds.close();
//            //<===========DELETE
        } catch (FormatException ex) {
            Logger.getLogger(OpenFLIM_GOI_hostframe.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(OpenFLIM_GOI_hostframe.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(OpenFLIM_GOI_hostframe.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
       
    public boolean is_running(boolean kill_if_live){
        if(gui_.acquisitions().isAcquisitionRunning() || gui_.live().getIsLiveModeOn()){
            if(kill_if_live){
                if(gui_.acquisitions().isAcquisitionRunning()){
                    gui_.acquisitions().haltAcquisition();
                }
                if(gui_.live().getIsLiveModeOn()){
                    gui_.live().setLiveMode(false);
                }
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }
    
    public void set_busy_condition(boolean busy_state){
        uberparent_.set_is_busy(busy_state);
        if(busy_state){
            setting_was_sel = Settings.getSelectedIndex();
            Settings.setSelectedIndex(1);//Graphs panel
            disable_snap_button();
            set_progbar_going();
        } else {
            //Settings.setSelectedComponent(setting_was_sel);
            Settings.setSelectedIndex(setting_was_sel);
            enable_snap_button();
            stop_progbar_going();
        }
    }
    
    public boolean get_busy_condition() {
        return uberparent_.is_busy();
    }    
        
    public void add_infopanel_message(String message){
        infopanel1.add_message(message);
    }
    
    public void override_infopanel_info(){
        infopanel1.set_messages(infobox_messages);
    }
    
    public void remove_infopanel_message(String remove_this){
        infopanel1.remove_exact_message(remove_this);
    }
    
    public void remove_infopanel_message_containing(String searchstring){
        infopanel1.remove_message_containing(searchstring);
    }    
       
    public void optimise_triggering(){
        set_busy_condition(true);
        run_timer(13,"Triggering oprimisation should be done in: ");//13 is ~manually optimised
        Settings.setSelectedIndex(1);   
        if(DEL_DEV_ID.equals("Kentech HDG")){
            try {
                gui_.core().setProperty(DEL_DEV_ID, "Trigger threshold optimised?","Optimise");
            } catch (Exception ex) {
                Logger.getLogger(OpenFLIM_GOI_hostframe.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            String[] options = new String[]{"OK"};
            int option_popup = utils2.option_popup(this, "Warning", "CURRENTLY ONLY IMPLEMENTED FOR KENTECH HDG!", options);
        }
        updateGUIifneeded();
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        del_slider = new javax.swing.JSlider();
        TFB_indic = new javax.swing.JTextField();
        optimise_triggering = new javax.swing.JButton();
        load_calib = new javax.swing.JButton();
        autoscale_y = new javax.swing.JToggleButton();
        autoscale_x = new javax.swing.JToggleButton();
        Settings = new javax.swing.JTabbedPane();
        config_panel2 = new MM2.openFLIM_GOI.UI.config_panel();
        del_graph_panel2 = new MM2.openFLIM_GOI.UI.del_graph_panel();
        jLabel1 = new javax.swing.JLabel();
        FLIM_snap_button = new javax.swing.JToggleButton();
        del_ind_panel1 = new MM2.openFLIM_GOI.UI.del_ind_panel();
        infopanel1 = new MM2.openFLIM_GOI.UI.infopanel();
        Graph_progbar = new javax.swing.JProgressBar();
        mindelval_txt = new javax.swing.JLabel();
        maxdelval_txt = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        del_slider.setMaximum(12500);
        del_slider.setMinorTickSpacing(25);
        del_slider.setSnapToTicks(true);

        TFB_indic.setEditable(false);
        TFB_indic.setDisabledTextColor(new java.awt.Color(0, 0, 200));

        optimise_triggering.setText("Optimise triggering");
        optimise_triggering.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                optimise_triggeringActionPerformed(evt);
            }
        });

        load_calib.setText("Load calibration");
        load_calib.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                load_calibActionPerformed(evt);
            }
        });

        autoscale_y.setText("Y");
        autoscale_y.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoscale_yActionPerformed(evt);
            }
        });

        autoscale_x.setText("X");
        autoscale_x.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoscale_xActionPerformed(evt);
            }
        });

        Settings.addTab("Settings", config_panel2);

        javax.swing.GroupLayout del_graph_panel2Layout = new javax.swing.GroupLayout(del_graph_panel2);
        del_graph_panel2.setLayout(del_graph_panel2Layout);
        del_graph_panel2Layout.setHorizontalGroup(
            del_graph_panel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 404, Short.MAX_VALUE)
        );
        del_graph_panel2Layout.setVerticalGroup(
            del_graph_panel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 310, Short.MAX_VALUE)
        );

        Settings.addTab("Graphs", del_graph_panel2);

        jLabel1.setText("Autoscale:");

        FLIM_snap_button.setText("SNAP!");
        FLIM_snap_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FLIM_snap_buttonActionPerformed(evt);
            }
        });

        mindelval_txt.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        mindelval_txt.setText("0");

        maxdelval_txt.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        maxdelval_txt.setText("12500");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(mindelval_txt, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(maxdelval_txt))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(del_ind_panel1, javax.swing.GroupLayout.PREFERRED_SIZE, 247, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(Settings)
                                    .addComponent(Graph_progbar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                        .addGap(0, 8, Short.MAX_VALUE))
                    .addComponent(infopanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(TFB_indic, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(optimise_triggering, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(load_calib, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(FLIM_snap_button, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(autoscale_x)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(autoscale_y)))
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(del_slider, javax.swing.GroupLayout.PREFERRED_SIZE, 631, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(del_slider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(mindelval_txt)
                    .addComponent(maxdelval_txt))
                .addGap(7, 7, 7)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(Settings, javax.swing.GroupLayout.PREFERRED_SIZE, 338, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(Graph_progbar, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(del_ind_panel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(TFB_indic, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(optimise_triggering)
                    .addComponent(load_calib)
                    .addComponent(autoscale_y)
                    .addComponent(autoscale_x)
                    .addComponent(jLabel1)
                    .addComponent(FLIM_snap_button))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(infopanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void autoscale_xActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autoscale_xActionPerformed
        del_graph_panel2.set_autoscale_x(autoscale_x.isSelected());
    }//GEN-LAST:event_autoscale_xActionPerformed

    private void autoscale_yActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autoscale_yActionPerformed
        del_graph_panel2.set_autoscale_y(autoscale_y.isSelected());
    }//GEN-LAST:event_autoscale_yActionPerformed

    private void optimise_triggeringActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_optimise_triggeringActionPerformed
        disable_snap_button();
        show_progbar(true);
        ot_thread = new Thread(new Optimise_trigger_thread(this));
        ot_thread.start();
        updateGUIifneeded();
    }//GEN-LAST:event_optimise_triggeringActionPerformed

    private void load_calibActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_load_calibActionPerformed
        if(DEL_DEV_ID.equals("Kentech HDG")){
            try {
                gui_.core().setProperty(DEL_DEV_ID, "Calibration loaded?","Load");
                uberparent_.set_is_calib(true);
            } catch (Exception ex) {
                Logger.getLogger(OpenFLIM_GOI_hostframe.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            String[] options = new String[]{"OK"};
            int option_popup = utils2.option_popup(this, "Warning", "CURRENTLY ONLY IMPLEMENTED FOR KENTECH HDG!", options);
        }
        updateGUIifneeded();
    }//GEN-LAST:event_load_calibActionPerformed

    private void FLIM_snap_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FLIM_snap_buttonActionPerformed
        if(core_.getCameraDevice().equalsIgnoreCase("") || core_.getCameraDevice() == null){
            utils2.option_popup(this, "Warning!","No camera device found?",new String[]{"OK"});
        } else {
            remove_infopanel_message_containing("Files will be saved");
            if(uberparent_.get_is_calib()){
                threaded_FLIM_snap();
            } else {
                String[] options = new String[]{"Cancel snap","I don't have a calibration","Load calibration and snap"};
                int option_popup = utils2.option_popup(this, "Warning", "No calibration loaded yet!", options);
                if(option_popup == 0){

                } else if (option_popup == 1){
                    utils2.option_popup(this, "Warning!","Acquire a calibration first! Defaulting to linear calibration...",new String[]{"OK"});
                } else if (option_popup == 2){
                    load_calibActionPerformed(null);
                    threaded_FLIM_snap();
                }
            }
        }
    }//GEN-LAST:event_FLIM_snap_buttonActionPerformed

    void set_TFBdisplayed(int howgood) {
        TFB_indic.setText(Integer.toString(howgood));
    }    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(OpenFLIM_GOI_hostframe.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(OpenFLIM_GOI_hostframe.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(OpenFLIM_GOI_hostframe.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(OpenFLIM_GOI_hostframe.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new OpenFLIM_GOI_hostframe(gui_).setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JToggleButton FLIM_snap_button;
    private javax.swing.JProgressBar Graph_progbar;
    private javax.swing.JTabbedPane Settings;
    private javax.swing.JTextField TFB_indic;
    private javax.swing.JToggleButton autoscale_x;
    private javax.swing.JToggleButton autoscale_y;
    private MM2.openFLIM_GOI.UI.config_panel config_panel2;
    private MM2.openFLIM_GOI.UI.del_graph_panel del_graph_panel2;
    private MM2.openFLIM_GOI.UI.del_ind_panel del_ind_panel1;
    private javax.swing.JSlider del_slider;
    private MM2.openFLIM_GOI.UI.infopanel infopanel1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JButton load_calib;
    private javax.swing.JLabel maxdelval_txt;
    private javax.swing.JLabel mindelval_txt;
    private javax.swing.JButton optimise_triggering;
    // End of variables declaration//GEN-END:variables

    public void update_graph() {
        del_graph_panel2.update_graph();
    }
    
    void set_dels(ArrayList<Integer> new_dels){
        del_ind_panel1.set_del_list(new_dels);
    }
    
    void set_del_seq_in_MM_GUI(ArrayList<Integer> dels_in) {
        String hash_sep_dels_in = "";
        for (Integer del : dels_in){
            hash_sep_dels_in+=del.toString()+"#";
        }
        //System.out.println(hash_sep_dels_in);
        try {
            gui_.core().setProperty(DEL_DEV_ID, DEL_LIST_ID,hash_sep_dels_in);
            updateGUIifneeded();//Don't need to have these shown, but if you want it...
        } catch (Exception ex) {
            Logger.getLogger(OpenFLIM_GOI_hostframe.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Studio get_gui() {
        return gui_;
    }

    public void set_metadata(HashMap<String,String> new_metadata) {
        HashMap<String, String> tmp_metadata = new HashMap<String,String>();
        boolean overwrite_all = false;
        boolean skip_all = false;
        boolean abort = false;
        
        for(String key:new_metadata.keySet()){
            if(metadata.containsKey(key) && overwrite_all == false && abort == false){
                //ASK ABOUT OVERWRITE
                String[] opts = new String[]{"Cancel import", "Skip this value" , "Skip all", "Overwrite this value", "Overwrite all"};
                String err_msg = "A value in the imported metadata will overwrite this existing value: "+key+" : "+metadata.get(key)+" with "+new_metadata.get(key);
                if(!skip_all){
                    int popup_choice = utils2.option_popup((Object)this, "Warning!",(Object)err_msg, opts);
                    switch(popup_choice){
                        case 0:
                            abort = true;
                            break;
                        case 1:
                            tmp_metadata.put(key, metadata.get(key));//put the old value in...
                            break;
                        case 2:
                            skip_all = true;
                            break;
                        case 3: 
                            tmp_metadata.put(key, new_metadata.get(key));
                            break;
                        case 4:
                            overwrite_all = true;
                            break;
                        default:
                            tmp_metadata.put(key, metadata.get(key));//put the old value in...
                            //Default is same as skipping one for now
                            break;
                    }
                } else {
                    tmp_metadata.put(key, metadata.get(key));//put the old value in...
                }

            } else {
                if(!abort){
                    tmp_metadata.put(key, new_metadata.get(key));
                }
            }
        }
        if(!abort){
            metadata.clear();
            metadata.putAll(tmp_metadata);
        }
        //System.out.println("Metadata updated");
    }

    public HashMap<String,String> get_metadata() {
        return metadata;
    }
    
    public void determine_metadata(){
        metadata.put(OpenFLIM_GOI_MM2_Utils.CAMERA_DEVICE_KEY, core_.getCameraDevice());
    }
    
    public void setup_abort() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
        if(gui_.plugins().getMenuPlugins().containsKey("OmniSTOP.UI.OmniSTOP_main")){
            gui_.plugins().getMenuPlugins().get("OmniSTOP.UI.OmniSTOP_main").onPluginSelected();
            omnistop_instance = gui_.plugins().getMenuPlugins().get("OmniSTOP.UI.OmniSTOP_main");
            get_os_ans = omnistop_instance.getClass().getMethod("is_abort_requested");
            omnistop_available = true;
        }
    }
    
    public void test_function(){
        try {
            check_abort();
        } catch (InstantiationException ex) {
            Logger.getLogger(OpenFLIM_GOI_hostframe.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(OpenFLIM_GOI_hostframe.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(OpenFLIM_GOI_hostframe.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(OpenFLIM_GOI_hostframe.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(OpenFLIM_GOI_hostframe.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void check_abort() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException{
        if(omnistop_available){
            Object ans = get_os_ans.invoke(omnistop_instance);
            System.out.println((boolean)ans);
        } else {
            //Maybe put a popup here? Button shouldn't show up if not instantiated anyway
        }
    }
}
