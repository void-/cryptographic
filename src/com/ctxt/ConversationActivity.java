package com.ctxt;

import com.db.MessageReader;
import com.db.MessageInserter;
import com.db.Message;
import com.key.Key;
import com.key.Fetcher;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.EditText;

import android.util.Log;
import android.telephony.SmsManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager; //For storing self public key

public class ConversationActivity extends Activity
{
  /**
   *  Class Variables.
   *
   *  NUMBER used as a key for intent passing.
   *  TAG used for debugging
   */
  public static final String NUMBER = "ConversationActivity.number";
  public static final String TAG = "CONVERSATION";

  /**
   *  Member Variables.
   *
   *  m
   *  recipient String for number of the other person the user is talking to.
   *  reader
   *  writer
   */
  private SmsManager m;
  private String recipient;
  private MessageReader reader;
  private MessageInserter writer;
  private TextView messages;

  /**
   *  onCreate() called when first created.
   */
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.conversation);

    m = SmsManager.getDefault();
    //open a connection to the database
    reader = new MessageReader(getApplicationContext());

    recipient = getIntent().getExtras().getString(ConversationActivity.NUMBER);

    TextView no = (TextView) findViewById(R.id.recipientName);
    messages = (TextView) findViewById(R.id.messages);
    no.setText(recipient);
    populateConversation();
    reader.close();
    writer = new MessageInserter(getApplicationContext());
  }

  @Override
  public void onDestroy()
  {
    super.onDestroy();
    writer.close();
  }

  /**
   *  populateConversation() writes the entire conversation to a text view.
   */
  private void populateConversation()
  {
    for(Message m: reader.getConversationIterator(recipient))
    {
      messages.append(m.message);
    }
  }

  /**
   *  onSend() press a button, get a phone number, get the message, encrypt and
   *  send it to the recipient.
   */
  public void onSend(View view)
  {
    String msg = (((EditText) findViewById(R.id.msg)).getText()).toString();

    m.sendDataMessage(recipient, null, (short) 16101,
      Fetcher.encrypt(msg.getBytes(),
        ((Key.getFetcher(getApplicationContext())).fetchKey(recipient))
        .getKey()),
      null,
      null);
    writer.insertMessage(recipient, msg);
    messages.append(msg);
  }
}
