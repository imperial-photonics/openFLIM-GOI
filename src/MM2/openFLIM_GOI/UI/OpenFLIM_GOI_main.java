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

import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JFrame;
import org.micromanager.MenuPlugin;
import org.micromanager.Studio;
import org.micromanager.events.NewDisplayEvent;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;

/**
 *
 * @author Fogim
 */
@Plugin(type = MenuPlugin.class)
public class OpenFLIM_GOI_main implements MenuPlugin, SciJavaPlugin{
    //Name for the plugin
    public static final String menuName = "openFLIM-GOI for MM2";
    private Studio gui_;
    public static OpenFLIM_GOI_hostframe frame_ = null;
    private boolean init = false;
    private boolean busy_condition = false;
    private boolean is_calib = false;
    
        
    @Override
    public String getSubMenu() {
        return("openScopes");
    }

    @Override
    public void onPluginSelected() {
        if(frame_ == null){
            frame_ = new OpenFLIM_GOI_hostframe(gui_);
        }
        frame_.setparent(this);
        frame_.setVisible(true);
        init = true;
    }
  
    @Override
    public void setContext(Studio studio) {
        gui_ = studio;
    }
  
    @Override
    public String getName() {
        return menuName;
    }

    @Override
    public String getHelpText() {
        return("Widefield GOI-based FLIM acquisition software - see https://github.com/imperial-photonics/openFLIM-GOI/");
    }

    @Override
    public String getVersion() {
        return("0.1.0");
    }

    @Override
    public String getCopyright() {
        return("Copyright Imperial College London [2020]");
    } 
    
    //"API" for plugin consists of the following functions
    
    public void ensure_instantiated(){
        if(init){
            //Everything is fine!
        } else {
            onPluginSelected();
        }
    }
    
    public HashMap<String,String> get_metadata(){
        return frame_.get_metadata();
    }
    
    public void set_metadata(HashMap<String,String> new_metadata){
        frame_.set_metadata(new_metadata);
    }
    
    public ArrayList<Integer> get_delays(){
        ensure_instantiated();
        return frame_.get_dels();
    }
    
    public void set_delays(ArrayList<Integer> new_dels){
        ensure_instantiated();
        frame_.set_dels(new_dels);
    }
    
    public boolean is_busy(){
        return busy_condition;
    }
    
    public void set_is_busy(boolean set_to){
        busy_condition = set_to;
    }
        
    public void set_is_calib(boolean is_it){
        is_calib = is_it;
    }
    
    public boolean get_is_calib(){
        return is_calib;
    }
        
    public boolean fire_snap(){
        ensure_instantiated();
        frame_.threaded_FLIM_snap();
        return true;
    }        
}
