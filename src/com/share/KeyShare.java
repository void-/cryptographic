package com.share;

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

import java.io.IOException;
import java.io.Serializable;
import java.io.ObjectOutput;
import java.io.ObjectInput;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;

/**
 *  KeyShare used for sharing public keys.
 *
 *  I'm not sure exactly what role this class plays in the key sharing process.
 *  I suppose have some method like: startSharingThisKey(pubkey);
 */
public class KeyShare extends Activity implements
    NfcAdapter.OnNdefPushCompleteCallback
{

  /**
   *  Member Variables.
   *
   *  nfcAdapter object used for interfacing with nfc communication.
   *  pairToShare the user's (number:public key) to share via nfc.
   *  textLog TextView for informing the user which keys have been recieved.
   */
  NfcAdapter nfcAdapter;
  NumberKeyPair pairToShare;
  TextView textLog;

  /**
   *  Class Variables.
   *
   *  TAG constant string representing the tag to use when logging events that
   *    originate from calls to KeyPair methods.
   */
  private static final String TAG = "KEYSHARE";

  /**
   *  onCreate()
   *  Register callbacks and related; set public key to share.
   *
   *  On setting the Ndef push message.
   *  The user's NumberKeyPair is serialized into a byte array.
   *  The Ndef message is a NdefRecord for a MIME type that is simply the
   *    serialization of the NumberKeyPair.
   */
  @Override
  public void onCreate(Bundle savedInstance)
  {
    super.onCreate(savedInstance);
    setContentView(R.layout.keyshare);

    textLog = (TextView) findViewById(R.id.textView);

    nfcAdapter = NfcAdapter.getDefaultAdapter(this);
    //NFC is unavailable
    if(nfcAdapter == null)
    {
      throw new NullPointerException();
    }
    this.pairToShare = (Key.getFetcher()).shareKey();
    //serialize the NumberKeyPair to share
    ByteArrayOutputStream bo = new ByteArrayOutputStream();
    ObjectOutput out = null;
    byte[] bin = null;
    try
    {
      out = new ObjectOutputStream(bo);
      out.writeObject(pairToShare);
      bin = bo.toByteArray();
      out.close();
      bo.close();
    }
    catch(IOException e) {Log.e(KeyShare.TAG, "exception", e); }

    //set the NFC message to share
    nfcAdapter.setNdefPushMessage(new NdefMessage(new NdefRecord[] {new
      NdefRecord(
      NdefRecord.TNF_MIME_MEDIA,
      "object/com.key.NumberKeyPair".getBytes(
        Charset.forName("US-ASCII")),
      null,
      bin)}), this);

    //register callback for successful message transmission
    nfcAdapter.setOnNdefPushCompleteCallback(this, this);
  }

  /**
   *  onResume().
   *
   *  Receive a new intent, if its for nfc, call processIntent() on it.
   */
  @Override
  public void onResume()
  {
    super.onResume();
    if(NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction()))
    {
      processIntent(getIntent());
    }
  }

  /**
   *  onNdefPushComplete() is called upon successfull message transmission.
   *
   *  -----currently nonfunctional because the toast will be made on the binder
   *  thread and be invisible
   *
   *  Display a toast that the user's public key was shared.
   */
  @Override
  public void onNdefPushComplete(NfcEvent event)
  {
    //(Toast.makeText(getApplicationContext(), "key shared", Toast.LENGTH_SHORT)).show();
  }

  /**
   *  onNewIntent() is called when this activity receives a new intent: do it.
   *  By calling setIntent(), onResume() will be called after.
   *
   *  @param intent given Intent should already filtered to be NDEF_DISCOVERED.
   */
  @Override
  public void onNewIntent(Intent intent)
  {
    setIntent(intent);
  }

  /**
   *  processIntent() given an intent will determine what to do with it.
   *  In this case, it will always assume it is a new public key.
   *
   *  @param Intent the intent to process: presumably contains an NdefMessage.
   */
  void processIntent(Intent intent)
  {
    Parcelable[] rawMsgs =
      intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
    if(rawMsgs.length <= 0)
    {
      return;
    }
    //Extract NdefMessage: process only 1 message
    NdefMessage msg = (NdefMessage) rawMsgs[0];

    //Deserialize a NumberKeyPair from the NdefRecord payload
    ByteArrayInputStream b = new ByteArrayInputStream(
      msg.getRecords()[0].getPayload());
    ObjectInput in = null;
    NumberKeyPair pair = null;
    try
    {
      in = new ObjectInputStream(b);
    }
    catch(java.io.StreamCorruptedException e)
    {Log.e(KeyShare.TAG, "exception", e); }
    catch(IOException e) {Log.e(KeyShare.TAG, "exception", e); }
    try
    {
      pair = (NumberKeyPair) in.readObject();
      b.close();
      in.close();
    }
    catch(ClassNotFoundException e) {Log.e(KeyShare.TAG, "exception", e); }
    catch(IOException e) {Log.e(KeyShare.TAG, "exception", e); }
    //Add the new NumberKeyPair public key to the fetcher
    try
    {
      (Key.getFetcher()).newKey(pair.getNumber(), pair.getKey());
    }
    catch(KeyAlreadyExistsException e)
    {Log.e(KeyShare.TAG, "exception", e); }
    //Write key added to text view; have user verify the phone number is right
    textLog.append("Received key for number:" + pair.getNumber());
  }
}
