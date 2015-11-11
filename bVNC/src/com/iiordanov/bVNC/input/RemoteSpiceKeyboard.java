/**
 * Copyright (C) 2013- Iordan Iordanov
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
 * USA.
 */


package com.iiordanov.bVNC.input;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import android.content.res.Resources;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.iiordanov.bVNC.Constants;
import com.iiordanov.bVNC.MetaKeyBean;
import com.iiordanov.bVNC.RemoteCanvas;
import com.iiordanov.bVNC.SpiceCommunicator;

public class RemoteSpiceKeyboard extends RemoteKeyboard {
	private final static String TAG = "RemoteSpiceKeyboard";
	private HashMap<Integer, Integer[]> table;
    final static int SCANCODE_SHIFT_MASK = 0x10000;
    final static int SCANCODE_ALTGR_MASK = 0x20000;
    final static int SCANCODE_CIRCUMFLEX_MASK = 0x40000;
    final static int SCANCODE_DIAERESIS_MASK = 0x80000;
    final static int UNICODE_MASK = 0x100000;
    final static int UNICODE_META_MASK = KeyEvent.META_CTRL_MASK|KeyEvent.META_META_MASK|KeyEvent.META_CAPS_LOCK_ON;

	public RemoteSpiceKeyboard (Resources resources, SpiceCommunicator r, RemoteCanvas v, Handler h, String layoutMapFile) throws IOException {
	    super(r, v, h);
	    context = v.getContext();
	    this.table = loadKeyMap(resources, "layouts/" + layoutMapFile);
	}

