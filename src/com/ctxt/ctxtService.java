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
  /**
   *  Member Variables.
   *  thisSMSreceiver SMSreceiver registered for onReceive().
   *  SMSintentFilter intentFilter to filter to handle received SMS.
   */
  private SMSreceiver thisSMSreceiver;
  private intentFilter SMSintentFilter;

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
   *  SMSreceiver subclass of BroadcastReceiver for receiving SMS.
   *
   *  Do special handling for receiving SMS ciphertext and push it to the SMS
   *  folder as plaintext. Access the private key for decryption.
   *
   *  If a plaintext SMS is received: What do?
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
