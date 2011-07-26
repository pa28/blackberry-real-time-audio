/*
 * XYField.java
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

package audio.display;

import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.DrawStyle;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.XYRect;
import net.rim.device.api.system.KeypadListener;
import net.rim.device.api.system.Display;
import net.rim.device.api.math.Fixed32;


/**
 * A custome Field to display X-Y data. Data is provided in an array, the X coordinate
 * is derrived from the index, the Y coordinat is the value of the array element.
 */
public class XYField extends Field {
    public final static int
        MODE_NONE = 0,
        MODE_XY = 1,
        MODE_MIN_MAX = 2;
        
    private int mode, width, height, maxx;
    private int[] x, y;
    private float[] f;
    private float miny, rngy;
    private XYFieldListener listener;
    private boolean acceptsFocus, hasFocus;
    private int cursor;
  
    /**
     * Default constructor
     * 
     * No listener, so it will not accept focus.
     */
    public XYField() {
        super(Field.USE_ALL_HEIGHT);
        listener = null;
        acceptsFocus = false;
        construct();
    }
    
    /**
     * Constructor with a specified listener
     * @param listener An XYFieldListener
     * 
     * This listener is called when events, usually as a result of user action, occurr on the field.
     */
    public XYField(XYFieldListener listener) {
        super(Field.USE_ALL_HEIGHT);
        this.listener = listener;
        acceptsFocus = true;
        construct();
    }
    
    /**
     * Code common to all constructors.
     */
    private void construct() {
        width = 0;
        height = 0;
        x = null;
        y = null;
        f = null;
        miny = -5F;
        rngy = 15F;
        mode = MODE_NONE;
        cursor = -1;
        hasFocus = false;
    }
    
    /**
     * Auto calibrate the Y axis of the field to attempt to provide the best display
     * for the data.
     */
    public void autoCalibrate() {
        if (f == null) return;
        
        float maxy = miny = f[0];
        
        for (int i = 1; i < f.length; i++) {
            float d = f[i];
            miny = Math.min(miny, d);
            maxy = Math.max(maxy, d);
        }
        
        if (maxy == miny) {
            miny = miny - 5F;
            rngy = 15F;
        } else {
            rngy = maxy - miny;
        }
    }
    
    /**
     * Set the data to be plotted in the field.
     * 
     * @param y An array with the data.
     */
    public void setFunction(float[] y) {
        f = y;
        maxx = f.length;
        
        if (width == 0) {
            width = getWidth();
        }
        
        if (height == 0) {
            height = getHeight();
        }
        
        calibrate();
    }
    
    /**
     * Normalize a value on the Y axis
     * 
     * @param f The 'real' Y value to plot
     * @return The 'screen' Y value to plot
     */
    private int normalizeY(float f) {
        int n = height - (int)(((f - miny) / rngy) * (float)height);
        return n;
    }
    
    /**
     * Normalize a value on the X axis
     * 
     * @param x the 'real' X value to plot
     * @return The 'screen' X value to plot
     */
    private int normalizeX(int x) {
        return Fixed32.toRoundedInt(Fixed32.mul( Fixed32.div( Fixed32.toFP(width), Fixed32.toFP(maxx) ), Fixed32.toFP(x)));
    }
    
    /**
     * Calibrate the field to best display the data.
     */
    private void calibrate() {
        if (f == null) return;
        
        int rangeError = 0;
        
        /**
         * If the number of values in the data array is more than twice the number of pixels
         * in the horizontal dimension of the field, divide the data in to bins and draw a 
         * vertical line from the maximum to the minimum value in that bin.
         */
        if (f.length > width*2) {
            mode = MODE_MIN_MAX;
            int wFP = Fixed32.toFP(width);
            int s = Fixed32.div( wFP, Fixed32.toFP(f.length));
            if (x == null || x.length != width) x = new int[width];
            if (y == null || y.length != width) y = new int[width];
            int j = 0;
            int idx = 0;
            int idx0 = -1;
            for (int i = 0; i < wFP; i += s, j++) {
                int n = normalizeY(f[j]);
                
                if (n < 0 || n > height) {
                    rangeError++;
                    if (rangeError > width / 10) {
                        autoCalibrate();
                        i = j = idx = idx0 = 0;
                    }
                }
                
                idx = Fixed32.toInt(i);
                if (idx != idx0) {
                    x[idx] = y[idx] = n;
                    idx0 = idx;
                } else {
                    x[idx] = Math.max(x[idx], n);
                    y[idx] = Math.min(y[idx], n);
                }
            }
        /**
         * Otherwise just plot the data as a set of X-Y point joined by lines
         */
        } else {
            mode = MODE_XY;
            if (x == null || x.length != width) x = new int[f.length];
            if (y == null || y.length != width) y = new int[f.length];
            for (int i = 0; i < f.length; i++) {
                int n = normalizeY(f[i]);

                if (n < 0 || n > height) {
                    rangeError++;
                    if (rangeError > width / 10) {
                        autoCalibrate();
                        i = 0;
                    }
                }

                y[i] = n;
                x[i] = normalizeX(i);
            }
        }
        
        /**
         * Invalidate the field to trigger a re-draw
         */
        synchronized(getScreen().getApplication().getAppEventLock()) {
            invalidate();
        }
    }
    
