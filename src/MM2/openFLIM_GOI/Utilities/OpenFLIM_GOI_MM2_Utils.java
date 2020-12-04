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

import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import javax.swing.JOptionPane;

/**
 *
 * @author Fogim
 */
public class OpenFLIM_GOI_MM2_Utils {
    boolean test;
    public static final String TIME_SEQ = "Time step";
    public static final String XY_SEQ = "XY position";
    public static final String Z_SEQ = "Z stack";
    public static final String CONF_SEQ = "Config change";
    public static final String HL1_ON = "<font color = 'ff0000'>";
    public static final String HL1_OFF = "</font>";
    public static final String RUNNABLE_SUBFOLDER = "OpenHCA2_runnables";
    public static final String JSON_DELIMITER = "$#!+*";
    public static final String FILE_ENDING = "_OHCA2.ome.tif";
    
    public static final String CAMERA_DEVICE_KEY = "Camera";
    
    int max_possible_del_val_ps = 20000;
    int zeropadlength = 4;
    
    public OpenFLIM_GOI_MM2_Utils(){
    }
    
    public int max_allowd_del(){
        return max_possible_del_val_ps;
    }
    
    public double abs_min_diff_dbl(ArrayList<Double> inputlist){
        double min_diff = Double.MAX_VALUE;//Default position is to give the worst-case scenario
        if(inputlist.size()<2){
            min_diff = 0;
        } else {
            Collections.sort(inputlist);
            int i=0;
            while (i<(inputlist.size()-1)){
                double delta = inputlist.get(i+1)-inputlist.get(i);
                if(delta<Math.abs(min_diff)){
                    min_diff = delta;
                }
                i++;
            }
        }
        return min_diff;   
    }
    
    public int abs_min_diff_int(ArrayList<Integer> inputlist){
        ArrayList<Double> trans_list = new ArrayList<Double>();
        for(int val : inputlist){
            trans_list.add((double)val);
        }
        double ans = abs_min_diff_dbl(trans_list);
        return (int)ans;
    }
    
    public boolean does_file_exist(File file_to_check){
        return false;
    }
    
    public int s_to_ms(double s){
        return Integer.parseInt(read_num_sensible(String.valueOf(s*1000.0),true));
    }
    
    public int min_to_ms(double s){
        return Integer.parseInt(read_num_sensible(String.valueOf(s*1000*60),true));
    }    
    
    public int hr_to_ms(double s){
        return Integer.parseInt(read_num_sensible(String.valueOf(s*1000*60*60),true));
    }        
    
    public double ms_to_s(int ms){
        return Double.parseDouble(read_num_sensible(String.valueOf(ms/1000),false,true,3));//3d.p.s
    }
    
    public double ms_to_min(int ms){
        return Double.parseDouble(read_num_sensible(String.valueOf(ms/(60*1000)),false,true,3));//3d.p.s
    }    
    
    public double ms_to_hr(int ms){
        return Double.parseDouble(read_num_sensible(String.valueOf(ms/(60*60*1000)),false,true,3));//3d.p.s
    }        
    
    public String give_html(String inputstr, String format){
        boolean skip = false;
        format = format.toUpperCase();
        if (format == "I" || format == "B" || format =="U"){
        } else {
            skip = true;
        }
        if (skip == true){
            return(inputstr);
        } else {
            return("<"+format+">"+inputstr+"</"+format+">");
        }
    }
    
    public int option_popup(Object parent, String title, Object message, Object[] options){
        return JOptionPane.showOptionDialog((Component) parent, message,title, JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE,null,options,options[0]);
    }
    
    //https://deano.me/2012/01/java-resize-arrays-multi-dimensional-arrays/
    public static Object resizeArray(Object oldArray, int newSize) {
        int oldSize = java.lang.reflect.Array.getLength(oldArray);
        Class elementType = oldArray.getClass().getComponentType();
        Object newArray = java.lang.reflect.Array.newInstance(elementType,newSize);
        int preserveLength = Math.min(oldSize,newSize);
        if (preserveLength > 0) {
            System.arraycopy(oldArray,0,newArray,0,preserveLength);
        }
        return newArray;
    }

