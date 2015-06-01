package ctxt.db;

import ctxt.db.Names;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

/**
 *  MessageDatabaseHelper class helps create the sent and received sms
 *  database.
 *
 *  All sms's are stored unencrypted.
 *
 *  The database uses the schema:
 *  (message number, conversation, sender's number, send time, message body).
 */
class MessageDatabaseHelper extends SQLiteOpenHelper
{
  /**
   *  MessageDatabaseHelper() constructs a new MessageDatabaseHelper
   *  instance.
   *
   *  This constructor is not responsible for creating the database for the
   *  first time. This constructor simply calls the super constructor with the
   *  correct arguments.
   *
   *  A default cursor is requested under the database name,
   *  Names.DATABASE_NAME and version Names.VERSION.
   */
  MessageDatabaseHelper(Context context)
  {
    super(context, Names.DATABASE_NAME, null, Names.VERSION);
  }

  /**
   *  onCreate() creates the database for the first time. The database
   *  constructed is based on constants Strings found in the Names class.
   *
   *  @param db SQLiteDatabase for which to create the new database.
   */
  @Override
  public void onCreate(SQLiteDatabase db)
  {
    db.execSQL(
      "CREATE TABLE " + Names.TABLE_NAME + "(" +
      Names.MESSAGE_NO + " " + Names.MESSAGE_NO_TYPE + ", " +
      Names.CONVERSATION_ID + " " + Names.CONVERSATION_ID_TYPE + ", " +
      Names.SENDER_NAME + " " + Names.SENDER_TYPE + ", " +
      Names.RECEIPT_DATE + " " + Names.RECEIPT_TYPE + ", " +
      Names.MESSAGE + " " + Names.MESSAGE_TYPE+");");

    Log.d(Names.TAG, "Database created for the first time.");
  }

  /**
   *  Do nothing; no upgrading this database.
   */
  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }
}
