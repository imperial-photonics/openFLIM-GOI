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

import MM2.openFLIM_GOI.Threads.Graph_progbar_thread;
import MM2.openFLIM_GOI.Utilities.OpenFLIM_GOI_MM2_Utils;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.lang.Math;
import org.apache.commons.lang.ArrayUtils;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.Month;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

/**
 *
 * @author Fogim
 */
public class del_graph_panel extends javax.swing.JPanel {
    OpenFLIM_GOI_hostframe parent_ = null;
    int[] x_data = {0};
    int[] y_data = {0};
    final int T_PAD = 20;
    final int B_PAD = 50;
    final int L_PAD = 60;
    final int R_PAD = 20;
    int marksize = 2;
    int tickheight = 5;
    int textheight = 5;
    int default_x_max = 20000;
    int default_y_max = 5000;    
    int default_x_tickspacing = 500;
    int default_y_tickspacing = 100;
    int x_range_min = 0;
    int y_range_min = 0;    
    int x_range_max = default_x_max;
    int y_range_max = default_y_max;
    int min_x = 0;
    int min_y = 0;
    int max_x = default_x_max;
    int max_y = default_y_max;       
    int x_tickspacing = 500;
    int y_tickspacing = 100;
    boolean autoscale_x = false;
    boolean autoscale_y = false;
    String x_ax_label = "DEFAULT - [ps]";
    String y_ax_label = "DEFAULT - [DN]";
    ArrayList<Double> range_options = new ArrayList(Arrays.asList(1.0,2.0,5.0,10.0,25.0,50.0,100.0,250.0,500.0,1000.0));
    OpenFLIM_GOI_MM2_Utils utils_ = null;
    int[][] graph_to_show = null;
    Thread gm_thread;
    AffineTransform txt_rot_trans = new AffineTransform();
    int fontsize = 10;
    public Font normalfont = new Font(null, Font.PLAIN, fontsize);
    public Font rotatedfont = new Font(null, Font.PLAIN, fontsize);
    
    /**
     * Creates new form del_graph_panel
     */
    public del_graph_panel() {
        initComponents();
        utils_ = new MM2.openFLIM_GOI.Utilities.OpenFLIM_GOI_MM2_Utils();
        txt_rot_trans.rotate(Math.toRadians(-90), 0, 0);
        rotatedfont = normalfont.deriveFont(txt_rot_trans);
        //gm_thread = new Thread(new Graph_monitor_thread(this));
        //gm_thread.start();
    }
    
    
    public void set_parent(OpenFLIM_GOI_hostframe parent_frame){
        parent_ = parent_frame;
    }
    
    public void set_def_xrange_max(int newmax){
        default_x_max = newmax;
    }
    
    public void set_graph(int[][] new_vals){
        graph_to_show = new_vals;
        System.out.println("new graph values loaded");
    }
    
    public void update_graph() {
        if(graph_to_show!=null){
            int length = graph_to_show.length;//should be 2, for x and y
            x_data = graph_to_show[0];
            y_data = graph_to_show[1];
            //System.out.println(x_data.length+" datapoints");
            List x_list;
            List y_list;
            x_list = Arrays.asList(ArrayUtils.toObject(x_data));
            y_list = Arrays.asList(ArrayUtils.toObject(y_data));

        if(length>0){
                max_x = (int) Collections.max(x_list);
                max_y = (int) Collections.max(y_list);       
                min_x = (int) Collections.min(x_list);
                min_y = (int) Collections.min(y_list);               
            } else {
                max_x = 0;
                max_y = 0;
                min_x = 0;
                min_y = 0;
            }
            if(max_x<1){
                max_x = 1;
            }
            if(max_y<1){
                max_y = 1;
            }
            if (min_x<0){
                min_x = 0;
            }
            if (min_y<0){
                min_y = 0;
            }         
            //System.out.println("("+min_x+","+min_y+") - ("+max_x+","+max_y+")");
            set_autoscale_x(autoscale_x);
            set_autoscale_y(autoscale_y);
            repaint();            
        }
    }
    
    void set_autoscale_x(boolean scale){
        autoscale_x = scale;
        if(scale == true){
            int num_x_steps = 11;
            int stepsize_x = utils_.set_decent_step(range_options, (double) (max_x-min_x), num_x_steps);
            x_range_min = utils_.round_down_to_nearest(0.9*min_x, stepsize_x);
            x_range_max = utils_.round_up_to_nearest(1.1*max_x, stepsize_x);
        } else {
            x_range_min = 0;
            x_range_max = default_x_max;
        }
        //System.out.println("Autoscale - X: ("+x_range_min+"-"+x_range_max+")");
        repaint();
    }
    
