/*
 * RealTimeAudio.java
 *
 * � H. R. Buckley, 2003-2011
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


/**
 * Simple UiApplication implementation to start the application
 * and push the main screen onto the display stack.
 *
 */
class RealTimeAudio extends UiApplication {
    
    public static void main(String args[]) {
        final RealTimeAudio rta = new RealTimeAudio();
        rta.pushScreen(new RealTimeAudioScreen());
        
        rta.invokeLater(new Runnable() {
            public void run() {
                OpeningDialog.postOpeningDialog(rta);
            }
        });
        
        rta.enterEventDispatcher();
    }
    
    RealTimeAudio() {    }
} 
