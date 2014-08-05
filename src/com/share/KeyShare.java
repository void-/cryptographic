package com.share;

import com.share.Connection;
import com.share.ShareConfirmationDialogFragment;
import com.ctxt.R;

import com.key.Key;
import com.key.NumberKeyPair;
import com.key.KeyAlreadyExistsException;

import android.util.Log;
import android.nfc.*;
import android.app.Activity;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.Handler;
import android.os.Message;
import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import android.app.PendingIntent;

import java.io.IOException;
import java.io.Serializable;
import java.io.ObjectOutput;
import java.io.ObjectInput;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.lang.System;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import java.util.UUID;

/**
 *  KeyShare Activity for sharing public keys via Bluetooth.
 *    DOCS DESCRIBE NFC ITERATION; UPDATE DOCS TO BLUETOOTH
 *
 *  When this Activity is launched, it will set a static payload to share via
 *  NFC(the user's phone number and public key). It should be the case that
 *  when two individuals wish to share public keys, the exchange will occur at
 *  the same time.
 *
 *  If NFC is unavailable on the user's device, the KeyShare activity does
 *  nothing.
 *
 *  bluetooth process:
 *    serialize the user's key
 *    request the user make themselves discoverable by bluetooth in on resume
 *    start listening AND display a dialog listing visible devices
 *    accept a connection OR ask the user to pick a device
 */
