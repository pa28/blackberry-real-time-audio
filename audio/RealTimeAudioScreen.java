/*
 * RealTimeAudioScreen.java
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

import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.system.USBPortListener;

import net.rim.device.api.system.DeviceInfo;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.container.MainScreen;
import net.rim.device.api.ui.component.LabelField;
import audio.transform.FourierStream;
import audio.transform.StoppableThread;
import audio.transform.AudioThread;
import audio.transform.TestThread;
import audio.transform.FourierStreamListener;
import audio.display.XYField;
import audio.display.DisplayManager;
import audio.display.XYFieldListener;

import net.rim.device.api.media.control.AudioPathControl;
import net.rim.device.api.math.Fixed32;
import net.rim.device.api.util.MathUtilities;

import javax.microedition.media.*;
import javax.microedition.media.control.*;

/*
**
*   Main screen for RealTimeAudio.
*/

public class RealTimeAudioScreen extends MainScreen implements FourierStreamListener, XYFieldListener, USBPortListener {
    private StoppableThread audioThread;
    private FourierStream stream;
    private XYField xyField, expField;
    private boolean log;
    private LabelField status, expLabel;
    private long lastStatusUpdate;
    private int n, expN;
    private float[] real, exp;
    private DisplayManager displayManager;
    
    /**
     * Create the screen.
     */
    public RealTimeAudioScreen() {
        super(Manager.NO_VERTICAL_SCROLL);
        setTitle("Real Time Audio");
        status = new LabelField("Status:");
        setStatus(status);
        add(displayManager = new DisplayManager(Field.USE_ALL_HEIGHT));
        
        // Add an XYField to display the exponential average of the spectrum
        displayManager.add(expField = new XYField(this));
        
        // Add the label field
        displayManager.add(expLabel = new LabelField(""));
        
        // Add an XYField to display the real time spectrum
        displayManager.add(xyField = new XYField(this));
        
        stream = null;
        n = 128;
        expN = 8;
        real = null;
        exp = null;
        audioThread = null;
        status.setText(statusText());
        expLabel.setText(expText());
        
        UiApplication.getApplication().addIOPortListener(this);
    }
    
    // USBPortListener
    
    public void connectionRequested() {
        System.out.println("USB connectionRequested()");
    }
    
    public void dataNotSent() {
        System.out.println("USB dataNotSent()");
    }
    
    public int getChannel() {
        System.out.println("USB getChannel()");
        return -1;
    }
    
    // IOPortListener
    
    public void connected() {
        System.out.println("USB connected()");
    }
    
    public void dataReceived(int length) {
        System.out.println("USB dataReceived()");
    }
    
    public void dataSent() {
        System.out.println("USB dataSent()");
    }
    
    public void disconnected() {
        System.out.println("USB disconnected()");
    }
    
    public void patternReceived(byte [] pattern) {
        System.out.println("USB patternReceived()");
    }
    
    public void receiveError(int error) {
        System.out.println("USB receiveError()");
    }
    
    // FourierStreamListener
    
    /**
     * Called when the Fourier transform completes and a buffer full of data is ready for display
     * 
     * @param data an array holding the complex spectrum result of the FFT
     */
    public void transformReady(float[] data) {
        //audioThread.stop();
        
        /**
         * Ensure the buffers exist and are the correct size
         */
        if (real == null || real.length != (data.length/2)) real = new float[data.length/2];
        if (exp == null || exp.length != (data.length/2)) {
            exp = new float[data.length/2];
            clearExpField();
        }

        double e2 = 1D / (double)expN;
        double e1 = (double)(expN - 1) * e2;

        /**
         * Convert the I and Q values in the buffer to amplitude values (discarding phase information)
         */
        for (int i = 0; i < data.length-1; i+=2) {
            double d = Math.sqrt(data[i] * data[i] + data[i+1] * data[i+1]);
            if (log) d = MathUtilities.log(d);
            real[i/2] = (float)d;
            exp[i/2] = (float)(exp[i/2] * e1 + d * e2);
        }
        
        /**
         * Apply the data to the appropriate fields.
         */
        xyField.setFunction(real);
        expField.setFunction(exp);
        
        /**
         * Update the status display once per second or once per FFT frame which ever is later.
         */
        if (System.currentTimeMillis() - lastStatusUpdate > 1000) {
            status();
        }
    }
    
    // XYFieldListener
    
