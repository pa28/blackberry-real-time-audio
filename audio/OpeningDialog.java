/*
 * OpeningDialog.java
 *
 * © H. R. Buckley, 2003-2011
    BlackBerry Real Time Audio - real time audio analysis
    Copyright (C) 2011  H. R. Buckley

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package audio;

import java.io.InputStream;
import java.io.IOException;
import net.rim.device.api.ui.component.Dialog;

/**
 * 
 */
public class OpeningDialog {
    public OpeningDialog() {
        try {
            InputStream istrm = getClass().getResourceAsStream("license.txt");
            int a = istrm.available();
            byte[] b = new byte[a];
            istrm.read(b);
            
            String s = new String(b);
            Dialog.inform(s);
        } catch (IOException ioe) {
        }
    }
}

