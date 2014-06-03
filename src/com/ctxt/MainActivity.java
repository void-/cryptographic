package com.ctxt;

import com.share.KeyShare;
import com.key.Key;
import com.key.NumberKeyPair;

import android.app.Activity;
import android.os.Bundle;
import android.content.*;
//import android.content.Intent;
import android.view.View;

import android.widget.EditText;

import android.util.Log;

import android.telephony.SmsManager;

import javax.crypto.Cipher;

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
    //thisSMSreceiver = new SMSreceiver();
    //SMSintentFilter = new IntentFilter();
    //SMSintentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
    //SMSintentFilter.addDataScheme("sms");
    //SMSintentFilter.addDataAuthority("*","16101");
    //registerReceiver(thisSMSreceiver, SMSintentFilter);
  }

  /**
   *  onShareKey() provides a dedicated button to switch to the KeyShare
   *  activity.
   */
  public void onShareKey(View view)
  {
    startActivity(new Intent(this, KeyShare.class));
  }

  public void onRegSmsTest(View view)
  {
    byte[] msg = "Hello, World! Automated test sms.".getBytes();
    String no = (((EditText) findViewById(R.id.number)).getText()).toString();
    Log.d(TAG, "1! Sending a regular sms!");
    m.sendDataMessage(no, null, (short)8901, msg, null, null);
  }

  public void onBinSmsTest(View view)
  {
    //encrypted: lololol, this is encrypted via RSA in a binary sms
    byte[] msg =
    {
      (byte)0x90, (byte)0x14, (byte)0x9b, (byte)0x73, (byte)0x0f, (byte)0x0e, (byte)0xb0, (byte)0xcb, (byte)0xc3, (byte)0x0a, (byte)0x4e,
      (byte)0x75, (byte)0x71, (byte)0x46, (byte)0x83, (byte)0x19, (byte)0x99, (byte)0x1a, (byte)0xeb, (byte)0xa1, (byte)0xa4, (byte)0x3a,
      (byte)0xe4, (byte)0xec, (byte)0x64, (byte)0xf7, (byte)0xb5, (byte)0x7f, (byte)0x2d, (byte)0x13, (byte)0xaa, (byte)0x2d, (byte)0x6e,
      (byte)0x19, (byte)0x1c, (byte)0x01, (byte)0x80, (byte)0x71, (byte)0x2f, (byte)0x76, (byte)0x36, (byte)0x11, (byte)0xe0, (byte)0x00,
      (byte)0x8c, (byte)0x59, (byte)0x04, (byte)0xce, (byte)0x5a, (byte)0x56, (byte)0xdc, (byte)0xc4, (byte)0x62, (byte)0x0e, (byte)0x60,
      (byte)0x0c, (byte)0x16, (byte)0x61, (byte)0xab, (byte)0xe8, (byte)0xe1, (byte)0xa0, (byte)0x45, (byte)0x33, (byte)0xcf, (byte)0x19,
      (byte)0x49, (byte)0x9c, (byte)0x01, (byte)0x23, (byte)0x5b, (byte)0x0b, (byte)0xeb, (byte)0xfe, (byte)0x5f, (byte)0x37, (byte)0x72,
      (byte)0xfc, (byte)0x72, (byte)0xe2, (byte)0xa9, (byte)0xa2, (byte)0x6b, (byte)0x43, (byte)0x91, (byte)0xb7, (byte)0x3b, (byte)0xcb,
      (byte)0x89, (byte)0xaa, (byte)0xd2, (byte)0x21, (byte)0x6a, (byte)0xfa, (byte)0x0a, (byte)0x15, (byte)0xc2, (byte)0x89, (byte)0xb8,
      (byte)0xc4, (byte)0xe2, (byte)0x94, (byte)0xaa, (byte)0xd2, (byte)0xff, (byte)0xd8, (byte)0x88, (byte)0xa3, (byte)0x35, (byte)0xca,
      (byte)0xa7, (byte)0xb0, (byte)0xfd, (byte)0x76, (byte)0xed, (byte)0xe4, (byte)0x14, (byte)0xb6, (byte)0x89, (byte)0x09, (byte)0x00,
      (byte)0x8c, (byte)0x8e, (byte)0x91, (byte)0xee, (byte)0xf2, (byte)0xa3, (byte)0x56
    };
    //Cipher c = null;
    //try
    //{
    //  c = Cipher.getInstance((Key.getStorer()).CIPHER);
    //  c.init(Cipher.ENCRYPT_MODE, (PublicKey) (Key.getFetcher().shareKey()).getKey())
    //}
    //catch(javax.crypto.NoSuchAlgorithmException e) {Log.e(Storer.TAG, "exception", e); }
    //catch(javax.crypto.NoSuchPaddingException e) {Log.e(Storer.TAG, "exception", e); }
    //catch(javax.crypto.IllegalBlockSizeException e) {Log.e(Storer.TAG, "exception", e); }
    //catch(javax.crypto.BadPaddingException e) {Log.e(Storer.TAG, "exception", e); }

    //byte[] msg = c.doFinal(new {(byte)'l', (byte)'o', (byte)'l', (byte)'.'});

    Log.d(TAG, "2! Sending a BINARY sms!!");
    String no = (((EditText) findViewById(R.id.number)).getText()).toString();
    m.sendDataMessage(no, null, (short)16101, msg, null, null);
  }

  public void onSend(View view)
  {
    //byte[] msg = {(byte)'l', (byte)'o', (byte)'l', (byte)'.'};
    byte[] msg = "9Hello, World! Automated test sms.".getBytes();
    String no = (((EditText) findViewById(R.id.number)).getText()).toString();
    Log.d(TAG, "3! sms sent to given number");
    m.sendDataMessage(no, null, (short) 16101, msg, null, null);
  }
}
