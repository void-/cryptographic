package com.share;

import com.share.KeyShare;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 *  Manage a bluetooth connection as a server or a client.
 *
 *  Use an insecure Rfcomm connection for the following reasons:
 *    stay compatable with more devices that have worse bluetooth hardware
 *    don't bug the user with yet another confirmation dialog
 *    better(more entropic) authentication will be done on the phone number and
 *      keys anyways, no need to do it twice
 */
class Connection
{
  /**
   *  Class Variables.
   *
   *  TAG
   *  NAME
   *  STATE_NONE
   *  STATE_LISTENING
   *  STATE_CONNECTING
   *  STATE_CONNECTED
   *  SERIALIZED_SIZE
   */
  private static final String TAG = "Connection";
  private static final String NAME = "KeyShare";
  static final int STATE_NONE = 0;
  static final int STATE_LISTENING = 1;
  static final int STATE_CONNECTING = 2;
  static final int STATE_CONNECTED = 3;
  private static final int SERIALIZED_SIZE = 592;

  /**
   *  Member Variables.
   *
   *  makeConnectionThread Thread for trying to connection to an exterior
   *    device. This device acts as the client.
   *  listenConnectionThread Thread for listening for incoming connections.
   *    This device acts as the server.
   *  connectionThread ConnectThread for maintaining a connection as either a
   *    client or a server.
   *  state int represeting the current state of Connection.
   *  serializedKey
   *  handler Handler for communicating with the instantiater of this.
   */
  private BluetoothAdapter bluetoothAdapter;
  private MakeConnectionThread makeConnectionThread;
  private ListenThread listenConnectionThread;
  private ConnectionThread connectionThread;
  private int state;
  private byte[] serializedKey;
  private Handler handler;

  /**
   *  Connection() construct a new Construction instance.
   *
   *  @param bluetoothAdapter non-null BluetoothAdapter for Bluetooth.
   *  @param key byte array representing the serialized key to share.
   *  @param handler Handler for communicating.
   */
  Connection(BluetoothAdapter bluetoothAdapter, byte[] key, Handler handler)
  {
    this.bluetoothAdapter = bluetoothAdapter;
    state = Connection.STATE_NONE;
    makeConnectionThread = null;
    listenConnectionThread = null;
    connectionThread = null;
    this.serializedKey = key;
    this.handler = handler;
  }

  /**
   *  set the connection state.
   *
   *  @param state the new state to set.
   */
  private synchronized void setState(int state)
  {
    Log.d(TAG, "setState(): " + this.state + " -> " + state);
    this.state = state;
  }

  /**
   *  getState returns the Connection's current state.
   *
   *  @return connecivity state.
   */
  synchronized int getState()
  {
    return this.state;
  }

  /**
   *  start listening as a bluetooth server.
   */
  synchronized void startListening()
  {
    //stop trying to connect
    if(makeConnectionThread != null)
    {
      makeConnectionThread.cancel();
      makeConnectionThread = null;
    }
    //stop any current connections
    if(connectionThread != null)
    {
      connectionThread.cancel();
      connectionThread = null;
    }
    setState(Connection.STATE_LISTENING);

    //start listening if not already
    if(listenConnectionThread == null)
    {
      listenConnectionThread = new ListenThread();
      listenConnectionThread.start();
    }
  }

  /**
   *  given a bluetooth device, start trying to connect to it
   */
  synchronized void connectTo(BluetoothDevice d)
  {
    Log.d(TAG, "Connecting to device:"+d.getAddress());
    //stop trying to connect
    if(state == Connection.STATE_CONNECTING && makeConnectionThread != null)
    {
      makeConnectionThread.cancel();
      makeConnectionThread = null;
    }

    //stop any current connections
    if(connectionThread != null)
    {
      connectionThread.cancel();
      connectionThread = null;
    }

    //start making a connection
    makeConnectionThread = new MakeConnectionThread(d);
    makeConnectionThread.start();
    setState(Connection.STATE_CONNECTING);
  }

