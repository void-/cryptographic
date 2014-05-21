package com.ctxt;
// What do I even include?

/**
 *  ServiceCommunicator service for receiving crypto SMS in the background.
 *
 *  Registers an SMS receiver defined as a nested class.
 *
 */
public class ServiceCommunicator extends Service
{
  private SMSreceiver thisSMSreceiver;
  private intentFilter SMSintentFilter;

  /**
   *  onCreate() registers an SMSreceiver.
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
   *  SMSreceiver subclass of BroadcastReceiver for receiving encrypted SMS.
   */
  private class SMSreceiver extends BroadcastReceiver
  {
    /**
     *  onReceive() receives SMS and decrypts them using the private key.
     */
    @Override
    public void onReceive(Context context, Intent intent)
    {
      Bundle extras = intent.getExtras();

      if(extras == null)
      {
        return;
      }
    }
  }
}
