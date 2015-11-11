package com.iiordanov.bVNC.input;

import android.os.Handler;
import android.os.SystemClock;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.iiordanov.bVNC.MetaKeyBean;
import com.iiordanov.bVNC.RdpCommunicator;
import com.iiordanov.bVNC.RfbConnectable;
import com.iiordanov.bVNC.RemoteCanvas;
import com.iiordanov.tigervnc.rfb.UnicodeToKeysym;

public class RemoteRdpKeyboard extends RemoteKeyboard {
    private final static String TAG = "RemoteRdpKeyboard";
    protected RdpKeyboardMapper keyboardMapper;
        
    public RemoteRdpKeyboard (RfbConnectable r, RemoteCanvas v, Handler h) {
        super(r, v, h);
        
        context = v.getContext();
        
        keyboardMapper = new RdpKeyboardMapper();
        keyboardMapper.init(context);
        keyboardMapper.reset((RdpKeyboardMapper.KeyProcessingListener)r);
    }
    
    public boolean processLocalKeyEvent(int keyCode, KeyEvent evt, int additionalMetaState) {
        if (rfb != null && rfb.isInNormalProtocol()) {
            RemotePointer pointer = canvas.getPointer();
            boolean down = (evt.getAction() == KeyEvent.ACTION_DOWN) ||
                           (evt.getAction() == KeyEvent.ACTION_MULTIPLE);            
            int metaState = additionalMetaState | convertEventMetaState (evt);

            if (keyCode == KeyEvent.KEYCODE_MENU)
                return true;                           // Ignore menu key

            if (pointer.handleHardwareButtons(keyCode, evt, metaState|onScreenMetaState|hardwareMetaState))
                return true;

            // Detect whether this event is coming from a default hardware keyboard.
            boolean defaultHardwareKbd = (evt.getDeviceId() == 0);
            if (!down) {
                switch (evt.getScanCode()) {
                case SCAN_LEFTCTRL:
                case SCAN_RIGHTCTRL:
                    hardwareMetaState &= ~CTRL_MASK;
                    break;
                }
                
                switch(keyCode) {
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    hardwareMetaState &= ~CTRL_MASK;
                    break;
                case KeyEvent.KEYCODE_ALT_LEFT:
                    // Leaving KeyEvent.KEYCODE_ALT_LEFT for symbol input on hardware keyboards.
                    if (!defaultHardwareKbd)
                        hardwareMetaState &= ~ALT_MASK;
                    break;
                case KeyEvent.KEYCODE_ALT_RIGHT:
                    hardwareMetaState &= ~ALT_MASK;
                    break;
                }
            } else {
                // Look for standard scan-codes from hardware keyboards
                switch (evt.getScanCode()) {
                case SCAN_LEFTCTRL:
                case SCAN_RIGHTCTRL:
                    hardwareMetaState |= CTRL_MASK;
                    break;
                }
                
                switch(keyCode) {
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    hardwareMetaState |= CTRL_MASK;
                    break;
                case KeyEvent.KEYCODE_ALT_LEFT:
                    // Leaving KeyEvent.KEYCODE_ALT_LEFT for symbol input on hardware keyboards.
                    if (!defaultHardwareKbd)
                        hardwareMetaState |= ALT_MASK;
                    break;
                case KeyEvent.KEYCODE_ALT_RIGHT:
                    hardwareMetaState |= ALT_MASK;
                    break;
                }
            }

            // Update the meta-state with writeKeyEvent.
            metaState = onScreenMetaState|hardwareMetaState|metaState;
            rfb.writeKeyEvent(keyCode, metaState, down);
            if (down) {
                lastDownMetaState = metaState;
            } else {
                lastDownMetaState = 0;
            }
            

            if (keyCode == 0 /*KEYCODE_UNKNOWN*/) {
                String s = evt.getCharacters();
                if (s != null) {
                    int numchars = s.length();
                    int i = numJunkCharactersToSkip (numchars, evt);
                    for (; i < numchars; i++) {
                        KeyEvent event = new KeyEvent(evt.getEventTime(), s.substring(i, i+1), KeyCharacterMap.FULL, 0);
                        keyboardMapper.processAndroidKeyEvent(event);
                    }
                }
                return true;
            } else {
                // Send the key to be processed through the KeyboardMapper.
                return keyboardMapper.processAndroidKeyEvent(evt);
            }
        } else {
            return false;
        }
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
            // TODO: I should not need to treat this specially anymore.
            int savedMetaState = onScreenMetaState|hardwareMetaState;
            // Update the metastate
            rfb.writeKeyEvent(0, RemoteKeyboard.CTRL_MASK|RemoteKeyboard.ALT_MASK, false);
            keyboardMapper.processAndroidKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, 112));
            keyboardMapper.processAndroidKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, 112));
            rfb.writeKeyEvent(0, savedMetaState, false);
        } else {
            sendKeySym (meta.getKeySym(), meta.getMetaFlags());
        }
    }
}