    public String strip_non_numeric(String stringtostrip){
        int negcheck = stringtostrip.indexOf("-");
        String strippedstring = stringtostrip.replaceAll("[^\\d.]", "");
        String nodots = strippedstring.replaceAll("[^\\d]","");
        int firstdot = strippedstring.indexOf(".");//First decimal point will be taken
        String floatval;
        if(firstdot>=0){
            floatval = nodots.substring(0,firstdot)+"."+nodots.substring(firstdot);
        } else {
            floatval = nodots;
        }
        if(negcheck == 0 && nodots.length()>0){//First character is a - sign, and there is actually a number there
            floatval = "-"+floatval;
        }
        return floatval;
    }
    
    public String ensure_unique_filename(String dir_path, String fileName){
        String new_name = "";
        File f = new File(dir_path+"\\"+fileName+OpenFLIM_GOI_MM2_Utils.FILE_ENDING);
//        if(next_num_index(dir_path, fileName)>0){
//            String[] split = fileName.split("_");
//            String last_num = "";
//            if(split.length>0){//might have a _#### in it...
//                last_num = split[split.length];//Should be just before the _OHCA2.ome.tiff which isn't appended yet
//                String last_num_sensible = read_num_sensible(last_num,true,true);
//                if(last_num_sensible.length() == last_num.length() && last_num.length()>0){ //Was it all just digits
//                    String pre_num = fileName.substring(0,fileName.lastIndexOf("_"));
//                    int indexed = Integer.parseInt(last_num_sensible)+1;
//                    new_name = pre_num+"_"+zero_pad(Integer.toString(indexed),zeropadlength);
//                } 
//            }
        if(next_num_index(dir_path, fileName)>0){
            String[] split = fileName.split("_");
            String last_num = "";
            if(split.length>0){//might have a _#### in it...
                last_num = split[split.length-1];//Should be just before the _OHCA2.ome.tiff which isn't appended yet
                String pre_num = fileName;
                if(fileName.contains("_")){
                    //MIGHT NEED TO CHECK IF NUMERIC ONLY
                    if(is_post_num  (last_num)){
                        pre_num = fileName.substring(0,fileName.lastIndexOf("_"));
                    } else {
                        pre_num = fileName;
                    }
                }
                new_name = pre_num+"_"+zero_pad(Integer.toString(next_num_index(dir_path, fileName)),zeropadlength);
            }
        } else {
            new_name = fileName+"_"+zero_pad("0",zeropadlength);
        }
        return dir_path+"\\"+new_name;
    }
    
    public boolean num_only(String check_this){
        if(check_this.length() == strip_non_numeric(check_this).length()){
            return true;
        } else {
            return false;
        }
    }
    
    public boolean is_post_num(String check_this){
        if(num_only(check_this) && check_this.length() == zeropadlength){
            return true;
        } else {
            return false;
        }
    }
    
    public int next_num_index(String dir_path, String fileName){
        int next_post_num = 0;
        //We want to look for something containing an exact match of our name string
        File search_dir = new File(dir_path);
        File[] existing_files = search_dir.listFiles();
        ArrayList<String> matched_files = new ArrayList<String>();
        if(existing_files!=null){
            for(File search_file : existing_files){
                if(search_file.toString().contains(fileName)){
                    String found_file = search_file.toString().substring((dir_path.length()+1),search_file.toString().length());
                    found_file = found_file.replace(OpenFLIM_GOI_MM2_Utils.FILE_ENDING, "");
                    matched_files.add(found_file);
                }
            }
        }
        //Now we have a list of files containing the match - we can parse them to see if the bit to the right contains a number
        //This should only pay attention to auto-generated numbers? But what if the search string contains it somehow? CHECK LATER
        if(matched_files.isEmpty()){
            //no matches, just append zeros
            return 0;
        } else {
            next_post_num = 0;
            for(String name : matched_files){
                String splitter = name;
                if(name.contains("_")){
                    //MIGHT NEED TO CHECK IF NUMERIC ONLY
                    splitter = name.substring(0,name.lastIndexOf("_")+1);
                }
                String[] split = name.split(splitter);
                String remaining = "";
                for (String fragment : split){
                    remaining = remaining.concat(fragment);
                }
                int pn_val = Integer.parseInt(read_num_sensible(remaining, true, true, zeropadlength));
                if(pn_val>=next_post_num){
                    next_post_num = pn_val+1;
                }
            }
        }
        return next_post_num;
    }
    
