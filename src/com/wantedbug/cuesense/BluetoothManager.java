/**
 * Copyright (C) 2014 Tampere University of Technology 
 */

package com.wantedbug.cuesense;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * This class is the interface for Bluetooth connection and transmission
 * functions.
 * It has threads for:
 *  - connecting with a device
 *  - transmitting adn listening to a device when connected
 * @author vikasprabhu
 */
public class BluetoothManager {
	// Debugging
	private static final String TAG = "BluetoothManager";
	
	/**
	 * Constants
	 */
	// Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    
    // UUID for serial connection
    private static final UUID SERIAL_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // Bluetooth address of the wearable
	public static final String DEVICE_MAC = "00:06:66:60:1D:07";
	// Termination character to append to a Bluetooth transmission to
	// signify the end of the same
	private static final String TERM_CHAR = "|";
	
	/**
	 * Members
	 */
	// Bluetooth adapter
	private final BluetoothAdapter mAdapter;
	
	// Handler from the UI
	private final Handler mHandler;

	// The wearable device
	private BluetoothDevice mWearable;
	// Thread to create the connection to the wearable device
	private ConnectThread mConnectThread;
	// Thread to transmit and listen to the wearable device
	private ConnectedThread mConnectedThread;
	// Bluetooth connection state of the wearable device
	private int mWearableState;
	
	// Flag that specifies whether device is ready to receive new data
	private boolean mWearableReady;
	
    /**
     * C'tor
     * @param context
     */
	public BluetoothManager(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandler = handler;
        mWearableState = STATE_NONE;
	}
	
	/**
	 * Return current state of the wearable connection
	 * @return
	 */
	public synchronized int getWearableState() {
        return mWearableState;
    }
	
	/**
	 * Set current state of the wearable connection
	 * @param state
	 */
	private synchronized void setWearableState(int state) {
        Log.d(TAG, "setWearableState() " + mWearableState + " -> " + state);
        mWearableState = state;
	}
	
	/**
	 * Returns true if wearable is ready to receive data
	 * @return
	 */
	public synchronized boolean isDeviceReady() {
		return mWearableReady;
	}
	
	/**
	 * Sets the ready status of the wearable
	 */
	private synchronized void setDeviceReadyStatus(boolean status) {
		mWearableReady = status;
	}
	
	/**
	 * Start all the threads
	 */
	public synchronized void setup() {
        Log.d(TAG, "start");

        stopWearableThreads();
        setWearableState(STATE_CONNECTING);
	}
	
	/**
	 * Kill existing connections and retry
	 */
	public synchronized void restart() {
        Log.d(TAG, "restart()");

        // Stop connection threads and retry connecting
        stopWearableThreads();
        setWearableState(STATE_CONNECTING);
        connectWearable(mWearable);
	}
	
	/**
	 * Stop all threads
	 */
	public synchronized void stop() {
        Log.d(TAG, "stop");

        stopWearableThreads();
        setWearableState(STATE_NONE);
    }
	
	/**
	 * Stops ConnectThread and ConnectedThread
	 */
	private synchronized void stopWearableThreads() {
		if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
	}
	
	/**
	 * Start the ConnectThread to initiate a connection to a remote device.
	 * @param device BluetoothDevice to connect
	 */
	public synchronized void connectWearable(BluetoothDevice device) {
        Log.d(TAG, "connect to: " + device);
        mWearable = device;

        // Cancel any thread attempting to make a connection
        if (mWearableState == STATE_CONNECTING) {
            if (mConnectThread != null) {
            	mConnectThread.cancel();
            	mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
        	mConnectedThread.cancel();
        	mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setWearableState(STATE_CONNECTING);
    }
	
	/**
	 * Start ConnectedThread to send data
	 * @param socket BluetoothSocket on which the connection was made
	 * @param device BluetoothDevice that has been connected
	 */
	public synchronized void wearableConnected(BluetoothSocket socket, BluetoothDevice device) {
        Log.d(TAG, "connected");

        stopWearableThreads();

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        setWearableState(STATE_CONNECTED);
    }
	
	/**
     * Indicate that the connection attempt failed and notify the UI
     */
    private void wearableConnectionFailed() {
        // Send a failure message back to the UI
        Message msg = mHandler.obtainMessage(MainActivity.BT_MSG_TOAST);
        Bundle bundle = new Bundle();
        bundle.putInt(MainActivity.BT_MSG_ERROR, MainActivity.BT_ERR_CONN_FAILED);
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        BluetoothManager.this.restart();
    }
    
    /**
     * Indicate that the connection was lost and notify the UI
     */
    private void wearableConnectionLost() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MainActivity.BT_MSG_TOAST);
        Bundle bundle = new Bundle();
        bundle.putInt(MainActivity.BT_MSG_ERROR, MainActivity.BT_ERR_CONN_FAILED);
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        BluetoothManager.this.restart();
    }
    