  /**
   *  given a socket and bluetoothDevice, begin a bluetooth connection
   *
   *  @param socket BluetoothSocket to make the connection on.
   *  @param d BluetoothDevice to connect to.
   *  @param isServer boolean indicating whether this device acts as server.
   */
  synchronized void connected(BluetoothSocket socket, BluetoothDevice d,
      int isServer)
  {
    //stop trying to make a connection
    if(makeConnectionThread != null)
    {
      makeConnectionThread.cancel();
      makeConnectionThread = null;
    }

    //stop listening for a connection
    if(listenConnectionThread != null)
    {
      listenConnectionThread.cancel();
      listenConnectionThread = null;
    }

    //stop any current connections
    if(connectionThread != null)
    {
      connectionThread.cancel();
      connectionThread = null;
    }

    Log.d(TAG, "connected() " + d);

    //start a connection
    connectionThread = new ConnectionThread(socket, isServer);
    connectionThread.start();
    setState(Connection.STATE_CONNECTED);
    //send key to the other device
    connectionThread.write(this.serializedKey);
  }

  /**
   *  ListenThread listens for other devices trying to make a bluetooth
   *  connection to this device.
   */
  private class ListenThread extends Thread
  {

    /**
     *  Member Variables.
     *
     *  btSocket
     */
    private final BluetoothServerSocket btSocket;

    /**
     *  ListenThread() constructor
     */
    ListenThread()
    {
      BluetoothServerSocket tmp = null;
      try
      {
        tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
          Connection.NAME, KeyShare.BT_UUID);
      }
      catch(IOException e)
      {
        Log.e(Connection.TAG, "secure connection failed.");
      }
      btSocket = tmp;
    }

    /**
     *  listen as a bluetooth server, run until a client initates a connection.
     *  Don't connect to more than one device.
     */
    @Override
    public void run()
    {
      Log.d(TAG, "BEGIN ListenThread " + this);
      setName("ListenThread");

      BluetoothSocket socket = null;

      while(state != Connection.STATE_CONNECTED && socket == null)
      {
        try
        {
          Log.d(TAG, "waiting for accept");
          socket = btSocket.accept();
          Log.d(TAG, "accept() stopped blocking");
        }
        catch(IOException e)
        {
          Log.e(TAG, "failed to accept()", e);
          break;
        }
      }

      if(socket == null) //no connection made
      {
        Log.d(TAG, "no connection made after listening");
        return;
      }

      synchronized(Connection.this)
      {
        switch(state)
        {
          case Connection.STATE_LISTENING:
          case Connection.STATE_CONNECTING:
            //act as the server in the connection
            connected(socket, socket.getRemoteDevice(), 1);
            break;
          case Connection.STATE_NONE:
          case Connection.STATE_CONNECTED:
            //already connected, don't connect to any more devices
            Log.d(TAG, "already connected: stopping connecting to device.");
            try
            {
              socket.close();
            }
            catch(IOException e)
            {
              Log.e(TAG, "could not close extra socket", e);
            }
            break;
        }
      }
    }

