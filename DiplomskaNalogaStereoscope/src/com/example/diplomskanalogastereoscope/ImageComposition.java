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

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

public class ImageComposition extends Activity {

	Button bt;
	ToggleButton btAlowMoving;
	ToggleButton btAlowCrop;
	Boolean alowMoving = false;
	Boolean alowCrop = false;
	Paint paint = new Paint();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.urejevalnik_slik);
		final DrawingView dv = new DrawingView(this);
		LinearLayout layout1 = (LinearLayout) findViewById(R.id.linearContainer);
		layout1.addView(dv);

		bt = (Button) findViewById(R.id.bt);
		bt.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub

				dv.saveCache();
			}
		});

		btAlowMoving = (ToggleButton) findViewById(R.id.alowMoving);
		btAlowMoving
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						if (isChecked) {
							alowMoving = true;
							if (btAlowCrop.isChecked()) {
								btAlowCrop.setChecked(false);
								alowCrop = false;
							}
						} else {
							alowMoving = false;
						}

					}
				});

		btAlowCrop = (ToggleButton) findViewById(R.id.alowCrop);
		btAlowCrop
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						if (isChecked) {
							alowCrop = true;
							if (btAlowMoving.isChecked()) {
								btAlowMoving.setChecked(false);
								alowMoving = false;

							}
							dv.izris();
						} else {
							alowCrop = false;
							dv.izris();
						}

					}
				});
	}

	static public void notifyMediaScannerService(Context context, String path) {
		MediaScannerConnection.scanFile(context, new String[] { path }, null,
				new MediaScannerConnection.OnScanCompletedListener() {
					public void onScanCompleted(String path, Uri uri) {
						Log.i("ExternalStorage", "Scanned " + path + ":");
						Log.i("ExternalStorage", "-> uri=" + uri);
					}
				});
	}

	class DrawingView extends View {
		Bitmap bitmap;
		Bitmap bitmap2;
		float x, y;
		float xPrej, yPrej;
		Rect cropRect = new Rect(50, 50, 500, 500);

		public DrawingView(Context context) {
			super(context);
			// bitmap = BitmapFactory.decodeResource(context.getResources(),
			// R.drawable.ic_launcher);
			String photoPath = Environment.getExternalStorageDirectory()
					+ "/StereoScope/Temp/testCaptured.jpg"; // "1-left.jpg";
			bitmap = BitmapFactory.decodeFile(photoPath);

			// scale bitmap to screen demensions
			WindowManager wm = (WindowManager) context
					.getSystemService(Context.WINDOW_SERVICE);
			Display display = wm.getDefaultDisplay();
			@SuppressWarnings("deprecation")
			int width = display.getWidth();
			@SuppressWarnings("deprecation")
			int height = display.getHeight();

			bitmap = Bitmap.createScaledBitmap(bitmap, height - height / 8,
					width, false);

			// zarotiramo bitmap ( narobe so obrnjeni)
			bitmap = RotateBitmap(bitmap, 90);

			bitmap = doColorFilter(bitmap, 1, 0, 0);

			String photoPath1 = Environment.getExternalStorageDirectory()
					+ "/StereoScope/Temp/testCaptured2.jpg";// "/2-right.jpg";
			bitmap2 = BitmapFactory.decodeFile(photoPath1);
			bitmap2 = Bitmap.createScaledBitmap(bitmap2, height - height / 8,
					width, false);
			// zarotiramo bitmap ( narobe so obrnjeni)
			bitmap2 = RotateBitmap(bitmap2, 90);
			bitmap2 = doColorFilter(bitmap2, 0, 1, 1);

			setDrawingCacheEnabled(true);
		}

		public Bitmap RotateBitmap(Bitmap source, float angle) {
			Matrix matrix = new Matrix();
			matrix.postRotate(angle);
			return Bitmap.createBitmap(source, 0, 0, source.getWidth(),
					source.getHeight(), matrix, true);
		}

		// shranimo Bitmap na sd kartico
		public void saveBitmap(Bitmap bm, String fileName) {
			try {
				String mFilePath = Environment.getExternalStorageDirectory()
						+ "/StereoScope/Slike/" + fileName + ".jpg";
				FileOutputStream stream = new FileOutputStream(mFilePath);
				bm.compress(CompressFormat.JPEG, 40, stream);
				stream.flush();
				stream.close();
				notifyMediaScannerService(getApplicationContext(),
						Environment.getExternalStorageDirectory()
								+ "/StereoScope/Slike/" + fileName + ".jpg");
			} catch (Exception e) {
				Log.e("Could not save", e.getMessage());
			}
		}

		public boolean onTouchEvent(MotionEvent event) {
			if (alowMoving) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					break;

				case MotionEvent.ACTION_MOVE:

					x = (int) event.getX();
					y = (int) event.getY();
					invalidate();
					xPrej = x;
					yPrej = y;
					break;
				case MotionEvent.ACTION_UP:
					x = (int) event.getX();
					y = (int) event.getY();
					invalidate();
					xPrej = x;
					yPrej = y;
					break;
				}
			}

			if (alowCrop) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					break;

				case MotionEvent.ACTION_MOVE:
					if (Math.sqrt((cropRect.left - (int) event.getX())
							* (cropRect.left - (int) event.getX())
							+ (cropRect.top - (int) event.getY())
							* (cropRect.top - (int) event.getY())) < (40)) {
						cropRect.left = (int) event.getX();
						cropRect.top = (int) event.getY();
					} else if (Math.sqrt((cropRect.right - (int) event.getX())
							* (cropRect.right - (int) event.getX())
							+ (cropRect.top - (int) event.getY())
							* (cropRect.top - (int) event.getY())) < (40)) {
						cropRect.right = (int) event.getX();
						cropRect.top = (int) event.getY();
					} else if (Math.sqrt((cropRect.right - (int) event.getX())
							* (cropRect.right - (int) event.getX())
							+ (cropRect.bottom - (int) event.getY())
							* (cropRect.bottom - (int) event.getY())) < (40)) {
						cropRect.right = (int) event.getX();
						cropRect.bottom = (int) event.getY();
					} else if (Math.sqrt((cropRect.left - (int) event.getX())
							* (cropRect.left - (int) event.getX())
							+ (cropRect.bottom - (int) event.getY())
							* (cropRect.bottom - (int) event.getY())) < (40)) {
						cropRect.left = (int) event.getX();
						cropRect.bottom = (int) event.getY();
					}
					invalidate();

					break;
				case MotionEvent.ACTION_UP:
					if (Math.sqrt((cropRect.left - (int) event.getX())
							* (cropRect.left - (int) event.getX())
							+ (cropRect.top - (int) event.getY())
							* (cropRect.top - (int) event.getY())) < (40)) {
						cropRect.left = (int) event.getX();
						cropRect.top = (int) event.getY();
					} else if (Math.sqrt((cropRect.right - (int) event.getX())
							* (cropRect.right - (int) event.getX())
							+ (cropRect.top - (int) event.getY())
							* (cropRect.top - (int) event.getY())) < (40)) {
						cropRect.right = (int) event.getX();
						cropRect.top = (int) event.getY();
					} else if (Math.sqrt((cropRect.right - (int) event.getX())
							* (cropRect.right - (int) event.getX())
							+ (cropRect.bottom - (int) event.getY())
							* (cropRect.bottom - (int) event.getY())) < (40)) {
						cropRect.right = (int) event.getX();
						cropRect.bottom = (int) event.getY();
					} else if (Math.sqrt((cropRect.left - (int) event.getX())
							* (cropRect.left - (int) event.getX())
							+ (cropRect.bottom - (int) event.getY())
							* (cropRect.bottom - (int) event.getY())) < (40)) {
						cropRect.left = (int) event.getX();
						cropRect.bottom = (int) event.getY();
					}
					invalidate();

					break;
				}
			}
			return true;
		}

		public final void izris() {
			invalidate();
		}

		@SuppressLint("SimpleDateFormat")
		public final void saveCache() {
			x = xPrej;
			y = yPrej;
			try {
				SimpleDateFormat sdf = new SimpleDateFormat(
						"dd-MM-yyyy_HH:mm:ss");
				String currentDateandTime = sdf.format(new Date());

				getDrawingCache().compress(
						Bitmap.CompressFormat.JPEG,
						100,
						new FileOutputStream(new File(Environment
								.getExternalStorageDirectory()
								+ "/StereoScope/Slike/"
								+ currentDateandTime
								+ ".jpg")));

				notifyMediaScannerService(getApplicationContext(),
						Environment.getExternalStorageDirectory()
								+ "/StereoScope/Slike/" + currentDateandTime
								+ ".jpg");
				// crop èe je dovoljen
				if (alowCrop) {
					String photoPath = Environment
							.getExternalStorageDirectory()
							+ "/StereoScope/Slike/"
							+ currentDateandTime
							+ ".jpg";
					bitmap = BitmapFactory.decodeFile(photoPath);

					Bitmap newBm = Bitmap.createBitmap(bitmap, cropRect.left,
							cropRect.top, cropRect.right - cropRect.left,
							cropRect.bottom - cropRect.top);
					saveBitmap(newBm, currentDateandTime + "");
				}
				Toast.makeText(getContext(), R.string.shranjeno,
						Toast.LENGTH_SHORT).show();

				// ko shranimo sliko nam odpre gallerijo
				Intent intent = new Intent(getApplicationContext(),
						GalleryActivity.class);
				startActivity(intent);

			} catch (Exception e) {
				Log.e("Error--------->", e.toString());
			}
		}

		@Override
		public void onDraw(Canvas canvas) {

			paint.setStyle(Paint.Style.FILL);
			paint.setColor(Color.BLACK);
			paint.setAlpha(224);
			canvas.drawBitmap(bitmap, 0, 0, paint); // originally bitmap draw at
													// x=o and y=0
			paint.setAlpha(128);

			if (x != 0 && y != 0) {
				canvas.drawBitmap(bitmap2, x - bitmap2.getWidth() / 2, y
						- bitmap2.getHeight() / 2, paint);
			} else {
				canvas.drawBitmap(bitmap2, -64, 0, paint);
			}

			if (alowCrop) {
				// crop rect
				paint.setStyle(Paint.Style.STROKE);
				paint.setColor(Color.WHITE);
				paint.setStrokeWidth(1);
				canvas.drawRect(cropRect, paint);

				paint.setStyle(Paint.Style.FILL_AND_STROKE);
				paint.setColor(Color.WHITE);
				paint.setStrokeWidth(10);

				canvas.drawLine(cropRect.left - 5, cropRect.top,
						cropRect.left + 20, cropRect.top, paint);
				canvas.drawLine(cropRect.left, cropRect.top - 5, cropRect.left,
						cropRect.top + 20, paint);

				canvas.drawLine(cropRect.left - 5, cropRect.bottom,
						cropRect.left + 20, cropRect.bottom, paint);
				canvas.drawLine(cropRect.left, cropRect.bottom + 5,
						cropRect.left, cropRect.bottom - 20, paint);

				canvas.drawLine(cropRect.right + 5, cropRect.top,
						cropRect.right - 20, cropRect.top, paint);
				canvas.drawLine(cropRect.right, cropRect.top - 5,
						cropRect.right, cropRect.top + 20, paint);

				canvas.drawLine(cropRect.right + 5, cropRect.bottom,
						cropRect.right - 20, cropRect.bottom, paint);
				canvas.drawLine(cropRect.right, cropRect.bottom + 5,
						cropRect.right, cropRect.bottom - 20, paint);
			}
		}

		// nastavi barcne filter KANALE
		public Bitmap doColorFilter(Bitmap src, double red, double green,
				double blue) {
			// image size
			int width = src.getWidth();
			int height = src.getHeight();
			// create output bitmap
			Bitmap bmOut = Bitmap.createBitmap(width, height, src.getConfig());
			// color information
			int A, R, G, B;
			int pixel;

			// scan through all pixels
			for (int x = 0; x < width; ++x) {
				for (int y = 0; y < height; ++y) {
					// get pixel color
					pixel = src.getPixel(x, y);
					// apply filtering on each channel R, G, B
					A = Color.alpha(pixel);
					R = (int) (Color.red(pixel) * red);
					G = (int) (Color.green(pixel) * green);
					B = (int) (Color.blue(pixel) * blue);
					// set new color pixel to output bitmap
					bmOut.setPixel(x, y, Color.argb(A, R, G, B));
				}
			}

			// return final image
			return bmOut;
		}

	}
}
