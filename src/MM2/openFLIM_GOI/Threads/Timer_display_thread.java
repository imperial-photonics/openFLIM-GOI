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
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Fogim
 */
public class Timer_display_thread implements Runnable{
    private OpenFLIM_GOI_hostframe parent_ = null;
    private int num_s_to_countdown = 10;
    private String prepend_ = "";
    
    public Timer_display_thread(OpenFLIM_GOI_hostframe parent_frame, int startval_s, String prepend) {
        parent_ = parent_frame;
        num_s_to_countdown = startval_s;
        prepend_ = prepend;
    }
    
    @Override
    public void run() {
        //Get time
        Date date = new Date();
        long start_time_ms = date.getTime();
        long delta = num_s_to_countdown*1000;
        while(delta>0){
            date = new Date();
            delta = (start_time_ms+(num_s_to_countdown*1000))-date.getTime();
            try {
                parent_.remove_infopanel_message_containing(prepend_);
                Thread.sleep(10);
                parent_.add_infopanel_message(prepend_+Integer.toString((int)(delta/1000)));
                //Loop, updating every 0.1 seconds
                Thread.sleep(90);
            } catch (InterruptedException ex) {
                Logger.getLogger(Timer_display_thread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        parent_.remove_infopanel_message_containing(prepend_);
    }    
}
