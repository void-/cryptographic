package ctxt.share;

import ctxt.share.Connection;
import ctxt.share.ShareConfirmationDialogFragment;
import ctxt.ctxt.R;

import ctxt.key.Key;
import ctxt.key.NumberKeyPair;
import ctxt.key.KeyAlreadyExistsException;

import android.util.Log;
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
   *  BT_UUID UUID to uniquely identify this application via Bluetooth.
   *  REQUEST_DISCOVERABLE
   *  REQUEST_ENABLE
   *  KEY_ADDED
   *  KEY_REJECTED
   *  MESSAGE_KEY_RECEIVED
   */
  private static final String TAG = "KEYSHARE";
  static final UUID BT_UUID =
    UUID.fromString("885a4392-07a2-a613-0895-20a84ebaf087");
  private static final int REQUEST_DISCOVERABLE = 0x1;
  private static final int REQUEST_ENABLE = 0x2;
  private static final int KEY_ADDED = 0x3;
  private static final int KEY_REJECTED = 0x4;
  static final int MESSAGE_KEY_RECEIVED = 0x10;

  /**
   *  Member Variables.
   *
   *  bluetoothAdapter BluetoothAdapter for interfacing with Bluetooth.
   *  pairInQuestion NumberKeyPair that must be confirmed by the user before
   *    adding to Fetcher.
   *  connection Connection for abstracting out Bluetooth connection details.
   *  serializedKey byte array containing the serialized representing of
   *    the user's NumberKeyPair.
   *  deviceNamesAdapter ArrayAdapter for displaying visible bluetooth devices.
   *  status TextView displaying the current status of the connection.
   */
  private BluetoothAdapter bluetoothAdapter;
  private NumberKeyPair pairInQuestion;
  private Connection connection;
  private byte[] serializedKey;
  private ArrayAdapter<String> deviceNamesAdapter;
  private TextView status;

  /**
   *  onCreate() sets up UI and serializes the user's public key.
   *
   *  If Bluetooth is unavailable on the device, this activity (probably) does
   *  nothing.
   *
   *  @param savedInstance Bundle containing an saved data from a previous
   *    instance. This is unused.
   */
  @Override
  public void onCreate(Bundle savedInstance)
  {
    super.onCreate(savedInstance);
    setContentView(R.layout.keyshare);

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

    //setup the list view for displaying devices
    deviceNamesAdapter = new ArrayAdapter<String>(this,
      android.R.layout.simple_list_item_1);
    ListView l = (ListView) findViewById(R.id.device_list);
    l.setAdapter(deviceNamesAdapter);
    l.setOnItemClickListener(deviceClickListener);

    //serialize the user's NumberKeyPair to share
    serializedKey =
      serializeKey((Key.getFetcher(getApplicationContext())).shareKey());

    connection = new Connection(bluetoothAdapter, serializedKey, handler);
    hexify(serializedKey);
  }

  /**
   *  onStart() asks the user if he or she would like to be discoverable.
   *
   *  Do this in onStart(), rather than in onResume(), to avoid an infinite
   *  dialog loop if the user declines or dismisses the dialog.
   */
  @Override
  public void onStart()
  {
    super.onStart();
    makeDiscoverable(null);
  }

  /**
   *  onResume() starts scanning for other Bluetooth devices.
   */
  @Override
  public void onResume()
  {
    super.onResume();
    onScan(null);
  }

  /**
   *  onPause() stops scanning for other Bluetooth devices.
   *
   *  Discovering new Bluetooth devices when the activity is not focused is not
   *  useful.
   */
  @Override
  public void onPause()
  {
    super.onPause();
    Log.d(TAG, "onPause: canceling discovery");
    bluetoothAdapter.cancelDiscovery();
  }

  /**
   *  onDestory() stop listening for devices by unregistering
   *  discoveredReceiver.
   */
  @Override
  public void onDestroy()
  {
    super.onDestroy();
    unregisterReceiver(discoveredReceiver);
  }

  /**
   *  handler Handler allows for communication from a Connection instance to
   *  the KeyShare activity.
   *
   *  handler also allows for communication from the binder to the ui thread.
   *  This lets toasts be displayed regarding key acception and rejection.
   */
  private final Handler handler = new Handler()
  {

    /**
     *  handleMessage() callback processes a message given to the handler.
     *
     *  m Message to process.
     */
    @Override
    public void handleMessage(Message m)
    {
      Log.d(TAG, "handleMessage() called, what:" + m.what);
      switch(m.what)
      {
        case KeyShare.MESSAGE_KEY_RECEIVED: //Bluetooth got a key
          Log.d(TAG, "MESSAGE_KEY_RECEIVED");
          receiveKey((byte[]) m.obj, m.arg1, m.arg2);
          break;
        case KeyShare.KEY_ADDED:
          Log.d(TAG, "handling key added");
          Toast.makeText(getApplicationContext(), "Key added!",
            Toast.LENGTH_LONG).show();
          break;
        case KeyShare.KEY_REJECTED:
          Log.d(TAG, "handling key rejected");
          Toast.makeText(getApplicationContext(), "key rejected",
            Toast.LENGTH_LONG).show();
          break;
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
     *  onReceive() callback for when new Bluetooth devices are discovered.
     *
     *  This should already be filtered by an Intent filter for discovering a
     *  new bluetooth device. No additional checks are made.
     *
     *  This method adds a string representation of the Bluetooth device
     *  discovered in the form:
     *    "device_name-device_mac"
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
     *  When this method is called, the user has selected a Bluetooth device to
     *  share public keys with. In this case, the user will be the client and
     *  the other device will be the server.
     *
     *  Stop looking for bluetooth devices and start connecting to the selected
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
      //separate the mac address from the name using '-' as a separator
      connection.connectTo(bluetoothAdapter.getRemoteDevice(
        nameAndMac.substring(nameAndMac.lastIndexOf('-')+1)));

      //stop scanning for devices
      bluetoothAdapter.cancelDiscovery();
    }
  };

  /**
   *  onCreateOptionsMenu() callback for creating the action bar and options
   *  menu.
   *
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
   *  @param item not used, can be null. This simply exists so that
   *    makeDiscoverable() works as a callback from a ui button.
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
   *  @param item not used, can be null. This simply exists so that onScan()
   *    works as a callback from a ui button.
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
   *
   *  Bluetooth discoverability must be enabled before calling startListening()
   *  or else nothing will happen.
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
   *  @param requestCode integer representing the reason for the activity.
   *  @param resultCode the return status of the launched activity.
   *  @param data Intent if the called activity returned any data. Unused.
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
   *  receiveKey() is called when a serialized NumberKeyPair is received via
   *  Bluetooth.
   *
   *  Ask the user if the key is ok, then proceed to add it.
   *  Create an image for authentication.
   *  image : Server key || Client Key
   *  The keyBuffer should be large enough to store both serializations.
   *
   *  Warning: There are no checks done on if keyBuffer is large enough to
   *  store both users' serialized keys. This may expose a denial of service
   *  attack.
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
    Log.d(TAG, "deserializing key...");
    pairInQuestion = deserializeKey(keyBuffer, 0, len);
    Log.d(TAG, "Received key for number:" + pairInQuestion.getNumber());

    //create a combined image of both keys
    if(isServer > 0) //user was the server
    {
      Log.d(TAG, "server memcpy");
      //move the client's key to the end of the buffer
      System.arraycopy(keyBuffer, 0, keyBuffer, len, len);
      //put the server's key in the beggining of the buffer
      System.arraycopy(serializedKey, 0, keyBuffer, 0, serializedKey.length);
    }
    else
    {
      Log.d(TAG, "client memcpy");
      //user was client: put key at the end of the buffer
      System.arraycopy(serializedKey, 0, keyBuffer, len, serializedKey.length);
    }

    //launch a dialog to confirm addition of this public key
    Log.d(KeyShare.TAG, "Launching fragment for key.");
    ShareConfirmationDialogFragment f = new ShareConfirmationDialogFragment();
    Bundle bundle = new Bundle();
    bundle.putString(ShareConfirmationDialogFragment.PHONE_NUMBER,
      pairInQuestion.getNumber());
    bundle.putByteArray(ShareConfirmationDialogFragment.IMAGE, keyBuffer);
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
      handler.obtainMessage(KEY_REJECTED).sendToTarget();
      return;
    }
    //try to add the key
    try
    {
      (Key.getFetcher(getApplicationContext())).newKey(
        pairInQuestion.getNumber(),
        pairInQuestion.getKey());
      handler.obtainMessage(KEY_ADDED).sendToTarget();
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

  /**
   *  hexify() logs the hexadecimal representation of a byte array.
   */
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
