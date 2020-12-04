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
import java.io.File;
import java.io.FilenameFilter;

/**
 *
 * @author kumars2
 */
public class CLASS_Filefilter implements FilenameFilter{
    @Override
    public boolean accept(File directory, String fileName) {
        boolean is_ok = false;
        if (fileName.endsWith(".class")) {
            is_ok = true;
            //System.out.println("yes");
        } else {
            //System.out.println("no");
        }
        return is_ok;
    }
   
}
