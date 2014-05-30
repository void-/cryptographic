package com.ctxt;

import com.key.Key;

import android.util.Log;
import android.os.Bundle;
import android.app.Service;
import android.os.IBinder;
import android.content.*;
import android.telephony.SmsMessage;

/**
 *  CtxtService service for receiving crypto SMS in the background.
 *
 *  Registers an SMS receiver defined as a nested class.
 *
 */
public class CtxtService extends Service
{
  /**
   *  Member Variables.
   *  thisSMSreceiver SMSreceiver registered for onReceive().
   *  SMSintentFilter intentFilter to filter to handle received SMS.
   */
  private SMSreceiver thisSMSreceiver;
  private IntentFilter SMSintentFilter;

  /**
   *  onCreate() registers a BroadcastReceiver for receiving SMS.
   *
   *  Specifically, onCreate() registers a new instance of the nested
   *  SMSreceiver class filtered to handle SMS_RECEIVED intents.
   */
  @Override
  public void onCreate()
  {
    super.onCreate();
    thisSMSreceiver = new SMSreceiver();
    SMSintentFilter = new IntentFilter();
    SMSintentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
    registerReceiver(thisSMSreceiver, SMSintentFilter);
  }

  /**
   *  onBind() is called when another process wants to communicate with the
   *  CtxtService.
   *
   *  There does not appear to be any reason why one would want to bind with
   *  this service, so null is always returned.
   *
   *  @return null: binding to CtxtService is prohibited.
   */
  @Override
  public IBinder onBind(Intent intent)
  {
    return null;
  }

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
  private class SMSreceiver extends BroadcastReceiver
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
        (Key.getFetcher()).decrypt(
          (SmsMessage.createFromPdu(pdu)).getUserData());
      }
    }
  }
}
