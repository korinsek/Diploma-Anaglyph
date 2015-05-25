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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Toast;

public class CameraTwoPhones extends Activity implements OnClickListener,
		SurfaceHolder.Callback, Camera.PictureCallback {
	SurfaceView viewCam;
	SurfaceHolder surfaceHolder;
	Camera camera;

	private String imePovezaneNaprave = null;
	private ArrayAdapter<String> komunikacijaAa;
	private StringBuffer osb;
	private BluetoothAdapter btAdapter = null;
	private BluetoothService btService = null;

	private boolean sendImage = false;

	ImageView iv;
	LayoutInflater inflater = null;

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

		// overlay slika cez kamero
		inflater = LayoutInflater.from(getBaseContext());
		View view = inflater.inflate(R.layout.gumb_kamera_overlay, null);
		LayoutParams layoutParamsControl = new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		this.addContentView(view, layoutParamsControl);

		iv = (ImageView) findViewById(R.id.imageView1);
		iv.setImageResource(R.drawable.capture_button);

		btAdapter = BluetoothAdapter.getDefaultAdapter();

		if (btAdapter == null) {
			Toast.makeText(this, R.string.btNiDosegljiv, Toast.LENGTH_LONG)
					.show();
			finish();
			return;
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		if (!btAdapter.isEnabled()) {
			Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(i, Konstante.REQUEST_ENABLE_BT);
		} else if (btAdapter.isEnabled() && btService == null) {
			nastaviKomunikacijoBt();
		}
	}

	@Override
	public synchronized void onResume() {
		super.onResume();
		if (btService != null && btService.getStanje() == Konstante.STATE_NONE) {
			btService.start();
		}
	}

	private void nastaviKomunikacijoBt() {
		komunikacijaAa = new ArrayAdapter<String>(this, R.layout.sporocilo);
		btService = new BluetoothService(this, mHandler);
		osb = new StringBuffer("");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (btService != null)
			btService.stop();
	}

	private void sendMessage(String message) {
		if (btService.getStanje() != Konstante.STATE_CONNECTED) {
			Toast.makeText(this, R.string.ni_povezano, Toast.LENGTH_SHORT)
					.show();
			onCreate(null);
			return;
		}

		// èe je kaj za pošiljati in èe ne gre za pošiljanje slike ali za
		// komunikacijo
		if (message.length() > 0 && !sendImage && message.equals("sending")) {
			byte[] send = message.getBytes();
			btService.write(send);
			osb.setLength(0);
			sendImage = true;
		} else if (message.length() > 0 && message.equals("imageSend")) {

			String photoPath = Environment.getExternalStorageDirectory()
					+ "/StereoScope/Temp/testCaptured.jpg";
			Bitmap bm = null;
			try {
				bm = BitmapFactory.decodeStream(new FileInputStream(new File(
						photoPath)));
			} catch (Exception e) {
				Log.e("CameraTwoPhones", "CameraTwoPhones - sendMessage: ", e);
			}
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			bm.compress(Bitmap.CompressFormat.JPEG, 40, baos);
			byte[] b = baos.toByteArray();
			btService.write(b);
			sendImage = false;
		}
	}

	private final void setStatus(int resId) {
		final ActionBar actionBar = getActionBar();
		actionBar.setSubtitle(resId);
	}

	private final void setStatus(CharSequence subTitle) {
		final ActionBar actionBar = getActionBar();
		actionBar.setSubtitle(subTitle);
	}

	@SuppressLint("HandlerLeak")
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case Konstante.MESSAGE_STATE_CHANGE:
				switch (msg.arg1) {
				case Konstante.STATE_CONNECTED:
					setStatus(getString(R.string.naslovPovezanNa,
							imePovezaneNaprave));
					komunikacijaAa.clear();
					break;
				case Konstante.STATE_CONNECTING:
					setStatus(R.string.naslovPovetujem);
					break;
				case Konstante.STATE_LISTEN:
				case Konstante.STATE_NONE:
					setStatus(R.string.naslovNiPovezave);
					break;
				}
				break;
			case Konstante.MESSAGE_WRITE:
				byte[] writeBuf = (byte[]) msg.obj;
				String writeMessage = new String(writeBuf);
				komunikacijaAa.add(writeMessage);
				break;

			case Konstante.MESSAGE_READ:
				byte[] readBuf = (byte[]) msg.obj;
				String readMessage = new String(readBuf, 0, msg.arg1);

				// ce je komunikacija beremo ime ukaza in ce je slika jo
				// preberemo
				if (readMessage.equals("sending")) {
					komunikacijaAa
							.add(imePovezaneNaprave + ":  " + readMessage);

					onClick(null);
					sendImage = true;
				} else {
					try {
						// beremo sliko iz kanala bluetootha
						Bitmap bmp = BitmapFactory.decodeByteArray(readBuf, 0,
								msg.arg1);
						saveBitmap(bmp, "testCaptured2");
						Toast.makeText(getApplicationContext(),
								R.string.slikaPrejeta, Toast.LENGTH_SHORT)
								.show();

						// za zakljucek mora biti in vrednost zadnjih dveh bytov
						// -1 in -39
						byte by1 = readBuf[msg.arg1 - 1];
						String stirjeZadnjiBajti = Integer.toHexString(by1);
						String bajt1 = stirjeZadnjiBajti.substring(
								stirjeZadnjiBajti.length() - 2,
								stirjeZadnjiBajti.length());
						String bajt2 = stirjeZadnjiBajti.substring(
								stirjeZadnjiBajti.length() - 4,
								stirjeZadnjiBajti.length() - 2);
						// èe je konec prenašanja slike združuj slike
						// if (b2 == -39 && b4 == -1) {
						if (bajt1.equalsIgnoreCase("d9")
								&& bajt2.equalsIgnoreCase("ff")) {
							// ZdruziSliki();
							Intent intent = new Intent(getApplicationContext(),
									ImageComposition.class);
							startActivity(intent);
						}
					} catch (Exception e) {
						Log.e("CameraTwoPhones", "Handler - message_read: ", e);

					}
					sendImage = false;
				}
				break;

			case Konstante.MESSAGE_DEVICE_NAME:
				imePovezaneNaprave = msg.getData().getString(
						Konstante.IME_NAPRAVE);
				Toast.makeText(getApplicationContext(),
						R.string.povezanNaNapravo + imePovezaneNaprave,
						Toast.LENGTH_SHORT).show();
				break;
			case Konstante.MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(Konstante.TOAST),
						Toast.LENGTH_SHORT).show();
				break;
			}
		}
	};

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == Konstante.REQUEST_CONNECT_DEVICE
				&& resultCode == Activity.RESULT_OK) {
			poveziNapravo(data);
		} else if (requestCode == Konstante.REQUEST_ENABLE_BT) {
			if (resultCode == Activity.RESULT_OK) {
				nastaviKomunikacijoBt();
			} else {
				Toast.makeText(this, R.string.btNiPovezanIzhod,
						Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	private void poveziNapravo(Intent data) {
		String address = data.getExtras().getString(DeviceListActivity.MAC);
		BluetoothDevice device = btAdapter.getRemoteDevice(address);
		btService.connect(device);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.camera_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent serverIntent = null;
		if (item.getItemId() == R.id.connect_scan) {
			try {
				serverIntent = new Intent(this, DeviceListActivity.class);
				startActivityForResult(serverIntent,
						Konstante.REQUEST_CONNECT_DEVICE);
				return true;
			} catch (Exception e) {
				Log.e("CameraTwoPhones",
						"-onOptionsItemSelected override method: ", e);
			}
		} else if (item.getItemId() == R.id.discoverable) {
			zagotoviVidnost();
			return true;
		}
		return false;
	}

	private void zagotoviVidnost() {
		if (btAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(
					BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}
	}

	public void onClick(View v) {
		camera.takePicture(null, null, this);
	}

	public void onPictureTaken(byte[] data, Camera camera) {
		FileOutputStream outStream = null;
		try {
			outStream = new FileOutputStream(
					Environment.getExternalStorageDirectory()
							+ "/StereoScope/Temp/testCaptured.jpg");
			// zapiše sliko ki jo posname na sd kartico
			outStream.write(data);
			outStream.flush();

		} catch (FileNotFoundException e) {
			Log.e("CameraTwoPhones", "-onPictureTaken- file not foundt: ", e);
		} catch (IOException e) {
			Log.e("CameraTwoPhones", "-onPictureTaken- IOExeption: ", e);
		} finally {
			try {
				outStream.close();
			} catch (IOException e) {
				Log.e("CameraTwoPhones",
						"onPictureTaken- finally block-> nemorem zapreti fileOutputStrema: ",
						e);
			}
		}

		try {
			if (!sendImage) {
				// komunikacija (ukaz za zajemanje)
				String message = "sending";
				sendMessage(message);
			} else {
				String message = "imageSend";
				sendMessage(message);

			}

		} catch (Exception e) {
			Log.e("CameraTwoPhones",
					"onPictureTaken- nemorem poslati ukaza ali slike: ", e);
		}

	}

	public void ZdruziSliki() {
		// Vzame levo in desno sliko in ju shrani v Bitmap
		String photoPath = Environment.getExternalStorageDirectory()
				+ "/StereoScope/Temp/testCaptured.jpg";
		final Bitmap b1 = BitmapFactory.decodeFile(photoPath);
		String photoPath1 = Environment.getExternalStorageDirectory()
				+ "/StereoScope/Temp/testCaptured2.jpg";
		final Bitmap b2 = BitmapFactory.decodeFile(photoPath1);

		// dekodira sliki po kanalih
		Bitmap b3 = doColorFilter(b1, 1, 0, 0);
		saveBitmap(b3, "pics1");
		Bitmap b4 = doColorFilter(b2, 0, 1, 1);
		saveBitmap(b4, "pics2");

		// združi sliki v eno sliko
		Bitmap bmp3 = overlay(b4, b3); // levo sliko nastavimo rdec kanal in
										// overly na desno sliko
		saveBitmap(bmp3, "resultImage");
	}

	public static Bitmap overlay(Bitmap bm1, Bitmap bm2) {
		Bitmap newBitmap = null;

		try {
			// vzame WIDTH najširše slike ter HEIGHT najvišje slike
			// int w = bm1.getWidth() >= bm2.getWidth() ? bm1.getWidth():
			// bm2.getWidth();
			int w;
			if (bm1.getWidth() >= bm2.getWidth()) {
				w = bm1.getWidth();
			} else {
				w = bm2.getWidth();
			}

			int h;
			if (bm1.getHeight() >= bm2.getHeight()) {
				h = bm1.getHeight();
			} else {
				h = bm2.getHeight();
			}

			Config config = bm1.getConfig();
			if (config == null) {
				config = Bitmap.Config.ARGB_8888;
			}

			// nastavimo novi Bitmap ter ga dodamo v canvas kjer bomo naredili
			// overlay nad bitmap sliko
			newBitmap = Bitmap.createBitmap(w, h, config);
			Canvas newCanvas = new Canvas(newBitmap);

			// izrisemo levo sliko
			newCanvas.drawBitmap(bm1, 0, 0, null);

			// izrisemo desno sliko z alfa kanalom (transparenco)
			Paint paint = new Paint();
			paint.setAlpha(128); // 128 je polovica od 0...255 in naredi 50%
									// transparenco
			newCanvas.drawBitmap(bm2, 0, 0, paint);

		} catch (Exception e) {
			Log.e("CameraTwoPhones", "-overlay: ", e);
		}
		return newBitmap;
	}

	// nastavi barvne filter KANALE
	public static Bitmap doColorFilter(Bitmap src, double red, double green,
			double blue) {
		int sirina = src.getWidth();
		int visina = src.getHeight();
		Bitmap bm = Bitmap.createBitmap(sirina, visina, src.getConfig());

		int piksel;
		int alpha, rdeca, zelena, modra;

		for (int x = 0; x < sirina; ++x) {
			for (int y = 0; y < visina; ++y) {
				// barva piksla
				piksel = src.getPixel(x, y);
				// filtriranje na vsakem kanalu (rdecem, zelenem, modrem)
				alpha = Color.alpha(piksel);
				rdeca = (int) (Color.red(piksel) * red);
				zelena = (int) (Color.green(piksel) * green);
				modra = (int) (Color.blue(piksel) * blue);

				bm.setPixel(x, y, Color.argb(alpha, rdeca, zelena, modra));
			}
		}
		return bm;
	}

	// shranimo Bitmap na sd kartico
	public void saveBitmap(Bitmap bm, String fileName) {
		try {
			String mFilePath = Environment.getExternalStorageDirectory()
					+ "/StereoScope/Temp/" + fileName + ".jpg";
			FileOutputStream stream = new FileOutputStream(mFilePath);
			bm.compress(CompressFormat.JPEG, 40, stream);
			stream.flush();
			stream.close();

		} catch (Exception e) {
			Log.e("Could not save", e.getMessage());
		}
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		camera.startPreview();
	}

	public void surfaceCreated(SurfaceHolder holder) {
		camera = Camera.open();
		try {
			camera.setPreviewDisplay(holder);
			Camera.Parameters parameters = camera.getParameters();
			// List<Camera.Size> l = parameters.getSupportedPictureSizes();

			/*
			 * int minw=Integer.MAX_VALUE; int minWidth=0; int minHeight=0;
			 * for(Camera.Size s: l){ int width = s.width; if(minw>width){
			 * minw=width; minWidth = s.width; minHeight = s.height; } }
			 */

			// TODO spremeni parametre na min in ne na fixed vrednost
			parameters.setPictureSize(1280, 960);
			parameters.setPictureSize(1280, 960);
			if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
				parameters.set("orientation", "portrait");
				camera.setDisplayOrientation(90);
			}
			/*
			 * List<String> colorEffects =
			 * parameters.getSupportedColorEffects(); Iterator<String> cei =
			 * colorEffects.iterator(); while (cei.hasNext()) { String
			 * currentEffect = cei.next(); if
			 * (currentEffect.equals(Camera.Parameters.EFFECT_SOLARIZE)) {
			 * parameters .setColorEffect(Camera.Parameters.EFFECT_SOLARIZE);
			 * break; } }
			 */
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
