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
package MM2.openFLIM_GOI.Threads;

import MM2.openFLIM_GOI.UI.del_graph_panel;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Graph_monitor_thread implements Runnable {

    del_graph_panel loc_parent_;
    
    public Graph_monitor_thread(del_graph_panel parent) {
        loc_parent_ = parent;
    }

    @Override
    public void run() {
        while(!loc_parent_.should_i_kill_all_threads()){
            try {
                Thread.sleep(150);
            } catch (InterruptedException ex) {
                Logger.getLogger(Graph_monitor_thread.class.getName()).log(Level.SEVERE, null, ex);
            }
            loc_parent_.update_graph();
        }
    }
    
}
