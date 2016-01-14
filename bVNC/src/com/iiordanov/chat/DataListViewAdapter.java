package com.iiordanov.chat;

import java.util.ArrayList;

import android.content.Context;
import android.widget.BaseAdapter;


public abstract class DataListViewAdapter<T> extends BaseAdapter {
	private Context context;
	private ArrayList<T> dataList;
	
	public DataListViewAdapter(Context context, ArrayList<T> dataList) {
		this.context = context;
		this.dataList = dataList;
	}
	
	public void setDataList(ArrayList<T> dataList) {
		if (this.dataList != null) {
			this.dataList.clear();
			this.dataList = null;
		}
		this.dataList = dataList;
	}
	
	public void clearDataList() {
		if (this.dataList != null) {
			this.dataList.clear();
			notifyDataSetChanged();
		}
	}
	
	public Context getContext() {
		return context;
	}
	
	@Override
	public int getCount() {
		return dataList.size();
	}

	@Override
	public T getItem(int position) {
		return dataList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	public ArrayList<T> getDataList() {
		return dataList;
	}
	
	public void removeAt(int index) {
		dataList.remove(index);
	}

	public void addDataList(ArrayList<T> resultList) {
		dataList.addAll(resultList);		
	}
	
}