    public String ensure_valid_filename(String fileName){
        //https://stackoverflow.com/questions/15075890/replacing-illegal-character-in-filename
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }    
    
    public String zero_pad(String input_str, int num_digits){
        //Will just return input if output is longer than num_digits
        String output_str = "";
        int delta = num_digits-input_str.length();
        if(delta>0){
            for (int i=0;i<delta;i++){
                output_str+="0";
            }
        }
        output_str+=input_str;
        return output_str;
    }    
    
    public String constrain_val(String val, double min, double max){
        return Double.toString(constrain_val(Double.parseDouble(val),min,max));
    }
    
    public int set_decent_step(ArrayList<Double> scale_options, double range, int num_steps_min){
        Collections.sort(scale_options);
        int use_step = 1;
        for (double scale_opt : scale_options){
            if(range>(scale_opt*num_steps_min)){
                use_step = (int)scale_opt;
            }
        }
        return use_step;
    }
    
    public int round_to_nearest(double  value, int stepsize){
        return (int)(stepsize*(Math.round(value/stepsize)));
    }
    
    public int round_up_to_nearest(double  value, int stepsize){
        return (int)(stepsize*(Math.ceil(value/stepsize)));
    }    
    
    public int round_down_to_nearest(double value, int stepsize){
        return (int)(stepsize*(Math.floor(value/stepsize)));
    }        

    public double constrain_val(double val, double min, double max){    
        if (val<min){
            val = min;
        }
        if (val>max){
            val = max;
        }
        return val;
    }
    
    public float sum_arr(byte[] arr){
        float total = 0;
        for (int i=0;i<arr.length;i++){
            total+=arr[i];
        }
        return total;
    }
    
    public float sum_arr(short[] arr){
        float total = 0;
        for (int i=0;i<arr.length;i++){
            total+=arr[i];
        }
        return total;        
    }    
    
    public String read_num_sensible(String input_value, boolean force_int, boolean pos_only, int num_dp){
        //Probably a stupid way to get right # of decimal points, but it should at least be 'safe'...
        //https://stackoverflow.com/questions/153724/how-to-round-a-number-to-n-decimal-places-in-java
        String retval = "0.0";
        if(force_int){
            retval = "0";
        }
        if(strip_non_numeric(input_value).replaceAll("-", "").length()<1){//Also, just "-" would be bad...
            //Leave everything as zero...
        } else {
            double value = Double.parseDouble(strip_non_numeric(input_value));
            if(pos_only){
                value = Math.abs(value);
            }
            if(force_int){
                value = Math.round(value);
                return Integer.toString((int)value);
            }

            retval = Double.toString(value);
            if (num_dp>0){
                if(retval.indexOf(".")>0){
                    if(retval.length()>retval.indexOf(".")+num_dp){
                        retval = retval.substring(0, retval.indexOf(".")+num_dp+1);
                    } else {
                        retval = retval.substring(0, retval.length());
                    }
                }
            }
        }
        return retval;
    }    
    
    public String read_num_sensible(String input_value, boolean force_int, boolean pos_only){
        return read_num_sensible(input_value, force_int, pos_only, -1);
    }
    
    //Overloading for the lazy. This definitely won't lead to problems...
    public String read_num_sensible(String input_value, boolean force_int){
        return read_num_sensible(input_value, force_int, false);
    }
    public String read_num_sensible(String input_value){
        return read_num_sensible(input_value, false, false);
    }    
    
    public String force_sf(Double inputnum, Integer n_dp, Integer zero_pad_length){
        //Decimal places
        String string_val = String.format("%."+n_dp.toString()+"f", inputnum);
        //Zeropad (including the dot)
        while(string_val.length()<zero_pad_length){
            string_val = "0"+string_val;
            System.out.println(string_val);
        }
        return inputnum.toString();
    }
}
