/*
 * AudioThread.java
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.util.Timer;
import java.util.TimerTask;

import net.rim.device.api.system.EventLogger;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.ContextMenu;
import net.rim.device.api.ui.container.MainScreen;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.media.control.AudioPathControl;

import javax.microedition.media.*;
import javax.microedition.media.control.*;
import javax.microedition.io.Connector;


public class AudioThread extends StoppableThread
{
    public final static String[] PATH_NAME = { "Bluetooth", "Handset", "Handsfree", "Headset", "Headset Handsfree", "Unknown" };
    
    Player          player;
    RecordControl   rCtl;
    AudioPathControl    apc;
    OutputStream    strm;
    int             oldPath, path;
    
    public AudioThread(OutputStream os)
    {
        strm = os;
    }
    
    //public int getOldPath() { return oldPath; }
    //public int getNewPath() { return path; }
    
    /*
    public String getPathName(int path)
    {
        switch(path)
        {
            case AudioPathControl.AUDIO_PATH_BLUETOOTH:
            return PATH_NAME[0];
            case AudioPathControl.AUDIO_PATH_HANDSET:
            return PATH_NAME[1];
            case AudioPathControl.AUDIO_PATH_HANDSFREE:
            return PATH_NAME[2];
            case AudioPathControl.AUDIO_PATH_HEADSET:
            return PATH_NAME[3];
            case AudioPathControl.AUDIO_PATH_HEADSET_HANDSFREE:
            return PATH_NAME[4];
            default:
            return PATH_NAME[5];
        }
    }
    */
    
    public void run()
    {
        try
        {
            player = Manager.createPlayer("capture://audio?encoding=audio/basic");
            player.realize();
            
            rCtl = (RecordControl)player.getControl("RecordControl");
            
            Control[] c = player.getControls();
            for (int i = c.length-1; i >= 0; i--)
            {
                if (c[i] instanceof AudioPathControl)
                {
                    apc = (AudioPathControl)c[i];
            
                    oldPath = apc.getAudioPath();
                    apc.setAudioPath(AudioPathControl.AUDIO_PATH_HANDSFREE);
                    path = apc.getAudioPath();

                    break;
                }
            }
            
            rCtl.setRecordStream(strm);
            rCtl.startRecord();
            
            player.start();
        }
        
        catch (Exception e)
        {
            final String msg = e.toString();
            UiApplication.getUiApplication().invokeAndWait(new Runnable()
            {
                public void run()
                {
                    Dialog.inform(msg);
                }
            });
        }
    }
    
    public void stop()
    {
        try
        {
            apc.setAudioPath(oldPath);
        
            player.close();
            rCtl.commit();
        }
        catch (Exception e)
        {
            final String msg = e.toString();
            UiApplication.getUiApplication().invokeAndWait(new Runnable()
            {
                public void run()
                {
                    Dialog.inform(msg);
                }
            });
        }
    }
}
