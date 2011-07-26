/*
 * TestThread.java
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

package audio.transform;


import java.lang.Runnable;
import java.lang.InterruptedException;

import java.io.OutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.rim.device.api.math.Fixed32;

public class TestThread extends StoppableThread
{
    DataOutputStream    strm;
    boolean         loop;
    
    public TestThread(OutputStream os)
    {
        strm = new DataOutputStream(os);
    }
    
    public void run()
    {
        loop = true;
        
        double theta = 0D;
        /*
        try {
            sleep(10000);
        } catch(InterruptedException e) {
        }*/
        
        while(loop) {
            double x = Math.sin(theta);
            short d = (short)(x * Short.MAX_VALUE);
            theta += Math.PI / 8D;
            try {
                strm.writeByte(d & 0xFF);
                strm.writeByte(d >> 8);
            } catch (IOException e) {
            }
            
            if (theta > Math.PI) {
                theta -= Math.PI * 2D;
            }
        }
    }
    
    public void stop()
    {
        loop = false;
    }
}
