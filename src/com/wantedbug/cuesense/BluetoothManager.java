/**
 * Copyright (C) 2014 Tampere University of Technology 
 */

package com.wantedbug.cuesense;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.zip.DataFormatException;

import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
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
 *  - connecting with a wearable device
 *  - transmitting and listening to a wearable device when connected
 *  - listening, receiving and transmitting to another user
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
	public static final String DEVICE1_MAC = "00:06:66:60:1D:07";
//	public static final String DEVICE1_MAC = "00:06:66:60:DC:EE";
	public static final String DEVICE2_MAC = "00:06:66:67:DE:6B";
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
	public BluetoothManager(Context context, Handler handler, DistanceRangeListener distanceRangeListener) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandler = handler;
        mWearableState = STATE_NONE;
        mPairedUserState = STATE_NONE;
        mDistanceRangeListener = distanceRangeListener;
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

//        // Start the service over to restart listening mode
//        BluetoothManager.this.restart(); // probably not a good idea
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
		    	Log.e(TAG, "Wearable socket create failure: " + e);
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
		    		Log.e(TAG, "Could not connect to wearable " + connectException);
		    		if(MainActivity.DEBUG) mAdapter.startDiscovery();
		    		// Try to close the socket
		    		try {
		    			mSocket.close();
		    		} catch(IOException closeException) {
		    			Log.e(TAG, "Socket close error after wearable connect failure " + closeException);
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
	
	
	/**
	 * Constants
	 */
	// Name for the SDP record when creating server socket
    private static final String CUESENSE_SDP_SERVICE_NAME = "CueSenseAccept";
    // Unique UUID for this application
    private static final UUID UUID_CUESENSE = UUID.fromString("e8a262fb-db2b-49da-8c7d-3199a30ba929");

    /**
     * Members
     */
    private PairedUserAcceptThread mPairedUserAcceptThread;
    private PairedUserConnectThread mPairedUserConnectThread;
    private PairedUserConnectedThread mPairedUserConnectedThread;
    private int mPairedUserState;
    private JSONObject mSendData;
    
    
	/**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setPairedUserState(int state) {
        Log.d(TAG, "setPairedUserState() " + mPairedUserState + " -> " + state);
        mPairedUserState = state;
    }

    /**
     * Return the current connection state. */
    public synchronized int getPairedUserState() {
        return mPairedUserState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void startPairedUserThreads() {
        Log.d(TAG, "startPairedUserThreads");

        // Cancel any thread attempting to make a connection
        if (mPairedUserConnectThread != null) {
        	mPairedUserConnectThread.cancel();
        	mPairedUserConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mPairedUserConnectedThread != null) {
        	mPairedUserConnectedThread.cancel();
        	mPairedUserConnectedThread = null;
        }

        setPairedUserState(STATE_LISTEN);

        if (mPairedUserAcceptThread == null) {
            mPairedUserAcceptThread = new PairedUserAcceptThread();
            mPairedUserAcceptThread.start();
        } else {
        	mPairedUserAcceptThread.cancel();
        	mPairedUserAcceptThread = null;
        	mPairedUserAcceptThread = new PairedUserAcceptThread();
            mPairedUserAcceptThread.start();
        }
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    public synchronized void connectAndSend(BluetoothDevice device, JSONObject data) {
        Log.d(TAG, "connectAndSend " + device);

        // Cache data to be sent
        mSendData = null;
        mSendData = data;
        
        // Cancel any thread attempting to make a connection
        if (mPairedUserState == STATE_CONNECTING) {
            if (mPairedUserConnectThread != null) {
            	mPairedUserConnectThread.cancel();
            	mPairedUserConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mPairedUserConnectedThread != null) {
        	mPairedUserConnectedThread.cancel();
        	mPairedUserConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mPairedUserConnectThread = new PairedUserConnectThread(device);
        mPairedUserConnectThread.start();
        setPairedUserState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void pairedUserConnected(BluetoothSocket socket, BluetoothDevice device) {
        Log.d(TAG, "pairedUserConnected()");

        // Cancel the thread that completed the connection
        if (mPairedUserConnectThread != null) {
        	mPairedUserConnectThread.cancel();
        	mPairedUserConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mPairedUserConnectedThread != null) {
        	mPairedUserConnectedThread.cancel();
        	mPairedUserConnectedThread = null;
        }

        // Cancel listening to incoming connections as well
        if (mPairedUserAcceptThread != null) {
            mPairedUserAcceptThread.cancel();
            mPairedUserAcceptThread = null;
        }
        
        // Start the thread to manage the connection and perform transmissions
        mPairedUserConnectedThread = new PairedUserConnectedThread(socket, mSendData);
        mPairedUserConnectedThread.start();
        
        setPairedUserState(STATE_CONNECTED);
        
        // Notify handler that users are connected
        Message msg = mHandler.obtainMessage(MainActivity.BT_MSG_PAIREDUSERCONNECTED);
        mHandler.sendMessage(msg);
    }

    /**
     * Stop all threads
     */
    public synchronized void stopPairedUserThreads() {
        Log.d(TAG, "stopPairedUserThreads()");

        if (mPairedUserConnectThread != null) {
            mPairedUserConnectThread.cancel();
            mPairedUserConnectThread = null;
        }

        if (mPairedUserConnectedThread != null) {
            mPairedUserConnectedThread.cancel();
            mPairedUserConnectedThread = null;
        }

        if (mPairedUserAcceptThread != null) {
            mPairedUserAcceptThread.cancel();
            mPairedUserAcceptThread = null;
        }
        setPairedUserState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see PairedUserConnectedThread#write(byte[])
     */
    public void writeToPairedUser(byte[] out) {
    	Log.d(TAG, "writeToPairedUser() " + out);
        // Create temporary object
        PairedUserConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mPairedUserState != STATE_CONNECTED) {
            	Log.e(TAG, "NOT CONNECTED");
            	return;
            }
            r = mPairedUserConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void pairedUserConnectionFailed() {
    	Log.d(TAG, "pairedUserConnectionFailed()");
    	// Notify MainActivity that send/receive has failed
    	Message msg = mHandler.obtainMessage(MainActivity.BT_MSG_SENDRECV_ERROR);
    	mHandler.sendMessage(msg);
        // Start the service over to restart listening mode
//        BluetoothManager.this.startPairedUserThreads();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void pairedUserConnectionLost() {
    	Log.d(TAG, "pairedUserConnectionLost()");
    	// Notify MainActivity that send/receive has failed
    	Message msg = mHandler.obtainMessage(MainActivity.BT_MSG_SENDRECV_ERROR);
    	mHandler.sendMessage(msg);
        // Start the service over to restart listening mode
//        BluetoothManager.this.startPairedUserThreads();
    }

    /**
     * This thread is for listening for incoming connections from the nearby
     * user's phone. It behaves like a server-side client. It runs until a
     * connection is accepted (or until cancelled).
     * @author vikasprabhu
     */
    private class PairedUserAcceptThread extends Thread {
    	private static final String TAG = "PairedUserAcceptThread";
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;

        public PairedUserAcceptThread() {
        	Log.d(TAG, "create PairedUserAcceptThread");
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            try {
            	tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(CUESENSE_SDP_SERVICE_NAME, UUID_CUESENSE);
            } catch (IOException e) {
                Log.e(TAG, "PairedUserAcceptThread listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "BEGIN PairedUserAcceptThread");
            setName("PairedUserAcceptThread");

            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (mPairedUserState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "PairedUserAcceptThread accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothManager.this) {
                        switch (mPairedUserState) {
                        case STATE_LISTEN:
                        case STATE_CONNECTING:
                        	Log.i(TAG, "accept() succeeded, listen/connecting");
                            // Situation normal. Start the connected thread.
                        	Log.i(TAG, "socket connected: " + socket.isConnected());
                            pairedUserConnected(socket, socket.getRemoteDevice());
                            break;
                        case STATE_NONE:
                        case STATE_CONNECTED:
                        	Log.i(TAG, "accept() succeeded, none/connected");
                            // Either not ready or already connected. Terminate new socket.
                            try {
                                socket.close();
                            } catch (IOException e) {
                                Log.e(TAG, "Could not close unwanted socket", e);
                            }
                            break;
                        }
                    }
                }
            }
            Log.i(TAG, "END PairedUserAcceptThread");
        }

        public void cancel() {
            Log.d(TAG, "PairedUserAcceptThread cancel()");
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "PairedUserAcceptThread close() of server failed", e);
            }
        }
    }


    /**
     * This thread handles outgoing transmissions to and listens to
     * incoming transmissions from the wearable device.
     * @author vikasprabhu
     */
    private class PairedUserConnectThread extends Thread {
    	private static final String TAG = "PairedUserConnectThread";
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public PairedUserConnectThread(BluetoothDevice device) {
        	Log.d(TAG, "create PairedUserConnectThread");
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
            	tmp = device.createInsecureRfcommSocketToServiceRecord(UUID_CUESENSE);
            } catch (IOException e) {
            	Log.e(TAG, "PairedUserConnectThread create() failed" + e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN PairedUserConnectThread");
            setName("PairedUserConnectThread");

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
            	Log.e(TAG, "unable to connect()" + e);
            	
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                pairedUserConnectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothManager.this) {
                mPairedUserConnectThread = null;
            }

            // Start the connected thread
            pairedUserConnected(mmSocket, mmDevice);
        }

        public void cancel() {
        	Log.d(TAG, "cancel()");
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed ", e);
            }
        }
    }
    
    public interface DistanceRangeListener {
    	/** Return the current distance range of the other user */
    	int currentDistanceRange();
    }
    
    DistanceRangeListener mDistanceRangeListener;

    /**
     * This thread handles outgoing transmissions to and listens to
     * incoming transmissions from the nearby user's phone.
     * @author vikasprabhu
     */
    private class PairedUserConnectedThread extends Thread {
    	private static final String TAG = "PairedUserConnectedThread";
    	private static final int BUFFER_SIZE = 4096;
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private JSONObject mmData;
        private boolean mmDataNotSent;

        public PairedUserConnectedThread(BluetoothSocket socket, JSONObject data) {
            Log.d(TAG, "create PairedUserConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            mmData = data;
            mmDataNotSent = (mmData == null) ? true : false;
            
            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created ", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN PairedUserConnectedThread");
            byte[] buffer = new byte[BUFFER_SIZE];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int bytes;
            String rcvd = null;
            byte[] rcvdBytes = null;
            boolean dataAvailable = false;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream when available
                	Log.i(TAG, "socket connected: " + mmSocket.isConnected() + mmInStream.available());
                	while (mmInStream.available() > 0 &&
                			(bytes = mmInStream.read(buffer)) > -1) {
                        baos.write(buffer, 0, bytes);
                        try {
                        	sleep(100);
                        } catch(InterruptedException e) {
                        	Log.e(TAG, "sleep interrupted " + e);
                        }
                        baos.flush();
                        dataAvailable = true;
                	}
                	if(dataAvailable) {
                		rcvdBytes = baos.toByteArray();
                		rcvdBytes = CompressionUtils.decompress(rcvdBytes);
                		rcvd = new String(rcvdBytes, "UTF-8");
                		break;
                	}
                } catch (IOException | DataFormatException e) {
                    Log.e(TAG, "PairedUserConnectedThread::run() read error ", e);
                    pairedUserConnectionLost();
                    // Start the threads over to restart listening mode
                    BluetoothManager.this.startPairedUserThreads();
                    break;
                }
            }
            // If we've received data, check if we have data to send
            if(rcvd != null && !rcvd.isEmpty()) {
            	Log.i(TAG, "received " + rcvdBytes.length + " characters");
            	// If we didn't have data earlier, we need to send it now, if any
            	try {
            		if(mmDataNotSent) {
            			rcvd = InfoPool.INSTANCE.fixRawData(rcvd);
            			JSONObject data = new JSONObject(rcvd);
            			int distance = data.optInt(InfoPool.JSON_DISTANCE_NAME);
            			if(distance == 0) distance = mDistanceRangeListener.currentDistanceRange();
            			if(distance > MainActivity.DISTANCE_OUTOFRANGE &&
            					distance <= MainActivity.DISTANCE_FAR) {
            				JSONObject myData = InfoPool.INSTANCE.getData(distance);
            				if(myData != null) write(myData.toString().getBytes());
            				mmDataNotSent = false;
            			} else {
            				Log.e(TAG, "Invalid data received " + distance);
            			}
            		}
            	} catch(JSONException e) {
            		Log.e(TAG, "PairedUserConnectedThread::run() read/write error " + e);
            	}
            } else {
            	rcvd = "";
            }
            // Notify MainActivity that send/receive is done
        	Message msg = mHandler.obtainMessage(MainActivity.BT_MSG_SENDRECV_DONE);
        	Bundle bundle = new Bundle();
        	bundle.putString(MainActivity.BT_MSG_SENDRECV_DATA, rcvd);
        	msg.setData(bundle);
        	mHandler.sendMessage(msg);
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
            	buffer = CompressionUtils.compress(buffer);
            	if(buffer != null) Log.d(TAG, "write() " + buffer.length + " characters");
                mmOutStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write ", e);
            }
        }

        public void cancel() {
        	Log.d(TAG, "cancel()");
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed ", e);
            }
        }
    }
}
