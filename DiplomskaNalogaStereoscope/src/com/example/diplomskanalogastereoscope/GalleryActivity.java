/*
 * Copyright (c) 2014, Denis Korinšek - korinsekdenis@gmail.com
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.example.diplomskanalogastereoscope;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;

public class GalleryActivity extends Activity {
	private int stSlik;

	private ImgAdapter imageAdapter;
	private ArrayList<String> potiSlik;
	private ArrayList<Bitmap> slike;

	Button refreshBt;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.galerija);

		final String[] stolpci = { MediaStore.Images.Media.DATA,
				MediaStore.Images.Media._ID };

		// pot do slik like string je za mapo
		Cursor kazalecNaSlike = getContentResolver().query(
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI, stolpci,
				MediaStore.Images.Media.DATA + " like ? ",
				new String[] { "%/StereoScope/Slike/%" }, null);

		// branje slik in dodeljevanje v stolpce
		int stolpecSlike = kazalecNaSlike
				.getColumnIndex(MediaStore.Images.Media._ID);
		this.stSlik = kazalecNaSlike.getCount();
		this.slike = new ArrayList<Bitmap>(this.stSlik);
		this.potiSlik = new ArrayList<String>(this.stSlik);

		if (kazalecNaSlike.moveToFirst()) {
			int i = 0;
			do {
				int id = kazalecNaSlike.getInt(stolpecSlike);
				int dataColumnIndex = kazalecNaSlike
						.getColumnIndex(MediaStore.Images.Media.DATA);
				slike.add(i, MediaStore.Images.Thumbnails.getThumbnail(
						getApplicationContext().getContentResolver(), id,
						MediaStore.Images.Thumbnails.MINI_KIND, null));
				this.potiSlik.add(i, kazalecNaSlike.getString(dataColumnIndex));
				i++;
			} while (kazalecNaSlike.moveToNext());
		}

		GridView imagegrid = (GridView) findViewById(R.id.PhoneImageGrid);
		imageAdapter = new ImgAdapter();

		imagegrid.setAdapter(imageAdapter);
		kazalecNaSlike.close();

		refreshBt = (Button) findViewById(R.id.refreshBt);
		refreshBt.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
				slike = null;

				potiSlik = null;
				imageAdapter = null;

				Intent intent1 = new Intent(getApplicationContext(),
						GalleryActivity.class);
				startActivity(intent1);
			}
		});
	}

	@Override
	public void onBackPressed() {
		imageAdapter = null;
		stSlik = 0;
		slike = null;
		potiSlik = null;
		finish();
		Intent intent = new Intent(getApplicationContext(), StartActivity.class);
		startActivity(intent);
	}

	public class ImgAdapter extends BaseAdapter {
		private LayoutInflater layoutInflater;

		public ImgAdapter() {
			layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		public View getView(int position, View view, ViewGroup parent) {
			ImageView imageview;

			view = layoutInflater.inflate(R.layout.slika_galerije, null);
			imageview = (ImageView) view.findViewById(R.id.thumbImage);

			// view.setTag(imageview);

			imageview.setId(position);
			imageview.setOnClickListener(new OnClickListener() {

				public void onClick(View v) {
					Intent intent = new Intent();
					intent.setAction(Intent.ACTION_VIEW);
					intent.setDataAndType(
							Uri.parse("file://" + potiSlik.get(v.getId())),
							"image/*");
					startActivity(intent);
				}
			});

			Bitmap bm = slike.get(position);
			imageview.setImageBitmap(bm);

			return view;
		}

		public int getCount() {
			return stSlik;
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return 0;
		}
	}

}