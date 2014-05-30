package com.ctxt;

import com.key.Key;

import android.util.Log;
import android.os.Bundle;
import android.app.Service;
import android.os.IBinder;
import android.content.*;
import android.telephony.SmsMessage;

/**
 *  SMSreceiver subclass of BroadcastReceiver for receiving SMS.
 *
 *  Do special handling for receiving SMS ciphertext and push it to the SMS
 *  folder as plaintext. Access the private key for decryption.
 *
 *  If a plaintext SMS is received: What do?
 *    A plaintext SMS should never be received due to port and intent
 *    filtering.
 */
public class SMSreceiver extends BroadcastReceiver
{
  private static final String ACTION_SMS_RECEIVED =
    "android.provider.Telephony.SMS_RECEIVED";
  private static String TAG = "smsSERVICE";
  /**
   *  onReceive() receives SMS and decrypts them using the private key.
   */
  @Override
  public void onReceive(Context context, Intent intent)
  {
    //abort if the intent is for receiving an sms: ?Non-binary => plaintext
    if(SMSreceiver.ACTION_SMS_RECEIVED.equals(intent.getAction()))
    {
      Log.d(TAG, "Received text message: aborted");
      return;
    }
    Bundle extras = intent.getExtras();

    if(extras == null)
    {
      Log.d(TAG, "null intent extras: aborted");
      return;
    }

    //iterate through all pdus, constructs sms, decrypt contents, push plain
    for(Object pdu: ((Object[]) extras.get("pdus")))
    {
      //just log the message body for now; display it later
      Log.d(TAG,new String((Key.getFetcher()).decrypt(
        (SmsMessage.createFromPdu(pdu)).getUserData())));
    }
  }
}
