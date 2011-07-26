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
 * 
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
  
    public XYField() {
        super(Field.USE_ALL_HEIGHT);
        listener = null;
        acceptsFocus = false;
        construct();
    }
    
    public XYField(XYFieldListener listener) {
        super(Field.USE_ALL_HEIGHT);
        this.listener = listener;
        acceptsFocus = true;
        construct();
    }
    
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
    
    private int normalizeY(float f) {
        int n = height - (int)(((f - miny) / rngy) * (float)height);
        return n;
    }
    
    private int normalizeX(int x) {
        return Fixed32.toRoundedInt(Fixed32.mul( Fixed32.div( Fixed32.toFP(width), Fixed32.toFP(maxx) ), Fixed32.toFP(x)));
    }
    
    private void calibrate() {
        if (f == null) return;
        
        int rangeError = 0;
        
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
    
    protected boolean keyChar(char c, int status, int time) {
        switch (c) {
            case 'a':
                autoCalibrate();
                invalidate();
                return true;
            case 'R':
                rngy *= 2F;
                invalidate();
                return true;
            case 'r':
                rngy /= 2F;
                invalidate();
                return true;
            case 'M':
                miny += rngy / 10F;
                invalidate();
                return true;
            case 'm':
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
    
    private void updateCursor(int dx) {
        setCursor(cursor + dx);
        if (listener != null) listener.cursorValue(this, cursor, (float)cursor/(float)width);
        invalidate();
    }
    
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
