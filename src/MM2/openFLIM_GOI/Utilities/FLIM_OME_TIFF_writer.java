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

import MM2.openFLIM_GOI.UI.OpenFLIM_GOI_hostframe;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import loci.common.DataTools;
import loci.common.services.ServiceException;
import loci.formats.CoreMetadata;
import loci.formats.FormatException;
import loci.formats.IFormatWriter;
import loci.formats.ImageWriter;
import loci.formats.ome.OMEXMLMetadata;
import loci.formats.services.OMEXMLServiceImpl;
import mmcorej.CMMCore;
import mmcorej.DeviceType;
import mmcorej.DoubleVector;
import ome.units.quantity.Length;
import ome.xml.model.enums.DimensionOrder;
import ome.xml.model.enums.PixelType;
import ome.xml.model.primitives.NonNegativeInteger;
import ome.xml.model.primitives.PositiveFloat;
import ome.xml.model.primitives.PositiveInteger;
import org.micromanager.Studio;
import org.micromanager.data.Image;

/**
 *
 * @author Fogim
 */
public class FLIM_OME_TIFF_writer {
    OpenFLIM_GOI_hostframe parent_;
    Studio gui_;
    CMMCore core_;
    CoreMetadata cm;
    ArrayList<Integer> delays;
    HashMap<String,String> metadata;
    OpenFLIM_GOI_MM2_Utils utils2 = new OpenFLIM_GOI_MM2_Utils();
    
    public FLIM_OME_TIFF_writer(){
        
    }
    
    public void set_parent(OpenFLIM_GOI_hostframe parent){
        parent_ = parent;
        gui_ = parent_.get_gui();
        core_ = gui_.getCMMCore();
        delays = new ArrayList<Integer>();
        delays.add(0);
    }
    
    public boolean FLIM_snap(String base_path, String file_name, String del_dev, String del_prop) throws ServiceException, FormatException, IOException, Exception{
        delays = parent_.get_dels();
        //No validity checks? Guess if a name is invalid, then the file shouldn't exist...
        String path = utils2.ensure_unique_filename(base_path,file_name);
        //path = "C:\\tmp\\TESTACQS\\HELLO";
        System.out.println("FLIM snap");
        if(gui_.acquisitions().isAcquisitionRunning() | gui_.live().getIsLiveModeOn()){
            gui_.acquisitions().haltAcquisition();
            gui_.live().setLiveMode(false);
        }
        OMEXMLMetadata m = setBasicMetadata();
        //path = "C:\\tmp\\TESTACQS\\dfsdfsd";
        IFormatWriter writer = generateWriter(path+OpenFLIM_GOI_MM2_Utils.FILE_ENDING, m);
        for (Integer delay:delays){
            int del = delays.indexOf(delay);
            core_.setProperty(del_dev, del_prop, delay);

            long dim = core_.getImageWidth()*core_.getImageHeight();
            int[] accImg = new int[(int)dim];
            //Accumulation?
            int n_acc = 1;
            core_.sleep((int)Math.round(2));//ms wait to settle
            for(int i=0;i<n_acc;i++){
                core_.snapImage();
                //core_.waitForDeviceType(DeviceType.CameraDevice);
                Object img = core_.getImage();
                
//                gui_.getDisplayManager().show((Image) img);
                //Frame accumulator
                if (core_.getBytesPerPixel() == 2){
                    short[] pixS = (short[]) img;
                    for (int j = 0; j < dim; j++) {
                        accImg[j] = (int) (accImg[j] + (int) (pixS[j] & 0xffff));
                    }
                } else if (core_.getBytesPerPixel() == 1){
                    byte[] pixB = (byte[]) img;
                    for (int j = 0; j < dim; j++) {
                        accImg[j] = (int) (accImg[j] + (int) (pixB[j] & 0xff));
                    }
                }
            }
            //REVERSE ENDIAN-NESS
            for(int idx = 0; idx<dim; idx++){
                accImg[idx] = Integer.reverseBytes(accImg[idx]);
            }
            saveLayersToOMETiff(writer, accImg, delays.indexOf(delay));         
        }
        writer.close();        
        return true;
    }
    
    public void FLIM_save(){
        
    }
    
    private void saveLayersToOMETiff(IFormatWriter writer, Object img, int layer)
            throws Exception {
//        Object img = core_.getImage();
        if (img instanceof byte[]) {
            System.out.println("Img is in bytes");
            writer.saveBytes(layer, (byte[]) img);
        } else if (img instanceof short[]) {
            byte[] bytes = DataTools.shortsToBytes((short[]) img, true);
//            System.out.println("Img is short[], converting to bytes, i = " + layer);
            writer.saveBytes(layer, bytes);
        } else  if (img instanceof int[]){
            byte[] bytes = DataTools.intsToBytes((int[]) img, true);
            writer.saveBytes(layer, bytes);
        } else
        {
            System.out.println("I don't know what type img is!");
        }
    }    
    
