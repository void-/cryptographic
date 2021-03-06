package ctxt.db;

import ctxt.ctxt.BuildConfig;
import ctxt.db.Updateable;
import ctxt.db.Names;
import ctxt.db.MessageDatabaseHelper;

import ctxt.key.Key;
import ctxt.key.Storer;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;
import android.telephony.SmsMessage;
import android.telephony.PhoneNumberUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
   *  callNumber phone number indicating for what conversation to callback.
   */
  private SQLiteDatabase db;
  private SQLiteStatement newMessageStatement;
  private Context context;
  private Updateable call;
  private String callNumber;
  private static final int EXPECTED_LENGTH = (Storer.KEYBITS/7);

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
   *  registerNotification() given an Updateable object and phone number will
   *  register that object to receive an update whenever a new message for that
   *  number in inserted via this MessageInserter.
   *
   *  Only one Updateable can be registered at a time.
   *  Call unregisterNotification() to disable.
   *
   *  number must be in an international format with country code and area
   *  code. Delimiters present in the number do not matter
   *
   *  @param m Updateable to call when a new message is inserted.
   *  @param number phone number indicating for which conversations updated to
   *    callback m.
   */
  public void registerNotification(Updateable m, String number)
  {
    this.call = m;
    this.callNumber = PhoneNumberUtils.stripSeparators(number);
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
    this.callNumber = null;
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
    Log.d(Names.TAG, m.getMessageBody());
    Log.d(Names.TAG, "len:"+(m.getMessageBody()).length());

    //only decode if its a proper length
    byte[] encryptedBody = (m.getMessageBody().length() == (EXPECTED_LENGTH))
      ? (Base128.decode(m.getMessageBody())) : null;
    //byte[] encryptedBody = (Base128.decode(m.getMessageBody()));
    hexify(encryptedBody);

    byte[] decryptedBody =
      (Key.getStorer(this.context)).decrypt(encryptedBody);
    //check if decryption failed
    if(decryptedBody == null)
    {
      Log.d(Names.TAG, "Could not decrypt ciphertext: aborting push");
      //if not debugging, dont add the message
      if(!BuildConfig.DEBUG)
      {
        return;
      }
      decryptedBody = MessageInserter.DECRYPT_FAILED.getBytes();
    }
    //decode decrypted bytes into a string
    String body = new String(decryptedBody);
    String recipientNumber = (m.getOriginatingAddress()).replaceAll("\\+", "");

    Log.d(Names.TAG, "addr:"+ recipientNumber +";time:"+
      m.getTimestampMillis()+";msg:"+body);
    //bind the prepared statement
    newMessageStatement.bindString(1, recipientNumber);
    newMessageStatement.bindLong(2, (long)0); //false: this wasn't sent by user
    newMessageStatement.bindLong(3, m.getTimestampMillis());
    newMessageStatement.bindString(4, body);
    //push data to database
    newMessageStatement.execute();
    //call callback
    notifyChanged(recipientNumber);
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
    String number = PhoneNumberUtils.stripSeparators(recipientNumber);
    newMessageStatement.bindString(1, number);
    newMessageStatement.bindLong(2, (long)1); //true: The user did send this
    newMessageStatement.bindLong(3, System.currentTimeMillis()); //sent now
    newMessageStatement.bindString(4, messageBody);
    //push data to database
    newMessageStatement.execute();
    //call callback
    notifyChanged(number);
  }

  /**
   *  Callback for when the database is inserted into.
   *
   *  Updateable call's update() method is called. If no callee was set via 
   *  registerNotification(), nothing happens. Only callback if the number
   *  received matches the one registered.
   *
   *  Given phone number must be in an international format with a country code
   *  and area code and without any delimiters. "1555215554" is a valid input.
   *
   *  @param number String representing the conversation updated without
   *    delimiters.
   */
  private void notifyChanged(String number)
  {
    if(call != null && number.equals(this.callNumber))
    {
      this.call.update();
    }
  }

  private static void hexify(byte[] bytes)
  {
    if(bytes == null)
    {
      return;
    }
    char[] HEX_CHARS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A',
           'B', 'C', 'D', 'E', 'F' };
    char[] hexChars = new char[bytes.length * 2];
    int v;
    for(int j = 0; j < bytes.length; j++)
    {
      v = bytes[j] & 0xFF;
      hexChars[j * 2] = HEX_CHARS[v >>> 4];
      hexChars[j * 2 + 1] = HEX_CHARS[v & 0x0F];
    }
    Log.d(Names.TAG, new String(hexChars));
  }

  /**
   *  finalize() implemented because cursor needs to be closed. No caller will
   *  close this MessageInserter because it is used with a singleton.
   */
  @Override
  protected void finalize()
  {
    this.close();
  }
}
