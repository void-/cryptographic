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
 *  KeyShare Activity for sharing public keys via NFC.
 *
 *  When this Activity is launched, it will set a static payload to share via
 *  NFC(the user's phone number and public key). It should be the case that
 *  when two individuals wish to share public keys, the exchange will occur at
 *  the same time.
 *
 *  If NFC is unavailable on the user's device, the KeyShare activity does
 *  nothing.
 */
public class KeyShare extends Activity implements
    NfcAdapter.OnNdefPushCompleteCallback
{
  /**
   *  Class Variables.
   *
   *  TAG constant string representing the tag to use when logging events that
   *    originate from calls to KeyPair methods.
   */
  private static final String TAG = "KEYSHARE";

  /**
   *  Member Variables.
   *
   *  nfcAdapter NfcAdapter object used for interfacing with nfc communication.
   *  pairToShare NumberKeyPair containing the user's (phone number:public key)
   *    to share via nfc.
   *  textLog TextView for informing the user which keys have been received or
   *    were failed to be received.
   */
  private NfcAdapter nfcAdapter;
  protected NumberKeyPair pairToShare;
  protected TextView textLog;

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

    textLog = (TextView) findViewById(R.id.textView);

    nfcAdapter = NfcAdapter.getDefaultAdapter(this);
    //NFC is unavailable
    if(nfcAdapter == null)
    {
      return;
    }
    this.pairToShare = (Key.getFetcher(getApplicationContext())).shareKey();
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

    //register a callback for successful message transmission
    nfcAdapter.setOnNdefPushCompleteCallback(this, this);
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
    if(nfcAdapter != null &&
        NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction()))
    {
      processIntent(getIntent());
    }
  }

  /**
   *  onNdefPushComplete() is called upon successful NFC message transmission.
   *
   *  This method currently does nothing because any toast made will be made on
   *  the binder thread and be invisible to the ui.
   *
   *  Display a toast that the user's public key was shared.
   *  @param event NfcEvent that has just occurred.
   */
  @Override
  public void onNdefPushComplete(NfcEvent event)
  {
    //(Toast.makeText(getApplicationContext(), "key shared", Toast.LENGTH_SHORT)).show();
  }

  /**
   *  onNewIntent() is called when this activity receives a new intent, when it
   *  is, set it to be the current intent.
   *  By calling setIntent(), onResume() will be called after.
   *
   *  @param intent Intent that should already filtered to for NDEF_DISCOVERED.
   */
  @Override
  public void onNewIntent(Intent intent)
  {
    setIntent(intent);
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
    Parcelable[] rawMsgs =
      intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
    if(rawMsgs.length <= 0)
    {
      return;
    }
    //Extract NdefMessage: process only 1 message; ignore all others
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
      (Key.getFetcher(getApplicationContext())).newKey(pair.getNumber(),
        pair.getKey());
      textLog.append("Received key for number:" + pair.getNumber());
      //TODO: launch a dialog to make sure the phone number is correct
    }
    catch(KeyAlreadyExistsException e)
    {
      textLog.append("Could not add public key for number:" + pair.getNumber()
        + "; you already have a public key for this number.");
    }
    //Write key added to text view; have user verify the phone number is right
  }
}
