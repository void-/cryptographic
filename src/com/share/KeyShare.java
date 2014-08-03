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
import android.content.Intent;
import android.widget.TextView;
import android.widget.Toast;

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


import android.bluetooth.BluetoothAdapter;
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


  /**
   *  Member Variables.
   *
   *  nfcAdapter NfcAdapter object used for interfacing with nfc communication.
   *  pairToShare NumberKeyPair containing the user's (phone number:public key)
   *    to share via nfc.
   *  textLog TextView for informing the user which keys have been received or
   *    were failed to be received.
   *  pairInQuestion NumberKeyPair that must be confirmed by the user before
   *    adding to Fetcher.
   */
  //private NfcAdapter nfcAdapter;
  private BluetoothAdapter bluetoothAdapter;
  protected NumberKeyPair pairToShare;
  protected TextView textLog;
  private boolean numberConfirmed;
  private NumberKeyPair pairInQuestion;
  private Connection connection;
  private byte[] serializedKey;

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
    textLog = (TextView) findViewById(R.id.textView);

    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    //bluetooth is unavailable
    if(bluetoothAdapter == null)
    {
      Log.d(TAG, "no bluetooth.");
      textLog.append("bluetooth unsupported.");
      finish();
      return;
    }

    connection = new Connection(bluetoothAdapter);

    this.pairToShare = (Key.getFetcher(getApplicationContext())).shareKey();
    //serialize the NumberKeyPair to share
    ByteArrayOutputStream bo = new ByteArrayOutputStream();
    ObjectOutput out = null;
    try
    {
      out = new ObjectOutputStream(bo);
      out.writeObject(pairToShare);
      serializedKey = bo.toByteArray();
      out.close();
      bo.close();
    }
    catch(IOException e) {Log.e(KeyShare.TAG, "exception", e); }

    hexify(bin);
  }

  /**
   *  onResume() checks if a new NFC intent is available to process.
   *
   *  If a new intent is available and its for an NDEF discovery, call
   *  processIntent() on the intent.
   */
  @Override
  public void onResume()
  {
    super.onResume();
    makeDiscoverable();
  }

  @Override
  public void onPause()
  {
    super.onPause();
  }

  /**
   *  makeDiscoverable() launches an activity to ask the user to make the
   *  device visible via bluetooth.
   *
   *  When done on both devices that would like to exchange keys, this makes it
   *  able for either one to function as the server in the connection.
   *  The user is the one that decides which device will be the client and
   *  which the server.
   */
  private void makeDiscoverable()
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
   *  Ask the user to connect to a device and start listening for connections.
   */
  private void connectable()
  {
    //start listening for connections
    connection.startListening();
    //get the user to discover devices
  }

  /**
   *  onActivityResult() callback for when a launched activity returns.
   *
   */
  public void onActivityResult(int requestCode, int resultCode, Intent data)
  {
    Log.d(TAG, requestCode + ":activity returned:" + resultCode);
    switch(requestCode)
    {
      case KeyShare.REQUEST_DISCOVERABLE:
        if(resultCode == Activity.RESULT_OK) //device is now discoverable
        {
          Log.d(TAG, "device made discoverable");
          if(bluetoothAdapter.startDiscovery())
          {
            Log.d(TAG, "attempting to discover devices...");
            connectable();
          }
          else
          {
            Log.d(TAG, "failed to start discovering devices.");
          }
        }
        else
        {
          Log.d(TAG, "setting device discovery failed");
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
    textLog.append("Received key for number:" + pairInQuestion.getNumber());
    //launch a dialog to confirm addition of this public key
    ShareConfirmationDialogFragment f = new ShareConfirmationDialogFragment();
    Bundle bundle = new Bundle();
    bundle.putString(ShareConfirmationDialogFragment.PHONE_NUMBER,
      pairInQuestion.getNumber());
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
      textLog.append("Rejected public key for number:" +
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
      textLog.append("Could not add public key for number:" +
        pairInQuestion.getNumber() +
        "; you already have a public key for this number.");
    }
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
