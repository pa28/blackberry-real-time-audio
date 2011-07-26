/*
 * FixedFourier.java
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


import net.rim.device.api.math.Fixed32;


/**
 * 
 */
public class FixedFourier extends Fourier {
    private int[] d;
    
    public FixedFourier() {}
    
    private void swap(int i, int j) {
        int t = d[i];
        d[i] = d[j];
        d[j] = t;
    }
    
    public void setData(int[] data) {
        int iscale = Fixed32.toFP((1<<16)-1);
        int one = Fixed32.toFP(1);
        int n = (data.length-1)/2;
        int n1 = Fixed32.toFP(n) - one;
        int da, dd;
        
        d = new int[data.length];
        for (int i = 1; i < n*2+1; i++) {
            da = data[i];
            int w = Fixed32.mul(Fixed32.HALF, one - Fixed32.Cos(Fixed32.div(Fixed32.mul(Fixed32.TWOPI,Fixed32.toFP(i)),n1)));
            dd = Fixed32.mul(data[i], w);
            d[i] = dd;
        }
    }
    
    /**
     * Takes an array of integers (assuming 16 bit signed audio samples)
     * previously set by a call to setData(int[] data) and computes 
     * the Fourier transform returning an array of Fixed32 values.
     * 
     * @param nn The number of complex values
     * @param reverse if true the inverse FFT is computed.
     * @return 
     */
    public int[] fft(int nn, boolean reverse) {
        int n, mmax, m, j, istep, i;
        
        int wtemp, wr, wpr, wpi, wi, theta;
        
        int tempr, tempi;
        
        int isign = Fixed32.toFP(( reverse ? -1 : 1));
        
        n = nn << 1;
        
        j = 1;
        for (i = 1; i < n; i += 2) {
            if (j > i) {
                swap(j,i);
                swap(j+1,i+1);
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
            theta = Fixed32.div(Fixed32.TWOPI, Fixed32.toFP((reverse ? -mmax : mmax)));
            wtemp = Fixed32.Sin(theta/2);
            wpr = Fixed32.mul(wtemp,wtemp) * -2;
            wpi = Fixed32.Sin(theta);
            wr = Fixed32.toFP(1);
            wi = Fixed32.toFP(0);
            for (m = 1; m < mmax; m += 2) {
                for (i = m; i <= n; i += istep) {
                    j = i + mmax;
                    tempr = Fixed32.mul(wr, d[j]) - Fixed32.mul(wi, d[j+1]);
                    tempi = Fixed32.mul(wr, d[j+1]) - Fixed32.mul(wi, d[j]);
                    d[j] = d[i] - tempr;
                    d[j+1] = d[i+1] - tempi;
                    d[i] += tempr;
                    d[i+1] += tempi;
                }
                wtemp = wr;
                wr = Fixed32.mul(wr, wpr) - Fixed32.mul(wi, wpi) + wr;
                wi = Fixed32.mul(wi, wpr) + Fixed32.mul(wtemp, wpi) + wi;
            }
            mmax = istep;
        }
        return d;
    }
    
    public int[] realfft(int n, boolean reverse) {
        int i, i1, i2, i3, i4, n2p3;
        int c1 = Fixed32.HALF, c2, h1r, h1i, h2r, h2i;
        int wr, wi, wpr, wpi, wtemp, theta;
        
        int isign = Fixed32.toFP(( reverse ? -1 : 1));
        
        theta = Fixed32.div(Fixed32.PI, Fixed32.toFP(n));
        if (!reverse) {
            c2 = - Fixed32.HALF;
            fft(n,reverse);
        } else {
            c2 = Fixed32.HALF;
            theta = - theta;
        }
        
        wtemp = Fixed32.Sin(Fixed32.mul(Fixed32.HALF, theta));
        wpr = Fixed32.mul(Fixed32.toFP(-2), Fixed32.mul(wtemp,wtemp));
        wpi = Fixed32.Sin(theta);
        wr = wpr + Fixed32.toFP(1);
        wi = wpi;
        n2p3 = 2*n+3;
        for (i = 2; i <= n/2; i++) {
            i1 = i + i - 1;
            i2 = 1 + i1;
            i3 = n2p3 - i2;
            i4 = 1 + i3;
            h1r = Fixed32.mul(c1, d[i1] + d[i3]);
            h1i = Fixed32.mul(c1, d[i2] - d[i4]);
            h2r = Fixed32.mul(-c2, d[i2] + d[i4]);
            h2i = Fixed32.mul(c2, d[i1] + d[i3]);
            d[i1] = h1r + Fixed32.mul(wr, h2r) - Fixed32.mul(wi, h2i);
            d[i2] = h1i + Fixed32.mul(wr, h2i) + Fixed32.mul(wi, h2r);
            d[i3] = h1r - Fixed32.mul(wr, h2r) + Fixed32.mul(wi, h2i);
            d[i4] = - h1i + Fixed32.mul(wr, h2i) + Fixed32.mul(wi, h2r);
            wtemp = wr;
            wr = Fixed32.mul(wr, wpr) - Fixed32.mul(wi, wpi) + wr;
            wi = Fixed32.mul(wi, wpr) + Fixed32.mul(wtemp, wpi) + wi;
        }
        if (!reverse) {
            h1r = d[1];
            d[1] = h1r + d[2];
            d[2] = h1r - d[2];
        } else {
            h1r = d[1];
            d[1] = Fixed32.mul(c1,h1r + d[2]);
            d[2] = Fixed32.mul(c1,h1r - d[2]);
            fft(n,reverse);
        }
        return d;
    }
} 
