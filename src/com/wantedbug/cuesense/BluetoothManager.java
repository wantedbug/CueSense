/**
 * Copyright (C) 2014 Tampere University of Technology 
 */

package com.wantedbug.cuesense;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

/**
 * This class is the wrapper for Bluetooth connection and transmission
 * functions.
 * It has threads for:
 *  - connecting with a device
 *  - transmitting to a device when connected  
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
    
    // Bluetooth device properties
    private static final String DEVICE_NAME = "BTMateSilver";
	public static final String DEVICE_MAC = "00:06:66:60:1D:07";
	
	// Termination character to append to a Bluetooth transmission to
	// signify the end of the same 
	private static final String TERM_CHAR = "|";
	
	/**
	 * Members
	 */
	private final BluetoothAdapter mAdapter;
	private ConnectThread mConnectThread;
	private ConnectedThread mConnectedThread;
	private int mState;
	
    /**
     * C'tor
     * @param context
     */
	public BluetoothManager(Context context)
	{
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
	}
	
	/**
	 * Return current state of the connection
	 * @return
	 */
	public synchronized int getState()
	{
        return mState;
    }
	
	/**
	 * Set current state of the connection
	 * @param state
	 */
	private synchronized void setState(int state)
	{
        Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;
	}
	
	/**
	 * Start all the threads
	 */
	public synchronized void start() {
        Log.d(TAG, "start");

        stopConnectedThreads();

        // TODO: May have to launch a listener thread to listen for another user's
        // Bluetooth-enabled smartphone
        
        setState(STATE_LISTEN);
	}
	
	/**
	 * Stop all threads
	 */
	public synchronized void stop()
	{
        Log.d(TAG, "stop");

        stopConnectedThreads();

        setState(STATE_NONE);
    }
	
	/**
	 * Stops ConnectThread and ConnectedThread
	 */
	private synchronized void stopConnectedThreads()
	{
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
	public synchronized void connect(BluetoothDevice device)
	{
        Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
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
        setState(STATE_CONNECTING);
    }
	
	/**
	 * Start ConnectedThread to send data
	 * @param socket BluetoothSocket on which the connection was made
	 * @param device BluetoothDevice that has been connected
	 */
	public synchronized void connected(BluetoothSocket socket, BluetoothDevice device)
	{
        Log.d(TAG, "connected");

        stopConnectedThreads();

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        setState(STATE_CONNECTED);
    }
	
	/**
	 * Write to ConnectedThread
	 * @param out
	 */
	public void write(String message) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
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
		
		public ConnectThread(BluetoothDevice device)
		{
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
		
		public void run()
		{
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
				}
		    	
		    	// Reset the ConnectThread because we're done
	            synchronized (BluetoothManager.this) {
	                mConnectThread = null;
	            }

	            // Start the connected thread to be able to send data
	            connected(mSocket, mDevice);
		    }
		}
		
		public void cancel()
		{
			Log.d(TAG, "Socket close from ConnectThread::cancel()");
			// Try to close the socket
			try {
				mSocket.close();
			} catch(IOException closeException) {
				Log.e(TAG, "Socket close error in Thread cancel()" + closeException);
			}
		}
	}
	
    /**
     * This thread handles outgoing transmissions.
     * @author vikasprabhu
     */
	private class ConnectedThread extends Thread {
		private final BluetoothSocket mSocket;
        private final OutputStream mOutStream;
        
        public ConnectedThread(BluetoothSocket socket)
        {
            mSocket = socket;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Output socket not created", e);
            }

            mOutStream = tmpOut;
        }
        
        public void run()
        {
            Log.d(TAG, "BEGIN mConnectedThread");
            // Do nothing as we don't have to listen for incoming data
        }

        /**
         * Write to OutputStream
         * @param buffer
         */
        public void write(byte[] buffer)
        {
            try {
                mOutStream.write(buffer);
                sleep(1);
            } catch (IOException | InterruptedException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel()
        {
        	Log.d(TAG, "Socket close from ConnectThread::cancel()");
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
        
	}
}
