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
package MM2.openFLIM_GOI.Utilities;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author <sunil.kumar@imperial.ac.uk>
 */
public class openFLIM_GOI_settings_obj {
    ArrayList<Integer> get_delays = new ArrayList<Integer>();
    boolean is_calib = false;
    HashMap<String,String> metadata = new HashMap<String,String>();
    int min_del_step = 25;    
    String DEL_DEV_ID = null;
    String DEL_PROP_ID = null;
    String DEL_LIST_ID = null;
    boolean calib_loaded = false;
    int [][] del_graph = null;
    
    int stored_maxpoint_ps = 3500;
    int stored_num_dels = 7;
    int stored_gatewidth_ps = 3000;
    int stored_tau_pred_ps = 3500;
    int stored_t_max_ps = 12500;
    double stored_reprate_mhz = 80.0;    
}
