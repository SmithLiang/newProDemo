package com.sclf.qkj.voicerecoder;

import java.util.List;

import com.sclf.qkj.voicerecoder.MainActivity.Recorder;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.TextView;
/**
 * 
 * @ClassName:  RecorderAdapter   
 * @Description:list的适配器 
 * @author:  张  维
 * @date:   2016-5-23 下午4:04:02   
 *
 */
public class RecorderAdapter extends ArrayAdapter<Recorder> {


	private LayoutInflater inflater;

	private int mMinItemWith;// 设置对话框的最大宽度和最小宽度
	private int mMaxItemWith;

	public RecorderAdapter(Context context, List<Recorder> dataList) {
		super(context, -1, dataList);
		
		inflater = LayoutInflater.from(context);

		// 获取系统宽度
		WindowManager wManager = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics outMetrics = new DisplayMetrics();
		wManager.getDefaultDisplay().getMetrics(outMetrics);
		mMaxItemWith = (int) (outMetrics.widthPixels * 0.7f);
		mMinItemWith = (int) (outMetrics.widthPixels * 0.15f);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder = null;
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.item_layout, parent, false);
			
			viewHolder=new ViewHolder();
			viewHolder.seconds=(TextView) convertView.findViewById(R.id.recorder_time);
			viewHolder.length=convertView.findViewById(R.id.recorder_length);
			
			convertView.setTag(viewHolder);
		}else {
			viewHolder=(ViewHolder) convertView.getTag();
		}
		
		
		viewHolder.seconds.setText(Math.round(getItem(position).time)+"\"");
		ViewGroup.LayoutParams lParams=viewHolder.length.getLayoutParams();
		lParams.width=(int) (mMinItemWith+mMaxItemWith/60f*getItem(position).time);
		viewHolder.length.setLayoutParams(lParams);
		
		return convertView;
	}

	class ViewHolder {
		TextView seconds;// 时间
		View length;// 对话框长度
	}

}
