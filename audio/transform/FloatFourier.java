/*
 * FloatFourier.java
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

import java.lang.Math;
import net.rim.device.api.util.MathUtilities;


/**
 * 
 */
public class FloatFourier extends Fourier {
    private int N;
    private float d[];
    private float r[];
   
    
    FloatFourier() {
        d = null;
        r = null;
    }
    
    public float[] realFFT(short[] data) {
        N = data.length;
        if (d == null || d.length != data.length+1) d = new float[data.length+1];
        
        for (int i = data.length - 1; i >= 0; i--)
            d[i+1] = ((float)data[i] / (float)Short.MAX_VALUE) * (float)hanning(i);
        
        doRealFFT(1);

        if (r == null || r.length != data.length) r = new float[N];
        
        for (int i = 0; i < N; i++) {
            r[i] = d[i+1];
        }
        
        return r;
    }
    
    /*
    public void realFFT(float[] data) {
        N = data.length;
        if (d == null || d.length != data.length+1) d = new float[data.length+1];
        
        // Apply window;
        for (int i = 0; i < N; i++) {
            d[i+1] = (float)(data[i] * hanning(i));
        }

        doRealFFT(1);
        
        for (int i = 0; i < N; i++) {
            data[i] = d[i+1];
        }
    }
    */
    
    private void doRealFFT(int isign) {
        int i, i1, i2, i3, i4, n2p3, n;
        float c1 = 0.5F, c2, h1r, h1i, h2r, h2i;
        double wr, wi, wpr, wpi, wtemp, theta;
        
        n = N/2;
        theta = Math.PI / ((double)n);
        if (isign == 1) {
            c2 = -0.5F;
            doComplexFFT(isign);
        } else {
            c2 = 0.5F;
            theta = -theta;
        }
        
        wtemp = Math.sin(0.5D * theta);
        wpr = -2D * wtemp * wtemp;
        wpi = Math.sin(theta);
        wr = 1D + wpr;
        wi = wpi;
        n2p3 = 2 * n + 3;
        for (i = 2; i <= n/2; i++) {
            i4 = 1 + (i3 = n2p3 - ( i2 = 1 + (i1 = i + i - 1)));
            h1r = c1 * (d[i1] + d[i3]);
            h1i = c1 * (d[i2] - d[i4]);
            h2r = -c2 * (d[i2] + d[i4]);
            h2i = c2 * (d[i1] - d[i3]);
            d[i1] = (float)(h1r + wr * h2r - wi * h2i);
            d[i2] = (float)(h1i + wr * h2i + wi * h2r);
            d[i3] = (float)(h1r - wr * h2r + wi * h2i);
            d[i4] = (float)(-h1i + wr * h2i + wi * h2r);
            wr = (wtemp = wr) * wpr - wi * wpi + wr;
            wi = wi * wpr + wtemp * wpi + wi;
        }
        
        if (isign == 1) {
            d[1] = (h1r = d[1]) + d[2];
            d[2] = h1r-d[2];
        } else {
            d[1] = c1 * ((h1r = d[1]) + d[2]);
            d[2] = c1 * (h1r - d[2]);
            doComplexFFT(isign);
        }
    }
    
    private void doComplexFFT(int isign) {
        int n, nn, mmax, m, j, istep, i;
        double wtemp, wr, wpr, wpi, wi, theta;
        double tempr, tempi;

        nn = (d.length-1)/2;
        n = nn << 1;
        j = 1;
        for (i = 1; i < n; i += 2) {
            if (j > i) {
                tempr = d[j];
                d[j] = d[i];
                d[i] = (float)tempr;
                
                tempr = d[j+1];
                d[j+1] = d[i+1];
                d[i+1] = (float)tempr;
            }
            m = n >> 1;
            while (m >= 2 && j > m) {
                j -= m;
                m >>= 1;
            }
            j += m;
        }
        
        mmax = 2;
        while (n > mmax) {
            istep = 2 * mmax;
            theta = (Math.PI * 2D) / (isign * mmax);
            wtemp = Math.sin(0.5D * theta);
            wpr = -2D * wtemp * wtemp;
            wpi = Math.sin(theta);
            wr = 1D;
            wi = 0D;
            for (m = 1; m < mmax; m += 2) {
                for (i = m; i <= n; i+= istep) {
                    j = i + mmax;
                    tempr = wr * d[j] - wi * d[j+1];
                    tempi = wr * d[j+1] + wi * d[j];
                    d[j] = d[i] - (float)tempr;
                    d[j+1] = d[i+1] - (float)tempi;
                    d[i] += tempr;
                    d[i+1] += tempi;
                }
                wr = (wtemp = wr) * wpr - wi * wpi + wr;
                wi = wi * wpr + wtemp * wpi + wi;
            }
            mmax = istep;
        }
    }
    
    private double hanning(int j) {
        return 0.5D * (1D - Math.cos((Math.PI * j)/ ((double)N - 1D)));
    }
} 