	/**
	 * Write to ConnectedThread
	 * @param out
	 */
	public void writeToWearable(String message) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mWearableState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Append termination character
        message = message.concat(TERM_CHAR);
        // Perform the write unsynchronized
        byte[] buf = message.getBytes();
        r.write(buf);
    }
	
	
	/**
	 * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
	 * @author vikasprabhu
	 */
	private class ConnectThread extends Thread {
		private final BluetoothSocket mSocket;
		private final BluetoothDevice mDevice;
		
		public ConnectThread(BluetoothDevice device) {
			mDevice = device;
			BluetoothSocket tmp = null;
			
			// Get a BluetoothSocket
			try {
		    	// Create an insecure RFComm socket for a serial connection
		    	tmp = device.createInsecureRfcommSocketToServiceRecord(SERIAL_UUID);
		    } catch (IOException e) {
		    	Log.e(TAG, "Socket create failure: " + e);
		    	e.printStackTrace();
		    }
			mSocket = tmp;
		}
		
		public void run() {
			Log.d(TAG, "ConnectThread::run()");
			mAdapter.cancelDiscovery();
			if(mSocket != null)
		    {
				// Try to connect to the device
		    	try {
		    		mSocket.connect(); // blocking call
		    	} catch (IOException connectException) {
		    		Log.e(TAG, "Socket connect error in Thread run()" + connectException);
		    		// Try to close the socket
		    		try {
		    			mSocket.close();
		    		} catch(IOException closeException) {
		    			Log.e(TAG, "Socket close error in Thread run()" + closeException);
		    		}
		    		wearableConnectionFailed();
		    		return;
				}
		    	mAdapter.startDiscovery();
		    	// Reset the ConnectThread because we're done
	            synchronized (BluetoothManager.this) {
	                mConnectThread = null;
	            }

	            // Start the connected thread to be able to send data
	            wearableConnected(mSocket, mDevice);
		    }
		}
		
		public void cancel() {
			Log.d(TAG, "ConnectThread::cancel()");
			// Try to close the socket
			try {
				mSocket.close();
			} catch(IOException closeException) {
				Log.e(TAG, "Socket close error in Thread cancel()" + closeException);
			}
		}
	}
	
    /**
     * This thread handles outgoing transmissions to and listens to
     * incoming transmissions from the wearable device.
     * @author vikasprabhu
     */
	private class ConnectedThread extends Thread {
		/**
		 * Constants
		 */
		private static final String CMD_READY = "R";
		/**
		 * Members
		 */
		// BluetoothSocket that the connection was made on
		private final BluetoothSocket mSocket;
		// Output stream
        private final OutputStream mOutStream;
        // Input stream
        private final InputStream mInStream;
        // Flag to monitor if the device is ready to receive data
        // Note: Since the sent messages can be of any length, the only way to
        // make sure that the entire message has been displayed (scrolled) on
        //  the wearable device is to have it communicate that it has done so
        
        public ConnectedThread(BluetoothSocket socket) {
            mSocket = socket;
            OutputStream tmpOut = null;
            InputStream tmpIn = null;
            mWearableReady = false;

            // Get the BluetoothSocket input and output streams
            try {
                tmpOut = socket.getOutputStream();
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Output socket not created: ", e);
            }

            mOutStream = tmpOut;
            mInStream = tmpIn;
            mWearableReady = true;
        }
        
        /**
         * Listen for incoming data
         */
        public void run() {
            Log.d(TAG, "ConnectedThread::run()");
            byte[] buffer = new byte[1];
            int bytes;
            
            // Listen for incoming ready signal
            while(true) {
            	try {
            		bytes = mInStream.read(buffer);
            		String temp = new String(buffer, "UTF-8");
            		Log.d(TAG, bytes + " " + temp);
            		if(temp.equals(CMD_READY)) {
            			setDeviceReadyStatus(true);
            		}
            	} catch(IOException e) {
            		Log.e(TAG, "Read error: " + e);
            		wearableConnectionLost();
            		break;
            	}
            }
        }

        /**
         * Write to OutputStream
         * @param buffer
         */
        public void write(byte[] buffer) {
        	Log.d(TAG, "ConnectedThread::write()");
            try {
            	// Write only if the device is ready
                if(isDeviceReady()) {
                	// Write out
                	mOutStream.write(buffer);
                	// Reset mDeviceReady flag
                	setDeviceReadyStatus(false);
                }
            } catch (IOException e) {
                Log.e(TAG, "Exception during write: ", e);
            }
        }

        public void cancel() {
        	Log.d(TAG, "ConnectedThread::cancel()");
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed: ", e);
            }
        }
	}
	
}
