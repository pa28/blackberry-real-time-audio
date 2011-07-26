/*
 * DisplayManager.java
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

import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.system.Display;


/**
 * A custom Manager to handle layout of the XYFields.
 */
public class DisplayManager extends Manager {
    private int[] x, y, w, h;
    private boolean[] f;
    
    public DisplayManager(long style) {
        super(style);
        x = y = w = h = null;
    }
    
    protected void sublayout(int width, int heigth) {
        int n = getFieldCount();
        
        if (x == null || x.length != n) x = new int[n];
        if (y == null || y.length != n) y = new int[n];
        if (w == null || w.length != n) w = new int[n];
        if (h == null || h.length != n) h = new int[n];
        if (f == null || f.length != n) f = new boolean[n];
        
        int totalCommittedHeight = 0;
        int nUnCommitted = 0;
        
        for (int i = 0; i < n; i++) {
            Field field = getField(i);
            f[i] = (field.getStyle() & Field.USE_ALL_HEIGHT) == Field.USE_ALL_HEIGHT;
            w[i] = width;
            if (f[i]) {
                nUnCommitted ++;
            } else {
                h[i] = field.getPreferredHeight();
                totalCommittedHeight += h[i];
            }
        }
        
        int xp = 0;
        int yp = 0;
        for (int i = 0; i < n; i++) {
            Field field = getField(i);
            if (f[i]) {
                h[i] = (heigth - totalCommittedHeight) / nUnCommitted;
            }
            x[i] = xp;
            y[i] = yp;
            layoutChild(field, w[i], h[i]);
            setPositionChild(field, x[i], y[i]);
            yp += h[i];
        }
        
        setExtent(width, heigth);
    }
    
    protected void subpaint(Graphics g) {
        int n = getFieldCount();
        for (int i = 0; i < n; i++) {
            Field field = getField(i);
            paintChild(g, field);
        }
    }
    
    /*
    public int getPreferedHeight() {
        return Display.getHeight();
    }
    
    public int getPreferedWidth() {
        return Display.getWidth();
    }
    */
} 
