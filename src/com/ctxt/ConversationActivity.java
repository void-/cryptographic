package com.ctxt;

import com.ctxt.R;

import com.db.MessageReader;
import com.db.Inserter;
import com.db.MessageInserter;
import com.db.Message;
import com.db.Updateable;
import com.db.Base128;
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
import android.text.TextWatcher;
import android.text.Editable;

import android.content.Intent;
import android.content.IntentFilter;
import android.widget.SimpleCursorAdapter;

import android.util.Log;
import android.telephony.SmsManager;
import android.telephony.PhoneNumberUtils;

import android.app.PendingIntent;

public class ConversationActivity extends Activity implements
    View.OnClickListener, Updateable
{
  /**
   *  Class Variables.
   *
   *  NUMBER used as a key for intent passing.
   *  TAG used for debugging
   *  PORT
   *  SENT
   *  RECEIVED
   */
  public static final String NUMBER = "ConversationActivity.number";
  public static final String TAG = "CONVERSATION";
  public static final short PORT = (short)8901;
  static final String SENT = "ConversationActivity.SMS_SENT";
  static final String RECEIVED = "ConversationActivity.SMS_RECEIVED";

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
  private SimpleCursorAdapter adapter;
  private EditText messageBox;
  private TextView charsLeft;

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

    this.messageBox = (EditText) findViewById(R.id.msg);
    charsLeft = (TextView) findViewById(R.id.charCount);
    messageBox.addTextChangedListener(new TextWatcher()
    {
      @Override
      public void afterTextChanged(Editable e)
      {
        charsLeft.setText(String.valueOf(messageBox.length()));
      }
      @Override
      public void beforeTextChanged(CharSequence s, int st, int c, int a) { }
      @Override
      public void onTextChanged(CharSequence s, int st, int b, int c) { }
    });
    TextView no = (TextView) findViewById(R.id.recipientName);
    no.setText(recipient);
    Button send = (Button) findViewById(R.id.send);
    send.setOnClickListener(this);
    ListView listView = (ListView) findViewById(R.id.messages);
    this.adapter = reader.getAdapter(this, recipient);
    listView.setAdapter(adapter);
    writer = Inserter.getMessageInserter(getApplicationContext());
    writer.registerNotification(this, recipient);

    //register received for sent&received broadcasts
    IntentFilter SMSintentFilter = new IntentFilter();
    SMSintentFilter.addAction(SENT);
    SMSintentFilter.addAction(RECEIVED);
  }

  @Override
  public void onDestroy()
  {
    super.onDestroy();
    //writer.close();
    writer.unregisterNotification();
    reader.close();
  }

  public void update()
  {
    Log.d(TAG, "updateConversation() called.");
    //do a check to make sure that the number is the same
    reader.updateAdapter(adapter, recipient);
  }

  /**
   *  onClick() press a button, get a phone number, get the message, encrypt and
   *  send it to the recipient. Clear the message box too.
   */
  public void onClick(View view)
  {
    String msg = (messageBox.getText()).toString();

    byte[] cipherText = Fetcher.encrypt(msg.getBytes(),
      ((Key.getFetcher(getApplicationContext())).fetchKey(recipient))
        .getKey());

    String encodedMsg = Base128.encode(cipherText);
    Log.d(TAG, encodedMsg);
    Log.d(TAG, "string len:" + encodedMsg.length());

    //let the unicode string get converted back into binary blob
    this.m.sendTextMessage(PhoneNumberUtils.stripSeparators(recipient), null,
      encodedMsg,
      PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(SENT),
        Intent.FILL_IN_ACTION),
      PendingIntent.getBroadcast(getApplicationContext(), 0,
        new Intent(RECEIVED), Intent.FILL_IN_ACTION)
    );
    writer.insertMessage(recipient, msg);
    //Clear the message box
    messageBox.setText("");
  }
}
