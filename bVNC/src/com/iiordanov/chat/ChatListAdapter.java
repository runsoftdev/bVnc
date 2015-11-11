package com.iiordanov.chat;

import java.util.ArrayList;

import com.iiordanov.bVNC.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

public class ChatListAdapter extends DataListViewAdapter<ChatData> {
	private static final String TAG = ChatListAdapter.class.getSimpleName();
	private OnItemClickListener onItemClickListener;
		
	public ChatListAdapter(Context context, ArrayList<ChatData> dataList) {
		super(context, dataList);	
	}
			
	@Override
	public int getCount() {
		return super.getCount();
	}
		
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		ChatData board = getItem(position);
//		LogUtil.d(TAG, String.format("[%d] size = %d , name = %s", getCount(), position, board.getContents()));
		
		ViewHolder itemViewHolder;
		
		if (convertView == null) {
			convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_list_row, null);
			convertView.setOnClickListener(btnClickListener);

			itemViewHolder = new ViewHolder();
			itemViewHolder.position = position;
			
			itemViewHolder.textViewChatMsg = (TextView) convertView.findViewById(R.id.textViewChatMsg);
			itemViewHolder.textViewRecvDate = (TextView) convertView.findViewById(R.id.textViewDate);
		      
			convertView.setTag(itemViewHolder);
		}
		else {
			itemViewHolder = (ViewHolder)convertView.getTag();
			itemViewHolder.position = position;
		}		
		
		itemViewHolder.textViewChatMsg.setText(board.msg);
		itemViewHolder.textViewRecvDate.setText(board.date);		
		
		return convertView;
	}
			
	public void setOnItemViewClickListener(OnItemClickListener onItemClickListener) {
		this.onItemClickListener = onItemClickListener;
	}
	
	private OnClickListener btnClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {			
			ViewHolder itemViewHolder = (ViewHolder)v.getTag();
						
						
			if (onItemClickListener != null) {
				onItemClickListener.onItemClick(null, v, itemViewHolder.position, getItemId(itemViewHolder.position));
			}
		}
	};		
}
