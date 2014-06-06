package com.ctxt;

import com.share.KeyShare;
import com.key.Key;
import com.key.Storer;
import com.key.NumberKeyPair;
import com.key.KeyAlreadyExistsException;

import android.app.Activity;
import android.os.Bundle;
import android.content.*;
import android.view.View;
import android.widget.EditText;
import android.util.Log;
import android.telephony.SmsManager;

import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.Cipher;
import java.nio.charset.Charset;

public class MainActivity extends Activity
{
  SmsManager m;
  private static String NUMBER = "5554";
  private static String TAG = "MAIN_ACTIVITY:";
  private SMSreceiver thisSMSreceiver;
  private IntentFilter SMSintentFilter;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    m = SmsManager.getDefault();

    //test ciphers
    try
    {
      PublicKey k = ((Key.getFetcher(getApplicationContext())).shareKey()).
        getKey();
      Cipher c = Cipher.getInstance(Storer.CIPHER);
      c.init(Cipher.ENCRYPT_MODE, k);
      //test that decryption is encryption inverse
      Log.d(TAG, "Hello, World ?= " + new
        String((Key.getStorer(getApplicationContext())).decrypt(
        c.doFinal("Hello, World!".getBytes()))));
    }
    catch(NoSuchAlgorithmException e) {Log.e(TAG, "exception", e); }
    catch(InvalidKeyException e) {Log.e(TAG, "exception", e); }
    catch(javax.crypto.NoSuchPaddingException e)
    {Log.e(TAG, "exception", e); }
    catch(javax.crypto.IllegalBlockSizeException e)
    {Log.e(TAG, "exception", e); }
    catch(javax.crypto.BadPaddingException e)
    {Log.e(TAG, "exception", e); }
  }

  /**
   *  onShareKey() provides a dedicated button to switch to the KeyShare
   *  activity.
   */
  public void onShareKey(View view)
  {
    startActivity(new Intent(this, KeyShare.class));
  }

  /**
   *  onSend() press a button, get a phone number, get the message, encrypt and
   *  send it to the recipient.
   */
  public void onSend(View view)
  {
    String msg = (((EditText) findViewById(R.id.msg)).getText()).toString();
    String no = (((EditText) findViewById(R.id.number)).getText()).toString();
    PublicKey k = ((Key.getFetcher(getApplicationContext())).fetchKey(no)).
      getKey();
    try
    {
      Cipher c = Cipher.getInstance(Storer.CIPHER);
      c.init(Cipher.ENCRYPT_MODE, k);
      byte[] emsg = c.doFinal(msg.getBytes());
      m.sendDataMessage(no, null, (short) 16101, emsg, null, null);
    }
    catch(NoSuchAlgorithmException e) {Log.e(TAG, "exception", e); }
    catch(InvalidKeyException e) {Log.e(TAG, "exception", e); }
    catch(javax.crypto.NoSuchPaddingException e)
    {Log.e(TAG, "exception", e); }
    catch(javax.crypto.IllegalBlockSizeException e)
    {Log.e(TAG, "exception", e); }
    catch(javax.crypto.BadPaddingException e)
    {Log.e(TAG, "exception", e); }
  }
}
