package com.ctxt;

import com.share.KeyShare;
import com.key.Key;
import com.key.Storer;
import com.key.Fetcher;
import com.key.NumberKeyPair;
import com.key.KeyAlreadyExistsException;

import android.app.Activity;
import android.os.Bundle;
import android.content.*;
import android.view.View;
import android.widget.EditText;
import android.util.Log;
import android.telephony.SmsManager;

import java.security.interfaces.RSAPublicKey;
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
    //Key.getStorer(getApplicationContext());
    Key.getFetcher(getApplicationContext());
    Log.d(TAG,"exp:"+((RSAPublicKey)((Key.getFetcher(getApplicationContext())).shareKey().getKey())).getPublicExponent());
    //test that decryption is encryption inverse
    Log.d(TAG, "Hello, World ?= " + new
      String((Key.getStorer(getApplicationContext())).decrypt(
      Fetcher.encrypt("Hello, World!".getBytes(),
      (Key.getFetcher(getApplicationContext())).shareKey().getKey()))));
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

    m.sendDataMessage(no, null, (short) 16101, Fetcher.encrypt(msg.getBytes(),
      ((Key.getFetcher(getApplicationContext())).fetchKey(no)).getKey()), null,
      null);
  }
}