    private IFormatWriter generateWriter(String path, OMEXMLMetadata m)
        throws FormatException, IOException {
        IFormatWriter writer = new ImageWriter().getWriter(path);
        writer.setWriteSequentially(true);
        writer.setMetadataRetrieve(m);
        writer.setCompression("LZW");
        writer.setId(path);
        return writer;
    }    
    
    private OMEXMLMetadata setBasicMetadata() throws ServiceException{
        OMEXMLServiceImpl serv = new OMEXMLServiceImpl();
        OMEXMLMetadata m = serv.createOMEXMLMetadata();
        metadata = parent_.get_metadata();
        
        int num_dels = delays.size();
        String[] del_arr = get_string_array(delays);
        m.createRoot();
        m.setImageID("Image:0", 0);
        m.setPixelsID("Pixels:0", 0);
        m.setPixelsDimensionOrder(DimensionOrder.XYZCT, 0);
        m.setChannelID("Channel:0:0", 0, 0);
        m.setChannelSamplesPerPixel(new PositiveInteger(1), 0, 0);
        m.setPixelsBinDataBigEndian(Boolean.FALSE, 0, 0);
        m.setImageDescription("PLACEHOLDER", 0);
        long bpp = core_.getBytesPerPixel();
        m.setPixelsType(PixelType.UINT32, 0);
        
        PositiveInteger w_orig = new PositiveInteger((int)core_.getImageWidth());
        PositiveInteger h_orig = new PositiveInteger((int)core_.getImageHeight());
        PositiveInteger num_gates = new PositiveInteger(num_dels);
        
        m.setPixelsSizeX(w_orig, 0);
        m.setPixelsSizeY(h_orig, 0);
        m.setPixelsSizeZ(new PositiveInteger(1), 0);
        m.setPixelsSizeC(new PositiveInteger(1), 0);        
        m.setPixelsSizeT(num_gates, 0);
        
        //#####################################################<STUFF I ADDED>
        //if(metadata.containsKey(ODC_MM2_Utils.CAMERA_DEVICE_KEY)){
        //    m.setDetectorModel(metadata.get(ODC_MM2_Utils.CAMERA_DEVICE_KEY), 0, 0);
        //}
        //####################################################</STUFF I ADDED>
        
        PositiveFloat pitch = checkPixelPitch();
        double pitchD = pitch.getValue();
        Length len = new Length(1,ome.units.UNITS.MICROM);
//ASSUMING CUBOID PIXELS WITH Z=1.0!
        m.setPixelsPhysicalSizeX(new Length(pitchD,ome.units.UNITS.MICROM), 0);
        m.setPixelsPhysicalSizeY(new Length(pitchD,ome.units.UNITS.MICROM), 0);
        m.setPixelsPhysicalSizeZ(new Length(1.0,ome.units.UNITS.MICROM), 0);
        PlatformIndependentGuidGen p = PlatformIndependentGuidGen.getInstance();
        
        for (int j=0;j<num_dels;j++){
            m.setUUIDFileName(del_arr[j], 0, j);
            m.setUUIDValue(p.genNewGuid(), 0, j);
            m.setTiffDataPlaneCount(new NonNegativeInteger(0), 0, j);
            m.setTiffDataIFD(new NonNegativeInteger(0), 0, j);
            m.setTiffDataFirstZ(new NonNegativeInteger(0), 0, j);
            m.setTiffDataFirstC(new NonNegativeInteger(0), 0, j);
            m.setTiffDataFirstT(new NonNegativeInteger(0), 0, j);
            m.setPlaneTheZ(new NonNegativeInteger(0), 0, j);
            m.setPlaneTheC(new NonNegativeInteger(0), 0, j);
//Looks to be the timeplane here            
            m.setPlaneTheT(new NonNegativeInteger(j), 0, j);
        }
        
// deal with FLIMfit issue loading single plane images with moduloAlongT     
        if (num_dels > 2){ 
            CoreMetadata cm = new CoreMetadata();

            cm.moduloT.labels = del_arr;
            cm.moduloT.unit = "ps";
            cm.moduloT.typeDescription = "Gated";
            cm.moduloT.type = loci.formats.FormatTools.LIFETIME;
            serv.addModuloAlong(m, cm, 0);
            System.out.println("did addModulo");
        }
        
        return m;
    }
    
    private String[] get_string_array(ArrayList<Integer> values){
        String[] stringvals = new String[values.size()];
        for (int i=0;i<values.size();i++){
            stringvals[i] = delays.get(i).toString();
        }
        return stringvals;
    }

    private PositiveFloat checkPixelPitch() {
        //BINNING SHOULD BE COMPENSATED FOR AUTOMATICALLY?
        double pixelSizeUm = core_.getPixelSizeUm();
        if(pixelSizeUm<=0.0){
            pixelSizeUm = 1.0;
        }
        PositiveFloat pitch = new PositiveFloat(pixelSizeUm);
        return pitch;
    }
    
}