    /**
     * Listen for cursor events on the XYFields
     * 
     * @param field The field on which the event occured
     * @param x The x coordinate of the cursor event
     * @param f The frequency corresponding to x
     */
    public void cursorValue(XYField field, int x, float f) {
        // Update the appropriate field
        if (field == xyField) {
            expField.setCursor(x);
        } else {
            xyField.setCursor(x);
        }
        
        // Update the frequency display
        expLabel.setText(expText((int)(4000F * f)));
    }
    
    /**
     * The screen is closing
     */
    public void close() {
        if (audioThread != null) audioThread.stop();
        super.close();
    }
    
    /**
     * Clear the exponential average field display
     */
    private void clearExpField() {
        for (int i = exp.length-1; i >= 0; i--) exp[i] = 0F;
    }
    
    /**
     * Stop audio reception and processing.
     */
    public void stop() {
        if (audioThread != null) {
            audioThread.stop();
            audioThread = null;
            
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {
                }
                stream = null;
            }
        }
    }
    
    public String expText() {
        StringBuffer sb = new StringBuffer("Exp: ");
        sb.append(expN);
        sb.append(" ");
        return sb.toString();
    }
    
    public String expText(int freq) {
        return expText() + "Freq: " + Integer.toString(freq) + " Hz";
    }
    
    public String statusText() {
        lastStatusUpdate = System.currentTimeMillis();
        StringBuffer sb = new StringBuffer("Status: ");
        if (audioThread == null) {
            sb.append("Stopped ");
        } else {
            sb.append("Running ");
        }
        
        if (log) {
            sb.append("LOG ");
        } else {
            sb.append("LIN ");
        }
        
        sb.append(n).append(" ");
        
        if (stream != null) {
            int totalBytes = stream.getBytesDiscarded() + stream.getBytesUsed();
            if (totalBytes > 0) {
                sb.append((stream.getBytesUsed() * 100) / totalBytes).append("% ");
            }
        }
        
        return sb.toString();
    }
    
    public void status() {
        synchronized( getScreen().getApplication().getAppEventLock()) {
            status.setText(statusText());
        }
    }
    
    /**
     * Process key events for the screen
     * 
     * @param c The key character
     * @param status Status of modifier keys
     * @param time When the event happend
     * @return true if this method consumed the event, false otherwise
     */
    protected boolean keyChar(char c, int status, int time) {
        switch (c) {
            case 's':   // Start processing (test processing in the simulator)
                stop();
                stream = new FourierStream(n, this);
                if (DeviceInfo.isSimulator()) {
                    audioThread = new TestThread(stream);
                } else {
                    audioThread = new AudioThread(stream);
                }
                stream.setBlocking(false);
                stream.clearStats();
                audioThread.start();
                return true;
            case 't':   // Start test processing
                stop();
                stream = new FourierStream(n, this);
                audioThread = new TestThread(stream);
                stream.setBlocking(true);
                stream.clearStats();
                audioThread.start();
                return true;
            case 'q':   // Stop processing
                stop();
                status();
                return true;
            case 'l':   // Toggle between logrythmic and linear processing
                log = !log;
                status();
                return true;
            case 'n':   // Reduce the number of samples used in the FFT, minimum 32
                if (n > 32) {
                    stop();
                    n >>= 1;
                    status();
                }
                return true;
            case 'N':   // Increase the number of samples used in the FFT, maximum 2048
                if (n < 2048) {
                    stop();
                    n <<= 1;
                    status();
                }
                return true;
            case 'x':   // Reduce the numer of frames averaged in the exponential display
                if (expN > 1) {
                    expN >>= 1;
                    clearExpField();
                    expLabel.setText(expText());
                }
                return true;
            case 'X':   // Increase the number of frames averaged in the exponential display
                if (expN < 32) {
                    expN <<= 1; 
                    clearExpField();
                    expLabel.setText(expText());
                }
                return true;
            case 'p':   // Locate the peak signal in the exponential display
                if (exp != null) {
                    int idx = 0;
                    float p = exp[0];
                    for (int i = exp.length - 1; i > 0; i--) {
                        if (exp[i] > p) {
                            p = exp[i];
                            idx = i;
                        }
                    }
                    p = (float)idx / (float)exp.length;
                    expField.setCursor(p);
                    xyField.setCursor(p);
                    expLabel.setText(expText((int)(4000F * p)));
                }
                return true;
        }
        
        return super.keyChar(c, status, time);
    }
} 
