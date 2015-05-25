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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BluetoothService {

	private AcceptThread nitZaSprejemanje;
	private ConnectThread nitZaPovezovanje;
	private ConnectedThread nitZaPovezanost;

	private final BluetoothAdapter btAdapter;
	private final Handler handlerBts;

	private int stanje;
	// Unikaten UUID za to aplikacijo
	private static final UUID UUIDAplikacije = UUID
			.fromString("0001101-0000-1000-8000-00805F9B34FB");

	// Ime za SDP zapis ko ustvarjamo server socket
	private static final String IME = "BluetoothService";

	// spremenljivka za debugiranje
	private static final String TAG = "BluetoothService";

	public BluetoothService(Context context, Handler handler) {
		handlerBts = handler;
		stanje = Konstante.STATE_NONE;
		btAdapter = BluetoothAdapter.getDefaultAdapter();
	}

	// Nastavi trenutno stanje povezave
	private synchronized void setState(int state) {
		// dodeli novo stanje handleju da se aktivnost lahko posodobi
		Message m = handlerBts.obtainMessage(Konstante.MESSAGE_STATE_CHANGE,
				state, -1);
		m.sendToTarget();

		stanje = state;
	}

	public synchronized void start() {
		cancelThreadAttemtping();
		cancelThreadRunning();
		setState(Konstante.STATE_LISTEN);
		startThreadListening();
	}

	// Povezava na napravo
	public synchronized void connect(BluetoothDevice naprava) {
		if (stanje == Konstante.STATE_CONNECTING) {
			cancelThreadAttemtping();
		}
		cancelThreadRunning();

		// Zaženi nit za povezavo z napravo
		nitZaPovezovanje = new ConnectThread(naprava);
		nitZaPovezovanje.start();
		setState(Konstante.STATE_CONNECTING);
	}

	public synchronized void connected(BluetoothSocket socket,
			BluetoothDevice naprava, final String socketType) {

		cancelThreadAttemtping();
		cancelThreadRunning();
		cancelAcceptThread();

		// zaženi nit za urejanje povezave in prenašanja
		nitZaPovezanost = new ConnectedThread(socket, socketType);
		nitZaPovezanost.start();

		// pošlji nazaj ime naprave
		sendMessageToActivity(Konstante.MESSAGE_DEVICE_NAME,
				Konstante.IME_NAPRAVE, naprava.getName());

		setState(Konstante.STATE_CONNECTED);
	}

	public synchronized void stop() {
		cancelThreadAttemtping();
		cancelThreadRunning();
		cancelAcceptThread();
		setState(Konstante.STATE_NONE);
	}

	public void write(byte[] out) {
		ConnectedThread r;
		// Synchronize a copy of the ConnectedThread
		synchronized (this) {
			if (stanje != Konstante.STATE_CONNECTED)
				return;
			r = nitZaPovezanost;
		}
		// Perform the write unsynchronized
		r.write(out);
	}

	private void connectionFailed() {
		sendMessageToActivity(Konstante.MESSAGE_TOAST, Konstante.TOAST,
				"Nemorem povezati naprave");
		// Ob napaki povezave pripravi ponovno za poslušanje
		startBluetooth();
	}

	private void connectionLost() {
		sendMessageToActivity(Konstante.MESSAGE_TOAST, Konstante.TOAST,
				"Povezava z napravo je izgubljena");
		startBluetooth();
	}

	// Nit za poslušanje prihajajoèih povezav. Nit tece dokler je povezava in
	// dokler ni prekinjena. Obnaša se kot prejemnih na strani serverja.
	private class AcceptThread extends Thread {
		// The local server socket
		private String localSocketType;
		private final BluetoothServerSocket localSocket;

		public AcceptThread() {
			BluetoothServerSocket tmp = null;
			try {
				tmp = btAdapter.listenUsingRfcommWithServiceRecord(IME,
						UUIDAplikacije);
			} catch (Exception e) {
				Log.e(TAG, "Metoda AcceptThread - konstruktor: ", e);
			}
			localSocket = tmp;
		}

		public void cancel() {
			try {
				localSocket.close();
			} catch (Exception e) {
				Log.e(TAG, "AcceptThread - cancel(): ", e);
			}
		}

		public void run() {

			setName("AcceptThread" + localSocketType);

			BluetoothSocket btSocketLocal = null;

			// Èe nismo povezani poslušamo serverSocketu
			while (stanje != Konstante.STATE_CONNECTED) {
				try {
					btSocketLocal = localSocket.accept();
					// prestavljeno ze
					// Ce je sprejeta povezava
					synchronized (BluetoothService.this) {
						// Iz switcha v if predelava.....
						if (stanje == Konstante.STATE_LISTEN
								|| stanje == Konstante.STATE_CONNECTING) {
							// Situation normal. Start the connected thread.
							connected(btSocketLocal,
									btSocketLocal.getRemoteDevice(),
									localSocketType);
						} else if (stanje == Konstante.STATE_NONE
								|| stanje == Konstante.STATE_CONNECTED) {
							try {
								btSocketLocal.close();
							} catch (Exception e) {
								Log.e(TAG, "AcceptThread - run():", e);
							}
						}
					}
				} catch (Exception e) {
					Log.e(TAG, "AcceptThread - run(): ", e);
					break;
				}
			}
		}
	}

	// Nit tece dokler poskusamo ustvariti izhodno povezavo z napravo.
	private class ConnectThread extends Thread {
		private String tipSocketaCt;
		private final BluetoothDevice napravaCt;
		private final BluetoothSocket socketCt;

		public ConnectThread(BluetoothDevice naprava) {
			napravaCt = naprava;
			BluetoothSocket tmp = null;

			try {
				tmp = naprava.createRfcommSocketToServiceRecord(UUIDAplikacije);
			} catch (Exception e) {
				Log.e(TAG, "ConnectThread - konstruktor:", e);
			}
			socketCt = tmp;
		}

		public void cancel() {
			try {
				socketCt.close();
			} catch (Exception e) {
				Log.e(TAG, "thred ConnectThread- method cancel() ", e);
			}
		}

		public void run() {
			setName("ConnectThread" + tipSocketaCt);

			// prekinemo iskanje naprav da pohitrimo delovanje
			btAdapter.cancelDiscovery();

			try {
				socketCt.connect();

				synchronized (BluetoothService.this) {
					nitZaPovezovanje = null;
				}

				connected(socketCt, napravaCt, tipSocketaCt);
			} catch (Exception e) {
				try {
					socketCt.close();
				} catch (Exception ex) {
					Log.e(TAG, "ConnectThread - run()", ex);
				}
				connectionFailed();
			}
		}

	}

	// nit ko je povezava med napravami. Skozi njo gredo vsi prenose
	private class ConnectedThread extends Thread {
		private InputStream is;
		private OutputStream os;
		private final BluetoothSocket socketCt;

		public ConnectedThread(BluetoothSocket socket, String tipSocketa) {
			socketCt = socket;
			// Get the BluetoothSocket input and output streams
			try {

				is = socket.getInputStream();
				os = socket.getOutputStream();

			} catch (Exception e) {
				is = null;
				os = null;
				Log.e(TAG, "ConectedThread- constructor:", e);
			}

		}

		public void cancel() {
			try {
				socketCt.close();
			} catch (Exception e) {
				Log.e(TAG, "ConnectedThread - cancel():", e);
			}
		}

		public void run() {
			byte[] buffer = new byte[1024];
			byte[] imgBuffer = new byte[1030 * 1090];// 1030*1090
			int pos = 0;

			while (true) {
				try {
					if (is.available() > 0) {
						int bytes = is.read(buffer);

						// kopiranje arraya
						if (pos + bytes >= imgBuffer.length) {
							byte[] newimgBuffer = new byte[imgBuffer.length
									+ buffer.length];
							for (int i = 0; i <= buffer.length; i++) {
								newimgBuffer[i] = imgBuffer[i];
							}
							imgBuffer = newimgBuffer;
						}
						System.arraycopy(buffer, 0, imgBuffer, pos, bytes);

						pos += bytes;

						if (bytes > 0) {
							// Posljemo byte k aktivnosti
							handlerBts.obtainMessage(Konstante.MESSAGE_READ,
									pos, -1, imgBuffer).sendToTarget();
						}
					}
				} catch (Exception e) {
					Log.e(TAG, "ConnectedThread - run(): ", e);
					connectionLost();
					startBluetooth();
					break;
				}
			}
		}

		// pisanje v povezavo
		public void write(byte[] arrayBytes) {
			try {
				os.write(arrayBytes);
				handlerBts.obtainMessage(Konstante.MESSAGE_WRITE, -1, -1,
						arrayBytes).sendToTarget();
			} catch (Exception e) {
				Log.e(TAG, "ConnectedThread - write: ", e);
			}
		}

	}

	private void cancelThreadAttemtping() {
		// Prekini vse niti ki èakajo na povezovanje
		if (nitZaPovezovanje != null) {
			nitZaPovezovanje.cancel();
			nitZaPovezovanje = null;
		}
	}

	private void cancelThreadRunning() {
		// Prekini vse niti ki teèejo v povezavi
		if (nitZaPovezanost != null) {
			nitZaPovezanost.cancel();
			nitZaPovezanost = null;
		}
	}

	private void cancelAcceptThread() {
		if (nitZaSprejemanje != null) {
			nitZaSprejemanje.cancel();
			nitZaSprejemanje = null;
		}
	}

	private void startThreadListening() {
		// Zaženi nit da posluša bluetoothu
		if (nitZaSprejemanje == null) {
			nitZaSprejemanje = new AcceptThread();
			nitZaSprejemanje.start();
		}
	}

	private void sendMessageToActivity(int what, String par1, String par2) {
		Message msg = handlerBts.obtainMessage(what);
		Bundle bundle = new Bundle();
		bundle.putString(par1, par2);
		msg.setData(bundle);
		handlerBts.sendMessage(msg);
	}

	private void startBluetooth() {
		BluetoothService.this.start();
	}

	public synchronized int getStanje() {
		return stanje;
	}
}