public class KeyShare extends Activity implements
    ShareConfirmationDialogFragment.ShareConfirmationListener
{
  /**
   *  Class Variables.
   *
   *  TAG constant string representing the tag to use when logging events that
   *    originate from calls to KeyPair methods.
   */
  private static final String TAG = "KEYSHARE";
  static final UUID BT_UUID =
    UUID.fromString("885a4392-07a2-a613-0895-20a84ebaf087");
  private static final int REQUEST_DISCOVERABLE = 0x1;
  private static final int REQUEST_ENABLE = 0x2;
  static final int MESSAGE_KEY_RECEIVED = 0x10;

  /**
   *  Member Variables.
   *
   *  nfcAdapter NfcAdapter object used for interfacing with nfc communication.
   *  pairToShare NumberKeyPair containing the user's (phone number:public key)
   *    to share via nfc.
   *  pairInQuestion NumberKeyPair that must be confirmed by the user before
   *    adding to Fetcher.
   */
  //private NfcAdapter nfcAdapter;
  private BluetoothAdapter bluetoothAdapter;
  protected NumberKeyPair pairToShare;
  private boolean numberConfirmed;
  private NumberKeyPair pairInQuestion;
  private Connection connection;
  private byte[] serializedKey;
  private ArrayAdapter<String> deviceNamesAdapter;
  private TextView status;

  /**
   *  onCreate() registers NFC callbacks and sets the public key to share.
   *
   *  If NFC is unavailable on the user's device, the activity will sit idle.
   *
   *  On the matter of setting the Ndef push message:
   *  The user's NumberKeyPair is serialized into a byte array.
   *  The Ndef message is a NdefRecord for a MIME type that is simply the
   *    serialization of the NumberKeyPair.
   *
   *  @param savedInstance Bundle containing an saved data from a previous
   *    instance.
   */
  @Override
  public void onCreate(Bundle savedInstance)
  {
    super.onCreate(savedInstance);
    setContentView(R.layout.keyshare);

    numberConfirmed = false;

    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    //bluetooth is unavailable
    if(bluetoothAdapter == null)
    {
      Log.d(TAG, "no bluetooth.");
      finish();
      return;
    }

    //set the device name
    ((TextView) findViewById(R.id.device_name)).append(
      bluetoothAdapter.getName());
    ((TextView) findViewById(R.id.device_name)).append(
      bluetoothAdapter.getAddress());
    status = (TextView) findViewById(R.id.connection_status);

    //register a receiver for discovering devices
    registerReceiver(discoveredReceiver,
      new IntentFilter(BluetoothDevice.ACTION_FOUND));

    deviceNamesAdapter = new ArrayAdapter<String>(this,
      android.R.layout.simple_list_item_1);
    //setup the list view for displaying devices
    ListView l = (ListView) findViewById(R.id.device_list);
    l.setAdapter(deviceNamesAdapter);
    l.setOnItemClickListener(deviceClickListener);

    this.pairToShare = (Key.getFetcher(getApplicationContext())).shareKey();
    //serialize the NumberKeyPair to share
    serializedKey = serializeKey(pairToShare);

    connection = new Connection(bluetoothAdapter, serializedKey, handler);
    hexify(serializedKey);
  }

  /**
   *  onStart() ask the user if they would like to be discoverable.
   *  Do this in onStart() to avoid an infinite dialog loop if the user
   *  declines.
   */
  @Override
  public void onStart()
  {
    super.onStart();
    makeDiscoverable(null);
  }

  /**
   *  onResume() start scanning for devices.
   */
  @Override
  public void onResume()
  {
    super.onResume();
    onScan(null);
  }

  /**
   *  onPause()
   */
  @Override
  public void onPause()
  {
    super.onPause();
    Log.d(TAG, "onPause: canceling discovery");
    bluetoothAdapter.cancelDiscovery();
  }

  /**
   *  handler Handler allows for communication from a Connection instance to
   *  the KeyShare activity.
   */
  private final Handler handler = new Handler()
  {

    /**
     *  handleMessage() callback.
     *
     *  m Message to pass over.
     */
    @Override
    public void handleMessage(Message m)
    {
      Log.d(TAG, "handleMessage() called, what:" + m.what);
      switch(m.what)
      {
        case KeyShare.MESSAGE_KEY_RECEIVED:
          Log.d(TAG, "MESSAGE_KEY_RECEIVED");
          receiveKey((byte[]) m.obj, m.arg1, m.arg2);
      }
    }
  };

  /**
   *  discoveredReceiver adds newly discovered bluetooth devices to the array
   *  adapter.
   */
  private final BroadcastReceiver discoveredReceiver = new BroadcastReceiver()
  {

    /**
     *  This should already be filtered for discovering a new bluetooth device.
     */
    @Override
    public void onReceive(Context context, Intent intent)
    {
      BluetoothDevice device =
        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
      deviceNamesAdapter.add(device.getName() + "-" + device.getAddress());
      Log.d(TAG, "found an new device:" + device.getName());
    }
  };

  /**
   *  deviceClickListener responds to clicks on devices listed in
   *  deviceNamesAdapter.
   *  
   *  Give the bluetooth device back to the calling activity.
   */
  private OnItemClickListener deviceClickListener = new OnItemClickListener()
  {

    /**
     *  onItemClick() callback when a bluetooth device is clicked.
     *
     *  stop looking for bluetooth devices and start connecting to the selected
     *  one.
     *
     *  @param a the AdapterView where the click happened.
     *  @param v the View that was clicked.
     *  @param position index of v in a.
     *  @param id the row id of v.
     */
    @Override
    public void onItemClick(AdapterView<?> a, View v, int position, long id)
    {
      String nameAndMac = (((TextView) v).getText()).toString();
      status.setText("connected to:" + nameAndMac);
      connection.connectTo(bluetoothAdapter.getRemoteDevice(
        nameAndMac.substring(nameAndMac.lastIndexOf('-')+1)));

      bluetoothAdapter.cancelDiscovery();
    }
  };

  /**
   *  called when creating the action bar and options menu.
   *  @return true.
   */
  @Override
  public boolean onCreateOptionsMenu(Menu menu)
  {
    getMenuInflater().inflate(R.layout.keyshare_menu, menu);
    return true;
  }

  /**
   *  makeDiscoverable() launches an activity to ask the user to make the
   *  device visible via bluetooth.
   *
   *  When done on both devices that would like to exchange keys, this makes it
   *  able for either one to function as the server in the connection.
   *  The user is the one that decides which device will be the client and
   *  which the server.
   *
   *  @param item not used, can be null.
   */
  public void makeDiscoverable(MenuItem item)
  {
    Log.d(TAG, "making discoverable");
    if(bluetoothAdapter.getScanMode() !=
        BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
    {
      startActivityForResult(
        new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE),
        KeyShare.REQUEST_DISCOVERABLE);
    }
  }

  /**
   *  onScan() callback when scan button clicked. Start scanning for devices.
   *
   *  @param item not used, can be null.
   */
  public void onScan(MenuItem item)
  {
    Log.d(TAG, "starting discovery");
    if(connection.getState() != Connection.STATE_CONNECTED)
    {
      bluetoothAdapter.startDiscovery();
    }
  }

  /**
   *  startListening() updates the ui and starts listening as a bluetooth
   *  server.
   */
  private void startListening()
  {
    //start listening for connections
    Log.d(TAG, "startListening() for bluetooth connections.");
    if(connection.getState() != Connection.STATE_CONNECTED)
    {
      status.setText("listening");
      connection.startListening();
    }
  }

  /**
   *  onActivityResult() callback for when a launched activity returns.
   *
   */
  public void onActivityResult(int requestCode, int resultCode, Intent data)
  {
    Log.d(TAG, requestCode + ":activity returned:" + resultCode);
    if(requestCode == KeyShare.REQUEST_DISCOVERABLE)
    {
      if(resultCode > 0)
      {
        Log.d(TAG, "device made discoverable, starting listening");
        startListening();
      }
      else
      {
        Log.d(TAG, "user denied discoverability");
        startActivityForResult(
          new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
          KeyShare.REQUEST_ENABLE);
      }
    }
    else if(requestCode == KeyShare.REQUEST_ENABLE)
    {
      if(resultCode == Activity.RESULT_OK)
      {
        Log.d(TAG, "Bluetooth enabled by user, but not discoverable.");
      }
      else
      {
        Log.d(TAG, "user denied turning on bluetooth");
      }
    }
  }

  /**
   *  processIntent() given an intent will determine what to do with it.
   *  In this case, processIntent() always assumes the intent is for receiving
   *  a public key via NFC.
   *
   *  If this method receives an array of NdefMessages in the intent, only the
   *  first one will be processed.
   *
   *  @param Intent the intent to process: presumably contains an NdefMessage.
   */
  void processIntent(Intent intent)
  {
    Log.d(KeyShare.TAG, "processIntent called.");
    Parcelable[] rawMsgs =
      intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
    if(rawMsgs.length <= 0)
    {
      Log.d(KeyShare.TAG, "Aborting, rawMsgs too short.");
      return;
    }
    //Extract NdefMessage: process only 1 message; ignore all others
    NdefMessage msg = (NdefMessage) rawMsgs[0];

    //Deserialize a NumberKeyPair from the NdefRecord payload
    ByteArrayInputStream b = new ByteArrayInputStream(
      msg.getRecords()[0].getPayload());
    ObjectInput in = null;
    try
    {
      in = new ObjectInputStream(b);
    }
    catch(java.io.StreamCorruptedException e)
    {Log.e(KeyShare.TAG, "exception", e); }
    catch(IOException e) {Log.e(KeyShare.TAG, "exception", e); }
    try
    {
      pairInQuestion = (NumberKeyPair) in.readObject();
      b.close();
      in.close();
    }
    catch(ClassNotFoundException e) {Log.e(KeyShare.TAG, "exception", e); }
    catch(IOException e) {Log.e(KeyShare.TAG, "exception", e); }
    Log.d(KeyShare.TAG, "Launching fragment for key.");
    //textLog.append("Received key for number:" + pairInQuestion.getNumber());
    //launch a dialog to confirm addition of this public key
    ShareConfirmationDialogFragment f = new ShareConfirmationDialogFragment();
    Bundle bundle = new Bundle();
    bundle.putString(ShareConfirmationDialogFragment.PHONE_NUMBER,
      pairInQuestion.getNumber());
    f.setArguments(bundle);
    f.show(getFragmentManager(), ShareConfirmationDialogFragment.FRAG_TAG);
  }

  /**
   *  receiveKey() called when a key is received via bluetooth.
   *
   *  Ask the user if the key is ok, then proceed to add it.
   *  Create an image for authentication.
   *  image : Server key || Client Key
   *
   *  @param keyBuffer byte array representing a serialized
   *    NumberKeyPair.
   *  @param len integer representing how many bytes in keyBuffer are used.
   *  @param isServer boolean indicating whether this device acted as the
   *    server in the bluetooth connection
   */
  void receiveKey(byte[] keyBuffer, int len, int isServer)
  {
    Log.d(TAG, "called receiveKey");
    //Deserialize a NumberKeyPair from keyBuffer
    Log.d(KeyShare.TAG, "Launching fragment for key.");
    Log.d(TAG, "Received key for number:" + pairInQuestion.getNumber());

    pairInQuestion = deserializeKey(keyBuffer, 0, len);

    //create a combined image of both keys
    byte[] img = new byte[len + serializedKey.length];
    if(isServer > 0) //user was the server
    {
      System.arraycopy(serializedKey, 0, img, 0, serializedKey.length);
      System.arraycopy(keyBuffer, 0, img, serializedKey.length, len);
    }
    else
    {
      System.arraycopy(keyBuffer, 0, img, 0, len);
      System.arraycopy(serializedKey, 0, img, len, serializedKey.length);
    }

    //launch a dialog to confirm addition of this public key
    ShareConfirmationDialogFragment f = new ShareConfirmationDialogFragment();
    Bundle bundle = new Bundle();
    bundle.putString(ShareConfirmationDialogFragment.PHONE_NUMBER,
      pairInQuestion.getNumber());
    bundle.putByteArray(ShareConfirmationDialogFragment.IMAGE, img);
    f.setArguments(bundle);
    f.show(getFragmentManager(), ShareConfirmationDialogFragment.FRAG_TAG);
  }

  /**
   *  receiveDialogResult() is called when a created dialog for phone number
   *  confirmation has a button clicked.
   *
   *  When called, this method will attempt to add the new public key if the
   *  user approves it, otherwise do nothing.
   *
   *  @param confirmed boolean indicating whether or not the posotive button
   *    was clicked.
   */
  @Override
  public void receiveDialogResult(boolean confirmed)
  {
    //user denied the number
    if(!confirmed)
    {
      Log.d(TAG, "Rejected public key for number:" +
        pairInQuestion.getNumber());
      return;
    }
    //try to add the key
    try
    {
      (Key.getFetcher(getApplicationContext())).newKey(
        pairInQuestion.getNumber(),
        pairInQuestion.getKey());
    }
    catch(KeyAlreadyExistsException e)
    {
      Log.d(TAG, "Could not add public key for number:" +
        pairInQuestion.getNumber()
        + "; you already have a public key for this number.");
    }
  }

  /**
   *  serializeKey() given a NumberKeyPair returns a byte array containing its
   *  serialized representation.
   *
   *  @param p NumberKeyPair to serialize.
   *  @return serialized NumberKeyPair p or null on error.
   */
  private static byte[] serializeKey(NumberKeyPair p)
  {
    ByteArrayOutputStream bo = new ByteArrayOutputStream();
    ObjectOutput out = null;
    try
    {
      out = new ObjectOutputStream(bo);
      out.writeObject(p);
      return bo.toByteArray();
    }
    catch(IOException e)
    {
      Log.e(KeyShare.TAG, "exception", e);
    }
    finally
    {
      try
      {
        out.close();
        bo.close();
      }
      catch(IOException e) {Log.e(KeyShare.TAG, "exception", e); }
    }
    return null; //if an exception is thrown
  }

  /**
   *  deserializeKey() given a byte array containing the serialized
   *  representation of a NumberKeyPair will return the original NumberKeyPair.
   *
   *  @param blob byte array to deserialize.
   *  @param offset starting index into b.
   *  @param len the number of bytes in b.
   *  @return NumberKeyPair from deserializing b or null on error.
   */
  private static NumberKeyPair deserializeKey(byte[] blob, int offset, int len)
  {
    ByteArrayInputStream b = new ByteArrayInputStream(blob, offset, len);
    ObjectInput in = null;
    try
    {
      in = new ObjectInputStream(b);
    }
    catch(java.io.StreamCorruptedException e)
    {Log.e(KeyShare.TAG, "exception", e); }
    catch(IOException e) {Log.e(KeyShare.TAG, "exception", e); }

    try
    {
      return (NumberKeyPair) in.readObject();
    }
    catch(ClassNotFoundException e) {Log.e(KeyShare.TAG, "exception", e); }
    catch(IOException e) {Log.e(KeyShare.TAG, "exception", e); }
    finally
    {
      try
      {
        b.close();
        in.close();
      }
      catch(IOException e) {Log.e(KeyShare.TAG, "exception", e); }
    }
    return null; //if an exception is thrown
  }

  private static void hexify(byte[] bytes)
  {
    char[] HEX_CHARS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A',
           'B', 'C', 'D', 'E', 'F' };
    char[] hexChars = new char[bytes.length * 2];
    int v;
    for(int j = 0; j < bytes.length; j++)
    {
      v = bytes[j] & 0xFF;
      hexChars[j * 2] = HEX_CHARS[v >>> 4];
      hexChars[j * 2 + 1] = HEX_CHARS[v & 0x0F];
    }
    Log.d(TAG, new String(hexChars));
  }
}
