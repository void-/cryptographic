package com.ctxt;

import com.ctxt.SMSreceiver;
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
}