    void set_autoscale_y(boolean scale){
        autoscale_y = scale;
        //System.out.println(scale);
        if(scale == true){
            int num_y_steps = 11;
            int stepsize_y = utils_.set_decent_step(range_options, (double) (max_y-min_y), num_y_steps);
            y_range_min = utils_.round_down_to_nearest(0.9*min_y, stepsize_y);
            y_range_max = utils_.round_up_to_nearest(1.1*max_y, stepsize_y);
            if(y_range_max == y_range_min){
                if(y_range_max<1){
                    y_range_max = 10;
                } else {
                    y_range_max = y_range_min*10;
                    y_tickspacing = (y_range_max-y_range_min)/10;
                }
            }
        } else {
            int num_y_steps = 11;
            y_range_min = 0;
            y_range_max = default_y_max;            
            y_tickspacing = (y_range_max-y_range_min)/10;
        }
        //System.out.println("Autoscale - Y:("+y_range_min+"-"+y_range_max+")");
        repaint();
    }    
            
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        //Y-axis
        g2.drawLine(L_PAD, B_PAD, L_PAD, h-B_PAD);
        //X-axis
        g2.drawLine(L_PAD, h-B_PAD, w-R_PAD, h-B_PAD);
        //Don't draw null data
        if(x_data.length>0){
            //System.out.println("Display ranges - X: ("+x_range_min+"-"+x_range_max+")  Y:("+y_range_min+"-"+y_range_max+")");
            double xScale = (w - (L_PAD+R_PAD))/((double)(x_range_max-x_range_min));
            //double maxValue = 100.0;
            double yScale = (h - (T_PAD+B_PAD))/((double)(y_range_max-y_range_min));
            // The origin location.
            int x0 = L_PAD;
            int y0 = h-B_PAD;
            // X-label location
            int xl_x = (w/2)+L_PAD;
            int xl_y = h-(B_PAD/3);
            // Y-label location
            int yl_x = L_PAD/3;
            int yl_y = (h/2)+T_PAD;
            //Draw ticks
            //X-ticks
            int tick_loc = x_range_min;
            int ctr = 0;
            int mjr_space = 5;
            while (tick_loc<x_range_max){
                int tickdist_x_px = (int)(xScale*(tick_loc-x_range_min));
                g2.drawLine((x0+tickdist_x_px), y0, (x0+tickdist_x_px), y0+tickheight);
                if(ctr%mjr_space==0){
                    String num_to_render = Integer.toString((int)(x_range_min+ctr*x_tickspacing));
                    int move_down_by = (2*tickheight)+g2.getFontMetrics().getHeight();
                    g2.drawString(num_to_render, (int)(x0+tickdist_x_px-(g2.getFontMetrics().stringWidth(num_to_render)/2)), (int)(y0+move_down_by));
                    g2.drawLine(x0+tickdist_x_px, y0, x0+tickdist_x_px, (int)(y0+(tickheight*1.5)));
                }
                tick_loc+=x_tickspacing;
                ctr++;
            }
            //Y-ticks
            tick_loc = y_range_min;
            ctr = 0;            
            int move_xl_text_down_by = 5;
            int move_yl_text_left_by = 5;
            g2.drawString("X label", (int)(xl_x-move_xl_text_down_by), (int)((xl_y)+(g2.getFontMetrics().getHeight()/1.5)));
            g2.setFont(rotatedfont);
            g2.drawString("Y label", (int)(yl_x-move_yl_text_left_by), (int)((yl_y)+(g2.getFontMetrics().getHeight()/1.5)));
            g2.setFont(normalfont);
            while (tick_loc<y_range_max){
                int tickdist_y_px = (int)(yScale*(tick_loc-y_range_min));
                g2.drawLine(x0, y0-tickdist_y_px, x0-tickheight, y0-tickdist_y_px);
                if(ctr%mjr_space==0){
                    String num_to_render = Integer.toString((int)(y_range_min+(ctr*y_tickspacing)));
                    int move_left_by = (tickheight*2)+g2.getFontMetrics().stringWidth(num_to_render);
                    g2.drawString(num_to_render, (int)(x0-move_left_by), (int)((y0-tickdist_y_px)+(g2.getFontMetrics().getHeight()/3)));
                    g2.drawLine(x0, y0-tickdist_y_px, (int)(x0-(tickheight*1.5)), y0-tickdist_y_px);
                }
                tick_loc+=y_tickspacing;
                ctr++;
            }            
            //Draw data
            g2.setPaint(Color.red);
            for(int j = 0; j < x_data.length; j++) {
                int x = x0 + (int)(xScale * (x_data[j]-x_range_min));
                int y = y0 - (int)(yScale * (y_data[j]-y_range_min));
                g2.fillOval(x-marksize, y-marksize, marksize*2, marksize*2);
            }
        }
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    public boolean should_i_kill_all_threads() {
        if(parent_ != null){
            return parent_.should_i_kill_all_threads();
        } else {
            return false;
        }
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
