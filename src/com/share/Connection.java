package com.share;

import com.share.KeyShare;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 *  Manage a bluetooth connection as a server or a client.
 */
class Connection
{
  /**
   *  Class Variables.
   */
  private static final String TAG = "Connection";
  private static final String NAME = "KeyShare";
  static final int STATE_NONE = 0;
  static final int STATE_LISTENING = 1;
  static final int STATE_CONNECTING = 2;
  static final int STATE_CONNECTED = 3;

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
   */
  private BluetoothAdapter bluetoothAdapter;
  private MakeConnectionThread makeConnectionThread;
  private ListenThread listenConnectionThread;
  private ConnectionThread connectionThread;
  private int state;

  Connection(BluetoothAdapter bluetoothAdapter)
  {
    this.bluetoothAdapter = bluetoothAdapter;
    state = Connection.STATE_NONE;
    makeConnectionThread = null;
    listenConnectionThread = null;
    connectionThread = null;
  }

  /**
   *  set the connection state.
   */
  private synchronized void setState(int state)
  {
    Log.d(TAG, "setState(): " + this.state + " -> " + state);
    this.state = state;
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

    //start listening
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

    makeConnectionThread = new MakeConnectionThread(d);
    makeConnectionThread.start();
    setState(Connection.STATE_CONNECTING);
  }

  /**
   *  given a socket and bluetoothDevice, begin a bluetooth connection
   */
  synchronized void connected(BluetoothSocket socket, BluetoothDevice d)
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

    connectionThread = new ConnectionThread(socket);
    connectionThread.start();
    setState(Connection.STATE_CONNECTED);
  }

  /**
   *  ListenThread listens for other devices trying to make a bluetooth
   *  connection to this device.
   */
  private class ListenThread extends Thread
  {
    private final BluetoothServerSocket btSocket;
    ListenThread()
    {
      BluetoothServerSocket tmp = null;
      try
      {
        tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(
          Connection.NAME, KeyShare.BT_UUID);
      }
      catch(IOException e)
      {
        Log.e(Connection.TAG, "secure connection failed.");
      }
      btSocket = tmp;
    }

    @Override
    public void run()
    {
      Log.d(TAG, "BEGIN ListenThread " + this);
      setName("ListenThread");

      BluetoothSocket socket = null;

      while(state != Connection.STATE_CONNECTED)
      {
        try
        {
          socket = btSocket.accept();
        }
        catch(IOException e)
        {
          Log.e(TAG, "failed to accept()", e);
          break;
        }
      }

      if(socket == null) //no connection made
      {
        return;
      }

      synchronized(Connection.this)
      {
        switch(state)
        {
          case Connection.STATE_LISTENING:
          case Connection.STATE_CONNECTING:
            connected(socket, socket.getRemoteDevice());
            break;
          case Connection.STATE_NONE:
          case Connection.STATE_CONNECTED:
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
   *  server.
   */
  private class MakeConnectionThread extends Thread
  {
    private final BluetoothSocket socket;
    private final BluetoothDevice device;

    MakeConnectionThread(BluetoothDevice d)
    {
      device = d;
      BluetoothSocket tmp = null;
      try
      {
        tmp = d.createRfcommSocketToServiceRecord(KeyShare.BT_UUID);
      }
      catch(IOException e)
      {
        Log.e(TAG, "make connection failed", e);
      }
      socket = tmp;
    }

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

      connected(socket, this.device);
    }

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
   *  ConnectThread maintains a bluetooth connection to another device.
   *
   *  This device either acts as the server or the client, it does not matter
   *  to ConnectThread.
   */
  private class ConnectionThread extends Thread
  {
    private final BluetoothSocket socket;
    private final InputStream in;
    private final OutputStream out;

    ConnectionThread(BluetoothSocket socket)
    {
      Log.d(TAG, "create connection");
      this.socket = socket;
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
    @Override
    public void run()
    {
      Log.d(TAG, "starting a connection");
      byte[] buffer = new byte[2048];
      while(true)
      {
        try
        {
          in.read(buffer);
          //do something with buffer
        }
        catch(IOException e)
        {
          Log.e(TAG, "failed to read during connection", e);
          break;
        }
      }
    }

    public void write(byte[] buffer)
    {
      try
      {
        out.write(buffer);
      }
      catch(IOException e)
      {
        Log.e(TAG, "Failed to write during connection", e);
      }
    }

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
