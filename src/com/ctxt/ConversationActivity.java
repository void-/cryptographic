package com.ctxt;

import com.ctxt.R;

import com.db.MessageReader;
import com.db.Inserter;
import com.db.MessageInserter;
import com.db.Message;
import com.db.Updateable;
import com.key.Key;
import com.key.Fetcher;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Button;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.widget.SimpleCursorAdapter;

import android.util.Log;
import android.telephony.SmsManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager; //For storing self public key

public class ConversationActivity extends Activity implements
    View.OnClickListener, Updateable
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
  private SimpleCursorAdapter adapter;

  //private BroadcastReceiver thisSMSreceiver = new BroadcastReceiver()
  //{
  //  @Override
  //  public void onReceive(Context context, Intent intent)
  //  {
  //    Log.d(TAG, "nested onReceive() called.");

  //    ConversationActivity.this.updateConversation(number);
  //  }
  //};

  /**
   *  onCreate() called when first created.
   */
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.conversation);

    reader = new MessageReader(getApplicationContext());
    m = SmsManager.getDefault();
    //open a connection to the database

    recipient = getIntent().getExtras().getString(ConversationActivity.NUMBER);

    TextView no = (TextView) findViewById(R.id.recipientName);
    no.setText(recipient);
    Button send = (Button) findViewById(R.id.send);
    send.setOnClickListener(this);
    //messages = (TextView) findViewById(R.id.messages);
    //populateConversation();
    ListView listView = (ListView) findViewById(R.id.messages);
    this.adapter = reader.getAdapter(this, recipient);
    listView.setAdapter(adapter);
    writer = Inserter.getMessageInserter(getApplicationContext());
    writer.registerNotification(this);

    //IntentFilter SMSintentFilter = new IntentFilter();
    //SMSintentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
    //registerReceiver(thisSMSreceiver, SMSintentFilter);
  }

  @Override
  public void onDestroy()
  {
    super.onDestroy();
    //writer.close();
    writer.unregisterNotification();
    reader.close();
    //unregisterReceiver(thisSMSreceiver);
  }

  /**
   *  ConversationSMSreceiver() extends SMSreceiver to provide additional
   *  functionality for updating the activity it is running in.
   */
  //private class ConversationSMSreceiver extends SMSreceiver
  //{
  //  /**
  //   *  only notify the activity that the conversation should be updated.
  //   */
  //  @Override
  //  public void onReceive(Context context, Intent intent)
  //  {
  //    //everything should already be in the database, update the activity
  //    ConversationActivity.this.updateConversation();
  //  }
  //}

  public void update()
  {
    Log.d(TAG, "updateConversation() called.");
    //do a check to make sure that the number is the same
    reader.updateAdapter(adapter, recipient);
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
   *  onClick() press a button, get a phone number, get the message, encrypt and
   *  send it to the recipient. Clear the message box too.
   */
  public void onClick(View view)
  {
    TextView t = (EditText) findViewById(R.id.msg);
    String msg = ((t).getText()).toString();

    m.sendDataMessage(recipient, null, (short) 16101,
      Fetcher.encrypt(msg.getBytes(),
        ((Key.getFetcher(getApplicationContext())).fetchKey(recipient))
        .getKey()),
      null,
      null);
    writer.insertMessage(recipient, msg);
    //Clear the message box
    t.setText("");
  }
}
