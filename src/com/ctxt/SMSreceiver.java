package com.ctxt;

import com.key.Key;

import com.db.MessageInserter;

import android.util.Log;
import android.os.Bundle;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsMessage;

import java.nio.charset.Charset;

/**
 *  SMSreceiver subclass of BroadcastReceiver for receiving SMS.
 *
 *  Do special handling for receiving SMS ciphertext and push it to the SMS
 *  folder as plaintext. Access the private key for decryption.
 *
 *  Unfortunately, port filtering is non-functional in the manifest, so this
 *  must be done by the reciever upon getting a broadcast.
 */
public class SMSreceiver extends BroadcastReceiver
{
  /**
   *  Class Variables.
   *
   *  ACTION_SMS_RECEIVED constant string to filter intents actions on.
   *  TAG constant string to tag debugging messages with.
   */
  private static final String ACTION_SMS_RECEIVED =
    "android.provider.Telephony.SMS_RECEIVED";
  private static String TAG = "smsSERVICE";
  /**
   *  Member Variables.
   *
   *  inserter MessageInserter for inserting new messages into the database.
   */
  private MessageInserter inserter;

  /**
   *  onReceive() receives an SMS broadcast and decrypts all sms's using the
   *  user's private key.
   *
   *  Check:
   *    The intent action is for receiving an sms.
   *    The intent data is not empty.
   *  If both conditions hold, attempt to decrypt the sms and push it to a
   *  database. If the sms cannot be decrypted, it was probably not encrypted
   *  under the user's public key.
   *
   *  @param context context in which the intent was received.
   *  @param intent data associated with the broadcast being processed.
   */
  @Override
  public void onReceive(Context context, Intent intent)
  {
    //open a connection to database to insert new messages
    inserter = new MessageInserter(context);
    //abort if the intent is for not receiving an sms: some strange intent
    Log.d(TAG, "onReceive() was called");
    if(!SMSreceiver.ACTION_SMS_RECEIVED.equals(intent.getAction()))
    {
      Log.d(TAG, "Received a non-sms intent.");
      return;
    }
    Bundle extras = intent.getExtras();

    if(extras == null)
    {
      Log.d(TAG, "null intent extras: aborted");
      return;
    }

    SmsMessage m = null;
    //byte[] decryptedBody = null;
    //iterate through all pdus, constructs sms, decrypt contents, push plaintxt
    for(Object pdu: ((Object[]) extras.get("pdus")))
    {
      m = SmsMessage.createFromPdu((byte[]) pdu);
      if(m == null) { continue; }
      inserter.insertMessage(m);
      ////just log the sender & message body for now; store it later
      //Log.d(TAG, "addr:"+m.getOriginatingAddress());
      //decryptedBody = (Key.getStorer(context)).decrypt(m.getUserData());
      //Log.d(TAG,("body:"+new String((byte[]) ((decryptedBody == null) ?
      //  "".getBytes() : decryptedBody))));
    }
    inserter.close();
  }
}
