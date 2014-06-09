package com.ctxt;

import com.db.MessageReader;
import com.db.Message;
import com.key.Key;
import com.key.Fetcher;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.EditText;

import android.telephony.SmsManager;

public class ConversationActivity extends Activity
{
  /**
   *
   */
  public static final String NUMBER = "ConversationActivity.number";

  /**
   *  Member Variables.
   *
   *  recipient String for number of the other person the user is talking to.
   */
  private SmsManager m;
  private String recipient;
  private MessageReader reader;

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
    TextView t = (TextView) findViewById(R.id.recipientName);
    t.setText(recipient);
    populateConversation();
  }

  /**
   *  populateConversation() writes the entire conversation to a text view.
   */
  private void populateConversation()
  {
    TextView t = (TextView) findViewById(R.id.messages);
    for(Message m: reader.getConversationIterator(recipient))
    {
      t.append(m.message);
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
  }
}