    public int getPreferedHeight() {
        return Display.getHeight();
    }
    
    public int getPreferedWidth() {
        return Display.getWidth();
    }
    
    protected void layout(int width, int height) {
        int dw = this.getPreferedWidth();
        int dh = this.getPreferedHeight();
        int w = Math.min(width, dw );
        int h = Math.min(height, dh );
        setExtent(w, h);
        if (cursor == -1) cursor = w/2;
    }
    
    protected void paint(Graphics g) {
        int c = g.getColor();
        
        if (hasFocus) {
            g.setBackgroundColor(0x000000);
            g.setColor(0x00c000);
        } else {
            g.setColor(0x000000);
        }
        
        g.clear();
        if (mode == MODE_MIN_MAX) {
            for (int i = x.length - 1; i >= 0; i--) {
                g.drawLine(i, x[i], i, y[i]);
            }
        } else if (mode == MODE_XY) {
            for (int i = x.length - 2; i >= 0; i--) {
                g.drawLine(x[i+1], y[i+1], x[i], y[i]);
            }
        }
        
        if (cursor > 0 && cursor < width) {
            if (hasFocus) {
                g.setColor(0xC00000);
            } else {
                g.setColor(0x000000);
            }
            g.drawLine(cursor, 0, cursor, height);
        }
        g.setColor(c);
    }
    
    /**
     * Handle key events that occure when this field has focus
     * 
     * @param c The key character
     * @param status The modifier key status
     * @param time The time the event happend
     * @return true if this method consumes the event, false otherwise
     */
    protected boolean keyChar(char c, int status, int time) {
        switch (c) {
            case 'a':   // Auto calibrate the field
                autoCalibrate();
                invalidate();
                return true;
            case 'R':   // Double the Y range
                rngy *= 2F;
                invalidate();
                return true;
            case 'r':   // Halve the Y range
                rngy /= 2F;
                invalidate();
                return true;
            case 'M':   // Increase the minimum displayed Y value by the Y axis range
                miny += rngy / 10F;
                invalidate();
                return true;
            case 'm':   // Decrease the minimum displayed Y value by the Y axis range
                miny -= rngy / 10F;
                invalidate();
                return true;
        }
        return super.keyChar(c, status, time);
    }

    
    public boolean isFocusable() {
        return acceptsFocus;
    }
    
    protected void onFocus(int direction) {
        hasFocus = true;
        invalidate();
    }
    
    protected void onUnfocus() {
        hasFocus = false;
        invalidate();
    }
    
    public void setCursor(float newCursor) {
        int c = (int)(width * newCursor + 0.5);
        if (c != cursor) setCursor(c);
    }
    
    public void setCursor(int newCursor) {
        cursor = newCursor;
        cursor = Math.max(cursor, 0);
        cursor = Math.min(cursor, width);
        invalidate();
    }
    
    /**
     * Update the cursor position and notify the listener if set.
     * 
     * @param dx The amout to move the cursor
     */
    private void updateCursor(int dx) {
        setCursor(cursor + dx);
        if (listener != null) listener.cursorValue(this, cursor, (float)cursor/(float)width);
        invalidate();
    }
    
    /**
     * Translate navigation movements of the Trackwheel, Trackball or Trackpad into cursor movement
     * 
     * @param dx Horizontal movement
     * @param dy Vertical movement
     * @param status Modifier key status
     * @param time Time of the event
     * @return true if this method consumes the event, false otherwise
     */
    protected boolean navigationMovement(int dx, int dy, int status, int time) {
        if ((status & KeypadListener.STATUS_TRACKWHEEL) == KeypadListener.STATUS_TRACKWHEEL) {
            if ((status & KeypadListener.STATUS_ALT) == KeypadListener.STATUS_ALT) {
                updateCursor(dx);
                return true;
            }
        } else if ((status & KeypadListener.STATUS_FOUR_WAY) == KeypadListener.STATUS_FOUR_WAY ) {
            if (dx != 0) {
                updateCursor(dx);
                return true;
            }
        }
        return super.navigationMovement(dx, dy, status, time);
    }
} 