    private HashMap<Integer,Integer[]>loadKeyMap(Resources r, String file) throws IOException {
        InputStream is;
        try {
            is = r.getAssets().open(file);
        } catch (IOException e) {
            // If layout map file was not found, load the default one.
            is = r.getAssets().open("layouts/" + Constants.DEFAULT_LAYOUT_MAP);
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        String line = in.readLine();
        HashMap<Integer,Integer[]> table = new HashMap<Integer,Integer[]> (500);
        while (line != null) {
            //android.util.Log.i (TAG, "Layout " + file + " " + line);
            String[] tokens = line.split(" ");
            Integer[] scanCodes = new Integer[tokens.length-1];
            for (int i = 1; i < tokens.length; i++) {
                scanCodes[i - 1] = Integer.parseInt(tokens[i]);
            }
            table.put(Integer.parseInt(tokens[0]), scanCodes);
            line = in.readLine();
        }
        return table;
    }

	/**
	 * Sets the hardwareMetaState based on certain keys and scancodes being detected.
	 * @param keyCode
	 * @param event
	 * @param down
	 */
	private void setHardwareMetaState (int keyCode, KeyEvent event, boolean down) {
		int metaMask = 0;
		switch (event.getScanCode()) {
		case SCAN_LEFTCTRL:
		case SCAN_RIGHTCTRL:
			metaMask |= CTRL_MASK;
			break;
		}
		
		switch(keyCode) {
		case KeyEvent.KEYCODE_DPAD_CENTER:
			metaMask |= CTRL_MASK;
			break;
		// TODO: We leave KeyEvent.KEYCODE_ALT_LEFT for symbol input on hardware keyboards for now.
		case KeyEvent.KEYCODE_ALT_RIGHT:
			metaMask |= ALT_MASK;
			break;
		}
		
		if (!down) {
			hardwareMetaState &= ~metaMask;
		} else {
			hardwareMetaState |= metaMask;
		}
	}

	public boolean processLocalKeyEvent(int keyCode, KeyEvent event, int additionalMetaState) {
		return keyEvent(keyCode, event, additionalMetaState);
	}

	public boolean keyEvent(int keyCode, KeyEvent event, int additionalMetaState) {
        android.util.Log.e(TAG, event.toString());
        int action = event.getAction();
        boolean down = (action == KeyEvent.ACTION_DOWN);
        // Combine current event meta state with any meta state passed in.
        int metaState = additionalMetaState | convertEventMetaState (event, event.getMetaState());
        
        /* TODO: Consider whether this is a good idea. At least some scan codes between
           my bluetooth keyboard and what the VM expects do not match. For example, d-pad does not send arrows.
        // If the event has a scan code, just send that along!
        if (event.getScanCode() != 0) {
            android.util.Log.e(TAG, "Event has a scancode, sending that: " + event.getScanCode());
            spicecomm.writeKeyEvent(event.getScanCode(), 0, down);
            return true;
        }*/
        
	    // Drop some meta key events which may be used to produce unicode characters.
	    if (down &&
	        (keyCode == KeyEvent.KEYCODE_ALT_LEFT || keyCode == KeyEvent.KEYCODE_ALT_RIGHT ||
	         keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT)) {
            lastDownMetaState = metaState;
	        return true;
	    }
	    
		// Set the hardware meta state from any special keys pressed.
		setHardwareMetaState (keyCode, event, down);
		
		// Ignore menu key and handle other hardware buttons here.
		if (keyCode == KeyEvent.KEYCODE_MENU ||
			canvas.getPointer().handleHardwareButtons(keyCode,
															 event,
															 metaState|onScreenMetaState|hardwareMetaState)) {
		} else if (rfb != null && rfb.isInNormalProtocol()) {
			// Combine metaState
			metaState = onScreenMetaState|hardwareMetaState|metaState;
			
			// If the event consists of multiple unicode characters, send them one by one.
			if (action == KeyEvent.ACTION_MULTIPLE) {
				String s = event.getCharacters();
				if (s != null) {
                    int numchars = s.length();
                    int i = numJunkCharactersToSkip (numchars, event);
					for (; i < numchars; i++) {
						android.util.Log.e(TAG, "Trying to convert unicode to KeyEvent: " + (int)s.charAt(i));
						if (!sendUnicode (s.charAt(i), additionalMetaState)) {
						    writeKeyEvent(true, (int)s.charAt(i), metaState, true, true);
						}
					}
				}
			} else {
                // Get unicode character that would result from this event, masking out Ctrl and Super keys.
                int unicode = event.getUnicodeChar(event.getMetaState()&~UNICODE_META_MASK);
                Integer[] scanCodes = null;
                int unicodeMetaState = 0;
                
                if (unicode > 0) {
                    // We managed to get a unicode value, if Alt is present in the metaState, check whether scan codes can be determined
                    // for this unicode character with this keymap
                    if ((event.getMetaState() & KeyEvent.META_ALT_MASK) != 0) {
                        scanCodes = table.get(unicode |= UNICODE_MASK);
                    }
                    
                    // If scan codes cannot be determined, try to get a unicode value without Alt and send Alt
                    // as meta-state.
                    if (scanCodes == null || scanCodes.length == 0) {
                        unicode = -1;
                    } else {
                        // We managed to get a unicode value with ALT potentially enabled, and valid scancodes.
                        // So convert and send that over without sending ALT as meta-state.
                        unicodeMetaState = additionalMetaState|onScreenMetaState|hardwareMetaState|
                                       convertEventMetaState(event, event.getMetaState()&~KeyEvent.META_SHIFT_MASK&~KeyEvent.META_ALT_MASK);
                    }
                }
                
                if (unicode <= 0) {
                    // Try to get a unicode value without ALT mask and if successful do not mask ALT out of the meta-state.
                    unicode = event.getUnicodeChar(event.getMetaState()&~UNICODE_META_MASK&~KeyEvent.META_ALT_MASK);
                    unicodeMetaState = additionalMetaState|onScreenMetaState|hardwareMetaState|
                                       convertEventMetaState(event, event.getMetaState()&~KeyEvent.META_SHIFT_MASK);
                }
                
                if (unicode > 0) {
                    android.util.Log.e(TAG, "Got unicode value from event: " + unicode);
                    writeKeyEvent(true, unicode, unicodeMetaState, down, false);
                } else {
                    // We were unable to obtain a unicode, or the list of scancodes was empty, so we have to try converting a keyCode.
                    android.util.Log.e(TAG, "Could not get unicode or determine scancodes for event. Keycode: " + event.getKeyCode());
                    writeKeyEvent(false, event.getKeyCode(), metaState, down, false);
                }
			}
		}
		return true;
	}
	
	private void writeKeyEvent(boolean isUnicode, int code, int metaState, boolean down, boolean sendUpEvents) {
        if (down) {
            lastDownMetaState = metaState;
        } else {
            lastDownMetaState = 0;
        }
        
	    if (isUnicode) {
	        code |= UNICODE_MASK;
	    }
        android.util.Log.e(TAG, "Trying to convert keycode or masked unicode: " + code);
        Integer[] scanCode = null;
        try {
            scanCode = table.get(code);
            
            for (int i = 0; i < scanCode.length; i++) {
                int scode = scanCode[i];
                int meta = metaState;
                android.util.Log.e(TAG, "Got back possibly masked scanCode: " + scode);
                if ((scode & SCANCODE_SHIFT_MASK) != 0) {
                    android.util.Log.e(TAG, "Found Shift mask.");
                    meta |= SHIFT_MASK;
                    scode &= ~SCANCODE_SHIFT_MASK;
                }
                if ((scode & SCANCODE_ALTGR_MASK) != 0) {
                    android.util.Log.e(TAG, "Found AltGr mask.");
                    meta |= ALTGR_MASK;
                    scode &= ~SCANCODE_ALTGR_MASK;
                }
                android.util.Log.e(TAG, "Will send scanCode: " + scode + " with meta: " + meta);
                rfb.writeKeyEvent(scode, meta, down);
                if (sendUpEvents) {
                    rfb.writeKeyEvent(scode, meta, false);
                    android.util.Log.i(TAG, "UNsetting lastDownMetaState");
                    lastDownMetaState = 0;
                }
            }
        } catch (NullPointerException e) {}
	}
	
    public void sendMetaKey(MetaKeyBean meta) {
        RemotePointer pointer = canvas.getPointer();
        int x = pointer.getX();
        int y = pointer.getY();
        
        if (meta.isMouseClick()) {
            //android.util.Log.e("RemoteRdpKeyboard", "is a mouse click");
            int button = meta.getMouseButtons();
            switch (button) {
            case RemoteVncPointer.MOUSE_BUTTON_LEFT:
                pointer.processPointerEvent(x, y, MotionEvent.ACTION_DOWN, meta.getMetaFlags()|onScreenMetaState|hardwareMetaState,
                        true, false, false, false, 0);
                break;
            case RemoteVncPointer.MOUSE_BUTTON_RIGHT:
                pointer.processPointerEvent(x, y, MotionEvent.ACTION_DOWN, meta.getMetaFlags()|onScreenMetaState|hardwareMetaState,
                        true, true, false, false, 0);
                break;
            case RemoteVncPointer.MOUSE_BUTTON_MIDDLE:
                pointer.processPointerEvent(x, y, MotionEvent.ACTION_DOWN, meta.getMetaFlags()|onScreenMetaState|hardwareMetaState,
                        true, false, true, false, 0);
                break;
            case RemoteVncPointer.MOUSE_BUTTON_SCROLL_UP:
                pointer.processPointerEvent(x, y, MotionEvent.ACTION_MOVE, meta.getMetaFlags()|onScreenMetaState|hardwareMetaState,
                        true, false, false, true, 0);
                break;
            case RemoteVncPointer.MOUSE_BUTTON_SCROLL_DOWN:
                pointer.processPointerEvent(x, y, MotionEvent.ACTION_MOVE, meta.getMetaFlags()|onScreenMetaState|hardwareMetaState,
                        true, false, false, true, 1);
                break;
            }
            try { Thread.sleep(50); } catch (InterruptedException e) {}
            pointer.processPointerEvent(x, y, MotionEvent.ACTION_UP, meta.getMetaFlags()|onScreenMetaState|hardwareMetaState,
                    false, false, false, false, 0);

            //rfb.writePointerEvent(x, y, meta.getMetaFlags()|onScreenMetaState|hardwareMetaState, button);
            //rfb.writePointerEvent(x, y, meta.getMetaFlags()|onScreenMetaState|hardwareMetaState, 0);
        } else if (meta.equals(MetaKeyBean.keyCtrlAltDel)) {
            rfb.writeKeyEvent(RemoteKeyboard.SCAN_DELETE, RemoteKeyboard.CTRL_MASK|RemoteKeyboard.ALT_MASK, true);
            rfb.writeKeyEvent(RemoteKeyboard.SCAN_DELETE, RemoteKeyboard.CTRL_MASK|RemoteKeyboard.ALT_MASK, false);
        } else {
            sendKeySym (meta.getKeySym(), meta.getMetaFlags());
        }
    }
}
