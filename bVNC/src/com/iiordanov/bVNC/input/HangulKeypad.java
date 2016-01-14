package com.iiordanov.bVNC.input;

import com.iiordanov.bVNC.R;
import com.iiordanov.bVNC.RemoteCanvas;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;

public class HangulKeypad extends LinearLayout implements OnClickListener {
	private RemoteCanvas mRemotecanvas;
	private OnClickListener mOnClickListener;
	
	public HangulKeypad(Context context) {
		super(context);
		init(context);
	}
	
	public HangulKeypad(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}
	
	public void setVncKeyboard(RemoteCanvas canvas) {
		this.mRemotecanvas = canvas;
	}
	
	public void setOnClickListener(OnClickListener mOnClickListener ) {
		this.mOnClickListener = mOnClickListener;
	}
		
	private void init(Context context) {
		final LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		addView(inflater.inflate(R.layout.keypad_kor, this, false));
		
		Button key = (Button) findViewById(R.id.num0);
		key.setOnClickListener(this);
		
		key = (Button) findViewById(R.id.num1);		
		key.setOnClickListener(this);
		
		key = (Button) findViewById(R.id.num2);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.num3);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.num4);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.num5);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.num6);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.num7);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.num8);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.num9);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.num0);		
		key.setOnClickListener(this);
		
		key = (Button) findViewById(R.id.key_a);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.key_b);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.key_c);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.key_d);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.key_e);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.key_f);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.key_g);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.key_h);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.key_i);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.key_j);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.key_k);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.key_l);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.key_m);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.key_n);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.key_o);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.key_p);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.key_q);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.key_r);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.key_s);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.key_t);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.key_u);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.key_v);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.key_w);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.key_x);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.key_y);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.key_z);		
		key.setOnClickListener(this);
		
		key = (Button) findViewById(R.id.key_shift);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.key_del);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.key_esc);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.key_hide);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.key_dot);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.key_space);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.key_swap);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.key_enter);		
		key.setOnClickListener(this);
		key = (Button) findViewById(R.id.key_show_eng);		
		key.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.key_hide) {
			if(mOnClickListener != null) {
				mOnClickListener.onClick(v);
			}
			return;
		}
		else if(v.getId() == R.id.key_shift) {
			toggleShift();
		}
		else if(v.getId() == R.id.key_del) {
			mRemotecanvas.getKeyboard().processLocalKeyEvent(KeyEvent.KEYCODE_DEL, new KeyEvent(KeyEvent.ACTION_DOWN,0));
		}
		else if(v.getId() == R.id.key_esc) {
			mRemotecanvas.getKeyboard().processLocalKeyEvent(KeyEvent.KEYCODE_ESCAPE, new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_ESCAPE));
			
		}
		else if(v.getId() == R.id.key_space) {
			mRemotecanvas.getKeyboard().processLocalKeyEvent(KeyEvent.KEYCODE_SPACE, new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_SPACE));
			
		}
		else if(v.getId() == R.id.key_swap) {
			mRemotecanvas.getKeyboard().processLocalKeyEvent(144, new KeyEvent(144,0));
		}
		else if(v.getId() == R.id.key_show_eng) {
			if(mOnClickListener != null) {
				mOnClickListener.onClick(v);
			}
		}
		else if(v.getId() == R.id.key_enter) {
			mRemotecanvas.getKeyboard().processLocalKeyEvent(KeyEvent.KEYCODE_ENTER, new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_ENTER));
			
		}
		else {		
			String charactoers = (String)v.getTag();
			
			KeyEvent evt = new KeyEvent(0, isShiftClicked? charactoers.toUpperCase() : charactoers, 0, 0);		
			int keyCode = 0;
			mRemotecanvas.getKeyboard().processLocalKeyEvent(keyCode, evt);
		}
	}
	
	private boolean isShiftClicked = false;
	
	private void toggleShift() {
				
		Button key = (Button) findViewById(R.id.key_shift);		
		
		key.setText(!isShiftClicked?"Shift(On)":"Shift(Off)");
		
		key = (Button) findViewById(R.id.key_q);		
		key.setText(isShiftClicked ? "ㅂ" :"ㅃ");
		
		key = (Button) findViewById(R.id.key_w);		
		key.setText(isShiftClicked ? "ㅈ" :"ㅉ");
		
		key = (Button) findViewById(R.id.key_e);		
		key.setText(isShiftClicked ? "ㄷ" :"ㄸ");
		
		key = (Button) findViewById(R.id.key_r);		
		key.setText(isShiftClicked ? "ㄱ" :"ㄲ");
		
		key = (Button) findViewById(R.id.key_t);		
		key.setText(isShiftClicked ? "ㅅ" :"ㅆ");
		
		key = (Button) findViewById(R.id.key_o);		
		key.setText(isShiftClicked ? "ㅐ" :"ㅒ");
		
		key = (Button) findViewById(R.id.key_p);		
		key.setText(isShiftClicked ? "ㅔ" :"ㅖ");
		
		isShiftClicked = !isShiftClicked;
	}
}



