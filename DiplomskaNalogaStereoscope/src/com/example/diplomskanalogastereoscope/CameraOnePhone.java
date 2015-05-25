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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

public class CameraOnePhone extends Activity implements OnClickListener,
		SurfaceHolder.Callback, Camera.PictureCallback {
	SurfaceView viewCam;
	SurfaceHolder surfaceHolder;
	Camera camera;
	LayoutInflater inflater = null;

	int counter = 0; // stevec posnetih fotografij
	ImageView iv;

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.kamera);
		viewCam = (SurfaceView) this.findViewById(R.id.CameraView);
		surfaceHolder = viewCam.getHolder();
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		surfaceHolder.addCallback(this);

		viewCam.setFocusable(true);
		viewCam.setFocusableInTouchMode(true);
		viewCam.setClickable(true);
		viewCam.setOnClickListener(this);

		inflater = LayoutInflater.from(getBaseContext());
		View view = inflater.inflate(R.layout.prikaz_prvega_posnetka_overlay,
				null);
		LayoutParams layoutParamsControl = new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		this.addContentView(view, layoutParamsControl);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.camera_main, menu);
		return true;
	}

	public void onClick(View v) {
		camera.takePicture(null, null, this);
	}

	public void onPictureTaken(byte[] data, Camera camera) {
		// Metoda se klièe ko je slika posneta in naredimo operacije nad sliko
		// oziroma jo shranimo
		FileOutputStream outStream = null;
		try {
			if (counter == 0) {
				outStream = new FileOutputStream(
						Environment.getExternalStorageDirectory()
								+ "/StereoScope/Temp/testCaptured.jpg");
				// zapiše sliko ki jo posname na sd kartico
				outStream.write(data);
				outStream.flush();
				outStream.close();

				// preberi sliko bitmap
				String photoPath = Environment.getExternalStorageDirectory()
						+ "/StereoScope/Temp/testCaptured.jpg";
				Bitmap bm = null;
				try {
					bm = BitmapFactory.decodeStream(new FileInputStream(
							new File(photoPath)));
					bm = obrniBitmap(bm, 90);
					bm = narediProsojno(bm, 128);
				} catch (Exception e) {
					// TODO: handle exception
				}
				iv = (ImageView) findViewById(R.id.imageView1);
				iv.setImageBitmap(bm);

				inflater = LayoutInflater.from(getBaseContext());
				View view = inflater.inflate(
						R.layout.prikaz_prvega_posnetka_overlay, null);
				LayoutParams layoutParamsControl = new LayoutParams(1080, 1920);
				this.addContentView(view, layoutParamsControl);
				camera.startPreview();
			} else if (counter == 1) {
				outStream = new FileOutputStream(
						Environment.getExternalStorageDirectory()
								+ "/StereoScope/Temp/testCaptured2.jpg");
				// zapiše sliko ki jo posname na sd kartico
				outStream.write(data);
				outStream.flush();
				outStream.close();
				finish();
				// zažene aktivnost za poravnavo slik
				Intent intent = new Intent(getApplicationContext(),
						ImageComposition.class);
				startActivity(intent);
				counter = 0;
			}
			counter++;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Bitmap narediProsojno(Bitmap src, int value) {
		Bitmap prosojno = Bitmap.createBitmap(src.getWidth(), src.getHeight(),
				Config.ARGB_8888);
		Canvas canvas = new Canvas(prosojno);
		canvas.drawARGB(0, 0, 0, 0);
		final Paint paint = new Paint();
		paint.setAlpha(value);
		canvas.drawBitmap(src, 0, 0, paint);
		return prosojno;
	}

	public Bitmap obrniBitmap(Bitmap source, float angle) {
		Matrix matrix = new Matrix();
		matrix.postRotate(angle);
		return Bitmap.createBitmap(source, 0, 0, source.getWidth(),
				source.getHeight(), matrix, true);
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		camera.startPreview();
	}

	public void surfaceCreated(SurfaceHolder holder) {
		camera = Camera.open();
		try {
			camera.setPreviewDisplay(holder);
			Camera.Parameters parameters = camera.getParameters();
			List<Camera.Size> l = parameters.getSupportedPictureSizes();

			/*
			 * int minw=Integer.MAX_VALUE; int minWidth=0; int minHeight=0;
			 * for(Camera.Size s: l){ int width = s.width; if(minw>width){
			 * minw=width; minWidth = s.width; minHeight = s.height; } }
			 */
			int maxw = Integer.MIN_VALUE;
			int maxWidth = 0;
			// int maxHeight = 0;
			for (Camera.Size s : l) {
				int width = s.width;
				if (maxw < width) {
					maxw = width;
					maxWidth = s.width;
					// maxHeight = s.height;

				}
			}

			// TODO spremeni parametre na min in ne na fixed vrednost
			if (maxWidth <= 2560) {
				parameters.setPictureSize(1280, 960);
			}

			if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
				parameters.set("orientation", "portrait");
				camera.setDisplayOrientation(90);
			}
			camera.setParameters(parameters);
		} catch (IOException exception) {
			camera.release();
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		camera.stopPreview();
		camera.release();
	}

}
