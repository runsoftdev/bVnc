/** 
* Copyright (C) 2012 Iordan Iordanov
 * Copyright (C) 2010 Michael A. MacDonald
 * 
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
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

//
// CanvasView is the Activity for showing VNC Desktop.
//
package com.iiordanov.runsoft.bVNC;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import com.iiordanov.android.bc.BCFactory;
import com.iiordanov.android.zoomer.ZoomControls;
import com.iiordanov.chat.ChatData;
import com.iiordanov.chat.ChatListAdapter;
import com.iiordanov.runsoft.bVNC.dialogs.EnterTextDialog;
import com.iiordanov.runsoft.bVNC.dialogs.MetaKeyDialog;
import com.iiordanov.runsoft.bVNC.input.AbstractInputHandler;
import com.iiordanov.runsoft.bVNC.input.HangulKeypad;
import com.iiordanov.runsoft.bVNC.input.Panner;
import com.iiordanov.runsoft.bVNC.input.RemoteKeyboard;
import com.iiordanov.runsoft.bVNC.input.SimulatedTouchpadInputHandler;
import com.iiordanov.runsoft.bVNC.input.SingleHandedInputHandler;
import com.iiordanov.runsoft.bVNC.input.TouchMouseDragPanInputHandler;
import com.iiordanov.runsoft.bVNC.input.TouchMouseSwipePanInputHandler;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;


public class RemoteCanvasActivity extends FragmentActivity implements OnKeyListener {
    
    private final static String TAG = "VncCanvasActivity";
    
    AbstractInputHandler inputHandler;
    
    public static RemoteCanvasActivity mThis;

    private RemoteCanvas canvas;

    private Database database;

    private MenuItem[] inputModeMenuItems;
    private MenuItem[] scalingModeMenuItems;
    private AbstractInputHandler inputModeHandlers[];
    private ConnectionBean connection;
/*    private static final int inputModeIds[] = { R.id.itemInputFitToScreen,
        R.id.itemInputTouchpad,
        R.id.itemInputMouse, R.id.itemInputPan,
        R.id.itemInputTouchPanTrackballMouse,
        R.id.itemInputDPadPanTouchMouse, R.id.itemInputTouchPanZoomMouse };
 */
    private static final int inputModeIds[] = { R.id.itemInputTouchpad,
                                                R.id.itemInputTouchPanZoomMouse,
                                                R.id.itemInputDragPanZoomMouse,
                                                R.id.itemInputSingleHanded };
    private static final int scalingModeIds[] = { R.id.itemZoomable, R.id.itemFitToScreen,
                                                  R.id.itemOneToOne};
    ZoomControls zoomer;
    Panner panner;
    SSHConnection sshConnection;
    Handler handler;

    RelativeLayout layoutKeys;
    ImageButton    keyStow;
    ImageButton    keyCtrl;
    boolean       keyCtrlToggled;
    ImageButton    keySuper;
    boolean       keySuperToggled;
    ImageButton    keyAlt;
    boolean       keyAltToggled;
    ImageButton    keyTab;
    ImageButton    keyEsc;
    ImageButton    keyShift;
    boolean       keyShiftToggled;
    ImageButton    keyUp;
    ImageButton    keyDown;
    ImageButton    keyLeft;
    ImageButton    keyRight;
    boolean       hardKeyboardExtended;
    boolean       extraKeysHidden = false;
    int            prevBottomOffset = 0;
    Button keyKor;
    HangulKeypad keypad;
    
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mThis = this;
        Utils.showMenu(this);
        initialize();
        if (connection != null && connection.isReadyForConnection())
        	continueConnecting();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    
        initChatListView();        
    }
    
	void initialize () {
        if (android.os.Build.VERSION.SDK_INT >= 9) {
            android.os.StrictMode.ThreadPolicy policy = new android.os.StrictMode.ThreadPolicy.Builder().permitAll().build();
            android.os.StrictMode.setThreadPolicy(policy);
        }
        
        handler = new Handler ();
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);

        database = new Database(this);

        Intent i = getIntent();
        connection = null;
        
        Uri data = i.getData();
        
        Log.e(TAG, "uri="+data);
        
        boolean isSupportedScheme = false;
        if (data != null) {
            String s = data.getScheme();
            isSupportedScheme = s.equals("rdp") || s.equals("spice") || s.equals("vnc");
        }
        
        if (isSupportedScheme || !Utils.isNullOrEmptry(i.getType())) {
            if (isMasterPasswordEnabled()) {
                Utils.showFatalErrorMessage(this, getResources().getString(R.string.master_password_error_intents_not_supported));
                return;
            }
            
            connection = ConnectionBean.createLoadFromUri(data, this);
            
            String host = data.getHost();
            if (!host.startsWith(Constants.CONNECTION)) {
                connection.parseFromUri(data);
            }
            
            if (connection.isSaved()) {
                connection.saveAndWriteRecent(false);
            }
            // we need to save the connection to display the loading screen, onclso otherwise we should exit
            if (!connection.isReadyForConnection()) {
            	if (!connection.isSaved()) {
            		Log.i(TAG, "Exiting - Insufficent information to connect and connection was not saved.");
            		Toast.makeText(this, getString(R.string.error_uri_noinfo_nosave), Toast.LENGTH_LONG).show();;
            	} else {
            		// launch bVNC activity
            		Log.i(TAG, "Insufficent information to connect, showing connection dialog.");
            		Intent bVncIntent = new Intent(this, bVNC.class);
            		startActivity(bVncIntent);
            	}
            	finish();
            	return;
            }
        } else {
        	connection = new ConnectionBean(this);
            Bundle extras = i.getExtras();

            if (extras != null) {
                  connection.Gen_populate((ContentValues) extras.getParcelable(Constants.CONNECTION));
            }

            // Parse a HOST:PORT entry
            String host = connection.getAddress();
            if (host.indexOf(':') > -1) {
                String p = host.substring(host.indexOf(':') + 1);
                try {
                    connection.setPort(Integer.parseInt(p));
                } catch (Exception e) {}
                connection.setAddress(host.substring(0, host.indexOf(':')));
              }
            
            if (connection.getPort() == 0)
                connection.setPort(Constants.DEFAULT_VNC_PORT);
            
            if (connection.getSshPort() == 0)
                connection.setSshPort(Constants.DEFAULT_SSH_PORT);
        }
    }

    void continueConnecting () {
        // TODO: Implement left-icon
        //requestWindowFeature(Window.FEATURE_LEFT_ICON);
        //setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.icon); 

        setContentView(R.layout.canvas);
        canvas = (RemoteCanvas) findViewById(R.id.vnc_canvas);
        zoomer = (ZoomControls) findViewById(R.id.zoomer);

        // Initialize and define actions for on-screen keys.
        initializeOnScreenKeys ();
    
        canvas.initializeCanvas(connection, database, new Runnable() {
            public void run() {
                try { setModes(); } catch (NullPointerException e) { }
            }
        });
        
        canvas.setOnKeyListener(this);
        canvas.setFocusableInTouchMode(true);
        canvas.setDrawingCacheEnabled(false);
        
        // This code detects when the soft keyboard is up and sets an appropriate visibleHeight in vncCanvas.
        // When the keyboard is gone, it resets visibleHeight and pans zero distance to prevent us from being
        // below the desktop image (if we scrolled all the way down when the keyboard was up).
        // TODO: Move this into a separate thread, and post the visibility changes to the handler.
        //       to avoid occupying the UI thread with this.
        final View rootView = ((ViewGroup)findViewById(android.R.id.content)).getChildAt(0);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                    Rect r = new Rect();

                    rootView.getWindowVisibleDisplayFrame(r);

                    // To avoid setting the visible height to a wrong value after an screen unlock event
                    // (when r.bottom holds the width of the screen rather than the height due to a rotation)
                    // we make sure r.top is zero (i.e. there is no notification bar and we are in full-screen mode)
                    // It's a bit of a hack.
                    if (r.top == 0) {
                        if (canvas.bitmapData != null) {
                            canvas.setVisibleHeight(r.bottom);
                            canvas.pan(0,0);
                        }
                    }
                    
                    // Enable/show the zoomer if the keyboard is gone, and disable/hide otherwise.
                    // We detect the keyboard if more than 19% of the screen is covered.
                    int offset = 0;
                    int rootViewHeight = rootView.getHeight();
                    if (r.bottom > rootViewHeight*0.81) {
                        offset = rootViewHeight - r.bottom;
                        // Soft Kbd gone, shift the meta keys and arrows down.
                        if (layoutKeys != null) {
                            layoutKeys.offsetTopAndBottom(offset);
                            keyStow.offsetTopAndBottom(offset);
                            if (prevBottomOffset != offset) { 
                                setExtraKeysVisibility(View.GONE, false);
                                canvas.invalidate();
                                zoomer.enable();
                            }
                        }
                    } else {
                        offset = r.bottom - rootViewHeight;
                        //  Soft Kbd up, shift the meta keys and arrows up.
                        if (layoutKeys != null) {
                            layoutKeys.offsetTopAndBottom(offset);
                            keyStow.offsetTopAndBottom(offset);
                            if (prevBottomOffset != offset) { 
                                setExtraKeysVisibility(View.VISIBLE, true);
                                canvas.invalidate();
                                zoomer.hide();
                                zoomer.disable();
                            }
                        }
                    }
                    setKeyStowDrawableAndVisibility();
                    prevBottomOffset = offset;
             }
        });

        zoomer.hide();
        
        zoomer.setOnZoomKeyboardClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	//TODO zoomer keypad
            	canvas.getKeyboard().processLocalKeyEvent(144, new KeyEvent(144,0));
            	
                keyKor.setVisibility(View.GONE);                   
                               
            	
            	keypad.setVisibility(View.VISIBLE);
            	keypad.setTag("1");
            }

        });
        
        zoomer.setOnShowMenuClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RemoteCanvasActivity.this.openOptionsMenu();
            }

        });
        panner = new Panner(this, canvas.handler);

        inputHandler = getInputHandlerById(R.id.itemInputTouchPanZoomMouse);
    }

    
    @SuppressWarnings("deprecation")
    private void setKeyStowDrawableAndVisibility() {
        Drawable replacer = null;
        if (layoutKeys.getVisibility() == View.GONE)
            replacer = getResources().getDrawable(R.drawable.showkeys);
        else
            replacer = getResources().getDrawable(R.drawable.hidekeys);
        
        int sdk = android.os.Build.VERSION.SDK_INT;
        if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            keyStow.setBackgroundDrawable(replacer);
        } else {
            keyStow.setBackground(replacer);
        }

        if (connection.getExtraKeysToggleType() == Constants.EXTRA_KEYS_OFF)
            keyStow.setVisibility(View.GONE);
        else {
            keyStow.setVisibility(View.VISIBLE);
//            if(keyKor != null)
//            	keyKor.setVisibility(View.VISIBLE);
        }
    }
   

	/**
     * Initializes the on-screen keys for meta keys and arrow keys.
     */
    private void initializeOnScreenKeys () {
        
        layoutKeys = (RelativeLayout) findViewById(R.id.layoutKeys);

        keyStow = (ImageButton)    findViewById(R.id.keyStow);
        setKeyStowDrawableAndVisibility();
        keyStow.setOnClickListener(new OnClickListener () {
            @Override
            public void onClick(View arg0) {
                if (layoutKeys.getVisibility() == View.VISIBLE) {
                    extraKeysHidden = true;
                    setExtraKeysVisibility(View.GONE, false);
                } else {
                    extraKeysHidden = false;
                    setExtraKeysVisibility(View.VISIBLE, true);
                }
                layoutKeys.offsetTopAndBottom(prevBottomOffset);
                setKeyStowDrawableAndVisibility();
            }
        });

        // Define action of tab key and meta keys.
        keyTab = (ImageButton) findViewById(R.id.keyTab);
        keyTab.setOnTouchListener(new OnTouchListener () {
            @Override
            public boolean onTouch(View arg0, MotionEvent e) {
                RemoteKeyboard k = canvas.getKeyboard();
                int key = KeyEvent.KEYCODE_TAB;
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    BCFactory.getInstance().getBCHaptic().performLongPressHaptic(canvas);
                    keyTab.setImageResource(R.drawable.tabon);
                    k.repeatKeyEvent(key, new KeyEvent(e.getAction(), key));
                    return true;
                } else if (e.getAction() == MotionEvent.ACTION_UP) {
                    keyTab.setImageResource(R.drawable.taboff);
                    resetOnScreenKeys (0);
                    k.stopRepeatingKeyEvent();
                    return true;
                }
                return false;
            }
        });

        keyEsc = (ImageButton) findViewById(R.id.keyEsc);
        keyEsc.setOnTouchListener(new OnTouchListener () {
            @Override
            public boolean onTouch(View arg0, MotionEvent e) {
                RemoteKeyboard k = canvas.getKeyboard();
                int key = 111; /* KEYCODE_ESCAPE */
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    BCFactory.getInstance().getBCHaptic().performLongPressHaptic(canvas);
                    keyEsc.setImageResource(R.drawable.escon);
                    k.repeatKeyEvent(key, new KeyEvent(e.getAction(), key));
                    return true;
                } else if (e.getAction() == MotionEvent.ACTION_UP) {
                    keyEsc.setImageResource(R.drawable.escoff);
                    resetOnScreenKeys (0);
                    k.stopRepeatingKeyEvent();
                    return true;
                }
                return false;
            }
        });

        keyCtrl = (ImageButton) findViewById(R.id.keyCtrl);
        keyCtrl.setOnClickListener(new OnClickListener () {
            @Override
            public void onClick(View arg0) {
                boolean on = canvas.getKeyboard().onScreenCtrlToggle();
                keyCtrlToggled = false;
                if (on)
                    keyCtrl.setImageResource(R.drawable.ctrlon);
                else
                    keyCtrl.setImageResource(R.drawable.ctrloff);
            }
        });
        
        keyCtrl.setOnLongClickListener(new OnLongClickListener () {
            @Override
            public boolean onLongClick(View arg0) {
                BCFactory.getInstance().getBCHaptic().performLongPressHaptic(canvas);
                boolean on = canvas.getKeyboard().onScreenCtrlToggle();
                keyCtrlToggled = true;
                if (on)
                    keyCtrl.setImageResource(R.drawable.ctrlon);
                else
                    keyCtrl.setImageResource(R.drawable.ctrloff);
                return true;
            }
        });

        keySuper = (ImageButton) findViewById(R.id.keySuper);
        keySuper.setOnClickListener(new OnClickListener () {
            @Override
            public void onClick(View arg0) {
                boolean on = canvas.getKeyboard().onScreenSuperToggle();
                keySuperToggled = false;
                if (on)
                    keySuper.setImageResource(R.drawable.superon);
                else
                    keySuper.setImageResource(R.drawable.superoff);
            }
        });

        keySuper.setOnLongClickListener(new OnLongClickListener () {
            @Override
            public boolean onLongClick(View arg0) {
                BCFactory.getInstance().getBCHaptic().performLongPressHaptic(canvas);
                boolean on = canvas.getKeyboard().onScreenSuperToggle();
                keySuperToggled = true;
                if (on)
                    keySuper.setImageResource(R.drawable.superon);
                else
                    keySuper.setImageResource(R.drawable.superoff);
                return true;
            }
        });
        
        keypad = (HangulKeypad) findViewById(R.id.hangul_keypad);
        keypad.setVncKeyboard(canvas);
        keypad.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				btnChatShowToggle.setVisibility(View.VISIBLE);
				keypad.setVisibility(View.GONE);	
				keypad.setTag("");
				keyKor.setVisibility(View.VISIBLE);
				if(v.getId() == R.id.key_show_eng) {
					InputMethodManager inputMgr = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
	                inputMgr.toggleSoftInput(0, 0);
				}
			}
		});
        
        //TODO
        keyKor = (Button) findViewById(R.id.keyKor);
        keyKor.setOnClickListener(new OnClickListener () {
            @Override
            public void onClick(View arg0) {
                String toggle = (String)keypad.getTag();
               
                if (TextUtils.isEmpty(toggle)) {
                	InputMethodManager inputMgr = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
	                inputMgr.hideSoftInputFromWindow(btnChatShowToggle.getWindowToken(), 0);
	                
	                new Handler().postDelayed(new Runnable() {
						
						@Override
						public void run() {
							canvas.getKeyboard().processLocalKeyEvent(144, new KeyEvent(144,0));
		                	
			                keyKor.setVisibility(View.GONE);		                	
		                	keypad.setVisibility(View.VISIBLE);
		                	keypad.setTag("1");
						}
					}, 500);	                
                }
                else {
                	keyKor.setVisibility(View.VISIBLE);
                	btnChatShowToggle.setVisibility(View.VISIBLE);
                	
                	keypad.setVisibility(View.GONE);
                	keypad.setTag("");
                }
            }
        });
        
        keyAlt = (ImageButton) findViewById(R.id.keyAlt);
        keyAlt.setOnClickListener(new OnClickListener () {
            @Override
            public void onClick(View arg0) {
                boolean on = canvas.getKeyboard().onScreenAltToggle();
                keyAltToggled = false;
                if (on)
                    keyAlt.setImageResource(R.drawable.alton);
                else
                    keyAlt.setImageResource(R.drawable.altoff);
            }
        });
       
        keyAlt.setOnLongClickListener(new OnLongClickListener () {
            @Override
            public boolean onLongClick(View arg0) {
                BCFactory.getInstance().getBCHaptic().performLongPressHaptic(canvas);
                boolean on = canvas.getKeyboard().onScreenAltToggle();
                keyAltToggled = true;
                if (on)
                    keyAlt.setImageResource(R.drawable.alton);
                else
                    keyAlt.setImageResource(R.drawable.altoff);
                return true;
            }
        });
        
        keyShift = (ImageButton) findViewById(R.id.keyShift);
        keyShift.setOnClickListener(new OnClickListener () {
            @Override
            public void onClick(View arg0) {
                boolean on = canvas.getKeyboard().onScreenShiftToggle();
                keyShiftToggled = false;
                if (on)
                    keyShift.setImageResource(R.drawable.shifton);
                else
                    keyShift.setImageResource(R.drawable.shiftoff);
            }
        });
        
        keyShift.setOnLongClickListener(new OnLongClickListener () {
            @Override
            public boolean onLongClick(View arg0) {
                BCFactory.getInstance().getBCHaptic().performLongPressHaptic(canvas);
                boolean on = canvas.getKeyboard().onScreenShiftToggle();
                keyShiftToggled = true;
                if (on)
                    keyShift.setImageResource(R.drawable.shifton);
                else
                    keyShift.setImageResource(R.drawable.shiftoff);
                return true;
            }
        });
        
        
        // vncCanvas.sendMetaKey(MetaKeyBean.keyArrowLeft);

        // Define action of arrow keys.
        keyUp = (ImageButton) findViewById(R.id.keyUpArrow);
        keyUp.setOnTouchListener(new OnTouchListener () {
            @Override
            public boolean onTouch(View arg0, MotionEvent e) {
                RemoteKeyboard k = canvas.getKeyboard();
                int key = KeyEvent.KEYCODE_DPAD_UP;
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    BCFactory.getInstance().getBCHaptic().performLongPressHaptic(canvas);
                    keyUp.setImageResource(R.drawable.upon);
                    k.repeatKeyEvent(key, new KeyEvent(e.getAction(), key));
                    return true;
                } else if (e.getAction() == MotionEvent.ACTION_UP) {
                    keyUp.setImageResource(R.drawable.upoff);
                    resetOnScreenKeys (0);
                    k.stopRepeatingKeyEvent();
                    return true;
                }
                return false;
            }
        });

        keyDown = (ImageButton) findViewById(R.id.keyDownArrow);
        keyDown.setOnTouchListener(new OnTouchListener () {
            @Override
            public boolean onTouch(View arg0, MotionEvent e) {
                RemoteKeyboard k = canvas.getKeyboard();
                int key = KeyEvent.KEYCODE_DPAD_DOWN;
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    BCFactory.getInstance().getBCHaptic().performLongPressHaptic(canvas);
                    keyDown.setImageResource(R.drawable.downon);
                    k.repeatKeyEvent(key, new KeyEvent(e.getAction(), key));
                    return true;
                } else if (e.getAction() == MotionEvent.ACTION_UP) {
                    keyDown.setImageResource(R.drawable.downoff);
                    resetOnScreenKeys (0);
                    k.stopRepeatingKeyEvent();
                    return true;
                }
                return false;
            }
        });

        keyLeft = (ImageButton) findViewById(R.id.keyLeftArrow);
        keyLeft.setOnTouchListener(new OnTouchListener () {
            @Override
            public boolean onTouch(View arg0, MotionEvent e) {
                RemoteKeyboard k = canvas.getKeyboard();
                int key = KeyEvent.KEYCODE_DPAD_LEFT;
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    BCFactory.getInstance().getBCHaptic().performLongPressHaptic(canvas);
                    keyLeft.setImageResource(R.drawable.lefton);
                    k.repeatKeyEvent(key, new KeyEvent(e.getAction(), key));
                    return true;
                } else if (e.getAction() == MotionEvent.ACTION_UP) {
                    keyLeft.setImageResource(R.drawable.leftoff);
                    resetOnScreenKeys (0);
                    k.stopRepeatingKeyEvent();
                    return true;
                }
                return false;
            }
        });

        keyRight = (ImageButton) findViewById(R.id.keyRightArrow);
        keyRight.setOnTouchListener(new OnTouchListener () {
            @Override
            public boolean onTouch(View arg0, MotionEvent e) {
                RemoteKeyboard k = canvas.getKeyboard();
                int key = KeyEvent.KEYCODE_DPAD_RIGHT;
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    BCFactory.getInstance().getBCHaptic().performLongPressHaptic(canvas);
                    keyRight.setImageResource(R.drawable.righton);
                    k.repeatKeyEvent(key, new KeyEvent(e.getAction(), key));
                    return true;    
                } else if (e.getAction() == MotionEvent.ACTION_UP) {
                    keyRight.setImageResource(R.drawable.rightoff);
                    resetOnScreenKeys (0);
                    k.stopRepeatingKeyEvent();
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * Resets the state and image of the on-screen keys.
     */
    private void resetOnScreenKeys (int keyCode) {
        // Do not reset on-screen keys if keycode is SHIFT.
        switch (keyCode) {
        case KeyEvent.KEYCODE_SHIFT_LEFT:
        case KeyEvent.KEYCODE_SHIFT_RIGHT: return;
        }
        if (!keyCtrlToggled) {
            keyCtrl.setImageResource(R.drawable.ctrloff);
            canvas.getKeyboard().onScreenCtrlOff();
        }
        if (!keyAltToggled) {
            keyAlt.setImageResource(R.drawable.altoff);
            canvas.getKeyboard().onScreenAltOff();
        }
        if (!keySuperToggled) {
            keySuper.setImageResource(R.drawable.superoff);
            canvas.getKeyboard().onScreenSuperOff();
        }
        if (!keyShiftToggled) {
            keyShift.setImageResource(R.drawable.shiftoff);
            canvas.getKeyboard().onScreenShiftOff();
        }
    }

    
    /**
     * Sets the visibility of the extra keys appropriately.
     */
    private void setExtraKeysVisibility (int visibility, boolean forceVisible) {
        Configuration config = getResources().getConfiguration();
        //Log.e(TAG, "Hardware kbd hidden: " + Integer.toString(config.hardKeyboardHidden));
        //Log.e(TAG, "Any keyboard hidden: " + Integer.toString(config.keyboardHidden));
        //Log.e(TAG, "Keyboard type: " + Integer.toString(config.keyboard));

        boolean makeVisible = forceVisible;
        if (config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO)
            makeVisible = true;

        if (!extraKeysHidden && makeVisible && 
            connection.getExtraKeysToggleType() == Constants.EXTRA_KEYS_ON) {
            layoutKeys.setVisibility(View.VISIBLE);
            layoutKeys.invalidate();
            return;
        }
        
        if (visibility == View.GONE) {
        	btnChatShowToggle.setVisibility(View.VISIBLE);
            layoutKeys.setVisibility(View.GONE);
            layoutKeys.invalidate();
        }
    }
    
    /*
     * TODO: REMOVE THIS AS SOON AS POSSIBLE.
     * onPause: This is an ugly hack for the Playbook, because the Playbook hides the keyboard upon unlock.
     * This causes the visible height to remain less, as if the soft keyboard is still up. This hack must go 
     * away as soon as the Playbook doesn't need it anymore.
     */
    @Override
    protected void onPause(){
        super.onPause();
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(canvas.getWindowToken(), 0);
        } catch (NullPointerException e) { }
    }

    /*
     * TODO: REMOVE THIS AS SOON AS POSSIBLE.
     * onResume: This is an ugly hack for the Playbook which hides the keyboard upon unlock. This causes the visible
     * height to remain less, as if the soft keyboard is still up. This hack must go away as soon
     * as the Playbook doesn't need it anymore.
     */
    @Override
    protected void onResume(){
        super.onResume();
        Log.i(TAG, "onResume called.");
        try {
            canvas.postInvalidateDelayed(600);
        } catch (NullPointerException e) { }
    }
    
    /**
     * Set modes on start to match what is specified in the ConnectionBean;
     * color mode (already done) scaling, input mode
     */
    void setModes() {
        AbstractInputHandler handler = getInputHandlerByName(connection.getInputMode());
        AbstractScaling.getByScaleType(connection.getScaleMode()).setScaleTypeForActivity(this);
        this.inputHandler = handler;
        showPanningState(false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreateDialog(int)
     */
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case R.layout.entertext:
            return new EnterTextDialog(this);
        case R.id.itemHelpInputMode:
            return createHelpDialog ();
        }
        
        // Default to meta key dialog
        return new MetaKeyDialog(this);
    }

    /**
     * Creates the help dialog for this activity.
     */
    private Dialog createHelpDialog() {
        AlertDialog.Builder adb = new AlertDialog.Builder(this)
                .setMessage(R.string.input_mode_help_text)
                .setPositiveButton(R.string.close,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
                                // We don't have to do anything.
                            }
                        });
        Dialog d = adb.setView(new ListView (this)).create();
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(d.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        d.show();
        d.getWindow().setAttributes(lp);
        return d;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
     */
    @SuppressWarnings("deprecation")
	@Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);
        if (dialog instanceof ConnectionSettable)
            ((ConnectionSettable) dialog).setConnection(connection);
    }

    /**
     * This runnable fixes things up after a rotation.
     */
    private Runnable rotationCorrector = new Runnable() {
        public void run() {
            try { correctAfterRotation (); } catch (NullPointerException e) { }
        }
    };

    /**
     * This function is called by the rotationCorrector runnable
     * to fix things up after a rotation.
     */
    private void correctAfterRotation () {
        // Its quite common to see NullPointerExceptions here when this function is called
        // at the point of disconnection. Hence, we catch and ignore the error.
        float oldScale = canvas.scaling.getScale();
        int x = canvas.absoluteXPosition;
        int y = canvas.absoluteYPosition;
        canvas.scaling.setScaleTypeForActivity(RemoteCanvasActivity.this);
        float newScale = canvas.scaling.getScale();
        canvas.scaling.adjust(this, oldScale/newScale, 0, 0);
        newScale = canvas.scaling.getScale();
        if (newScale <= oldScale) {
            canvas.absoluteXPosition = x;
            canvas.absoluteYPosition = y;
            canvas.scrollToAbsolute();
        }
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        
        try {
            setExtraKeysVisibility(View.GONE, false);
            
            // Correct a few times just in case. There is no visual effect.
            handler.postDelayed(rotationCorrector, 300);
            handler.postDelayed(rotationCorrector, 600);
            handler.postDelayed(rotationCorrector, 1200);
        } catch (NullPointerException e) { }
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            canvas.postInvalidateDelayed(800);
        } catch (NullPointerException e) { }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        try {
            canvas.postInvalidateDelayed(1000);
        } catch (NullPointerException e) { }
    }

    /** {@inheritDoc} */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        try {
            getMenuInflater().inflate(R.menu.vnccanvasactivitymenu, menu);

            menu.findItem(canvas.scaling.getId()).setChecked(true);
    
            Menu inputMenu = menu.findItem(R.id.itemInputMode).getSubMenu();
            inputModeMenuItems = new MenuItem[inputModeIds.length];
            for (int i = 0; i < inputModeIds.length; i++) {
                inputModeMenuItems[i] = inputMenu.findItem(inputModeIds[i]);
            }
            updateInputMenu();
            
            Menu scalingMenu = menu.findItem(R.id.itemScaling).getSubMenu();
            scalingModeMenuItems = new MenuItem[scalingModeIds.length];
            for (int i = 0; i < scalingModeIds.length; i++) {
                scalingModeMenuItems[i] = scalingMenu.findItem(scalingModeIds[i]);
            }
            updateScalingMenu();
            
            // Set the text of the Extra Keys menu item appropriately.
            if (connection.getExtraKeysToggleType() == Constants.EXTRA_KEYS_ON)
                menu.findItem(R.id.itemExtraKeys).setTitle(R.string.extra_keys_disable);
            else
                menu.findItem(R.id.itemExtraKeys).setTitle(R.string.extra_keys_enable);
            
    /*        menu.findItem(R.id.itemFollowMouse).setChecked(
                    connection.getFollowMouse());
            menu.findItem(R.id.itemFollowPan).setChecked(connection.getFollowPan());
     */
    /* TODO: This is how one detects long-presses on menu items. However, getActionView is not available in Android 2.3...
            menu.findItem(R.id.itemExtraKeys).getActionView().setOnLongClickListener(new OnLongClickListener () {
    
                @Override
                public boolean onLongClick(View arg0) {
                    Toast.makeText(arg0.getContext(), "Long Press Detected.", Toast.LENGTH_LONG).show();
                    return false;
                }
                
            });
    */
        } catch (NullPointerException e) { }
        return true;
    }

    /**
     * Change the scaling mode sub-menu to reflect available scaling modes.
     */
    void updateScalingMenu() {
        try {
            for (MenuItem item : scalingModeMenuItems) {
                // If the entire framebuffer is NOT contained in the bitmap, fit-to-screen is meaningless.
                if (item.getItemId() == R.id.itemFitToScreen) {
                    if ( canvas != null && canvas.bitmapData != null &&
                         (canvas.bitmapData.bitmapheight != canvas.bitmapData.framebufferheight ||
                          canvas.bitmapData.bitmapwidth  != canvas.bitmapData.framebufferwidth) )
                        item.setEnabled(false);
                    else
                        item.setEnabled(true);
                } else
                    item.setEnabled(true);
            }
        } catch (NullPointerException e) { }
    }    
    
    /**
     * Change the input mode sub-menu to reflect change in scaling
     */
    void updateInputMenu() {
        try {
            for (MenuItem item : inputModeMenuItems) {
                item.setEnabled(canvas.scaling.isValidInputMode(item.getItemId()));
                if (getInputHandlerById(item.getItemId()) == inputHandler)
                    item.setChecked(true);
            }
        } catch (NullPointerException e) { }
    }

    /**
     * If id represents an input handler, return that; otherwise return null
     * 
     * @param id
     * @return
     */
    AbstractInputHandler getInputHandlerById(int id) {
        boolean isRdp = getPackageName().contains("RDP");

        if (inputModeHandlers == null) {
            inputModeHandlers = new AbstractInputHandler[inputModeIds.length];
        }
        for (int i = 0; i < inputModeIds.length; ++i) {
            if (inputModeIds[i] == id) {
                if (inputModeHandlers[i] == null) {
                    switch (id) {
/*                    case R.id.itemInputFitToScreen:
                        inputModeHandlers[i] = new FitToScreenMode();
                        break;
                    case R.id.itemInputPan:
                        inputModeHandlers[i] = new PanMode();
                        break;
                    case R.id.itemInputTouchPanTrackballMouse:
                        inputModeHandlers[i] = new TouchPanTrackballMouse();
                        break;
                    case R.id.itemInputMouse:
                        inputModeHandlers[i] = new MouseMode();
                        break; 

                    case R.id.itemInputDPadPanTouchMouse:
                        inputModeHandlers[i] = new DPadPanTouchMouseMode();
                        break;
 */                    
                    case R.id.itemInputTouchPanZoomMouse:
                        inputModeHandlers[i] = new TouchMouseSwipePanInputHandler(this, canvas, isRdp);
                        break;
                    case R.id.itemInputDragPanZoomMouse:
                        inputModeHandlers[i] = new TouchMouseDragPanInputHandler(this, canvas, isRdp);
                        break;
                    case R.id.itemInputTouchpad:
                        inputModeHandlers[i] = new SimulatedTouchpadInputHandler(this, canvas, isRdp);
                        break;
                    case R.id.itemInputSingleHanded:
                        inputModeHandlers[i] = new SingleHandedInputHandler(this, canvas, isRdp);
                        break;

                    }
                }
                return inputModeHandlers[i];
            }
        }
        return null;
    }

    void clearInputHandlers() {
        if (inputModeHandlers == null)
            return;

        for (int i = 0; i < inputModeIds.length; ++i) {
            inputModeHandlers[i] = null;
        }
        inputModeHandlers = null;
    }
    
    AbstractInputHandler getInputHandlerByName(String name) {
        AbstractInputHandler result = null;
        for (int id : inputModeIds) {
            AbstractInputHandler handler = getInputHandlerById(id);
            if (handler.getName().equals(name)) {
                result = handler;
                break;
            }
        }
        if (result == null) {
            result = getInputHandlerById(R.id.itemInputTouchPanZoomMouse);
        }
        return result;
    }
    
    int getModeIdFromHandler(AbstractInputHandler handler) {
        for (int id : inputModeIds) {
            if (handler == getInputHandlerById(id))
                return id;
        }
        return R.id.itemInputTouchPanZoomMouse;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        canvas.getKeyboard().setAfterMenu(true);
        switch (item.getItemId()) {
        case R.id.itemInfo:
            canvas.showConnectionInfo();
            return true;
        case R.id.itemSpecialKeys:
            showDialog(R.layout.metakey);
            return true;
//        case R.id.itemColorMode:
//            selectColorModel();
//            return true;
            // Following sets one of the scaling options
        case R.id.itemZoomable:
        case R.id.itemOneToOne:
        case R.id.itemFitToScreen:
            AbstractScaling.getById(item.getItemId()).setScaleTypeForActivity(this);
            item.setChecked(true);
            showPanningState(false);
            return true;
        case R.id.itemCenterMouse:
            canvas.getPointer().warpMouse(canvas.absoluteXPosition + canvas.getVisibleWidth()  / 2,
                                             canvas.absoluteYPosition + canvas.getVisibleHeight() / 2);
            return true;
        case R.id.itemDisconnect:
            canvas.closeConnection();
            finish();
            return true;
//        case R.id.itemEnterText:
//            showDialog(R.layout.entertext);
//            return true;
        case R.id.itemCtrlAltDel:
            canvas.getKeyboard().sendMetaKey(MetaKeyBean.keyCtrlAltDel);
            return true;
/*        case R.id.itemFollowMouse:
            boolean newFollow = !connection.getFollowMouse();
            item.setChecked(newFollow);
            connection.setFollowMouse(newFollow);
            if (newFollow) {
                vncCanvas.panToMouse();
            }
            connection.save(database.getWritableDatabase());
            database.close();
            return true;
        case R.id.itemFollowPan:
            boolean newFollowPan = !connection.getFollowPan();
            item.setChecked(newFollowPan);
            connection.setFollowPan(newFollowPan);
            connection.save(database.getWritableDatabase());
            database.close();
            return true;
 
        case R.id.itemArrowLeft:
            vncCanvas.sendMetaKey(MetaKeyBean.keyArrowLeft);
            return true;
        case R.id.itemArrowUp:
            vncCanvas.sendMetaKey(MetaKeyBean.keyArrowUp);
            return true;
        case R.id.itemArrowRight:
            vncCanvas.sendMetaKey(MetaKeyBean.keyArrowRight);
            return true;
        case R.id.itemArrowDown:
            vncCanvas.sendMetaKey(MetaKeyBean.keyArrowDown);
            return true;
*/
        case R.id.itemSendKeyAgain:
            sendSpecialKeyAgain();
            return true;
        // Disabling Manual/Wiki Menu item as the original does not correspond to this project anymore.
        //case R.id.itemOpenDoc:
        //    Utils.showDocumentation(this);
        //    return true;
        case R.id.itemExtraKeys:
            if (connection.getExtraKeysToggleType() == Constants.EXTRA_KEYS_ON) {
                connection.setExtraKeysToggleType(Constants.EXTRA_KEYS_OFF);
                item.setTitle(R.string.extra_keys_enable);
                setExtraKeysVisibility(View.GONE, false);
            } else {
                connection.setExtraKeysToggleType(Constants.EXTRA_KEYS_ON);
                item.setTitle(R.string.extra_keys_disable);
                setExtraKeysVisibility(View.VISIBLE, false);
                extraKeysHidden = false;
            }
            setKeyStowDrawableAndVisibility();
            connection.save(database.getWritableDatabase());
            database.close();
            return true;
        case R.id.itemHelpInputMode:
            showDialog(R.id.itemHelpInputMode);
            return true;
        default:
            AbstractInputHandler input = getInputHandlerById(item.getItemId());
            if (input != null) {
                inputHandler = input;
                connection.setInputMode(input.getName());
                if (input.getName().equals(SimulatedTouchpadInputHandler.TOUCHPAD_MODE)) {
                    connection.setFollowMouse(true);
                    connection.setFollowPan(true);
                } else {
                    connection.setFollowMouse(false);
                    connection.setFollowPan(false);
                }

                item.setChecked(true);
                showPanningState(true);
                connection.save(database.getWritableDatabase());
                database.close();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private MetaKeyBean lastSentKey;

    private void sendSpecialKeyAgain() {
        if (lastSentKey == null
                || lastSentKey.get_Id() != connection.getLastMetaKeyId()) {
            ArrayList<MetaKeyBean> keys = new ArrayList<MetaKeyBean>();
            Cursor c = database.getReadableDatabase().rawQuery(
                    MessageFormat.format("SELECT * FROM {0} WHERE {1} = {2}",
                            MetaKeyBean.GEN_TABLE_NAME,
                            MetaKeyBean.GEN_FIELD__ID, connection
                                    .getLastMetaKeyId()),
                    MetaKeyDialog.EMPTY_ARGS);
            MetaKeyBean.Gen_populateFromCursor(c, keys, MetaKeyBean.NEW);
            c.close();
            if (keys.size() > 0) {
                lastSentKey = keys.get(0);
            } else {
                lastSentKey = null;
            }
        }
        if (lastSentKey != null)
            canvas.getKeyboard().sendMetaKey(lastSentKey);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mThis = null;
        if (canvas != null)
            canvas.closeConnection();
        if (database != null)
            database.close();
        canvas = null;
        connection = null;
        database = null;
        zoomer = null;
        panner = null;
        clearInputHandlers();
        inputHandler = null;
        System.gc();
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent evt) {

        boolean consumed = false;
        if (keyCode == KeyEvent.KEYCODE_BACK && chat_layout.getVisibility() == View.VISIBLE) {
        
        	chat_layout.setVisibility(View.GONE);
        	return false;
        }
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (evt.getAction() == KeyEvent.ACTION_DOWN)
                return super.onKeyDown(keyCode, evt);
            else
                return super.onKeyUp(keyCode, evt);
        }

        try {
            if (evt.getAction() == KeyEvent.ACTION_DOWN || evt.getAction() == KeyEvent.ACTION_MULTIPLE) {
                consumed = inputHandler.onKeyDown(keyCode, evt);
            } else if (evt.getAction() == KeyEvent.ACTION_UP){
                consumed = inputHandler.onKeyUp(keyCode, evt);
            }
            
            resetOnScreenKeys (keyCode);
        } catch (NullPointerException e) { }

        return consumed;
    }

	public void showPanningState(boolean showLonger) {
        if (showLonger) {
            final Toast t = Toast.makeText(this, inputHandler.getHandlerDescription(), Toast.LENGTH_LONG);
            TimerTask tt = new TimerTask () {
                @Override
                public void run() {
                    t.show();
                    try { Thread.sleep(2000); } catch (InterruptedException e) { }
                    t.show();
                }};
            new Timer ().schedule(tt, 2000);
            t.show();
        } else {
            Toast t = Toast.makeText(this, inputHandler.getHandlerDescription(), Toast.LENGTH_SHORT);
            t.show();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onTrackballEvent(android.view.MotionEvent)
     */
    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        try {
            // If we are using the Dpad as arrow keys, don't send the event to the inputHandler.
            if (connection.getUseDpadAsArrows())
                return false;
            return inputHandler.onTrackballEvent(event);
        } catch (NullPointerException e) { }
        return super.onTrackballEvent(event);
    }

    // Send touch events or mouse events like button clicks to be handled.
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            return inputHandler.onTouchEvent(event);
        } catch (NullPointerException e) { }
        return super.onTouchEvent(event);
    }

    // Send e.g. mouse events like hover and scroll to be handled.
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        // Ignore TOOL_TYPE_FINGER events that come from the touchscreen with HOVER type action
        // which cause pointer jumping trouble in simulated touchpad for some devices.
        int a = event.getAction();
        if (! ( (a == MotionEvent.ACTION_HOVER_ENTER ||
                 a == MotionEvent.ACTION_HOVER_EXIT  ||
                 a == MotionEvent.ACTION_HOVER_MOVE) &&
                event.getSource() == InputDevice.SOURCE_TOUCHSCREEN &&
                event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER
               ) ) {
            try {
                return inputHandler.onTouchEvent(event);
            } catch (NullPointerException e) { }
        }
        return super.onGenericMotionEvent(event);
    }

    private void selectColorModel() {

        String[] choices = new String[COLORMODEL.values().length];
        int currentSelection = -1;
        for (int i = 0; i < choices.length; i++) {
            COLORMODEL cm = COLORMODEL.values()[i];
            choices[i] = cm.toString();
            if (canvas.isColorModel(cm))
                currentSelection = i;
        }

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ListView list = new ListView(this);
        list.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_checked, choices));
        list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        list.setItemChecked(currentSelection, true);
        list.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                dialog.dismiss();
                COLORMODEL cm = COLORMODEL.values()[arg2];
                canvas.setColorModel(cm);
                connection.setColorModel(cm.nameString());
                connection.save(database.getWritableDatabase());
                database.close();
                Toast.makeText(RemoteCanvasActivity.this, getString(R.string.info_update_color_model_to) + cm.toString(), Toast.LENGTH_SHORT).show();
            }
        });
        dialog.setContentView(list);
        dialog.show();
    }
    
    long hideZoomAfterMs;
    static final long ZOOM_HIDE_DELAY_MS = 2500;
    HideZoomRunnable hideZoomInstance = new HideZoomRunnable();

    public void stopPanner() {
        panner.stop ();
    }
    
    public void showZoomer(boolean force) {
        if (force || zoomer.getVisibility() != View.VISIBLE) {
            zoomer.show();
            hideZoomAfterMs = SystemClock.uptimeMillis() + ZOOM_HIDE_DELAY_MS;
            canvas.handler.postAtTime(hideZoomInstance, hideZoomAfterMs + 10);
        }
    }

    private class HideZoomRunnable implements Runnable {
        public void run() {
            if (SystemClock.uptimeMillis() >= hideZoomAfterMs) {
                zoomer.hide();
            }
        }
    }
    
    public ConnectionBean getConnection() {
        return connection;
    }
    
    // Returns whether we are using D-pad/Trackball to send arrow key events.
    public boolean getUseDpadAsArrows() {
        return connection.getUseDpadAsArrows();
    }
    
    // Returns whether the D-pad should be rotated to accommodate BT keyboards paired with phones.
    public boolean getRotateDpad() {
        return connection.getRotateDpad();
    }
    
    public float getSensitivity() {
        // TODO: Make this a slider config option.
        return 2.0f;
    }
    
    public boolean getAccelerationEnabled() {
        return true;
    }

    public Database getDatabase() {
        return database;
    }

    public void setDatabase(Database database) {
        this.database = database;
    }

    public RemoteCanvas getCanvas() {
        return canvas;
    }

    public void setCanvas(RemoteCanvas vncCanvas) {
        this.canvas = vncCanvas;
    }
    
    public Panner getPanner() {
        return panner;
    }

    public void setPanner(Panner panner) {
        this.panner = panner;
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("WORKAROUND_FOR_BUG_19917_KEY", "WORKAROUND_FOR_BUG_19917_VALUE");
        super.onSaveInstanceState(outState);
    }
    
    private boolean isMasterPasswordEnabled() {
        SharedPreferences sp = getSharedPreferences(Constants.generalSettingsTag, Context.MODE_PRIVATE);
        return sp.getBoolean(Constants.masterPasswordEnabledTag, false);
    }

    private ChatListAdapter adapter;
    private ListView listView;
    private View chat_layout;
    private Button btnChatShowToggle;
    
    private ArrayList<ChatData> dataList = new ArrayList<ChatData>();
    
    private void initChatListView() {

    	listView = (ListView) findViewById(R.id.listViewChat);
    	btnChatShowToggle = (Button) findViewById(R.id.btnChatShowToggle);
    	Button btnChatClose = (Button) findViewById(R.id.btnChatClose);
    	Button btnSend = (Button) findViewById(R.id.btnSend);
    	chat_layout = findViewById(R.id.chat_layout);
    	    	
		if(adapter == null) {
			
			adapter = new ChatListAdapter(this, dataList);
		}
		
		listView.setAdapter(adapter);		
		
		btnChatShowToggle.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(TextUtils.isEmpty((CharSequence) btnChatShowToggle.getTag())) {
					chat_layout.setVisibility(View.VISIBLE);
					canvas.getKeyboard().setIsChating(true);
										
					btnChatShowToggle.setTag("toggle on");
					btnChatShowToggle.setVisibility(View.GONE);
				}
				else {
					canvas.getKeyboard().setIsChating(false);
					chat_layout.setVisibility(View.GONE);
					btnChatShowToggle.setTag("");					
				}				
			}
		});
		
		btnChatClose.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				btnChatShowToggle.setVisibility(View.VISIBLE);
				canvas.getKeyboard().setIsChating(false);
				chat_layout.setVisibility(View.GONE);
				btnChatShowToggle.setTag("");
			}
		});			
		
		btnSend.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO runsoft.com.runsupport.mqtt.MSGRECVD
				final EditText edit = new EditText(RemoteCanvasActivity.this);
			    edit.setBackgroundColor(Color.WHITE);
			    edit.setTextColor(getResources().getColor(R.color.button_text_color));

				new AlertDialog.Builder(RemoteCanvasActivity.this)
		        .setTitle("채팅메시지 입력")
		        .setView(edit)
		        .setNegativeButton("보내기",
		            new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String msg = edit.getText().toString().trim();
						
						if (TextUtils.isEmpty(msg)) {
						    Toast.makeText(mThis, "보내실 메시지를 입력해주세", Toast.LENGTH_LONG).show();
							
							return;
						}
						try {
							Intent broadcastIntent = new Intent();
							broadcastIntent.setAction("runsoft.com.runsupport.mqtt.MSGRECVD");
							broadcastIntent.putExtra("runsoft.com.runsupport.mqtt.MSGRECVD_MSGBODY", String.format(
									Locale.getDefault(), "{\"mode\":\"%s\",\"msg\":\"%s\"}", "map_find_send_msg", msg));
							sendBroadcast(broadcastIntent);
						} catch (Exception e1) {
							e1.printStackTrace();
						}

						ChatData chatData = new ChatData();
						chatData.msg = "나 : " + msg;
						chatData.date = getRegdateRelative();

						dataList.add(0, chatData);

						adapter.notifyDataSetChanged();

						InputMethodManager inputMgr = (InputMethodManager) getSystemService(
								Context.INPUT_METHOD_SERVICE);
						inputMgr.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
						
		              }
		            }).create().show();				
			}
		});
	}
    
    private long msgMills = 0;
    public void messageArrvied(String msg) {   
    	if(System.currentTimeMillis() - msgMills > 4500) {
    		Toast.makeText(this, "메시지도착 : "+msg, Toast.LENGTH_LONG).show();
    	}
    	
    	msgMills = System.currentTimeMillis();
    	
    	ChatData chatData = new ChatData();
    	chatData.msg = "고객 : "+msg;
    	chatData.date = getRegdateRelative();
    	
    	dataList.add(0, chatData);
    	
    	adapter.notifyDataSetChanged();
    }
    
    public String getRegdateRelative() {
		String label = DateUtils.getRelativeTimeSpanString(System.currentTimeMillis()).toString();
		if(label.equals("0분 전") || label.equals("0분 안에")) {
			label = "지금 막";
		}
		if(label.contains("년")) {
			return DateFormat.format("yyyy.MM.dd. aahh:mm", System.currentTimeMillis()).toString();
		}
		return label;
	}

}
 
