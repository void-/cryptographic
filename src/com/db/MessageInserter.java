package com.db;

import com.db.Names;
import com.db.MessageDatabaseHelper;

import com.key.Key;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;
import android.telephony.SmsMessage;

/**
 *  MessageInserter provides a writeonly database for storing incoming sms's.
 *
 */
public class MessageInserter
{
  /**
   *  Member Variables.
   *
   *  db
   *  newMessageStatement prepared statement for insertion.
   *  context
   */
  private SQLiteDatabase db;
  private SQLiteStatement newMessageStatement;
  private Context context;

  /**
   *  MessageInserter() constructs a new message inserter purposed for
   *  inserting new sms's into the message database.
   *
   *  @param context Context under which the Database should be opened.
   */
  public MessageInserter(Context context)
  {
    this.context = context;
    db = (new MessageDatabaseHelper(context)).getWritableDatabase();
    newMessageStatement = db.compileStatement(
      "INSERT into "+Names.TABLE_NAME+" VALUES (NULL, ?, ?, ?, ?);");
  }

  /**
   *  insertMessage() given an SmsMessage will extract relevant data and insert
   *  a new message into the database.
   *
   *  Use this method for messages(SmsMessage objects) that have been received.
   *  Use the overloaded method for messages sent.
   *
   *  @param m the message received.
   */
  public void insertMessage(SmsMessage m)
  {
    //decrypt the sms body
    byte[] decryptedBody =
      (Key.getStorer(this.context)).decrypt(m.getUserData());
    if(decryptedBody == null)
    {
      Log.d(Names.TAG, "Could not decrypt ciphertext: aborting push");
      //return;
    }
    //decode decrypted bytes into a string
    String body = new String((byte[]) ((decryptedBody != null) ? decryptedBody
      : "".getBytes()));

    Log.d(Names.TAG, "addr:"+m.getOriginatingAddress()+";time:"+
      m.getTimestampMillis()+";msg:"+body);
    //bind the prepared statement
    newMessageStatement.bindLong(1, Long.parseLong(m.getOriginatingAddress()));
    newMessageStatement.bindLong(2, (long)0); //false: The user didnt send this
    newMessageStatement.bindLong(3, m.getTimestampMillis());
    newMessageStatement.bindString(4, body);
    //push data to  decryptedBody
    newMessageStatement.execute();
  }

  /**
   *  insertMessage() inserts a message that was sent by the user to someone
   *  else.
   *
   *  It is assumed that the message was sent now.
   *  It is assumed that the sender was the user.
   *
   *  @param recipientNumber String representing the phone number of the
   *    message's recipient.
   *  @param messageBody String representing the contents of the message sent.
   */
  public void insertMessage(String recipientNumber, String messageBody)
  {
    newMessageStatement.bindLong(1, Long.parseLong(recipientNumber));
    newMessageStatement.bindLong(2, (long)1); //true: The user did send this
    newMessageStatement.bindLong(3, System.currentTimeMillis()); //sent now
    newMessageStatement.bindString(4, messageBody);
  }
}
