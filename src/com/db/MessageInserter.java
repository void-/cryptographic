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
  MessageInserter(Context context)
  {
    this.context = context;
    db = (new MessageDatabaseHelper(context)).getWritableDatabase();
    newMessageStatement = db.compileStatement(
      "INSERT into ? VALUES (?, ?, ?);");
    newMessageStatement.bindString(1, Names.TABLE_NAME);
  }

  /**
   *  insertMessage() given an SmsMessage will extract relevant data and insert
   *  a new message into the database.
   */
  public void insertMessage(SmsMessage m)
  {
    //decrypt the sms body
    byte[] decryptedBody =
      (Key.getStorer(this.context)).decrypt(m.getUserData());
    if(decryptedBody == null)
    {
      Log.d(Names.TAG, "Could not decrypt ciphertext: aborting push");
      return;
    }
    //decode decrypted bytes into a string
    String body = new String(decryptedBody);

    Log.d(Names.TAG, "addr:"+m.getOriginatingAddress()+";time:"+
      m.getTimestampMillis()+";msg:"+body);
    //bind the prepared statement
    newMessageStatement.bindString(2, m.getOriginatingAddress());
    newMessageStatement.bindLong(3, m.getTimestampMillis());
    newMessageStatement.bindString(4, body);
    //push data to  decryptedBody
    newMessageStatement.execute();
  }
}
