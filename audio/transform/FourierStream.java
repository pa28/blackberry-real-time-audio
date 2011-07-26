/*
 * FourierStream.java
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


import java.io.OutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.Thread;

/**
 * 
 */
public class FourierStream extends OutputStream {
    private int n, n2, idx, theNibble, bytesUsed, bytesDiscarded;
    private FourierStreamListener listener;
    private boolean busy, closed, nibble;
    private Fourier fourier;
    private short[] buf;
    private boolean blocking;
    
    class FourierThread extends Thread {
        private Fourier fourier;
        private short[] d;
        private FourierStreamListener listener;
        
        FourierThread(Fourier fourier, FourierStreamListener listener, short[] data, int n) {
            this.fourier = fourier;
            this.listener = listener;
            d = data;
        }
        
        public void run() {
            float[] f = fourier.realFFT(d);
            if (listener != null) {
                listener.transformReady(f);
            }
            busy = false;
        }
    }
    
    public FourierStream() {
        listener = null;
        construct();
        setSize(256);
    }
    
    public FourierStream(int n, FourierStreamListener listener) {
        this.listener = listener;
        construct();
        setSize(n);
    }
    
    private void construct() {
        bytesUsed = 0;
        bytesDiscarded = 0;
        busy = false;
        closed = false;
        fourier = new FloatFourier();
        buf = null;
        blocking = false;
        nibble = false;
    }
    
    public void setBlocking(boolean blocking) { this.blocking = blocking; }
    public void setSize(int n) { this.n = n; n2 = n * 2; }
    //public int getSize() { return n; }
    //public void setListener(FourierStreamListener listener) { this.listener = listener; }
    public int getBytesUsed() { return bytesUsed; }
    public int getBytesDiscarded() { return bytesDiscarded; }
    
    public void clearStats() { bytesUsed = bytesDiscarded = 0; }
    
    public void close() throws IOException {
        flush();
        closed = true;
    }
    
    public void flush() throws IOException {
        if (closed) throw new IOException("FourierStream is closed.");
    }
    
    private boolean checkBusy() {
        if (busy && blocking) {
            while (busy) {
                Thread.yield();
            }
        }
        
        return busy;
    }
    
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }
    
    public void write(byte[] b, int off, int len) throws IOException {
        if (closed) throw new IOException("FourierStream is closed.");
        
        for (int i = off; i < off + len && !busy; i++) {
            assemble(b[i]);
        }
    }

    public void write(int b) throws IOException {
        if (closed) throw new IOException("FourierStream is closed.");
        
        assemble(b);
    }
    
    private void assemble(int b) throws IOException {

        if (nibble) {
            short s = (short)((theNibble & 0xFF) | (b << 8));
            nibble = false;
            if (checkBusy()) {
                bytesDiscarded += 2;
                return;
            }
            if (buf == null || buf.length != n2) {
                buf = new short[n2];
                idx = 0;
            }
            buf[idx] = s;
            idx++;
            if (idx >= n2) {
                busy = true;
                idx = 0;
                FourierThread ft = new FourierThread(fourier, listener, buf, n);
                ft.start();
                bytesUsed += n2 * 2;
            }
        } else {
            nibble = true;
            theNibble = b;
        }
    }
} 
