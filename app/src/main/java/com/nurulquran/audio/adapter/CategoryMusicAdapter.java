package com.nurulquran.audio.adapter;

import java.util.List;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.nurulquran.audio.R;
import com.nurulquran.audio.config.GlobalValue;
import com.nurulquran.audio.object.CategoryMusic;

public class CategoryMusicAdapter extends BaseAdapter {
	private List<CategoryMusic> listCategoryMusics;
	private LayoutInflater layoutInflater;
	private AQuery listAq;
	Context context;

	public CategoryMusicAdapter(Context context, List<CategoryMusic> listCategoryMusics) {
		this.context = context;
		this.listCategoryMusics = listCategoryMusics;
		layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		listAq = new AQuery(context);
	}

	public int getCount() {
		return listCategoryMusics.size();
	}

	public Object getItem(int position) {
		return position;
	}

	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = layoutInflater.inflate(R.layout.item_category_music, null);
		}

		CategoryMusic item = listCategoryMusics.get(position);
		if (item != null) {

			((TextView) convertView.findViewById(R.id.lblCategotyMusicName)).setText(item.getTitle());
			((TextView)convertView.findViewById(R.id.lblCategotyMusicName)).setSelected(true);

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				Glide.with(context)
						.load(item.getImage())
						.into((ImageView) convertView.findViewById(R.id.imgCategoryMusic));
			}else
			{AQuery aq = listAq.recycle(convertView);
				aq.id(R.id.imgCategoryMusic).image(item.getImage(), true, true, 0, R.drawable.img_not_found,
						GlobalValue.bmNoImageAvailable, AQuery.FADE_IN_NETWORK, 0);
			}
		}

		return convertView;
	}
}
