package com.db;

import com.db.Updateable;
import com.db.Names;
import com.db.MessageDatabaseHelper;

import com.key.Key;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;
import android.telephony.SmsMessage;
import android.telephony.PhoneNumberUtils;

/**
 *  MessageInserter provides a writeonly database for storing incoming sms's.
 *
 *  This class still inserts sms's even when their bodies could not be
 *  decrypted. This feature is present for debugging purposes because the
 *  emulators appear to truncate and modify the binary sms's they receive.
 *
 *  This class is used through a singleton, Inserter. To get a static
 *  MessageInserter instance, call Inserter.getMessageInserter().
 */
public class MessageInserter
{
  /**
   *  Class Variables.
   *
   *  DECRYPT_FAILED constant String to use in place of a message body that
   *    could not be decrypted.
   */
  private static final String DECRYPT_FAILED = "decrypt_failed";

  /**
   *  Member Variables.
   *
   *  db SQLiteDatabase connection used for inserting into the database.
   *  newMessageStatement prepared statement for insertion.
   *  context Context under which the application operates and under which the
   *    database is opened.
   *  call Updateable object to call when a new message is inserter.
   */
  private SQLiteDatabase db;
  private SQLiteStatement newMessageStatement;
  private Context context;
  private Updateable call;

  /**
   *  MessageInserter() constructs a new MessageInserter purposed for inserting
   *  new sms's into the message database.
   *
   *  @param context Context under which the database should be opened.
   */
  MessageInserter(Context context)
  {
    this.context = context;
    db = (new MessageDatabaseHelper(context)).getWritableDatabase();
    //compile the prepared statement
    newMessageStatement = db.compileStatement(
      "INSERT into "+Names.TABLE_NAME+" VALUES (NULL, ?, ?, ?, ?);");
  }

  /**
   *  close() closes the connection to the database.
   */
  public void close()
  {
    db.close();
  }

  /**
   *  registerNotification() given an Updateable object will register that
   *  object to receive an update whenever a new message in inserted via this
   *  MessageInserter.
   *
   *  Only one Updateable can be registered at a time.
   *  Call unregisterNotification() to disable.
   *
   *  @param m Updateable to call when a new message is inserted.
   */
  public void registerNotification(Updateable m)
  {
    this.call = m;
  }

  /**
   *  unregisterNotification() will signal this MessageInserter stop calling
   *  the registered Updateable.
   *
   *  If no Updateable was ever registered via registerNotification() nothing
   *  exceptional happens. This method should be called to allow for garbage
   *  collection when the Updateable is no longer needed, but this
   *  MessageInserter still is.
   */
  public void unregisterNotification()
  {
    this.call = null;
  }

  /**
   *  insertMessage() given an SmsMessage object will extract relevant data and
   *  insert a new message into the database.
   *
   *  Use this method for messages(SmsMessage objects) that have been received.
   *  Use the other overloaded method for messages sent. If this method is used
   *  for an sms sent by the user, its entry in the database will be incorrect
   *  in that it will indicate the user received it instead.
   *
   *  Attempt to decrypt the message body. If decryption is unsuccessful, the
   *  current implementation still inserts the message with
   *  MessageInserter.DECRYPT_FAILED as its message body.
   *
   *  @param m SmSMessage representing the message received.
   */
  public void insertMessage(SmsMessage m)
  {
    //decrypt the sms body
    byte[] decryptedBody =
      (Key.getStorer(this.context)).decrypt(m.getUserData());
    //check if decryption failed
    if(decryptedBody == null)
    {
      Log.d(Names.TAG, "Could not decrypt ciphertext: aborting push");
      decryptedBody = MessageInserter.DECRYPT_FAILED.getBytes();
      //return;
    }
    //decode decrypted bytes into a string
    String body = new String(decryptedBody);

    Log.d(Names.TAG, "addr:"+m.getOriginatingAddress()+";time:"+
      m.getTimestampMillis()+";msg:"+body);
    //bind the prepared statement
    newMessageStatement.bindString(1, m.getOriginatingAddress());
    newMessageStatement.bindLong(2, (long)0); //false: this wasn't sent by user
    newMessageStatement.bindLong(3, m.getTimestampMillis());
    newMessageStatement.bindString(4, body);
    //push data to database
    newMessageStatement.execute();
    //call callback
    notifyChanged();
  }

  /**
   *  insertMessage() inserts a message that was sent by the user to someone
   *  else.
   *
   *  It is assumed that the message was sent now.
   *  It is assumed that the sender was the user. For messages received by
   *  user, use the overloaded method which accepts an SmsMessage as an
   *  argument.
   *
   *  @param recipientNumber String representing the phone number of the
   *    message's recipient.
   *  @param messageBody String representing the unencrypted message body.
   */
  public void insertMessage(String recipientNumber, String messageBody)
  {
    newMessageStatement.bindString(1, PhoneNumberUtils.stripSeparators(
      recipientNumber));
    newMessageStatement.bindLong(2, (long)1); //true: The user did send this
    newMessageStatement.bindLong(3, System.currentTimeMillis()); //sent now
    newMessageStatement.bindString(4, messageBody);
    //push data to database
    newMessageStatement.execute();
    //call callback
    notifyChanged();
  }

  /**
   *  Callback for when the database is inserted into.
   *
   *  Updateable call's update() method is called. If no callee was set via 
   *  registerNotification(), nothing happens.
   */
  private void notifyChanged()
  {
    if(call != null)
    {
      this.call.update();
    }
  }
}
