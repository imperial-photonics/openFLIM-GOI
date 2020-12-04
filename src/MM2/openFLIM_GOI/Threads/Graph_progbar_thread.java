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

import MM2.openFLIM_GOI.UI.OpenFLIM_GOI_hostframe;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Graph_progbar_thread implements Runnable {
    private OpenFLIM_GOI_hostframe parent_ = null;
    
    public  Graph_progbar_thread(OpenFLIM_GOI_hostframe parent_frame){
        parent_ = parent_frame;
    }    

    @Override
    public void run() {
        //System.out.println("Starting progbar");
        while(!parent_.should_i_kill_all_threads()){
            try {
                Thread.sleep(150);
            } catch (InterruptedException ex) {
                Logger.getLogger(Graph_progbar_thread.class.getName()).log(Level.SEVERE, null, ex);
            }
            if(parent_.is_progbar_active){
                parent_.show_progbar(true);
            } else {
                parent_.show_progbar(false);
            }
        }
        //System.out.println("Stopping progbar thread");
    }    
    
}
