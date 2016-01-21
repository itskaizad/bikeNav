package com.beproj.bikenav;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class BikeRouteAdapter extends BaseAdapter {

	List<RouteStep> names;
	Context ctxt;
	LayoutInflater myInflater;

	public BikeRouteAdapter(List<RouteStep> list, Context ct) {
		names = list;
		ctxt = ct;
		myInflater = (LayoutInflater) ct
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return names.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return names.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		if (convertView == null)
			convertView = myInflater.inflate(R.layout.itenaryitem, parent,
					false);
		TextView title = (TextView) convertView.findViewById(R.id.description);
		title.setText(names.get(position).getInstructions());
		TextView numbers = (TextView) convertView.findViewById(R.id.duration);
		numbers.setText(names.get(position).getDistance() + ", "+names.get(position).getDuration());
		ImageView img = (ImageView)convertView.findViewById(R.id.imageView1);
		if (names.get(position).getManeuver().equals("turn-left"))
			img.setImageResource(R.drawable.left);
		if (names.get(position).getManeuver().contains("turn-right"))
			img.setImageResource(R.drawable.right);

		return convertView;
	}

}