    /**
     *  cancel() stop listening for a connection.
     */
    public void cancel()
    {
      Log.d(TAG, "cancel listening " + this);
      try
      {
        btSocket.close();
      }
      catch(IOException e)
      {
        Log.e(TAG, "close() failed", e);
      }
    }
  }

  /**
   *  MakeConnectionThread trys to establish a bluetooth connection with a
   *  server. This is a connection that has not been made yet.
   */
  private class MakeConnectionThread extends Thread
  {
    /**
     *  Member Variables.
     *
     *  socket BluetoothSocket to try to make a connection to(as the client).
     *  device BluetoothDevice that acts as the server. Try to connect to this.
     */
    private final BluetoothSocket socket;
    private final BluetoothDevice device;

    /**
     *  MakeConnectionThread constructor
     *
     *  @param d BluetoothDevice to try to connect to.
     */
    MakeConnectionThread(BluetoothDevice d)
    {
      device = d;
      BluetoothSocket tmp = null;
      try
      {
        tmp = d.createInsecureRfcommSocketToServiceRecord(KeyShare.BT_UUID);
      }
      catch(IOException e)
      {
        Log.e(TAG, "make connection failed", e);
      }
      socket = tmp;
    }

    /**
     *  run() try to connect to the bluetooth device.
     *  connect() is a blocking call.
     */
    @Override
    public void run()
    {
      Log.d(TAG, "Attempting to make a connection");
      setName("MakeConnectionThread");

      bluetoothAdapter.cancelDiscovery();

      try
      {
        socket.connect();
      }
      catch(IOException e)
      {
        try
        {
          socket.close();
        }
        catch(IOException e1)
        {
          Log.e(TAG, "unable to close socket during connection", e1);
        }
      }

      synchronized(Connection.this)
      {
        makeConnectionThread = null;
      }

      //act as the client in the connection
      connected(socket, this.device, 0);
    }

    /**
     *  cancel() stop trying to initiate a connection.
     */
    public void cancel()
    {
      try
      {
        socket.close();
      }
      catch(IOException e)
      {
        Log.e(TAG, "could not close connection socket", e);
      }
    }
  }

  /**
   *  ConnectionThread maintains a bluetooth connection to another device.
   *  This is maintaining a connection after it has been initiated by
   *  a MakeConnectionThread.
   *
   *  This device either acts as the server or the client, it does not matter
   *  to ConnectThread.
   */
  private class ConnectionThread extends Thread
  {
    /**
     *  Member Variables.
     */
    private final BluetoothSocket socket;
    private final InputStream in;
    private final OutputStream out;
    private final int isServer;

    /**
     *  ConnectionThread constructor
     */
    ConnectionThread(BluetoothSocket socket, int isServer)
    {
      Log.d(TAG, "create connection");
      this.socket = socket;
      this.isServer = isServer;
      InputStream tmpIn = null;
      OutputStream tmpOut = null;
      try
      {
        tmpIn = socket.getInputStream();
        tmpOut = socket.getOutputStream();
      }
      catch(IOException e)
      {
        Log.e(TAG, "Could not get socket streams.", e);
      }
      in = tmpIn;
      out = tmpOut;
    }

    /**
     *  run() keeps reading into a buffer, report it to handler so that it can
     *  be processed.
     *
     *  run() infinitely loops until an exception is thrown during a read. This
     *  can be acheived by calling ConnectionThread.cancel().
     */
    @Override
    public void run()
    {
      Log.d(TAG, "starting a connection");
      byte[] buffer = new byte[SERIALIZED_SIZE<<1];
      int bytes = 0;
      while(true)
      {
        try
        {
          //give buffer to KeyShare
          Log.d(TAG, "trying to read data...");
          (handler.obtainMessage(KeyShare.MESSAGE_KEY_RECEIVED,
            in.read(buffer), this.isServer, buffer)).sendToTarget();
          Log.d(TAG, "sent a message to handler");
        }
        catch(IOException e)
        {
          Log.e(TAG, "failed to read during connection", e);
          break;
        }
      }
    }

    /**
     *  write() given a buffer preforms an unsynchronized write.
     *
     *  @param buffer byte array to send via Bluetooth.
     */
    public void write(byte[] buffer)
    {
      Log.d(TAG, "writing a buffer to send");
      try
      {
        out.write(buffer);
      }
      catch(IOException e)
      {
        Log.e(TAG, "Failed to write during connection", e);
      }
    }

    /**
     *  cancel() cancels the Bluetooth connection.
     */
    public void cancel()
    {
      try
      {
        socket.close();
      }
      catch(IOException e)
      {
        Log.e(TAG, "could not close connection socket", e);
      }
    }
  }
}
