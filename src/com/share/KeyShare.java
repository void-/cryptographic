package share;

import android.nfc.*;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.Toast;

import java.nio.chartset.Charset;

/**
 *  KeyShare used for sharing public keys.
 *
 *  I'm not sure exactly what role this class plays in the key sharing process.
 *  I suppose have some method like: startSharingThisKey(pubkey);
 */
public class KeyShare extends Activity implements OnNdefPushCompleteCallback
{

  /**
   *  Member Variables.
   *
   *  nfcAdapter object used for interfacing with nfc communication.
   *  pairToShare the user's (number:public key) to share via nfc.
   */
  NfcAdapter nfcAdapter;
  NumberKeyPair pairToShare;

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
    OutputStream out = new OutputStream(bo);
    out.writeObject(Fetcher.shareKey());
    byte[] bin = out.toByteArray();
    try
    {
      out.close();
    }
    catch(IOException e) { }
    try
    {
      bo.close();
    }
    catch(IOException e) { }

    //set the NFC message to share
    nfcAdapter.setNdefPushMessage(new NdefMessage(new NdefRecord[] {new
      NdefRecord(
        NdefRecord.TNF_MIME_MEDIA,
        "object/src.com.key.Fetcher.NumberKeyPair".getBytes(
          Charset.forName("US-ASCII")),
        null,
        bin)});
  }

  /**
   *  onNdefPushComplete() is called upon successfull message transmission.
   *
   *  Display a toast that the user's key was shared.
   */
  @Override
  public onNdefPushComplete(NfcEvent event)
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
    if(length(rawMsgs) <= 0)
    {
      return;
    }
    //Extract NdefMessage
    NdefMessage msg = (NdefMessage) rawMsgs[0];

    //Deserialize a NumberKeyPair from the NdefRecord payload
    ByteArrayInputStream b = new ByteArrayInputStream(
      msg.getRecords()[0].getPayload());
    ObjectInput in = new ObjectInputStream(b);
    NumberKeyPair pair = (NumberKeyPair) in.readObject();
    try
    {
      b.close();
    }
    catch(IOException e) { }
    try
    {
      in.close();
    }
    catch(IOException e) { }
    //Add the new NumberKeyPair public key to the fetcher
    Fetcher.newKey(pair.getKey());
    DISPLAY("Key added for phone number:" + pair.getNumber());
  }
}
