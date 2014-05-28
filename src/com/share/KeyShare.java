package src.com.share;

import src.com.key.*;

import android.util.Log;
import android.nfc.*;
import android.app.Activity;
import android.os.Bundle;
import android.os.Parcelable;
import android.content.Intent;
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
   */
  NfcAdapter nfcAdapter;
  Fetcher.NumberKeyPair pairToShare;

  private static final String TAG = "KEYSHARE";

  /**
   *  onCreate()
   *  Register callbacks and related; set public key to share.
   */
  @Override
  public void onCreate(Bundle savedInstance)
  {
    nfcAdapter = NfcAdapter.getDefaultAdapter(this);
    //NFC is unavailable
    if(nfcAdapter == null)
    {
      throw new NullPointerException();
    }
    //serialize the NumberKeyPair to share
    ByteArrayOutputStream bo = new ByteArrayOutputStream();
    ObjectOutput out = null;
    byte[] bin = null;
    try
    {
      out = new ObjectOutputStream(bo);
      out.writeObject((new Fetcher()).shareKey()); //FIXME: use static
      bin = bo.toByteArray();
      out.close();
      bo.close();
    }
    catch(IOException e) {Log.e(KeyShare.TAG, "exception", e); }

    //set the NFC message to share
    nfcAdapter.setNdefPushMessage(new NdefMessage(new NdefRecord[] {new
      NdefRecord(
      NdefRecord.TNF_MIME_MEDIA,
      "object/src.com.key.Fetcher.NumberKeyPair".getBytes(
        Charset.forName("US-ASCII")),
      null,
      bin)}), this);
  }

  /**
   *  onNdefPushComplete() is called upon successfull message transmission.
   *
   *  Display a toast that the user's key was shared.
   */
  @Override
  public void onNdefPushComplete(NfcEvent event)
  {
    (Toast.makeText(getApplicationContext(), "key shared.", Toast.LENGTH_SHORT)
      ).show();
  }

  /**
   *  NOTE: Not sure what this does
   *
   *  Check: potential security vulnerability with automatically setting the
   *  intent regardless of what it is.
   *
   */
  @Override
  public void onNewIntent(Intent intent)
  {
    setIntent(intent);
  }

  /**
   *  processIntent() given an intent will determine what to do with it. In
   *  this case, it will always assume it is a new public key.
   *
   */
  void processIntent(Intent intent)
  {
    Parcelable[] rawMsgs =
      intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
    if(rawMsgs.length <= 0)
    {
      return;
    }
    //Extract NdefMessage
    NdefMessage msg = (NdefMessage) rawMsgs[0];

    //Deserialize a NumberKeyPair from the NdefRecord payload
    ByteArrayInputStream b = new ByteArrayInputStream(
      msg.getRecords()[0].getPayload());
    ObjectInput in = null;
    Fetcher.NumberKeyPair pair = null;
    try
    {
      in = new ObjectInputStream(b);
    }
    catch(java.io.StreamCorruptedException e)
    {Log.e(KeyShare.TAG, "exception", e); }
    catch(IOException e) {Log.e(KeyShare.TAG, "exception", e); }
    try
    {
      pair = (Fetcher.NumberKeyPair) in.readObject();
      b.close();
      in.close();
    }
    catch(ClassNotFoundException e) {Log.e(KeyShare.TAG, "exception", e); }
    catch(IOException e) {Log.e(KeyShare.TAG, "exception", e); }
    //Add the new NumberKeyPair public key to the fetcher
    try
    {
      (new Fetcher()).newKey(pair.getNumber(), pair.getKey()); //FIXME: static
    }
    catch(Fetcher.KeyAlreadyExistsException e)
    {Log.e(KeyShare.TAG, "exception", e); }
    //Write key added to text view; have user verify the phone number is right
    //DISPLAY("Key added for phone number:" + pair.getNumber());
  }
}
